package notaria.ventanas;

import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Map;
import notaria.Configuracion;
import notaria.Nucleo;
import notaria.Sesion;
import notaria.modelo.Usuario;

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
        responder(ex, pagina("Sistema de Gestión Notarial", null,
                "<div class='login tarjeta'>" + mensajeOk(ex)
                + "<form method='post' action='/'>"
                + "<label>Usuario</label><input name='campoUsuario' required autofocus>"
                + "<label>Contraseña</label><input name='campoContrasena' type='password' required>"
                + "<button name='botonIngresar'>Ingresar</button>"
                + "</form>"
                + "<p class='suave'><a href='/recuperar'>¿Olvidó su contraseña?</a></p></div>"
                + "<p class='suave' style='text-align:center'>¿Es cliente? "
                + "<a href='/consulta'>Consulte el estado de su trámite</a></p>"));
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
        responder(ex, pagina("Recuperar contraseña", null,
                "<div class='login tarjeta'>"
                + "<p class='suave'>Se enviará una contraseña temporal al correo registrado del usuario.</p>"
                + "<form method='post' action='/recuperar'>"
                + "<label>Usuario</label><input name='campoUsuario' required>"
                + "<button>Enviar</button></form>"
                + "<p class='suave'><a href='/'>Volver al inicio de sesión</a></p></div>"));
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
