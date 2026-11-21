package notaria;

import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import notaria.modelo.*;
import org.bson.Document;

/**
 * NÚCLEO del sistema (vista de desarrollo). Aquí están las reglas del
 * negocio y la conexión con MongoDB. Las ventanas nunca hablan con la
 * base de datos: siempre le piden las cosas al Núcleo.
 *
 * Funcionamiento: al arrancar, leerBaseDatos() carga todo a memoria
 * (listas de Java). Cada vez que algo cambia, guardarBaseDatos(...)
 * guarda ese objeto en MongoDB.
 */
public class Nucleo {

    private ListaClientes listaClientes = new ListaClientes();
    private List<Usuario> usuarios = new ArrayList<>();
    private List<Tramite> tramites = new ArrayList<>();

    private MongoDatabase baseDatos;   // null = modo de prueba sin base de datos

    public Nucleo(String uriMongo, String nombreBaseDatos) {
        if (uriMongo == null || uriMongo.isBlank()) {
            System.out.println("AVISO: no hay MONGODB_URI. Se trabaja solo en memoria (los datos se pierden al cerrar).");
        } else {
            baseDatos = MongoClients.create(uriMongo).getDatabase(nombreBaseDatos);
            System.out.println("Conectado a MongoDB, base de datos: " + nombreBaseDatos);
        }
        leerBaseDatos();
        crearUsuariosIniciales();
    }

    // =====================================================================
    //  INICIAR SESIÓN
    // =====================================================================

    public Usuario iniciarSesion(String nombreUsuario, String contrasena) {
        Usuario usuario = buscarUsuario(nombreUsuario);
        if (usuario == null || !usuario.verificarContrasena(contrasena)) {
            throw new IllegalArgumentException("Usuario o contraseña incorrectos");
        }
        if (!usuario.isActivo()) {
            throw new IllegalArgumentException("El usuario está inactivo. Hable con el administrador");
        }
        return usuario;
    }

    // =====================================================================
    //  RADICAR TRÁMITE
    // =====================================================================

    /** Le da número de radicado y fecha al trámite y lo guarda. */
    public Tramite radicarTramite(Tramite tramite) {
        if (tramite.getClientes().isEmpty()) {
            throw new IllegalArgumentException("El trámite debe tener al menos un cliente");
        }
        // Si un cliente es nuevo, se agrega a la lista de clientes y se guarda
        for (Cliente c : tramite.getClientes()) {
            if (listaClientes.buscar(c.getNumeroDocumento()) == null) {
                listaClientes.agregar(c);
                guardarBaseDatos(c);
            }
        }
        tramite.setNumeroRadicado(siguienteNumeroRadicado());
        tramite.setFechaRadicacion(new Date());
        tramite.setEstado(EstadoTramite.RADICADO);
        tramites.add(tramite);
        guardarBaseDatos(tramite);
        return tramite;
    }

    /** Formato AÑO-consecutivo, por ejemplo 2026-000001. El consecutivo vuelve a 1 cada año. */
    private String siguienteNumeroRadicado() {
        int anio = Calendar.getInstance().get(Calendar.YEAR);
        int mayor = 0;
        for (Tramite t : tramites) {
            if (t.getNumeroRadicado().startsWith(anio + "-")) {
                int consecutivo = Integer.parseInt(t.getNumeroRadicado().substring(5));
                mayor = Math.max(mayor, consecutivo);
            }
        }
        return String.format("%d-%06d", anio, mayor + 1);
    }

    // =====================================================================
    //  CAMBIAR ESTADO (reglas del flujo del trámite)
    // =====================================================================

