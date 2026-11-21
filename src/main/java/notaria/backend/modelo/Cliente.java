package notaria.backend.modelo;

/** Persona que solicita trámites en la notaría (vista lógica). */
public class Cliente {

    private String numeroDocumento;
    private String nombres;
    private String telefono;
    private String tipoDocumento;   // CC, CE, NIT, PAS...
    private String correo;          // para notificarle el estado del trámite (opcional)

    public Cliente(String numeroDocumento, String nombres, String telefono, String tipoDocumento, String correo) {
        if (numeroDocumento == null || numeroDocumento.isBlank()) {
            throw new IllegalArgumentException("El número de documento del cliente es obligatorio");
        }
        if (nombres == null || nombres.isBlank()) {
            throw new IllegalArgumentException("Los nombres del cliente son obligatorios");
        }
        this.numeroDocumento = numeroDocumento.trim();
        this.nombres = nombres.trim();
        this.telefono = telefono;
        this.tipoDocumento = tipoDocumento;
        this.correo = (correo == null || correo.isBlank()) ? null : correo.trim();
    }

    public String getNumeroDocumento() { return numeroDocumento; }
    public String getNombres() { return nombres; }
    public String getTelefono() { return telefono; }
    public String getTipoDocumento() { return tipoDocumento; }
    public String getCorreo() { return correo; }

    public boolean tieneCorreo() {
        return correo != null;
    }
}
