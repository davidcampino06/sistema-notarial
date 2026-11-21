package notaria.frontend;

import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import notaria.backend.Nucleo;
import notaria.backend.Sesion;
import notaria.backend.modelo.*;

/**
 * Ventana principal (vista de desarrollo): bandeja de trámites.
 * Atributos: listaTramites, filtros, menuRol.
 * Métodos: abrirTramite(), filtrarTramites(), cerrarSesion().
 */
public class VentanaPrincipal extends Ventana {

    public VentanaPrincipal(Nucleo nucleo) {
        super(nucleo);   // todos los roles pueden entrar
    }

    @Override
    protected void manejar(HttpExchange ex, Sesion sesion) throws IOException {
        switch (subRuta(ex)) {
            case "/":
                filtrarTramites(ex, sesion);
                break;
            case "/tramite":
                abrirTramite(ex, sesion);
                break;
            case "/estado":
                if (!esPost(ex)) {
                    noEncontrado(ex, sesion);
                    return;
                }
                cambiarEstado(ex, sesion);
                break;
            case "/salir":
                cerrarSesion(ex);
                break;
            default:
                noEncontrado(ex, sesion);
        }
    }

    /** Menú con las ventanas que puede abrir cada rol. */
    public static String menuRol(Sesion sesion) {
        Rol rol = sesion.getRol();
        StringBuilder menu = new StringBuilder("<a href='/principal'>Trámites</a>");
        if (rol.es(Rol.RECEPCIONISTA)) menu.append("<a href='/radicacion'>Radicación</a>");
        if (rol.es(Rol.NOTARIO)) menu.append("<a href='/revision'>Revisión</a>");
        if (rol.es(Rol.CAJERO)) menu.append("<a href='/caja'>Caja</a>");
        if (rol.es(Rol.ADMINISTRADOR)) menu.append("<a href='/administracion'>Administración</a>");
        return menu.toString();
    }

    /** Muestra la lista de trámites aplicando los filtros (estado, tipo y texto). */
    private void filtrarTramites(HttpExchange ex, Sesion sesion) throws IOException {
        Map<String, String> filtros = parametros(ex);
        String estado = filtros.getOrDefault("estado", "");
        String tipo = filtros.getOrDefault("tipo", "");
        String texto = filtros.getOrDefault("texto", "").toLowerCase();

        List<Tramite> listaTramites = new ArrayList<>();
        for (Tramite t : nucleo.getTramites()) {
            boolean pasaEstado = estado.isEmpty() || t.getEstado().name().equals(estado);
            boolean pasaTipo = tipo.isEmpty() || t.getClass().getSimpleName().equals(tipo);
            boolean pasaTexto = texto.isEmpty() || t.getNumeroRadicado().contains(texto);
            for (Cliente c : t.getClientes()) {
                if (c.getNumeroDocumento().contains(texto) || c.getNombres().toLowerCase().contains(texto)) {
                    pasaTexto = true;
                }
            }
            if (pasaEstado && pasaTipo && pasaTexto) {
                listaTramites.add(0, t);   // los más nuevos primero
            }
        }

        StringBuilder html = new StringBuilder(mensajeOk(ex));
        html.append("<form class='tarjeta fila' method='get' action='/principal'>")
            .append("<div><label>Estado</label><select name='estado'><option value=''>Todos</option>");
        for (EstadoTramite e : EstadoTramite.values()) {
            html.append(opcion(e.name(), e.getTexto(), estado));
        }
        html.append("</select></div><div><label>Tipo</label><select name='tipo'><option value=''>Todos</option>")
            .append(opcion("Autenticacion", "Autenticación", tipo))
            .append(opcion("DeclaracionExtrajuicio", "Declaración extrajuicio", tipo))
            .append(opcion("PoderEspecial", "Poder especial", tipo))
            .append(opcion("EscrituraCompraventa", "Escritura de compraventa", tipo))
            .append("</select></div><div><label>Radicado, documento o nombre</label>")
            .append("<input name='texto' value='").append(esc(filtros.getOrDefault("texto", ""))).append("'></div>")
            .append("<div><button>Filtrar</button></div></form>");

        html.append("<div class='tarjeta tabla'><table><tr><th>Radicado</th><th>Tipo</th><th>Cliente(s)</th>")
            .append("<th>Fecha</th><th>Digitador</th><th>Estado</th><th></th></tr>");
        for (Tramite t : listaTramites) {
            html.append("<tr><td>").append(esc(t.getNumeroRadicado())).append("</td><td>").append(esc(t.getTipo()))
                .append("</td><td>").append(nombresClientes(t)).append("</td><td>").append(fecha(t.getFechaRadicacion()))
                .append("</td><td>").append(t.getDigitador() == null ? "—" : esc(t.getDigitador().getNombreUsuario()))
                .append("</td><td>").append(estado(t)).append("</td><td><a class='boton secundario' href='/principal/tramite?n=")
                .append(url(t.getNumeroRadicado())).append("'>Abrir</a></td></tr>");
        }
        if (listaTramites.isEmpty()) {
            html.append("<tr><td colspan='7' class='suave'>No hay trámites.</td></tr>");
        }
        html.append("</table></div>");
        responder(ex, pagina("Trámites", sesion, html.toString()));
    }

