package notaria.logica;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.UUID;
import notaria.modelo.Rol;
import notaria.modelo.Usuario;

/**
 * Sesión de un funcionario que ya inició sesión (vista de desarrollo):
 * guarda el usuarioActual y su rol.
 *
 * Al iniciar sesión se crea un código secreto (token). React lo guarda y
 * lo envía en cada petición (encabezado "Authorization: Bearer <token>");
 * con ese código se encuentra la sesión.
 */
public class Sesion {

    private static final Map<String, Sesion> sesionesActivas = new ConcurrentHashMap<>();

    private Usuario usuarioActual;
    private Rol rol;

    private Sesion(Usuario usuario) {
        this.usuarioActual = usuario;
        this.rol = usuario.getRol();
    }

    /** Crea una sesión para el usuario y devuelve su código (token). */
    public static String abrir(Usuario usuario) {
        String token = UUID.randomUUID().toString();
        sesionesActivas.put(token, new Sesion(usuario));
        return token;
    }

    /** Busca la sesión de un token; devuelve null si no existe. */
    public static Sesion buscar(String token) {
        return token == null ? null : sesionesActivas.get(token);
    }

    public static void cerrar(String token) {
        sesionesActivas.remove(token);
    }

    public Usuario getUsuarioActual() { return usuarioActual; }
    public Rol getRol() { return rol; }
}
