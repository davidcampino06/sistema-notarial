package notaria.backend;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import notaria.basedatos.BaseDatos;
import notaria.backend.modelo.*;

/**
 * NÚCLEO del sistema (vista de desarrollo). Aquí están las reglas del
 * negocio. Las ventanas (frontend) nunca hablan con la base de datos:
 * siempre le piden las cosas al Núcleo, y el Núcleo usa la clase
 * BaseDatos (carpeta basedatos) para guardar y leer en MongoDB.
 *
 * Funcionamiento: al arrancar, leerBaseDatos() carga todo a memoria
 * (listas de Java). Cada vez que algo cambia, guardarBaseDatos(...)
 * guarda ese objeto en MongoDB.
 */
public class Nucleo {

    private ListaClientes listaClientes = new ListaClientes();
    private List<Usuario> usuarios = new ArrayList<>();
    private List<Tramite> tramites = new ArrayList<>();

    private BaseDatos baseDatos;   // null = modo de prueba sin base de datos

    public Nucleo(String uriMongo, String nombreBaseDatos) {
        if (uriMongo == null || uriMongo.isBlank()) {
            System.out.println("AVISO: no hay MONGODB_URI. Se trabaja solo en memoria (los datos se pierden al cerrar).");
        } else {
            baseDatos = new BaseDatos(uriMongo, nombreBaseDatos);
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
                notificarCliente(tramite);
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
                notificarCliente(tramite);
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
    //  CORREO: NOTIFICACIONES AL CLIENTE
    // =====================================================================

    /**
     * Le avisa por correo a cada cliente del trámite que su trámite cambió de
     * estado (se usa al aprobar y al firmar). Si el correo falla, el cambio de
     * estado NO se pierde: solo se escribe el error en la consola.
     */
    public void notificarCliente(Tramite tramite) {
        if (!Configuracion.getInstancia().hayServidorCorreo()) {
            return;   // sin correo configurado no se notifica
        }
        String consulta = System.getenv("RENDER_EXTERNAL_URL");   // Render la crea sola
        for (Cliente c : tramite.getClientes()) {
            if (!c.tieneCorreo()) {
                continue;
            }
            String texto = "Hola " + c.getNombres() + ",\n\n"
                    + "Su trámite " + tramite.getTipo() + " con número de radicado " + tramite.getNumeroRadicado()
                    + " ahora está en estado: " + tramite.getEstado().getTexto() + ".\n"
                    + (tramite.getEstado() == EstadoTramite.FIRMADO
                            ? "Ya puede acercarse a la notaría a reclamarlo.\n" : "")
                    + (consulta == null ? "" : "\nConsulte el estado en: " + consulta + "/consulta\n")
                    + "\nNotaría - Sistema de Gestión Notarial";
            try {
                enviarCorreo(c.getCorreo(), "Su trámite " + tramite.getNumeroRadicado() + " está "
                        + tramite.getEstado().getTexto(), texto);
            } catch (IllegalStateException e) {
                System.out.println("No se pudo notificar a " + c.getCorreo() + ": " + e.getMessage());
            }
        }
    }

    /**
     * Envía un correo usando la API web de Brevo (por HTTPS).
     * Lanza IllegalStateException si el servidor de correo responde con error.
     */
    public void enviarCorreo(String destino, String asunto, String texto) {
        Configuracion conf = Configuracion.getInstancia();
        if (!conf.hayServidorCorreo()) {
            throw new IllegalStateException("El servidor de correo no está configurado");
        }
        // Mensaje en formato JSON, como lo pide la API de Brevo
        String json = "{\"sender\":{\"name\":\"Notaría\",\"email\":" + textoJson(conf.getRemitenteCorreo()) + "},"
                + "\"to\":[{\"email\":" + textoJson(destino) + "}],"
                + "\"subject\":" + textoJson(asunto) + ","
                + "\"textContent\":" + textoJson(texto) + "}";
        try {
            HttpRequest peticion = HttpRequest.newBuilder(URI.create(conf.getServidorCorreo()))
                    .timeout(Duration.ofSeconds(10))
                    .header("api-key", conf.getClaveCorreo())
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();
            HttpResponse<String> respuesta = HttpClient.newHttpClient()
                    .send(peticion, HttpResponse.BodyHandlers.ofString());
            if (respuesta.statusCode() >= 300) {
                throw new IllegalStateException("El servidor de correo respondió " + respuesta.statusCode()
                        + ": " + respuesta.body());
            }
            System.out.println("Correo enviado a " + destino);
        } catch (java.io.IOException | InterruptedException e) {
            throw new IllegalStateException("No se pudo conectar con el servidor de correo ("
                    + e.getClass().getSimpleName() + ")");
        }
    }

    /** Convierte un texto a formato JSON (con comillas y caracteres especiales escapados). */
    private static String textoJson(String texto) {
        StringBuilder sb = new StringBuilder("\"");
        for (char ch : texto.toCharArray()) {
            switch (ch) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': break;
                case '\t': sb.append("\\t"); break;
                default: sb.append(ch);
            }
        }
        return sb.append("\"").toString();
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
    //  BASE DE DATOS: el trabajo con MongoDB lo hace la clase BaseDatos
    //  (carpeta basedatos). Si no hay MONGODB_URI, todo queda en memoria.
    // =====================================================================

    public void guardarBaseDatos(Usuario u) {
        if (baseDatos != null) baseDatos.guardar(u);
    }

    public void guardarBaseDatos(Cliente c) {
        if (baseDatos != null) baseDatos.guardar(c);
    }

    public void guardarBaseDatos(Configuracion configuracion) {
        if (baseDatos != null) baseDatos.guardar(configuracion);
    }

    public void guardarBaseDatos(Tramite t) {
        if (baseDatos != null) baseDatos.guardar(t);
    }

    /** Carga usuarios, clientes, tarifas y trámites desde MongoDB a memoria. */
    public void leerBaseDatos() {
        if (baseDatos == null) {
            return;
        }
        usuarios.addAll(baseDatos.leerUsuarios());
        for (Cliente c : baseDatos.leerClientes()) {
            listaClientes.agregar(c);
        }
        baseDatos.leerTarifas(Configuracion.getInstancia());
        tramites.addAll(baseDatos.leerTramites(listaClientes, usuarios));
        System.out.println("Datos cargados: " + usuarios.size() + " usuarios, "
                + listaClientes.getClientes().size() + " clientes, " + tramites.size() + " trámites");
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
