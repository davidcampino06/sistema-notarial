package notaria.ventanas;

import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.util.Date;
import java.util.Map;
import notaria.Nucleo;
import notaria.Sesion;
import notaria.modelo.*;

/**
 * Ventana de radicación (vista de desarrollo). La usa el Recepcionista.
 * Atributos: selectorTipo, buscadorCliente, listaDocumentos.
 * Métodos: radicar(), adjuntarDocumento().
 */
public class VentanaRadicacion extends Ventana {

    public VentanaRadicacion(Nucleo nucleo) {
        super(nucleo, Rol.RECEPCIONISTA);
    }

    @Override
    protected void manejar(HttpExchange ex, Sesion sesion) throws IOException {
        if (!subRuta(ex).equals("/")) {
            noEncontrado(ex, sesion);
        } else if (esPost(ex)) {
            radicar(ex);
        } else {
            mostrar(ex, sesion);
        }
    }

    private void mostrar(HttpExchange ex, Sesion sesion) throws IOException {
        // buscadorCliente: si se buscó un documento, se muestran los datos del cliente
        String buscado = parametros(ex).getOrDefault("buscadorCliente", "");
        Cliente encontrado = buscado.isEmpty() ? null : nucleo.getListaClientes().buscar(buscado);

        StringBuilder html = new StringBuilder();
        html.append("<form class='tarjeta' method='get' action='/radicacion'><h2>Buscar cliente</h2>")
            .append("<div class='fila'><div><label>Número de documento</label><input name='buscadorCliente' value='")
            .append(esc(buscado)).append("'></div><div><button>Buscar</button></div></div>");
        if (!buscado.isEmpty()) {
            html.append(encontrado == null
                    ? "<p class='suave'>No existe un cliente con ese documento: llene sus datos abajo y se registrará al radicar.</p>"
                    : "<div class='alerta ok'>Cliente encontrado: " + esc(encontrado.getNombres()) + "</div>");
        }
        html.append("</form>");

        html.append("<form class='tarjeta' method='post' action='/radicacion'><h2>Datos del trámite</h2>")
            .append("<label>Tipo de trámite</label><select name='selectorTipo' id='selectorTipo' onchange='mostrarCampo()'>")
            .append("<option value='Autenticacion'>Autenticación</option>")
            .append("<option value='DeclaracionExtrajuicio'>Declaración extrajuicio</option>")
            .append("<option value='PoderEspecial'>Poder especial</option>")
            .append("<option value='EscrituraCompraventa'>Escritura de compraventa</option></select>")
            .append("<div class='campo' id='Autenticacion'><label>Número de folios</label><input name='numeroFolios' type='number' min='1'></div>")
            .append("<div class='campo' id='DeclaracionExtrajuicio'><label>Contenido de la declaración</label><textarea name='contenido'></textarea></div>")
            .append("<div class='campo' id='PoderEspecial'><label>Objeto del poder</label><textarea name='objetoPoder'></textarea></div>")
            .append("<div class='campo' id='EscrituraCompraventa'><label>Valor del inmueble</label><input name='valorInmueble' type='number' min='1'></div>");

        html.append(bloqueCliente("1", "Cliente principal", encontrado, true))
            .append(bloqueCliente("2", "Segundo cliente (opcional, ej. comprador)", null, false));

        html.append("<h2 style='margin-top:16px'>Documentos adjuntos</h2>")
            .append("<label>Nombre de cada documento (uno por línea)</label>")
            .append("<textarea name='listaDocumentos' required placeholder='cedula.pdf&#10;certificado_libertad.pdf'></textarea>")
            .append("<button>Radicar trámite</button></form>");

        // Pequeño script: muestra solo el campo del tipo de trámite escogido
        html.append("<script>function mostrarCampo(){var t=document.getElementById('selectorTipo').value;")
            .append("document.querySelectorAll('.campo').forEach(function(c){c.style.display=c.id===t?'block':'none';});}")
            .append("mostrarCampo();</script>");

        responder(ex, pagina("Radicar trámite", sesion, html.toString()));
    }