    /**
     * Cambia el estado de un trámite revisando quién lo pide y si se puede.
     * @param usuario       quien hace el cambio
     * @param nuevo         estado al que se quiere pasar
     * @param observaciones obligatorias al devolver o anular
     * @param digitador     obligatorio al asignar (Radicado -> En elaboración)
     */
    public void cambiarEstado(Usuario usuario, Tramite tramite, EstadoTramite nuevo,
                              String observaciones, Usuario digitador) {
        EstadoTramite actual = tramite.getEstado();

        switch (nuevo) {
            case EN_ELABORACION:
                if (actual == EstadoTramite.RADICADO) {
                    // Asignar el trámite a un digitador
                    exigirRol(usuario, Rol.RECEPCIONISTA, Rol.NOTARIO);
                    if (digitador == null || !digitador.getRol().es(Rol.DIGITADOR)) {
                        throw new IllegalArgumentException("Debe escoger un digitador");
                    }
                    tramite.setDigitador(digitador);
                } else if (actual == EstadoTramite.DEVUELTO) {
                    // El digitador corrige lo que devolvió el notario
                    exigirDigitadorAsignado(usuario, tramite);
                } else {
                    throw errorDeFlujo(actual, nuevo);
                }
                tramite.avanzar();
                break;

            case EN_REVISION:
                exigirEstado(tramite, nuevo, EstadoTramite.EN_ELABORACION);
                exigirDigitadorAsignado(usuario, tramite);
                tramite.avanzar();
                break;

            case APROBADO:
                exigirEstado(tramite, nuevo, EstadoTramite.EN_REVISION);
                exigirRol(usuario, Rol.NOTARIO);          // solo el notario aprueba
                tramite.setObservaciones(null);
                tramite.avanzar();
                break;

            case DEVUELTO:
                exigirEstado(tramite, nuevo, EstadoTramite.EN_REVISION);
                exigirRol(usuario, Rol.NOTARIO);
                exigirTexto(observaciones, "Escriba las observaciones de la devolución");
                tramite.setObservaciones(observaciones);
                tramite.setEstado(EstadoTramite.DEVUELTO);
                break;

            case FIRMADO:
                exigirEstado(tramite, nuevo, EstadoTramite.APROBADO);
                exigirRol(usuario, Rol.NOTARIO);          // solo el notario firma
                if (tramite.getLiquidacion() == null || !tramite.getLiquidacion().estaPagada()) {
                    throw new IllegalStateException("No se puede firmar: el trámite no tiene pago registrado");
                }
                tramite.avanzar();
                break;

            case ENTREGADO:
                exigirEstado(tramite, nuevo, EstadoTramite.FIRMADO);
                exigirRol(usuario, Rol.RECEPCIONISTA);
                tramite.avanzar();
                break;

            case ANULADO:
                exigirRol(usuario, Rol.NOTARIO);
                if (actual == EstadoTramite.FIRMADO || actual == EstadoTramite.ENTREGADO
                        || actual == EstadoTramite.ANULADO) {
                    throw new IllegalStateException("Un trámite " + actual.getTexto() + " no se puede anular");
                }
                exigirTexto(observaciones, "Escriba el motivo de la anulación");
                tramite.setObservaciones(observaciones);
                tramite.setEstado(EstadoTramite.ANULADO);
                break;

            default:
                throw errorDeFlujo(actual, nuevo);
        }
        guardarBaseDatos(tramite);
    }

    private void exigirRol(Usuario usuario, String... roles) {
        if (!usuario.getRol().es(roles)) {
            throw new IllegalStateException("Su rol (" + usuario.getRol().getNombre() + ") no puede hacer esta acción");
        }
    }

    private void exigirDigitadorAsignado(Usuario usuario, Tramite tramite) {
        exigirRol(usuario, Rol.DIGITADOR);
        if (tramite.getDigitador() == null
                || !tramite.getDigitador().getNombreUsuario().equals(usuario.getNombreUsuario())) {
            throw new IllegalStateException("Este trámite no está asignado a usted");
        }
    }

    private void exigirEstado(Tramite tramite, EstadoTramite nuevo, EstadoTramite requerido) {
        if (tramite.getEstado() != requerido) {
            throw errorDeFlujo(tramite.getEstado(), nuevo);
        }
    }

    private void exigirTexto(String texto, String mensaje) {
        if (texto == null || texto.isBlank()) {
            throw new IllegalArgumentException(mensaje);
        }
    }

    private IllegalStateException errorDeFlujo(EstadoTramite actual, EstadoTramite nuevo) {
        return new IllegalStateException("No se puede pasar de " + actual.getTexto() + " a " + nuevo.getTexto());
    }

    // =====================================================================
    //  CAJA: LIQUIDAR Y REGISTRAR PAGO
    // =====================================================================

    public Liquidacion liquidar(Usuario usuario, Tramite tramite) {
        exigirRol(usuario, Rol.CAJERO);
        if (tramite.getEstado() == EstadoTramite.ANULADO) {
            throw new IllegalStateException("No se puede liquidar un trámite anulado");
        }
        if (tramite.getLiquidacion() != null) {
            throw new IllegalStateException("El trámite ya está liquidado");
        }
        // POLIMORFISMO: cada tipo de trámite calcula sus propios derechos
        Liquidacion liquidacion = new Liquidacion(tramite.calcularDerechos());
        tramite.setLiquidacion(liquidacion);
        guardarBaseDatos(tramite);
        return liquidacion;
    }

    public Pago registrarPago(Usuario usuario, Tramite tramite, MedioPago medioPago) {
        exigirRol(usuario, Rol.CAJERO);
        Liquidacion liquidacion = tramite.getLiquidacion();
        if (liquidacion == null) {
            throw new IllegalStateException("Primero debe liquidar el trámite");
        }
        if (liquidacion.estaPagada()) {
            throw new IllegalStateException("Este trámite ya fue pagado");
        }
        if (medioPago == null) {
            throw new IllegalArgumentException("Escoja el medio de pago");
        }
        Pago pago = new Pago(liquidacion.getTotal(), medioPago, new Date());
        liquidacion.setPago(pago);
        guardarBaseDatos(tramite);
        return pago;
    }

