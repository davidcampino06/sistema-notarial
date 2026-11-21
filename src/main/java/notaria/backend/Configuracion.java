package notaria.backend;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Configuración del sistema (vista de desarrollo): tarifas vigentes y
 * servidor de correo. El correo se envía por la API web de Brevo (HTTPS),
 * porque el plan gratis de Render bloquea el correo por SMTP.
 * Usa el patrón SINGLETON: existe una sola
 * configuración para todo el programa y se obtiene con getInstancia().
 */
public class Configuracion {

    // Nombres de las tarifas
    public static final String TARIFA_AUTENTICACION_FOLIO = "Autenticación (valor por folio)";
    public static final String TARIFA_DECLARACION = "Declaración extrajuicio (valor fijo)";
    public static final String TARIFA_PODER = "Poder especial (valor fijo)";
    public static final String TARIFA_COMPRAVENTA_PORCENTAJE = "Compraventa (% del valor del inmueble)";

    private static Configuracion instancia;

    private Map<String, Double> tarifasVigentes = new LinkedHashMap<>();
    private String servidorCorreo;      // dirección de la API de correo
    private String claveCorreo;         // clave de la API (variable BREVO_API_KEY)
    private String remitenteCorreo;     // correo verificado en Brevo (variable CORREO_REMITENTE)

    private Configuracion() {
        // Valores de ejemplo; el administrador los cambia en VentanaAdministracion
        tarifasVigentes.put(TARIFA_AUTENTICACION_FOLIO, 2500.0);
        tarifasVigentes.put(TARIFA_DECLARACION, 15000.0);
        tarifasVigentes.put(TARIFA_PODER, 20000.0);
        tarifasVigentes.put(TARIFA_COMPRAVENTA_PORCENTAJE, 0.3);
        // El servidor de correo se configura con variables de entorno (ver README)
        String url = System.getenv("CORREO_API_URL");
        servidorCorreo = (url == null || url.isBlank()) ? "https://api.brevo.com/v3/smtp/email" : url;
        claveCorreo = System.getenv("BREVO_API_KEY");
        remitenteCorreo = System.getenv("CORREO_REMITENTE");
    }

    public static Configuracion getInstancia() {
        if (instancia == null) {
            instancia = new Configuracion();
        }
        return instancia;
    }

    public double getTarifa(String nombre) {
        Double valor = tarifasVigentes.get(nombre);
        if (valor == null) {
            throw new IllegalStateException("No existe la tarifa: " + nombre);
        }
        return valor;
    }

    public void setTarifa(String nombre, double valor) {
        if (!tarifasVigentes.containsKey(nombre)) {
            throw new IllegalArgumentException("No existe la tarifa: " + nombre);
        }
        if (valor < 0) {
            throw new IllegalArgumentException("La tarifa no puede ser negativa");
        }
        tarifasVigentes.put(nombre, valor);
    }

    public Map<String, Double> getTarifasVigentes() {
        return tarifasVigentes;
    }

    public String getServidorCorreo() {
        return servidorCorreo;
    }

    public String getClaveCorreo() {
        return claveCorreo;
    }

    public String getRemitenteCorreo() {
        return remitenteCorreo;
    }

    /** Hay correo configurado si existen la clave de la API y el remitente. */
    public boolean hayServidorCorreo() {
        return claveCorreo != null && !claveCorreo.isBlank()
                && remitenteCorreo != null && !remitenteCorreo.isBlank();
    }
}
