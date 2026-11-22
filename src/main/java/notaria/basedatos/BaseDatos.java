package notaria.basedatos;

import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import notaria.logica.Configuracion;
import notaria.logica.ListaClientes;
import notaria.modelo.*;
import org.bson.Document;

/**
 * Todo lo que tiene que ver con MongoDB está en esta clase.
 * El Núcleo la usa desde guardarBaseDatos() y leerBaseDatos().
 *
 * Colecciones (se crean solas la primera vez):
 *   usuarios       _id = nombreUsuario
 *   clientes       _id = numeroDocumento
 *   tramites       _id = numeroRadicado   (incluye documentos, liquidación y pago)
 *   configuracion  _id = "tarifas"
 *
 * Convierte cada objeto Java en un "Document" de MongoDB (para guardar)
 * y cada Document en un objeto Java (para leer).
 */
public class BaseDatos {

    private static final ReplaceOptions CREAR_SI_NO_EXISTE = new ReplaceOptions().upsert(true);

    private MongoDatabase baseDatos;

    /** Se conecta a MongoDB Atlas con la cadena de conexión (MONGODB_URI). */
    public BaseDatos(String uriMongo, String nombreBaseDatos) {
        baseDatos = MongoClients.create(uriMongo).getDatabase(nombreBaseDatos);
        System.out.println("Conectado a MongoDB, base de datos: " + nombreBaseDatos);
    }

    // =====================================================================
    //  GUARDAR (objeto Java -> documento de MongoDB)
    // =====================================================================

    /** Guarda (o reemplaza si ya existe) un documento usando su _id. */
    private void guardar(String coleccion, Document documento) {
        MongoCollection<Document> c = baseDatos.getCollection(coleccion);
        c.replaceOne(Filters.eq("_id", documento.get("_id")), documento, CREAR_SI_NO_EXISTE);
    }

    public void guardar(Usuario u) {
        guardar("usuarios", new Document("_id", u.getNombreUsuario())
                .append("correo", u.getCorreo())
                .append("contrasena", u.getContrasenaCifrada())
                .append("activo", u.isActivo())
                .append("rol", u.getRol().getNombre()));
    }

    public void guardar(Cliente c) {
        guardar("clientes", new Document("_id", c.getNumeroDocumento())
                .append("nombres", c.getNombres())
                .append("telefono", c.getTelefono())
                .append("tipoDocumento", c.getTipoDocumento())
                .append("correo", c.getCorreo()));
    }

    public void guardar(Configuracion configuracion) {
        Document tarifas = new Document();
        for (Map.Entry<String, Double> e : configuracion.getTarifasVigentes().entrySet()) {
            tarifas.append(e.getKey(), e.getValue());
        }
        guardar("configuracion", new Document("_id", "tarifas").append("valores", tarifas));
    }

    public void guardar(Tramite t) {
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

    // =====================================================================
    //  LEER (documento de MongoDB -> objeto Java)
    // =====================================================================

    public List<Usuario> leerUsuarios() {
        List<Usuario> usuarios = new ArrayList<>();
        for (Document d : baseDatos.getCollection("usuarios").find()) {
            Usuario u = new Usuario(d.getString("_id"), d.getString("correo"), new Rol(d.getString("rol")));
            u.setContrasenaCifrada(d.getString("contrasena"));
            u.setActivo(d.getBoolean("activo"));
            usuarios.add(u);
        }
        return usuarios;
    }

    public List<Cliente> leerClientes() {
        List<Cliente> clientes = new ArrayList<>();
        for (Document d : baseDatos.getCollection("clientes").find()) {
            clientes.add(new Cliente(d.getString("_id"), d.getString("nombres"),
                    d.getString("telefono"), d.getString("tipoDocumento"), d.getString("correo")));
        }
        return clientes;
    }

    /** Pone en la Configuración las tarifas guardadas (si hay). */
    public void leerTarifas(Configuracion configuracion) {
        for (Document d : baseDatos.getCollection("configuracion").find(Filters.eq("_id", "tarifas"))) {
            Document valores = d.get("valores", Document.class);
            for (String nombre : valores.keySet()) {
                configuracion.setTarifa(nombre, valores.getDouble(nombre));
            }
        }
    }

    /**
     * Lee los trámites. Necesita los clientes y usuarios ya cargados para
     * volver a unir cada trámite con sus clientes y su digitador.
     */
    public List<Tramite> leerTramites(ListaClientes listaClientes, List<Usuario> usuarios) {
        List<Tramite> tramites = new ArrayList<>();
        for (Document d : baseDatos.getCollection("tramites").find()) {
            tramites.add(leerTramite(d, listaClientes, usuarios));
        }
        return tramites;
    }

    private Tramite leerTramite(Document d, ListaClientes listaClientes, List<Usuario> usuarios) {
        // Según el "tipo" guardado se crea el objeto de la subclase correcta
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
        String digitador = d.getString("digitador");
        for (Usuario u : usuarios) {
            if (u.getNombreUsuario().equals(digitador)) {
                t.setDigitador(u);
            }
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
}
