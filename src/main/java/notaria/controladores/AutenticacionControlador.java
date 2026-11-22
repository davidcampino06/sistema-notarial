package notaria.controladores;

import java.security.SecureRandom;
import java.util.Map;
import notaria.logica.Configuracion;
import notaria.logica.Nucleo;
import notaria.logica.Sesion;
import notaria.modelo.Usuario;
import org.springframework.web.bind.annotation.*;

/**
 * Inicio y cierre de sesión. Lo usa VentanaLogin (React):
 * iniciarSesion(), recuperarContrasena() y VentanaPrincipal: cerrarSesion().
 */
@RestController
@RequestMapping("/api/sesion")
public class AutenticacionControlador extends Controlador {

    public AutenticacionControlador(Nucleo nucleo) {
        super(nucleo);
    }

    record DatosIngreso(String nombreUsuario, String contrasena) {}

    /** POST /api/sesion → valida usuario y contraseña y devuelve el token de la sesión. */
    @PostMapping
    public Map<String, Object> iniciarSesion(@RequestBody DatosIngreso datos) {
        Usuario usuario = nucleo.iniciarSesion(datos.nombreUsuario(), datos.contrasena());
        String token = Sesion.abrir(usuario);
        return Map.of("token", token,
                "nombreUsuario", usuario.getNombreUsuario(),
                "rol", usuario.getRol().getNombre());
    }

    /** DELETE /api/sesion → cierra la sesión. */
    @DeleteMapping
    public Map<String, String> cerrarSesion(@RequestHeader(value = "Authorization", required = false) String autorizacion) {
        if (autorizacion != null && autorizacion.startsWith("Bearer ")) {
            Sesion.cerrar(autorizacion.substring(7));
        }
        return Map.of("mensaje", "Sesión cerrada");
    }

    record DatosRecuperar(String nombreUsuario) {}

    /**
     * POST /api/sesion/recuperar → genera una contraseña temporal y la envía
     * por correo. La contraseña solo cambia si el correo se pudo enviar.
     */
    @PostMapping("/recuperar")
    public Map<String, String> recuperarContrasena(@RequestBody DatosRecuperar datos) {
        Usuario usuario = nucleo.buscarUsuario(datos.nombreUsuario());
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
        return Map.of("mensaje", "Se envió una contraseña temporal a su correo");
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
