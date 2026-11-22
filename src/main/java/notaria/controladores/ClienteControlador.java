package notaria.controladores;

import java.util.List;
import notaria.logica.Nucleo;
import notaria.modelo.Cliente;
import notaria.modelo.Rol;
import org.springframework.web.bind.annotation.*;

/**
 * Componente "Gestión de Clientes" de la vista funcional: entrega los
 * datos del cliente. Lo usa el buscadorCliente de VentanaRadicacion.
 */
@RestController
@RequestMapping("/api/clientes")
public class ClienteControlador extends Controlador {

    public ClienteControlador(Nucleo nucleo) {
        super(nucleo);
    }

    /** GET /api/clientes/{numeroDocumento} → busca un cliente en ListaClientes. */
    @GetMapping("/{numeroDocumento}")
    public Cliente buscarCliente(@RequestHeader(value = "Authorization", required = false) String autorizacion,
                                 @PathVariable String numeroDocumento) {
        sesion(autorizacion, Rol.RECEPCIONISTA);
        Cliente cliente = nucleo.getListaClientes().buscar(numeroDocumento.trim());
        if (cliente == null) {
            throw new IllegalArgumentException("No existe un cliente con ese documento");
        }
        return cliente;
    }

    /** GET /api/clientes → todos los clientes (los reportes del administrador los cuentan). */
    @GetMapping
    public List<Cliente> listarClientes(@RequestHeader(value = "Authorization", required = false) String autorizacion) {
        sesion(autorizacion, Rol.ADMINISTRADOR);
        return nucleo.getListaClientes().getClientes();
    }
}
