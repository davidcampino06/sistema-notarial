package notaria.ventanas;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import notaria.Nucleo;
import notaria.Sesion;

/**
 * Clase PADRE de todas las ventanas (HERENCIA). Tiene lo que todas
 * comparten: revisar la sesión y el rol, leer formularios, dibujar la
 * página HTML y mostrar los errores. Cada ventana hija solo implementa
 * manejar(), con lo que es propio de ella.
 */
public abstract class Ventana implements HttpHandler {

    protected final Nucleo nucleo;
    private final String[] rolesPermitidos;   // vacío = cualquier usuario con sesión

    protected Ventana(Nucleo nucleo, String... rolesPermitidos) {
        this.nucleo = nucleo;
        this.rolesPermitidos = rolesPermitidos;
    }

    /** Lo que hace cada ventana cuando le llega una petición. */
    protected abstract void manejar(HttpExchange ex, Sesion sesion) throws IOException;

    /** El login no exige sesión; las demás ventanas sí. */
    protected boolean requiereSesion() {
        return true;
    }

    @Override
    public final void handle(HttpExchange ex) throws IOException {
        Sesion sesion = Sesion.buscar(leerCookie(ex, "sesion"));
        try {
            if (requiereSesion()) {
                if (sesion == null) {
                    redirigir(ex, "/");
                    return;
                }
                if (rolesPermitidos.length > 0 && !sesion.getRol().es(rolesPermitidos)) {
                    throw new IllegalStateException("Su rol no tiene acceso a esta ventana");
                }
            }
            manejar(ex, sesion);
        } catch (IllegalArgumentException | IllegalStateException e) {
            // Errores de las reglas del negocio: se muestran al usuario
            responder(ex, 400, pagina("No se pudo completar", sesion,
                    "<div class='alerta error'>" + esc(e.getMessage()) + "</div>"
                    + "<a class='boton secundario' href='javascript:history.back()'>Volver</a>"));
        } catch (Exception e) {
            e.printStackTrace();
            responder(ex, 500, pagina("Error interno", sesion,
                    "<div class='alerta error'>Ocurrió un error inesperado. Revise la consola del servidor.</div>"));
        } finally {
            ex.close();
        }
    }

    // ------------------------------------------------------------------
    //  Ayudas para leer la petición
    // ------------------------------------------------------------------

    protected boolean esPost(HttpExchange ex) {
        return "POST".equalsIgnoreCase(ex.getRequestMethod());
    }

    /** Ruta dentro de la ventana. Ej: en /caja/recibo devuelve "/recibo". */
    protected String subRuta(HttpExchange ex) {
        String ruta = ex.getRequestURI().getPath();
        String base = ex.getHttpContext().getPath();
        String resto = ruta.substring(base.length());
        return resto.startsWith("/") ? resto : "/" + resto;
    }

    /** Datos que vienen en la dirección (?n=2026-000001&estado=...). */
    protected Map<String, String> parametros(HttpExchange ex) {
        return separar(ex.getRequestURI().getRawQuery());
    }

    /** Datos que se enviaron con un formulario (método POST). */
    protected Map<String, String> formulario(HttpExchange ex) throws IOException {
        String cuerpo = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        return separar(cuerpo);
    }

    private Map<String, String> separar(String texto) {
        Map<String, String> datos = new HashMap<>();
        if (texto == null || texto.isEmpty()) {
            return datos;
        }
        for (String par : texto.split("&")) {
            String[] kv = par.split("=", 2);
            String clave = URLDecoder.decode(kv[0], StandardCharsets.UTF_8);
            String valor = kv.length > 1 ? URLDecoder.decode(kv[1], StandardCharsets.UTF_8) : "";
            datos.put(clave, valor.trim());
        }
        return datos;
    }

    protected String leerCookie(HttpExchange ex, String nombre) {
        String cookies = ex.getRequestHeaders().getFirst("Cookie");
        if (cookies == null) {
            return null;
        }
        for (String c : cookies.split(";")) {
            String[] kv = c.trim().split("=", 2);
            if (kv.length == 2 && kv[0].equals(nombre)) {
                return kv[1];
            }
        }
        return null;
    }

    // ------------------------------------------------------------------
    //  Ayudas para responder
    // ------------------------------------------------------------------

