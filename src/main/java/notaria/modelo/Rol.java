package notaria.modelo;

/**
 * Rol de un usuario. Un Usuario "tiene" exactamente 1 Rol (vista lógica).
 * El nombre del rol decide qué ventanas y qué acciones puede usar.
 */
public class Rol {

    // Nombres de los roles del sistema
    public static final String RECEPCIONISTA = "Recepcionista";
    public static final String DIGITADOR = "Digitador";
    public static final String NOTARIO = "Notario";
    public static final String CAJERO = "Cajero";
    public static final String ADMINISTRADOR = "Administrador";

    public static final String[] TODOS = {RECEPCIONISTA, DIGITADOR, NOTARIO, CAJERO, ADMINISTRADOR};

    private String nombre;

    public Rol(String nombre) {
        for (String valido : TODOS) {
            if (valido.equals(nombre)) {
                this.nombre = nombre;
                return;
            }
        }
        throw new IllegalArgumentException("Rol no válido: " + nombre);
    }

    public String getNombre() {
        return nombre;
    }

    /** Dice si este rol es alguno de los roles recibidos. */
    public boolean es(String... nombres) {
        for (String n : nombres) {
            if (n.equals(nombre)) {
                return true;
            }
        }
        return false;
    }
}
