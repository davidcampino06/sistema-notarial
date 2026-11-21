package notaria.ventanas;

import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import notaria.Nucleo;
import notaria.Sesion;
import notaria.modelo.*;

/**
 * Ventana de caja (vista de desarrollo). La usa el Cajero.
 * Atributos: detalleLiquidacion, selectorMedioPago.
 * Métodos: registrarPago(), generarRecibo(), cierreCaja().
 * (Liquidar lo hace el Núcleo con liquidar(); aquí solo está el botón.)
 */
public class VentanaCaja extends Ventana {

    public VentanaCaja(Nucleo nucleo) {
        super(nucleo, Rol.CAJERO);
    }

    @Override
    protected void manejar(HttpExchange ex, Sesion sesion) throws IOException {
        switch (subRuta(ex)) {
            case "/":
                mostrar(ex, sesion);
                break;
            case "/liquidar":
                if (esPost(ex)) {
                    Tramite t = nucleo.buscarTramite(formulario(ex).get("n"));
                    Liquidacion l = nucleo.liquidar(sesion.getUsuarioActual(), t);
                    redirigir(ex, "/caja", "Trámite " + t.getNumeroRadicado() + " liquidado por " + dinero(l.getTotal()));
                } else {
                    noEncontrado(ex, sesion);
                }
                break;
            case "/pago":
                if (esPost(ex)) {
                    registrarPago(ex, sesion);
                } else {
                    noEncontrado(ex, sesion);
                }
                break;
            case "/recibo":
                generarRecibo(ex, sesion);
                break;
            case "/cierre":
                cierreCaja(ex, sesion);
                break;
            default:
                noEncontrado(ex, sesion);
        }
    }

    /** Lista de trámites por liquidar o por pagar, y los pagos de hoy. */
    private void mostrar(HttpExchange ex, Sesion sesion) throws IOException {
        StringBuilder pendientes = new StringBuilder();
        StringBuilder pagadosHoy = new StringBuilder();
        String hoy = dia(new Date());

        for (Tramite t : nucleo.getTramites()) {
            if (t.getEstado() == EstadoTramite.ANULADO) {
                continue;
            }
            Liquidacion l = t.getLiquidacion();
            String n = "<input type='hidden' name='n' value='" + esc(t.getNumeroRadicado()) + "'>";
            String inicio = "<tr><td>" + esc(t.getNumeroRadicado()) + "<br>" + VentanaPrincipal.estado(t) + "</td><td>"
                    + esc(t.getTipo()) + "<br><span class='suave'>" + esc(t.getDetalle()) + "</span></td><td>"
                    + VentanaPrincipal.nombresClientes(t) + "</td><td>";

            if (l == null) {
                pendientes.append(inicio).append("<span class='suave'>Sin liquidar</span></td><td>")
                    .append("<form method='post' action='/caja/liquidar'>").append(n).append("<button>Liquidar</button></form></td></tr>");
            } else if (!l.estaPagada()) {
                // detalleLiquidacion + selectorMedioPago
                pendientes.append(inicio).append(detalleLiquidacion(l)).append("</td><td>")
                    .append("<form method='post' action='/caja/pago'>").append(n)
                    .append("<select name='selectorMedioPago'>");
                for (MedioPago m : MedioPago.values()) {
                    pendientes.append("<option>").append(m.name()).append("</option>");
                }
                pendientes.append("</select><button>Registrar pago</button></form></td></tr>");
            } else if (dia(l.getPago().getFecha()).equals(hoy)) {
                pagadosHoy.append(inicio).append(dinero(l.getTotal())).append(" · ").append(l.getPago().getMedioPago())
                    .append("</td><td><a class='boton secundario' href='/caja/recibo?n=").append(url(t.getNumeroRadicado()))
                    .append("'>Recibo</a></td></tr>");
            }
        }

        String cabecera = "<tr><th>Radicado</th><th>Trámite</th><th>Cliente(s)</th><th>Liquidación</th><th></th></tr>";
        String vacio = "<tr><td colspan='5' class='suave'>No hay registros.</td></tr>";
        String html = mensajeOk(ex)
                + "<a class='boton secundario' href='/caja/cierre'>Cierre de caja</a>"
                + "<h2 style='margin-top:16px'>Pendientes de liquidar o pagar</h2><div class='tarjeta tabla'><table>"
                + cabecera + (pendientes.length() == 0 ? vacio : pendientes) + "</table></div>"
                + "<h2>Pagados hoy</h2><div class='tarjeta tabla'><table>"
                + cabecera + (pagadosHoy.length() == 0 ? vacio : pagadosHoy) + "</table></div>";
        responder(ex, pagina("Caja", sesion, html));
    }

