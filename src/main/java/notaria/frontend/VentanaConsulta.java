package notaria.frontend;

import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.util.Map;
import notaria.backend.Nucleo;
import notaria.backend.Sesion;
import notaria.backend.modelo.Cliente;
import notaria.backend.modelo.Tramite;

/**
 * Ventana PÚBLICA de consulta (vista de desarrollo): el cliente, sin iniciar
 * sesión, escribe su número de radicado y su documento y ve el estado de su
 * trámite (flujo "Radicado y estado del trámite" de la vista de contexto).
 * Atributos: campoRadicado, campoDocumento.
 * Método: consultarEstado().
 */
public class VentanaConsulta extends Ventana {

    public VentanaConsulta(Nucleo nucleo) {
        super(nucleo);
    }

    @Override
    protected boolean requiereSesion() {
        return false;   // la puede usar cualquier persona
    }

    @Override
    protected void manejar(HttpExchange ex, Sesion sesion) throws IOException {
        if (!subRuta(ex).equals("/")) {
            noEncontrado(ex, sesion);
            return;
        }
        Map<String, String> datos = parametros(ex);
        String radicado = datos.getOrDefault("campoRadicado", "");
        String documento = datos.getOrDefault("campoDocumento", "");

        String resultado = "";
        if (!radicado.isEmpty() || !documento.isEmpty()) {
            resultado = consultarEstado(radicado, documento);
        }

        String html = "<form method='get' action='/consulta'>"
                // campoRadicado
                + "<label for='campoRadicado'>Número de radicado</label>"
                + "<div class='campo'>" + ICONO_DOCUMENTO
                + "<input id='campoRadicado' name='campoRadicado' required placeholder='Ej: 2026-000001' value='"
                + esc(radicado) + "'></div>"
                // campoDocumento
                + "<label for='campoDocumento'>Documento del cliente</label>"
                + "<div class='campo'>" + ICONO_TARJETA
                + "<input id='campoDocumento' name='campoDocumento' required placeholder='Número de cédula o NIT' value='"
                + esc(documento) + "'></div>"
                + "<button class='boton-principal'>Consultar " + ICONO_FLECHA + "</button></form>"
                + resultado
                + "<p class='pie'><a href='/'>Ingreso de funcionarios</a></p>";
        responder(ex, paginaPublica("Consultar trámite", "Vea en qué estado está su trámite", html));
    }

    /**
     * Busca el trámite y solo muestra el estado si el documento corresponde a
     * uno de sus clientes (así nadie ve trámites ajenos).
     */
    private String consultarEstado(String radicado, String documento) {
        Tramite tramite;
        try {
            tramite = nucleo.buscarTramite(radicado.trim());
        } catch (IllegalArgumentException e) {
            tramite = null;
        }
        boolean esCliente = false;
        if (tramite != null) {
            for (Cliente c : tramite.getClientes()) {
                if (c.getNumeroDocumento().equals(documento.trim())) {
                    esCliente = true;
                }
            }
        }
        if (!esCliente) {
            return "<div class='alerta error'>No encontramos un trámite con ese radicado "
                    + "y ese documento. Revise los datos.</div>";
        }
        return "<div class='resultado-consulta'>"
                + "<b>" + esc(tramite.getTipo()) + "</b> · " + esc(tramite.getNumeroRadicado()) + "<br>"
                + "Radicado el " + fecha(tramite.getFechaRadicacion()) + "<br>"
                + "Estado actual: " + VentanaPrincipal.estado(tramite) + "</div>";
    }
}