    /** Muestra todo el detalle de un trámite y las acciones que el usuario puede hacer. */
    private void abrirTramite(HttpExchange ex, Sesion sesion) throws IOException {
        Tramite t = nucleo.buscarTramite(parametros(ex).get("n"));
        Usuario yo = sesion.getUsuarioActual();
        Rol rol = sesion.getRol();
        EstadoTramite estado = t.getEstado();
        boolean soyDigitadorAsignado = t.getDigitador() != null
                && t.getDigitador().getNombreUsuario().equals(yo.getNombreUsuario());

        StringBuilder html = new StringBuilder(mensajeOk(ex));
        html.append(detalleTramite(t));

        // ---- Acciones según el rol y el estado ----
        StringBuilder acciones = new StringBuilder();
        if (estado == EstadoTramite.RADICADO && rol.es(Rol.RECEPCIONISTA, Rol.NOTARIO)) {
            acciones.append(formEstado(t, EstadoTramite.EN_ELABORACION, "Asignar digitador", selectorDigitador()));
        }
        if (estado == EstadoTramite.EN_ELABORACION && soyDigitadorAsignado) {
            acciones.append(formEstado(t, EstadoTramite.EN_REVISION, "Enviar a revisión", ""));
        }
        if (estado == EstadoTramite.DEVUELTO && soyDigitadorAsignado) {
            acciones.append(formEstado(t, EstadoTramite.EN_ELABORACION, "Corregir (volver a elaboración)", ""));
        }
        if ((estado == EstadoTramite.EN_REVISION || estado == EstadoTramite.APROBADO) && rol.es(Rol.NOTARIO)) {
            acciones.append("<a class='boton' href='/revision'>Ir a revisión</a> ");
        }
        if (estado != EstadoTramite.ANULADO && rol.es(Rol.CAJERO)) {
            acciones.append("<a class='boton' href='/caja'>Ir a caja</a> ");
        }
        if (estado == EstadoTramite.FIRMADO && rol.es(Rol.RECEPCIONISTA)) {
            acciones.append(formEstado(t, EstadoTramite.ENTREGADO, "Marcar como entregado", ""));
        }
        if (rol.es(Rol.NOTARIO) && estado != EstadoTramite.FIRMADO && estado != EstadoTramite.ENTREGADO
                && estado != EstadoTramite.ANULADO) {
            acciones.append(formEstado(t, EstadoTramite.ANULADO, "Anular trámite",
                    "<label>Motivo de anulación</label><textarea name='observaciones' required></textarea>"));
        }
        if (acciones.length() > 0) {
            html.append("<div class='tarjeta acciones'><h2>Acciones</h2>").append(acciones).append("</div>");
        }
        html.append("<a class='boton secundario' href='/principal'>Volver a la lista</a>");
        responder(ex, pagina("Trámite " + t.getNumeroRadicado(), sesion, html.toString()));
    }

    /** Recibe el formulario de una acción y le pide al Núcleo cambiar el estado. */
    private void cambiarEstado(HttpExchange ex, Sesion sesion) throws IOException {
        Map<String, String> datos = formulario(ex);
        Tramite t = nucleo.buscarTramite(datos.get("n"));
        EstadoTramite nuevo = EstadoTramite.valueOf(datos.get("nuevo"));
        Usuario digitador = datos.containsKey("digitador") ? nucleo.buscarUsuario(datos.get("digitador")) : null;
        nucleo.cambiarEstado(sesion.getUsuarioActual(), t, nuevo, datos.get("observaciones"), digitador);
        redirigir(ex, "/principal/tramite?n=" + url(t.getNumeroRadicado()),
                "El trámite quedó en estado: " + t.getEstado().getTexto());
    }