    // =====================================================================
    //  BÚSQUEDAS (usadas por las ventanas)
    // =====================================================================

    public Tramite buscarTramite(String numeroRadicado) {
        for (Tramite t : tramites) {
            if (t.getNumeroRadicado().equals(numeroRadicado)) {
                return t;
            }
        }
        throw new IllegalArgumentException("No existe el trámite " + numeroRadicado);
    }

    public Usuario buscarUsuario(String nombreUsuario) {
        for (Usuario u : usuarios) {
            if (u.getNombreUsuario().equalsIgnoreCase(nombreUsuario == null ? "" : nombreUsuario.trim())) {
                return u;
            }
        }
        return null;
    }

    public void agregarUsuario(Usuario usuario) {
        if (buscarUsuario(usuario.getNombreUsuario()) != null) {
            throw new IllegalArgumentException("Ya existe el usuario " + usuario.getNombreUsuario());
        }
        usuarios.add(usuario);
        guardarBaseDatos(usuario);
    }

    public List<Tramite> getTramites() { return tramites; }
    public List<Usuario> getUsuarios() { return usuarios; }
    public ListaClientes getListaClientes() { return listaClientes; }

    // =====================================================================
    //  BASE DE DATOS (MongoDB)
    //  Colecciones: usuarios, clientes, tramites, configuracion
    // =====================================================================

    private static final ReplaceOptions CREAR_SI_NO_EXISTE = new ReplaceOptions().upsert(true);

    /** Guarda (o reemplaza) un documento usando su _id. */
    private void guardar(String coleccion, Document documento) {
        if (baseDatos == null) {
            return;   // modo de prueba sin base de datos
        }
        MongoCollection<Document> c = baseDatos.getCollection(coleccion);
        c.replaceOne(Filters.eq("_id", documento.get("_id")), documento, CREAR_SI_NO_EXISTE);
    }

    public void guardarBaseDatos(Usuario u) {
        guardar("usuarios", new Document("_id", u.getNombreUsuario())
                .append("correo", u.getCorreo())
                .append("contrasena", u.getContrasenaCifrada())
                .append("activo", u.isActivo())
                .append("rol", u.getRol().getNombre()));
    }

    public void guardarBaseDatos(Cliente c) {
        guardar("clientes", new Document("_id", c.getNumeroDocumento())
                .append("nombres", c.getNombres())
                .append("telefono", c.getTelefono())
                .append("tipoDocumento", c.getTipoDocumento()));
    }

    public void guardarBaseDatos(Configuracion configuracion) {
        Document tarifas = new Document();
        for (Map.Entry<String, Double> e : configuracion.getTarifasVigentes().entrySet()) {
            tarifas.append(e.getKey(), e.getValue());
        }
        guardar("configuracion", new Document("_id", "tarifas").append("valores", tarifas));
    }

    public void guardarBaseDatos(Tramite t) {
        Document d = new Document("_id", t.getNumeroRadicado())
                .append("tipo", t.getClass().getSimpleName())
                .append("fechaRadicacion", t.getFechaRadicacion())
                .append("estado", t.getEstado().name())
                .append("observaciones", t.getObservaciones())
                .append("digitador", t.getDigitador() == null ? null : t.getDigitador().getNombreUsuario());

        // Dato propio de cada tipo de trámite
        if (t instanceof Autenticacion) {
            d.append("numeroFolios", ((Autenticacion) t).getNumeroFolios());
        } else if (t instanceof DeclaracionExtrajuicio) {
            d.append("contenido", ((DeclaracionExtrajuicio) t).getContenido());
        } else if (t instanceof PoderEspecial) {
            d.append("objetoPoder", ((PoderEspecial) t).getObjetoPoder());
        } else if (t instanceof EscrituraCompraventa) {
            d.append("valorInmueble", ((EscrituraCompraventa) t).getValorInmueble());
        }

        // Clientes: solo se guarda su número de documento (los datos están en "clientes")
        List<String> clientes = new ArrayList<>();
        for (Cliente c : t.getClientes()) {
            clientes.add(c.getNumeroDocumento());
        }
        d.append("clientes", clientes);

        List<Document> documentos = new ArrayList<>();
        for (DocumentoAdjunto doc : t.getDocumentos()) {
            documentos.add(new Document("nombreArchivo", doc.getNombreArchivo())
                    .append("fechaCarga", doc.getFechaCarga()));
        }
        d.append("documentos", documentos);

        Liquidacion l = t.getLiquidacion();
        if (l != null) {
            Document liq = new Document("valorDerechos", l.getValorDerechos())
                    .append("iva", l.getIva())
                    .append("total", l.getTotal());
            if (l.getPago() != null) {
                liq.append("pago", new Document("valor", l.getPago().getValor())
                        .append("medioPago", l.getPago().getMedioPago().name())
                        .append("fecha", l.getPago().getFecha()));
            }
            d.append("liquidacion", liq);
        }
        guardar("tramites", d);
    }

