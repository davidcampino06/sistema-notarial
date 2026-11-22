package notaria.controladores;

import notaria.logica.Nucleo;
import notaria.logica.Sesion;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Clase PADRE de todos los controladores (HERENCIA). Tiene el Núcleo y
 * el método que revisa la sesión y el rol de quien hace la petición.
 */
public abstract class Controlador {

    protected final Nucleo nucleo;

    protected Controlador(Nucleo nucleo) {
        this.nucleo = nucleo;
    }

    /**
     * Revisa el token que envía React ("Bearer <token>") y que el rol esté
     * entre los permitidos. Si no se pasan roles, sirve cualquier rol.
     */
    protected Sesion sesion(String autorizacion, String... rolesPermitidos) {
        String token = (autorizacion != null && autorizacion.startsWith("Bearer "))
                ? autorizacion.substring(7) : null;
        Sesion sesion = Sesion.buscar(token);
        if (sesion == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Debe iniciar sesión");
        }
        if (rolesPermitidos.length > 0 && !sesion.getRol().es(rolesPermitidos)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Su rol no tiene acceso a esta acción");
        }
        return sesion;
    }
}