    private void cerrarSesion(HttpExchange ex) throws IOException {
        Sesion.cerrar(leerCookie(ex, "sesion"));
        ex.getResponseHeaders().add("Set-Cookie", "sesion=; Path=/; Max-Age=0");
        redirigir(ex, "/", "Sesión cerrada");
    }

    // ------------------------------------------------------------------
    //  Piezas de HTML reutilizadas también por otras ventanas
    // ------------------------------------------------------------------

    /** Tarjeta con todos los datos del trámite (la usan también Revisión y Caja). */
    static String detalleTramite(Tramite t) {
        StringBuilder html = new StringBuilder("<div class='tarjeta'>");
        html.append("<h2>").append(esc(t.getTipo())).append(" · ").append(esc(t.getNumeroRadicado())).append(" ")
            .append(estado(t)).append("</h2>")
            .append("<p>").append(esc(t.getDetalle())).append("</p>")
            .append("<p class='suave'>Radicado el ").append(fecha(t.getFechaRadicacion()))
            .append(" · Digitador: ").append(t.getDigitador() == null ? "sin asignar" : esc(t.getDigitador().getNombreUsuario()))
            .append("</p>");
        if (t.getObservaciones() != null) {
            html.append("<div class='alerta error'><b>Observaciones:</b> ").append(esc(t.getObservaciones())).append("</div>");
        }
        html.append("<div class='fila'><div><b>Clientes</b><ul>");
        for (Cliente c : t.getClientes()) {
            html.append("<li>").append(esc(c.getNombres())).append(" — ").append(esc(c.getTipoDocumento())).append(" ")
                .append(esc(c.getNumeroDocumento())).append(c.getTelefono() == null || c.getTelefono().isEmpty()
                        ? "" : " · Tel. " + esc(c.getTelefono()))
                .append(c.tieneCorreo() ? " · " + esc(c.getCorreo()) : "").append("</li>");
        }
        html.append("</ul></div><div><b>Documentos adjuntos</b><ul>");
        for (DocumentoAdjunto d : t.getDocumentos()) {
            html.append("<li>").append(esc(d.getNombreArchivo())).append(" <span class='suave'>(")
                .append(fecha(d.getFechaCarga())).append(")</span></li>");
        }
        html.append("</ul></div><div><b>Liquidación</b><br>");
        Liquidacion l = t.getLiquidacion();
        if (l == null) {
            html.append("<span class='suave'>Sin liquidar</span>");
        } else {
            html.append("Derechos: ").append(dinero(l.getValorDerechos())).append("<br>IVA: ").append(dinero(l.getIva()))
                .append("<br><b>Total: ").append(dinero(l.getTotal())).append("</b><br>")
                .append(l.estaPagada() ? "Pagado con " + l.getPago().getMedioPago() + " el " + fecha(l.getPago().getFecha())
                        : "<span class='suave'>Pendiente de pago</span>");
        }
        html.append("</div></div></div>");
        return html.toString();
    }

    static String estado(Tramite t) {
        return "<span class='estado " + t.getEstado().name() + "'>" + t.getEstado().getTexto() + "</span>";
    }

    static String nombresClientes(Tramite t) {
        StringBuilder sb = new StringBuilder();
        for (Cliente c : t.getClientes()) {
            if (sb.length() > 0) sb.append("<br>");
            sb.append(esc(c.getNombres()));
        }
        return sb.toString();
    }

    private String formEstado(Tramite t, EstadoTramite nuevo, String textoBoton, String camposExtra) {
        String clase = nuevo == EstadoTramite.ANULADO ? "peligro" : "";
        return "<form method='post' action='/principal/estado'>"
                + "<input type='hidden' name='n' value='" + esc(t.getNumeroRadicado()) + "'>"
                + "<input type='hidden' name='nuevo' value='" + nuevo.name() + "'>"
                + camposExtra + "<button class='" + clase + "'>" + textoBoton + "</button></form>";
    }

    private String selectorDigitador() {
        StringBuilder sb = new StringBuilder("<label>Digitador</label><select name='digitador' required>");
        for (Usuario u : nucleo.getUsuarios()) {
            if (u.isActivo() && u.getRol().es(Rol.DIGITADOR)) {
                sb.append("<option>").append(esc(u.getNombreUsuario())).append("</option>");
            }
        }
        return sb.append("</select>").toString();
    }

    private static String opcion(String valor, String texto, String seleccionado) {
        return "<option value='" + valor + "'" + (valor.equals(seleccionado) ? " selected" : "") + ">" + texto + "</option>";
    }
}
