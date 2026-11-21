package notaria.ventanas;

import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Map;
import java.util.Properties;
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
                + "<p class='suave'><a href='/recuperar'>¿Olvidó su contraseña?</a></p></div>"));
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
     * Genera una contraseña temporal y la envía por correo (Servidor de correo, SMTP:587).
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
        enviarCorreo(usuario.getCorreo(), "Recuperación de contraseña - Notaría",
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

    /** Envía un correo usando el servidor SMTP de la Configuración (variables SMTP_*). */
    private void enviarCorreo(String destino, String asunto, String texto) {
        String usuarioSmtp = System.getenv("SMTP_USUARIO");
        String claveSmtp = System.getenv("SMTP_CONTRASENA");
        String puerto = System.getenv("SMTP_PUERTO");

        Properties props = new Properties();
        props.put("mail.smtp.host", Configuracion.getInstancia().getServidorCorreo());
        props.put("mail.smtp.port", puerto == null ? "587" : puerto);
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.connectiontimeout", "10000");
        props.put("mail.smtp.timeout", "10000");

        jakarta.mail.Session sesionCorreo = jakarta.mail.Session.getInstance(props, new jakarta.mail.Authenticator() {
            @Override
            protected jakarta.mail.PasswordAuthentication getPasswordAuthentication() {
                return new jakarta.mail.PasswordAuthentication(usuarioSmtp, claveSmtp);
            }
        });
        try {
            jakarta.mail.internet.MimeMessage mensaje = new jakarta.mail.internet.MimeMessage(sesionCorreo);
            mensaje.setFrom(new jakarta.mail.internet.InternetAddress(usuarioSmtp));
            mensaje.setRecipients(jakarta.mail.Message.RecipientType.TO,
                    jakarta.mail.internet.InternetAddress.parse(destino));
            mensaje.setSubject(asunto, "UTF-8");
            mensaje.setText(texto, "UTF-8");
            jakarta.mail.Transport.send(mensaje);
        } catch (jakarta.mail.MessagingException e) {
            System.out.println("Error enviando correo: " + e.getMessage());
            throw new IllegalStateException("No se pudo enviar el correo. Pida al administrador una contraseña nueva");
        }
    }
}
