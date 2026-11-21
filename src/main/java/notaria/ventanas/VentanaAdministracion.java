package notaria.ventanas;

import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.util.Map;
import notaria.Configuracion;
import notaria.Nucleo;
import notaria.Sesion;
import notaria.modelo.Rol;
import notaria.modelo.Usuario;

/**
 * Ventana de administración (vista de desarrollo). La usa el Administrador.
 * Atributos: tablaUsuarios, tablaTarifas.
 * Métodos: gestionarUsuarios(), configurarTarifas().
 */
public class VentanaAdministracion extends Ventana {

    public VentanaAdministracion(Nucleo nucleo) {
        super(nucleo, Rol.ADMINISTRADOR);
    }

    @Override
    protected void manejar(HttpExchange ex, Sesion sesion) throws IOException {
        String ruta = subRuta(ex);
        if (ruta.equals("/") && !esPost(ex)) {
            mostrar(ex, sesion);
        } else if (ruta.equals("/usuarios") && esPost(ex)) {
            gestionarUsuarios(ex, sesion);
        } else if (ruta.equals("/tarifas") && esPost(ex)) {
            configurarTarifas(ex);
        } else {
            noEncontrado(ex, sesion);
        }
    }

    private void mostrar(HttpExchange ex, Sesion sesion) throws IOException {
        // ---- tablaUsuarios ----
        StringBuilder tablaUsuarios = new StringBuilder("<table><tr><th>Usuario</th><th>Correo</th><th>Rol</th>"
                + "<th>Estado</th><th>Acciones</th></tr>");
        for (Usuario u : nucleo.getUsuarios()) {
            String n = "<input type='hidden' name='nombreUsuario' value='" + esc(u.getNombreUsuario()) + "'>";
            tablaUsuarios.append("<tr><td>").append(esc(u.getNombreUsuario())).append("</td><td>").append(esc(u.getCorreo()))
                .append("</td><td>").append(esc(u.getRol().getNombre())).append("</td><td>")
                .append(u.isActivo() ? "Activo" : "<span class='suave'>Inactivo</span>").append("</td><td class='acciones'>")
                .append("<form method='post' action='/administracion/usuarios'>").append(n)
                .append("<input type='hidden' name='accion' value='").append(u.isActivo() ? "desactivar" : "activar").append("'>")
                .append("<button class='secundario'>").append(u.isActivo() ? "Desactivar" : "Activar").append("</button></form>")
                .append("<form method='post' action='/administracion/usuarios'>").append(n)
                .append("<input type='hidden' name='accion' value='contrasena'>")
                .append("<input name='contrasena' type='password' placeholder='Nueva contraseña' required style='width:150px'>")
                .append("<button class='secundario'>Cambiar contraseña</button></form></td></tr>");
        }
        tablaUsuarios.append("</table>");

        StringBuilder roles = new StringBuilder();
        for (String r : Rol.TODOS) {
            roles.append("<option>").append(r).append("</option>");
        }

        // ---- tablaTarifas ----
        StringBuilder tablaTarifas = new StringBuilder();
        for (Map.Entry<String, Double> e : Configuracion.getInstancia().getTarifasVigentes().entrySet()) {
            tablaTarifas.append("<div><label>").append(esc(e.getKey())).append("</label><input name='")
                .append(esc(e.getKey())).append("' type='number' step='any' min='0' value='")
                .append(e.getValue()).append("'></div>");
        }

        String html = mensajeOk(ex)
                + "<div class='tarjeta'><h2>Usuarios</h2><div class='tabla'>" + tablaUsuarios + "</div></div>"
                + "<form class='tarjeta' method='post' action='/administracion/usuarios'><h2>Nuevo usuario</h2>"
                + "<input type='hidden' name='accion' value='crear'><div class='fila'>"
                + "<div><label>Nombre de usuario</label><input name='nombreUsuario' required></div>"
                + "<div><label>Correo</label><input name='correo' type='email'></div>"
                + "<div><label>Rol</label><select name='rol'>" + roles + "</select></div>"
                + "<div><label>Contraseña</label><input name='contrasena' type='password' required></div>"
                + "</div><button>Crear usuario</button></form>"
                + "<form class='tarjeta' method='post' action='/administracion/tarifas'><h2>Tarifas vigentes</h2>"
                + "<div class='fila'>" + tablaTarifas + "</div><button>Guardar tarifas</button></form>";
        responder(ex, pagina("Administración", sesion, html));
    }

    /** Crear usuarios, activarlos/desactivarlos y cambiarles la contraseña. */
    private void gestionarUsuarios(HttpExchange ex, Sesion sesion) throws IOException {
        Map<String, String> datos = formulario(ex);
        String accion = datos.get("accion");

        if ("crear".equals(accion)) {
            Usuario nuevo = new Usuario(datos.get("nombreUsuario"), datos.get("correo"), new Rol(datos.get("rol")));
            nuevo.cambiarContrasena(datos.get("contrasena"));
            nucleo.agregarUsuario(nuevo);
            redirigir(ex, "/administracion", "Usuario " + nuevo.getNombreUsuario() + " creado");
            return;
        }

        Usuario u = nucleo.buscarUsuario(datos.get("nombreUsuario"));
        if (u == null) {
            throw new IllegalArgumentException("No existe el usuario");
        }
        switch (accion) {
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
                u.cambiarContrasena(datos.get("contrasena"));
                break;
            default:
                throw new IllegalArgumentException("Acción no válida");
        }
        nucleo.guardarBaseDatos(u);
        redirigir(ex, "/administracion", "Usuario " + u.getNombreUsuario() + " actualizado");
    }

    /** Guarda los nuevos valores de las tarifas. */
    private void configurarTarifas(HttpExchange ex) throws IOException {
        Map<String, String> datos = formulario(ex);
        Configuracion configuracion = Configuracion.getInstancia();
        for (String nombre : configuracion.getTarifasVigentes().keySet()) {
            String valor = datos.get(nombre);
            if (valor != null && !valor.isEmpty()) {
                try {
                    configuracion.setTarifa(nombre, Double.parseDouble(valor));
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Valor no válido para " + nombre);
                }
            }
        }
        nucleo.guardarBaseDatos(configuracion);
        redirigir(ex, "/administracion", "Tarifas guardadas");
    }
}
