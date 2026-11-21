package notaria.ventanas;

import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import notaria.Configuracion;
import notaria.Nucleo;
import notaria.Sesion;
import notaria.modelo.EstadoTramite;
import notaria.modelo.Liquidacion;
import notaria.modelo.MedioPago;
import notaria.modelo.Rol;
import notaria.modelo.Tramite;
import notaria.modelo.Usuario;

/**
 * Ventana de administración (vista de desarrollo). La usa el Administrador.
 * Atributos: tablaUsuarios, tablaTarifas.
 * Métodos: gestionarUsuarios(), configurarTarifas(), generarReportes().
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
        } else if (ruta.equals("/reportes") && !esPost(ex)) {
            generarReportes(ex, sesion);
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
                + "<a class='boton' href='/administracion/reportes' style='margin:0 0 16px'>Ver reportes</a>"
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

    /**
     * Reportes para el administrador (flujo "Reportes" de la vista de contexto):
     * trámites por estado y por tipo, y dinero recaudado por medio de pago.
     */
    private void generarReportes(HttpExchange ex, Sesion sesion) throws IOException {
        Map<EstadoTramite, Integer> porEstado = new LinkedHashMap<>();
        for (EstadoTramite e : EstadoTramite.values()) {
            porEstado.put(e, 0);
        }
        Map<String, Integer> cantidadPorTipo = new LinkedHashMap<>();
        Map<String, Double> recaudoPorTipo = new LinkedHashMap<>();
        Map<MedioPago, Double> recaudoPorMedio = new LinkedHashMap<>();
        for (MedioPago m : MedioPago.values()) {
            recaudoPorMedio.put(m, 0.0);
        }
        double totalRecaudado = 0;
        int pagados = 0;

        for (Tramite t : nucleo.getTramites()) {
            porEstado.put(t.getEstado(), porEstado.get(t.getEstado()) + 1);
            cantidadPorTipo.put(t.getTipo(), cantidadPorTipo.getOrDefault(t.getTipo(), 0) + 1);
            Liquidacion l = t.getLiquidacion();
            if (l != null && l.estaPagada()) {
                double valor = l.getPago().getValor();
                recaudoPorTipo.put(t.getTipo(), recaudoPorTipo.getOrDefault(t.getTipo(), 0.0) + valor);
                recaudoPorMedio.put(l.getPago().getMedioPago(), recaudoPorMedio.get(l.getPago().getMedioPago()) + valor);
                totalRecaudado += valor;
                pagados++;
            }
        }

        StringBuilder filasEstado = new StringBuilder();
        for (Map.Entry<EstadoTramite, Integer> e : porEstado.entrySet()) {
            filasEstado.append("<tr><td>").append(e.getKey().getTexto()).append("</td><td>")
                .append(e.getValue()).append("</td></tr>");
        }
        StringBuilder filasTipo = new StringBuilder();
        for (Map.Entry<String, Integer> e : cantidadPorTipo.entrySet()) {
            filasTipo.append("<tr><td>").append(esc(e.getKey())).append("</td><td>").append(e.getValue())
                .append("</td><td>").append(dinero(recaudoPorTipo.getOrDefault(e.getKey(), 0.0))).append("</td></tr>");
        }
        if (cantidadPorTipo.isEmpty()) {
            filasTipo.append("<tr><td colspan='3' class='suave'>Aún no hay trámites.</td></tr>");
        }
        StringBuilder filasMedio = new StringBuilder();
        for (Map.Entry<MedioPago, Double> e : recaudoPorMedio.entrySet()) {
            filasMedio.append("<tr><td>").append(e.getKey()).append("</td><td>").append(dinero(e.getValue()))
                .append("</td></tr>");
        }

        String html = "<div class='fila'>"
                + "<div class='tarjeta'><h2>Resumen</h2><p>Trámites radicados: <b>" + nucleo.getTramites().size()
                + "</b><br>Clientes registrados: <b>" + nucleo.getListaClientes().getClientes().size()
                + "</b><br>Trámites pagados: <b>" + pagados
                + "</b><br>Total recaudado: <b>" + dinero(totalRecaudado) + "</b></p></div>"
                + "<div class='tarjeta'><h2>Trámites por estado</h2><table><tr><th>Estado</th><th>Cantidad</th></tr>"
                + filasEstado + "</table></div></div>"
                + "<div class='fila'>"
                + "<div class='tarjeta'><h2>Por tipo de trámite</h2><table><tr><th>Tipo</th><th>Cantidad</th><th>Recaudado</th></tr>"
                + filasTipo + "</table></div>"
                + "<div class='tarjeta'><h2>Recaudo por medio de pago</h2><table><tr><th>Medio</th><th>Total</th></tr>"
                + filasMedio + "<tr><th>TOTAL</th><th>" + dinero(totalRecaudado) + "</th></tr></table></div></div>"
                + "<div class='no-imprimir'><button onclick='window.print()'>Imprimir</button> "
                + "<a class='boton secundario' href='/administracion'>Volver a administración</a></div>";
        responder(ex, pagina("Reportes", sesion, html));
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