    private String bloqueCliente(String n, String titulo, Cliente c, boolean obligatorio) {
        String req = obligatorio ? " required" : "";
        return "<h2 style='margin-top:16px'>" + titulo + "</h2><div class='fila'>"
                + "<div><label>Tipo de documento</label><select name='tipoDocumento" + n + "'>"
                + opcionDoc("CC", c) + opcionDoc("CE", c) + opcionDoc("NIT", c) + opcionDoc("PAS", c) + "</select></div>"
                + "<div><label>Número de documento</label><input name='numeroDocumento" + n + "'" + req
                + " value='" + (c == null ? "" : esc(c.getNumeroDocumento())) + "'></div>"
                + "<div><label>Nombres</label><input name='nombres" + n + "'"
                + " value='" + (c == null ? "" : esc(c.getNombres())) + "'></div>"
                + "<div><label>Teléfono</label><input name='telefono" + n + "'"
                + " value='" + (c == null ? "" : esc(c.getTelefono())) + "'></div>"
                + "<div><label>Correo (para avisarle)</label><input name='correo" + n + "' type='email'"
                + " value='" + (c == null ? "" : esc(c.getCorreo())) + "'></div></div>";
    }

    private String opcionDoc(String tipo, Cliente c) {
        boolean sel = c != null && tipo.equals(c.getTipoDocumento());
        return "<option" + (sel ? " selected" : "") + ">" + tipo + "</option>";
    }

    /** Crea el trámite del tipo escogido, le agrega clientes y documentos y lo envía al Núcleo. */
    private void radicar(HttpExchange ex) throws IOException {
        Map<String, String> datos = formulario(ex);

        // selectorTipo: según el tipo se crea un objeto de una subclase distinta
        Tramite tramite;
        switch (datos.get("selectorTipo")) {
            case "Autenticacion":
                tramite = new Autenticacion((int) numero(datos.get("numeroFolios"), "el número de folios"));
                break;
            case "DeclaracionExtrajuicio":
                tramite = new DeclaracionExtrajuicio(datos.get("contenido"));
                break;
            case "PoderEspecial":
                tramite = new PoderEspecial(datos.get("objetoPoder"));
                break;
            case "EscrituraCompraventa":
                tramite = new EscrituraCompraventa(numero(datos.get("valorInmueble"), "el valor del inmueble"));
                break;
            default:
                throw new IllegalArgumentException("Escoja un tipo de trámite");
        }

        agregarCliente(tramite, datos, "1");
        agregarCliente(tramite, datos, "2");

        for (String nombre : datos.getOrDefault("listaDocumentos", "").split("\n")) {
            if (!nombre.isBlank()) {
                adjuntarDocumento(tramite, nombre.trim());
            }
        }
        if (tramite.getDocumentos().isEmpty()) {
            throw new IllegalArgumentException("Debe adjuntar al menos un documento");
        }

        nucleo.radicarTramite(tramite);
        redirigir(ex, "/principal/tramite?n=" + url(tramite.getNumeroRadicado()),
                "Trámite radicado con el número " + tramite.getNumeroRadicado());
    }

    /** Agrega un documento al trámite con la fecha de hoy. */
    private void adjuntarDocumento(Tramite tramite, String nombreArchivo) {
        tramite.agregarDocumento(new DocumentoAdjunto(nombreArchivo, new Date()));
    }

    /** Si el cliente ya existe se usa el guardado; si no, se crea uno nuevo con los datos escritos. */
    private void agregarCliente(Tramite tramite, Map<String, String> datos, String n) {
        String documento = datos.getOrDefault("numeroDocumento" + n, "");
        if (documento.isEmpty()) {
            return;
        }
        Cliente cliente = nucleo.getListaClientes().buscar(documento);
        if (cliente == null) {
            cliente = new Cliente(documento, datos.get("nombres" + n), datos.get("telefono" + n),
                    datos.get("tipoDocumento" + n), datos.get("correo" + n));
        }
        tramite.agregarCliente(cliente);
    }

    private double numero(String texto, String campo) {
        try {
            return Double.parseDouble(texto);
        } catch (NumberFormatException | NullPointerException e) {
            throw new IllegalArgumentException("Escriba " + campo + " como un número");
        }
    }
}
