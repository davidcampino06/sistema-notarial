package notaria.frontend;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import notaria.backend.Nucleo;
import notaria.backend.Sesion;

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
            String mensaje = "<div class='alerta error'>" + esc(e.getMessage()) + "</div>";
            if (sesion == null) {
                responder(ex, 400, paginaPublica("No se pudo completar", "",
                        mensaje + "<a class='boton-claro' href='javascript:history.back()'>Volver</a>"));
            } else {
                responder(ex, 400, pagina("No se pudo completar", sesion,
                        mensaje + "<a class='boton secundario' href='javascript:history.back()'>Volver</a>"));
            }
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
            encabezado = "<header><div class='marca'><span class='sello-mini'>N</span>Notaría · SGN</div>"
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

    /**
     * Página PÚBLICA (inicio de sesión, recuperar contraseña y consulta):
     * foto del edificio a la izquierda y panel de vidrio con el formulario a la derecha.
     */
    protected String paginaPublica(String titulo, String subtitulo, String contenido) {
        return "<!doctype html><html lang='es'><head><meta charset='utf-8'>"
                + "<meta name='viewport' content='width=device-width, initial-scale=1'>"
                + "<title>" + esc(titulo) + " · Sistema de Gestión Notarial</title>"
                + "<style>" + ESTILOS + "</style></head><body class='publico'>"
                + "<section class='portada'><div class='sello'>N</div>"
                + "<h2>Sistema de Gestión<br>Notarial</h2>"
                + "<p>Radicación, revisión, firma y caja de los trámites de la notaría en un solo lugar.</p></section>"
                + "<main class='vidrio'><h1>" + esc(titulo) + "</h1>"
                + (subtitulo.isEmpty() ? "" : "<p class='subtitulo'>" + esc(subtitulo) + "</p>")
                + contenido + "</main></body></html>";
    }

    // Íconos (dibujos SVG pequeños) que usan los formularios
    protected static final String ICONO_USUARIO = "<svg class='icono' viewBox='0 0 24 24' fill='none' stroke='currentColor' "
            + "stroke-width='2' stroke-linecap='round' stroke-linejoin='round'><circle cx='12' cy='8' r='4'/>"
            + "<path d='M4 21c0-4 4-6 8-6s8 2 8 6'/></svg>";
    protected static final String ICONO_CANDADO = "<svg class='icono' viewBox='0 0 24 24' fill='none' stroke='currentColor' "
            + "stroke-width='2' stroke-linecap='round' stroke-linejoin='round'><rect x='4' y='11' width='16' height='10' rx='2'/>"
            + "<path d='M8 11V7a4 4 0 0 1 8 0v4'/></svg>";
    protected static final String ICONO_DOCUMENTO = "<svg class='icono' viewBox='0 0 24 24' fill='none' stroke='currentColor' "
            + "stroke-width='2' stroke-linecap='round' stroke-linejoin='round'><path d='M14 3H6a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V9z'/>"
            + "<path d='M14 3v6h6M8 13h8M8 17h5'/></svg>";
    protected static final String ICONO_TARJETA = "<svg class='icono' viewBox='0 0 24 24' fill='none' stroke='currentColor' "
            + "stroke-width='2' stroke-linecap='round' stroke-linejoin='round'><rect x='3' y='5' width='18' height='14' rx='2'/>"
            + "<circle cx='9' cy='12' r='2.5'/><path d='M14 10h4M14 14h3'/></svg>";
    protected static final String ICONO_OJO = "<svg viewBox='0 0 24 24' fill='none' stroke='currentColor' stroke-width='2' "
            + "stroke-linecap='round' stroke-linejoin='round'><path d='M1 12s4-7 11-7 11 7 11 7-4 7-11 7S1 12 1 12z'/>"
            + "<circle cx='12' cy='12' r='3'/></svg>";
    protected static final String ICONO_FLECHA = "<svg viewBox='0 0 24 24' fill='none' stroke='currentColor' stroke-width='2' "
            + "stroke-linecap='round' stroke-linejoin='round'><path d='M5 12h14M13 6l6 6-6 6'/></svg>";

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

    /**
     * Los estilos se leen una sola vez del archivo estilos.css, y la imagen de
     * fondo (fondo.svg) se mete dentro de los estilos. Ambos están en esta misma
     * carpeta frontend.
     */
    private static final String ESTILOS = leerArchivo("estilos.css").replace("__FONDO__",
            "\"data:image/svg+xml;base64," + Base64.getEncoder().encodeToString(
                    leerArchivo("fondo.svg").getBytes(StandardCharsets.UTF_8)) + "\"");

    private static String leerArchivo(String nombre) {
        try (var archivo = Ventana.class.getResourceAsStream(nombre)) {
            if (archivo != null) {
                return new String(archivo.readAllBytes(), StandardCharsets.UTF_8);   // dentro del .jar
            }
            // Si se ejecuta desde VS Code, se lee directamente de la carpeta del proyecto
            return java.nio.file.Files.readString(java.nio.file.Path.of("src/main/java/notaria/frontend/" + nombre));
        } catch (Exception e) {
            System.out.println("No se encontró " + nombre + ": " + e.getMessage());
            return "";
        }
    }
}