    private String detalleLiquidacion(Liquidacion l) {
        return "Derechos " + dinero(l.getValorDerechos()) + "<br>IVA " + dinero(l.getIva())
                + "<br><b>Total " + dinero(l.getTotal()) + "</b>";
    }

    private void registrarPago(HttpExchange ex, Sesion sesion) throws IOException {
        Map<String, String> datos = formulario(ex);
        Tramite t = nucleo.buscarTramite(datos.get("n"));
        MedioPago medio = MedioPago.valueOf(datos.get("selectorMedioPago"));
        nucleo.registrarPago(sesion.getUsuarioActual(), t, medio);
        redirigir(ex, "/caja/recibo?n=" + url(t.getNumeroRadicado()), "Pago registrado");
    }

    /** Recibo de pago listo para imprimir. */
    private void generarRecibo(HttpExchange ex, Sesion sesion) throws IOException {
        Tramite t = nucleo.buscarTramite(parametros(ex).get("n"));
        Liquidacion l = t.getLiquidacion();
        if (l == null || !l.estaPagada()) {
            throw new IllegalStateException("El trámite " + t.getNumeroRadicado() + " todavía no tiene pago");
        }
        String html = mensajeOk(ex)
                + "<div class='tarjeta'><h2>Recibo de pago</h2>"
                + "<p><b>Radicado:</b> " + esc(t.getNumeroRadicado()) + "<br><b>Trámite:</b> " + esc(t.getTipo())
                + " — " + esc(t.getDetalle()) + "<br><b>Cliente(s):</b><br>" + VentanaPrincipal.nombresClientes(t) + "</p>"
                + "<table><tr><td>Derechos notariales</td><td>" + dinero(l.getValorDerechos()) + "</td></tr>"
                + "<tr><td>IVA (19 %)</td><td>" + dinero(l.getIva()) + "</td></tr>"
                + "<tr><th>Total pagado</th><th>" + dinero(l.getTotal()) + "</th></tr></table>"
                + "<p><b>Medio de pago:</b> " + l.getPago().getMedioPago() + "<br><b>Fecha:</b> " + fecha(l.getPago().getFecha())
                + "<br><b>Cajero:</b> " + esc(sesion.getUsuarioActual().getNombreUsuario()) + "</p></div>"
                + "<div class='no-imprimir'><button onclick='window.print()'>Imprimir</button> "
                + "<a class='boton secundario' href='/caja'>Volver a caja</a></div>";
        responder(ex, pagina("Recibo " + t.getNumeroRadicado(), sesion, html));
    }

    /** Suma los pagos de un día, separados por medio de pago. */
    private void cierreCaja(HttpExchange ex, Sesion sesion) throws IOException {
        String elDia = parametros(ex).getOrDefault("fecha", dia(new Date()));

        Map<MedioPago, Double> totales = new LinkedHashMap<>();
        for (MedioPago m : MedioPago.values()) {
            totales.put(m, 0.0);
        }
        double granTotal = 0;
        int cantidad = 0;
        for (Tramite t : nucleo.getTramites()) {
            Liquidacion l = t.getLiquidacion();
            if (l != null && l.estaPagada() && dia(l.getPago().getFecha()).equals(elDia)) {
                Pago p = l.getPago();
                totales.put(p.getMedioPago(), totales.get(p.getMedioPago()) + p.getValor());
                granTotal += p.getValor();
                cantidad++;
            }
        }

        StringBuilder filas = new StringBuilder();
        for (Map.Entry<MedioPago, Double> e : totales.entrySet()) {
            filas.append("<tr><td>").append(e.getKey()).append("</td><td>").append(dinero(e.getValue())).append("</td></tr>");
        }
        String html = "<form class='tarjeta fila no-imprimir' method='get' action='/caja/cierre'>"
                + "<div><label>Día</label><input type='date' name='fecha' value='" + esc(elDia) + "'></div>"
                + "<div><button>Consultar</button></div></form>"
                + "<div class='tarjeta'><h2>Cierre de caja del " + esc(elDia) + "</h2>"
                + "<p>Pagos registrados: <b>" + cantidad + "</b></p><table><tr><th>Medio de pago</th><th>Total</th></tr>"
                + filas + "<tr><th>TOTAL DEL DÍA</th><th>" + dinero(granTotal) + "</th></tr></table></div>"
                + "<div class='no-imprimir'><button onclick='window.print()'>Imprimir</button> "
                + "<a class='boton secundario' href='/caja'>Volver a caja</a></div>";
        responder(ex, pagina("Cierre de caja", sesion, html));
    }

    private static String dia(Date fecha) {
        return new SimpleDateFormat("yyyy-MM-dd").format(fecha);
    }
}
