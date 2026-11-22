package notaria.controladores;

import java.util.List;
import java.util.Map;
import notaria.logica.Configuracion;
import notaria.logica.Nucleo;
import notaria.logica.Sesion;
import notaria.modelo.Rol;
import notaria.modelo.Usuario;
import org.springframework.web.bind.annotation.*;

/**
 * Interfaz "configurar sistema" (Administrador) de la vista funcional.
 * Lo usa VentanaAdministracion: gestionarUsuarios() y configurarTarifas().
 */
@RestController
@RequestMapping("/api")
public class AdministracionControlador extends Controlador {

    public AdministracionControlador(Nucleo nucleo) {
        super(nucleo);
    }

    /** GET /api/usuarios → lista de usuarios (sin contraseñas). */
    @GetMapping("/usuarios")
    public List<Usuario> listarUsuarios(@RequestHeader(value = "Authorization", required = false) String autorizacion) {
        sesion(autorizacion, Rol.ADMINISTRADOR);
        return nucleo.getUsuarios();
    }

    record NuevoUsuario(String nombreUsuario, String correo, String rol, String contrasena) {}

    /** POST /api/usuarios → crea un usuario. */
    @PostMapping("/usuarios")
    public Usuario crearUsuario(@RequestHeader(value = "Authorization", required = false) String autorizacion,
                                @RequestBody NuevoUsuario datos) {
        sesion(autorizacion, Rol.ADMINISTRADOR);
        Usuario nuevo = new Usuario(datos.nombreUsuario(), datos.correo(), new Rol(datos.rol()));
        nuevo.cambiarContrasena(datos.contrasena());
        nucleo.agregarUsuario(nuevo);
        return nuevo;
    }

    record CambioUsuario(String accion, String contrasena) {}

    /** PUT /api/usuarios/{nombreUsuario} → activar, desactivar o cambiar contraseña. */
    @PutMapping("/usuarios/{nombreUsuario}")
    public Usuario gestionarUsuario(@RequestHeader(value = "Authorization", required = false) String autorizacion,
                                    @PathVariable String nombreUsuario, @RequestBody CambioUsuario cambio) {
        Sesion sesion = sesion(autorizacion, Rol.ADMINISTRADOR);
        Usuario u = nucleo.buscarUsuario(nombreUsuario);
        if (u == null) {
            throw new IllegalArgumentException("No existe el usuario");
        }
        switch (cambio.accion() == null ? "" : cambio.accion()) {
            case "activar":
                u.setActivo(true);
                break;
            case "desactivar":
                if (u.getNombreUsuario().equals(sesion.getUsuarioActual().getNombreUsuario())) {
                    throw new IllegalArgumentException("No puede desactivarse a sí mismo");
                }
                u.setActivo(false);
                break;
            case "contrasena":
                u.cambiarContrasena(cambio.contrasena());
                break;
            default:
                throw new IllegalArgumentException("Acción no válida");
        }
        nucleo.guardarBaseDatos(u);
        return u;
    }

    /** GET /api/tarifas → tarifas vigentes. */
    @GetMapping("/tarifas")
    public Map<String, Double> tarifas(@RequestHeader(value = "Authorization", required = false) String autorizacion) {
        sesion(autorizacion, Rol.ADMINISTRADOR);
        return Configuracion.getInstancia().getTarifasVigentes();
    }

    /** PUT /api/tarifas → guarda los nuevos valores de las tarifas. */
    @PutMapping("/tarifas")
    public Map<String, Double> configurarTarifas(@RequestHeader(value = "Authorization", required = false) String autorizacion,
                                                 @RequestBody Map<String, Double> nuevas) {
        sesion(autorizacion, Rol.ADMINISTRADOR);
        Configuracion configuracion = Configuracion.getInstancia();
        for (Map.Entry<String, Double> e : nuevas.entrySet()) {
            if (e.getValue() != null) {
                configuracion.setTarifa(e.getKey(), e.getValue());
            }
        }
        nucleo.guardarBaseDatos(configuracion);
        return configuracion.getTarifasVigentes();
    }
}
