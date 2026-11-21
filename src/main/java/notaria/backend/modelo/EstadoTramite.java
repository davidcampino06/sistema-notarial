package notaria.backend.modelo;

/**
 * Estados por los que pasa un trámite (enumeración de la vista lógica).
 * Cada estado guarda el texto bonito que se muestra en pantalla.
 */
public enum EstadoTramite {
    RADICADO("Radicado"),
    EN_ELABORACION("En elaboración"),
    EN_REVISION("En revisión"),
    DEVUELTO("Devuelto"),
    APROBADO("Aprobado"),
    FIRMADO("Firmado"),
    ENTREGADO("Entregado"),
    ANULADO("Anulado");

    private final String texto;

    EstadoTramite(String texto) {
        this.texto = texto;
    }

    public String getTexto() {
        return texto;
    }
}