    protected void responder(HttpExchange ex, int codigo, String html) throws IOException {
        byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
        ex.sendResponseHeaders(codigo, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }

    protected void responder(HttpExchange ex, String html) throws IOException {
        responder(ex, 200, html);
    }

    protected void redirigir(HttpExchange ex, String url) throws IOException {
        ex.getResponseHeaders().set("Location", url);
        ex.sendResponseHeaders(303, -1);
    }

    /** Redirige mostrando un mensaje de éxito en la siguiente página. */
    protected void redirigir(HttpExchange ex, String url, String mensaje) throws IOException {
        String separador = url.contains("?") ? "&" : "?";
        redirigir(ex, url + separador + "ok=" + URLEncoder.encode(mensaje, StandardCharsets.UTF_8));
    }

    protected void noEncontrado(HttpExchange ex, Sesion sesion) throws IOException {
        responder(ex, 404, pagina("Página no encontrada", sesion,
                "<p>La dirección no existe.</p><a class='boton' href='/principal'>Ir al inicio</a>"));
    }

    // ------------------------------------------------------------------
    //  Dibujo de la página HTML
    // ------------------------------------------------------------------

    /** Muestra el mensaje verde de éxito que llegó en la dirección (?ok=...). */
    protected String mensajeOk(HttpExchange ex) {
        String ok = parametros(ex).get("ok");
        return ok == null ? "" : "<div class='alerta ok'>" + esc(ok) + "</div>";
    }

    protected String pagina(String titulo, Sesion sesion, String contenido) {
        String encabezado = "";
        if (sesion != null) {
            encabezado = "<header><div class='marca'>Notaría · SGN</div>"
                    + "<nav>" + VentanaPrincipal.menuRol(sesion) + "</nav>"
                    + "<div class='usuario'>" + esc(sesion.getUsuarioActual().getNombreUsuario())
                    + " <span>(" + esc(sesion.getRol().getNombre()) + ")</span>"
                    + " <a href='/principal/salir'>Cerrar sesión</a></div></header>";
        }
        return "<!doctype html><html lang='es'><head><meta charset='utf-8'>"
                + "<meta name='viewport' content='width=device-width, initial-scale=1'>"
                + "<title>" + esc(titulo) + " · Sistema de Gestión Notarial</title>"
                + "<style>" + ESTILOS + "</style></head><body>"
                + encabezado + "<main><h1>" + esc(titulo) + "</h1>" + contenido + "</main></body></html>";
    }

    /** Evita que un texto escrito por el usuario se interprete como HTML. */
    protected static String esc(String texto) {
        if (texto == null) {
            return "";
        }
        return texto.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }

    protected static String dinero(double valor) {
        return "$ " + String.format("%,.0f", valor);
    }

    protected static String fecha(Date fecha) {
        return fecha == null ? "" : new SimpleDateFormat("yyyy-MM-dd HH:mm").format(fecha);
    }

    protected static String url(String texto) {
        return URLEncoder.encode(texto, StandardCharsets.UTF_8);
    }

    private static final String ESTILOS = String.join("\n",
        ":root{--fondo:#f5f3ee;--tarjeta:#fff;--texto:#1f2a37;--suave:#6b7280;--borde:#e3ded3;",
        "--primario:#1e3a5f;--acento:#b08d57;--ok:#1d6b45;--okf:#e6f4ec;--error:#a12b2b;--errorf:#fbeaea}",
        "*{box-sizing:border-box}body{margin:0;font-family:system-ui,-apple-system,Segoe UI,Roboto,sans-serif;",
        "background:var(--fondo);color:var(--texto);font-size:15px}",
        "header{display:flex;flex-wrap:wrap;gap:12px;align-items:center;background:var(--primario);color:#fff;padding:10px 20px}",
        "header a{color:#fff;text-decoration:none}.marca{font-weight:700;letter-spacing:.5px;color:#f1e3c8}",
        "nav{display:flex;flex-wrap:wrap;gap:4px;flex:1}nav a{padding:6px 10px;border-radius:6px}nav a:hover{background:rgba(255,255,255,.15)}",
        ".usuario{font-size:13px}.usuario span{opacity:.75}.usuario a{margin-left:8px;text-decoration:underline}",
        "main{max-width:1100px;margin:0 auto;padding:20px 16px}h1{font-size:24px;color:var(--primario);margin:6px 0 16px}",
        "h2{font-size:18px;color:var(--primario);margin:0 0 12px}",
        ".tarjeta{background:var(--tarjeta);border:1px solid var(--borde);border-radius:10px;padding:16px;margin-bottom:16px}",
        "table{width:100%;border-collapse:collapse;background:#fff}th,td{text-align:left;padding:8px;border-bottom:1px solid var(--borde);vertical-align:top}",
        "th{font-size:12px;text-transform:uppercase;color:var(--suave);letter-spacing:.4px}",
        ".tabla{overflow-x:auto}.tabla table{min-width:720px}td.acciones{min-width:300px}label{display:block;font-size:13px;color:var(--suave);margin:8px 0 4px}",
        "input,select,textarea{width:100%;padding:8px;border:1px solid var(--borde);border-radius:6px;font:inherit;background:#fff}",
        "textarea{min-height:70px}.fila{display:grid;grid-template-columns:repeat(auto-fit,minmax(180px,1fr));gap:12px}",
        ".boton,button{display:inline-block;background:var(--primario);color:#fff;border:0;border-radius:6px;padding:8px 14px;",
        "font:inherit;cursor:pointer;text-decoration:none;margin-top:10px}.boton:hover,button:hover{opacity:.9}",
        ".secundario{background:#fff;color:var(--primario);border:1px solid var(--primario)}.peligro{background:var(--error)}",
        ".alerta{padding:10px 14px;border-radius:8px;margin-bottom:14px}.ok{background:var(--okf);color:var(--ok)}",
        ".error{background:var(--errorf);color:var(--error)}",
        ".estado{display:inline-block;padding:2px 8px;border-radius:12px;font-size:12px;background:#eef2f7;color:var(--primario);white-space:nowrap}",
        ".estado.ANULADO{background:var(--errorf);color:var(--error)}.estado.DEVUELTO{background:#fff4e0;color:#8a5a00}",
        ".estado.FIRMADO,.estado.ENTREGADO,.estado.APROBADO{background:var(--okf);color:var(--ok)}",
        ".suave{color:var(--suave);font-size:13px}.acciones form{display:inline-block;margin-right:8px}",
        ".login{max-width:380px;margin:60px auto}.login h1{text-align:center}",
        "@media print{header,.no-imprimir{display:none}body{background:#fff}.tarjeta{border:0}}");
}
