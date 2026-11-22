package notaria.logica;

import java.util.ArrayList;
import java.util.List;
import notaria.modelo.Cliente;

/** Lista de todos los clientes de la notaría (vista de desarrollo). */
public class ListaClientes {

    private List<Cliente> clientes = new ArrayList<>();

    /** Busca un cliente por su número de documento; devuelve null si no existe. */
    public Cliente buscar(String numeroDocumento) {
        for (Cliente c : clientes) {
            if (c.getNumeroDocumento().equals(numeroDocumento)) {
                return c;
            }
        }
        return null;
    }

    public void agregar(Cliente cliente) {
        if (buscar(cliente.getNumeroDocumento()) != null) {
            throw new IllegalArgumentException("Ya existe un cliente con documento " + cliente.getNumeroDocumento());
        }
        clientes.add(cliente);
    }

    public List<Cliente> getClientes() {
        return clientes;
    }
}
