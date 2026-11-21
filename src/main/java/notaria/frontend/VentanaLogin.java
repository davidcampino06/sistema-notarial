package notaria.frontend;

import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Map;
import notaria.backend.Configuracion;
import notaria.backend.Nucleo;
import notaria.backend.Sesion;
import notaria.backend.modelo.Usuario;

/**
 * Ventana de inicio de sesión (vista de desarrollo).
 * Campos: campoUsuario, campoContrasena, botonIngresar.
 * Métodos: iniciarSesion(), recuperarContrasena().
 */
public class VentanaLogin extends Ventana {

    public VentanaLogin(Nucleo nucleo) {
        super(nucleo);
    }

    @Override
    protected boolean requiereSesion() {
        return false;
    }

    @Override
    protected void manejar(HttpExchange ex, Sesion sesion) throws IOException {
        switch (subRuta(ex)) {
            case "/":
                if (esPost(ex)) {
                    iniciarSesion(ex);
                } else if (sesion != null) {
                    redirigir(ex, "/principal");
                } else {
                    mostrar(ex);
                }
                break;
            case "/recuperar":
                if (esPost(ex)) {
                    recuperarContrasena(ex);
                } else {
                    mostrarRecuperar(ex);
                }
                break;
            default:
                noEncontrado(ex, sesion);
        }
    }

    private void mostrar(HttpExchange ex) throws IOException {
        String html = mensajeOk(ex)
                + "<form method='post' action='/'>"
                // campoUsuario
                + "<label for='campoUsuario'>Usuario</label>"
                + "<input id='campoUsuario' name='campoUsuario' autocomplete='username' required autofocus>"
                // campoContrasena (con el ojo para mostrar u ocultar la contraseña)
                + "<label for='campoContrasena'>Contraseña</label>"
                + "<div class='campo'>"
                + "<input id='campoContrasena' name='campoContrasena' type='password' autocomplete='current-password' required>"
                + "<button type='button' class='ver-clave' title='Mostrar contraseña' onclick=\"var c=document.getElementById('campoContrasena');"
                + "c.type=c.type==='password'?'text':'password'\">" + ICONO_OJO + "</button></div>"
                + "<a class='enlace-pequeno' href='/recuperar'>¿Olvidó su contraseña?</a>"
                // botonIngresar
                + "<button name='botonIngresar' class='boton-principal'>Ingresar</button>"
                + "</form>"
                + "<p class='separador'>¿Es cliente? Revise cómo va su trámite con el número de radicado.</p>"
                + "<a class='boton-claro' href='/consulta'>Consultar mi trámite</a>"
                + "<p class='pie'>¿No tiene usuario? Pídalo al administrador de la notaría.</p>";
        responder(ex, paginaPublica("Iniciar sesión", "Para funcionarios de la notaría.", html));
    }

    /** Valida usuario y contraseña con el Núcleo y abre la sesión. */
    private void iniciarSesion(HttpExchange ex) throws IOException {
        Map<String, String> datos = formulario(ex);
        Usuario usuario = nucleo.iniciarSesion(datos.get("campoUsuario"), datos.get("campoContrasena"));
        String token = Sesion.abrir(usuario);
        ex.getResponseHeaders().add("Set-Cookie", "sesion=" + token + "; Path=/; HttpOnly; SameSite=Lax");
        redirigir(ex, "/principal");
    }

    private void mostrarRecuperar(HttpExchange ex) throws IOException {
        String html = "<form method='post' action='/recuperar'>"
                + "<label for='campoUsuario'>Usuario</label>"
                + "<input id='campoUsuario' name='campoUsuario' autocomplete='username' required autofocus>"
                + "<button class='boton-principal'>Enviar contraseña temporal</button></form>"
                + "<p class='pie'><a href='/'>Volver al inicio de sesión</a></p>";
        responder(ex, paginaPublica("Recuperar contraseña",
                "Le enviaremos una contraseña temporal al correo que tiene registrado.", html));
    }

    /**
     * Genera una contraseña temporal y la envía por correo (Servidor de correo).
     * La contraseña solo se cambia si el correo se pudo enviar.
     */
    private void recuperarContrasena(HttpExchange ex) throws IOException {
        Usuario usuario = nucleo.buscarUsuario(formulario(ex).get("campoUsuario"));
        if (usuario == null || usuario.getCorreo() == null || usuario.getCorreo().isBlank()) {
            throw new IllegalArgumentException("El usuario no existe o no tiene correo registrado. Hable con el administrador");
        }
        if (!Configuracion.getInstancia().hayServidorCorreo()) {
            throw new IllegalStateException("El servidor de correo no está configurado. Pida al administrador una contraseña nueva");
        }
        String temporal = generarContrasenaTemporal();
        nucleo.enviarCorreo(usuario.getCorreo(), "Recuperación de contraseña - Notaría",
                "Hola " + usuario.getNombreUsuario() + ",\n\nSu contraseña temporal es: " + temporal
                + "\n\nIngrese al sistema y pida al administrador cambiarla si lo desea.");
        usuario.cambiarContrasena(temporal);
        nucleo.guardarBaseDatos(usuario);
        redirigir(ex, "/", "Se envió una contraseña temporal a su correo");
    }

    private String generarContrasenaTemporal() {
        String letras = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
        SecureRandom azar = new SecureRandom();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            sb.append(letras.charAt(azar.nextInt(letras.length())));
        }
        return sb.toString();
    }
}
