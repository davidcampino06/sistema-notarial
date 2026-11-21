package notaria.ventanas;

import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.util.Map;
import notaria.Nucleo;
import notaria.Sesion;
import notaria.modelo.EstadoTramite;
import notaria.modelo.Rol;
import notaria.modelo.Tramite;

/**
 * Ventana de revisión (vista de desarrollo). La usa el Notario.
 * Atributos: visorDocumento, campoObservaciones.
 * Métodos: aprobar(), devolver(), firmar().
 */
public class VentanaRevision extends Ventana {

    public VentanaRevision(Nucleo nucleo) {
        super(nucleo, Rol.NOTARIO);
    }

    @Override
    protected void manejar(HttpExchange ex, Sesion sesion) throws IOException {
        String ruta = subRuta(ex);
        if (ruta.equals("/") && !esPost(ex)) {
            mostrar(ex, sesion);
            return;
        }
        if (!esPost(ex)) {
            noEncontrado(ex, sesion);
            return;
        }
        Map<String, String> datos = formulario(ex);
        Tramite tramite = nucleo.buscarTramite(datos.get("n"));
        switch (ruta) {
            case "/aprobar":
                aprobar(sesion, tramite);
                break;
            case "/devolver":
                devolver(sesion, tramite, datos.get("campoObservaciones"));
                break;
            case "/firmar":
                firmar(sesion, tramite);
                break;
            default:
                noEncontrado(ex, sesion);
                return;
        }
        redirigir(ex, "/revision", "Trámite " + tramite.getNumeroRadicado() + ": " + tramite.getEstado().getTexto());
    }

    private void aprobar(Sesion sesion, Tramite tramite) {
        nucleo.cambiarEstado(sesion.getUsuarioActual(), tramite, EstadoTramite.APROBADO, null, null);
    }

    private void devolver(Sesion sesion, Tramite tramite, String observaciones) {
        nucleo.cambiarEstado(sesion.getUsuarioActual(), tramite, EstadoTramite.DEVUELTO, observaciones, null);
    }

    private void firmar(Sesion sesion, Tramite tramite) {
        nucleo.cambiarEstado(sesion.getUsuarioActual(), tramite, EstadoTramite.FIRMADO, null, null);
    }

    /** Muestra los trámites por revisar y los aprobados que esperan firma. */
    private void mostrar(HttpExchange ex, Sesion sesion) throws IOException {
        StringBuilder porRevisar = new StringBuilder();
        StringBuilder porFirmar = new StringBuilder();

        for (Tramite t : nucleo.getTramites()) {
            String n = "<input type='hidden' name='n' value='" + esc(t.getNumeroRadicado()) + "'>";
            if (t.getEstado() == EstadoTramite.EN_REVISION) {
                // visorDocumento = detalle del trámite; campoObservaciones = texto para devolver
                porRevisar.append(VentanaPrincipal.detalleTramite(t))
                    .append("<div class='tarjeta acciones'>")
                    .append("<form method='post' action='/revision/aprobar'>").append(n).append("<button>Aprobar</button></form>")
                    .append("<form method='post' action='/revision/devolver' style='display:block'>").append(n)
                    .append("<label>Observaciones para devolver</label><textarea name='campoObservaciones' required></textarea>")
                    .append("<button class='secundario'>Devolver al digitador</button></form></div>");
            } else if (t.getEstado() == EstadoTramite.APROBADO) {
                boolean pagado = t.getLiquidacion() != null && t.getLiquidacion().estaPagada();
                porFirmar.append("<tr><td>").append(esc(t.getNumeroRadicado())).append("</td><td>").append(esc(t.getTipo()))
                    .append("</td><td>").append(VentanaPrincipal.nombresClientes(t)).append("</td><td>")
                    .append(pagado ? "Pagado" : "<span class='suave'>Sin pago</span>").append("</td><td>")
                    .append(pagado ? "<form method='post' action='/revision/firmar'>" + n + "<button>Firmar</button></form>"
                                   : "<span class='suave'>Debe pagar en caja</span>")
                    .append("</td></tr>");
            }
        }

        StringBuilder html = new StringBuilder(mensajeOk(ex));
        html.append("<h2>Por revisar</h2>")
            .append(porRevisar.length() == 0 ? "<p class='tarjeta suave'>No hay trámites en revisión.</p>" : porRevisar)
            .append("<h2>Aprobados (pendientes de firma)</h2><div class='tarjeta tabla'><table>")
            .append("<tr><th>Radicado</th><th>Tipo</th><th>Cliente(s)</th><th>Pago</th><th></th></tr>")
            .append(porFirmar.length() == 0 ? "<tr><td colspan='5' class='suave'>No hay trámites aprobados.</td></tr>" : porFirmar)
            .append("</table></div>");
        responder(ex, pagina("Revisión del notario", sesion, html.toString()));
    }
}
