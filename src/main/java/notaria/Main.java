package notaria;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import notaria.ventanas.*;

/**
 * Punto de entrada del programa (vista de desarrollo).
 * 1. inicializarNucleo(): crea el Núcleo y se conecta a MongoDB.
 * 2. abrirVentanaLogin(): arranca el servidor web; la primera página es el login.
 */
public class Main {

    private static Nucleo nucleo;

    public static void main(String[] args) throws IOException {
        inicializarNucleo();
        abrirVentanaLogin();
    }

    public static void inicializarNucleo() {
        String uri = System.getenv("MONGODB_URI");          // cadena de conexión de MongoDB Atlas
        String nombreBd = System.getenv("MONGODB_DB");
        nucleo = new Nucleo(uri, nombreBd == null ? "notaria" : nombreBd);
    }

    public static void abrirVentanaLogin() throws IOException {
        String puerto = System.getenv("PORT");               // Render dice en qué puerto escuchar
        int numeroPuerto = puerto == null ? 8080 : Integer.parseInt(puerto);

        HttpServer servidor = HttpServer.create(new InetSocketAddress("0.0.0.0", numeroPuerto), 0);

        // Cada dirección (ruta) la atiende una ventana
        servidor.createContext("/", new VentanaLogin(nucleo));
        servidor.createContext("/principal", new VentanaPrincipal(nucleo));
        servidor.createContext("/radicacion", new VentanaRadicacion(nucleo));
        servidor.createContext("/revision", new VentanaRevision(nucleo));
        servidor.createContext("/caja", new VentanaCaja(nucleo));
        servidor.createContext("/administracion", new VentanaAdministracion(nucleo));
        servidor.createContext("/consulta", new VentanaConsulta(nucleo));

        // Sin "executor" las peticiones se atienden una por una (más simple y seguro)
        servidor.setExecutor(null);
        servidor.start();
        System.out.println("Sistema de Gestión Notarial listo en http://localhost:" + numeroPuerto);
    }
}