    /** Carga usuarios, clientes, tarifas y trámites desde MongoDB a memoria. */
    public void leerBaseDatos() {
        if (baseDatos == null) {
            return;
        }
        for (Document d : baseDatos.getCollection("usuarios").find()) {
            Usuario u = new Usuario(d.getString("_id"), d.getString("correo"), new Rol(d.getString("rol")));
            u.setContrasenaCifrada(d.getString("contrasena"));
            u.setActivo(d.getBoolean("activo"));
            usuarios.add(u);
        }

        for (Document d : baseDatos.getCollection("clientes").find()) {
            listaClientes.agregar(new Cliente(d.getString("_id"), d.getString("nombres"),
                    d.getString("telefono"), d.getString("tipoDocumento")));
        }

        for (Document d : baseDatos.getCollection("configuracion").find(Filters.eq("_id", "tarifas"))) {
            Document valores = d.get("valores", Document.class);
            for (String nombre : valores.keySet()) {
                Configuracion.getInstancia().setTarifa(nombre, valores.getDouble(nombre));
            }
        }

        for (Document d : baseDatos.getCollection("tramites").find()) {
            tramites.add(leerTramite(d));
        }
        System.out.println("Datos cargados: " + usuarios.size() + " usuarios, "
                + listaClientes.getClientes().size() + " clientes, " + tramites.size() + " trámites");
    }

    private Tramite leerTramite(Document d) {
        Tramite t;
        switch (d.getString("tipo")) {
            case "Autenticacion":
                t = new Autenticacion(d.getInteger("numeroFolios"));
                break;
            case "DeclaracionExtrajuicio":
                t = new DeclaracionExtrajuicio(d.getString("contenido"));
                break;
            case "PoderEspecial":
                t = new PoderEspecial(d.getString("objetoPoder"));
                break;
            case "EscrituraCompraventa":
                t = new EscrituraCompraventa(d.getDouble("valorInmueble"));
                break;
            default:
                throw new IllegalStateException("Tipo de trámite desconocido: " + d.getString("tipo"));
        }
        t.setNumeroRadicado(d.getString("_id"));
        t.setFechaRadicacion(d.getDate("fechaRadicacion"));
        t.setEstado(EstadoTramite.valueOf(d.getString("estado")));
        t.setObservaciones(d.getString("observaciones"));
        if (d.getString("digitador") != null) {
            t.setDigitador(buscarUsuario(d.getString("digitador")));
        }
        for (String documento : d.getList("clientes", String.class)) {
            t.agregarCliente(listaClientes.buscar(documento));
        }
        for (Document doc : d.getList("documentos", Document.class)) {
            t.agregarDocumento(new DocumentoAdjunto(doc.getString("nombreArchivo"), doc.getDate("fechaCarga")));
        }
        Document liq = d.get("liquidacion", Document.class);
        if (liq != null) {
            Liquidacion l = new Liquidacion(liq.getDouble("valorDerechos"), liq.getDouble("iva"), liq.getDouble("total"));
            Document pago = liq.get("pago", Document.class);
            if (pago != null) {
                l.setPago(new Pago(pago.getDouble("valor"), MedioPago.valueOf(pago.getString("medioPago")),
                        pago.getDate("fecha")));
            }
            t.setLiquidacion(l);
        }
        return t;
    }

    /** La primera vez (base de datos vacía) crea un usuario por cada rol para poder entrar. */
    private void crearUsuariosIniciales() {
        if (!usuarios.isEmpty()) {
            return;
        }
        String[][] iniciales = {
            {"admin", Rol.ADMINISTRADOR},
            {"recepcion", Rol.RECEPCIONISTA},
            {"digitador", Rol.DIGITADOR},
            {"notario", Rol.NOTARIO},
            {"cajero", Rol.CAJERO},
        };
        for (String[] datos : iniciales) {
            Usuario u = new Usuario(datos[0], null, new Rol(datos[1]));
            u.cambiarContrasena("clave123");
            agregarUsuario(u);
        }
        guardarBaseDatos(Configuracion.getInstancia());
        System.out.println("Se crearon los usuarios iniciales (contraseña: clave123)");
    }
}
