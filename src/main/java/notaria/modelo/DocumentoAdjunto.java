package notaria.modelo;

import java.util.Date;

/**
 * Documento que acompaña un trámite. Composición: un Trámite "contiene"
 * 0..* documentos y cada documento pertenece a un solo trámite (vista lógica).
 * Según el diagrama solo se registra el nombre del archivo y la fecha de carga.
 */
public class DocumentoAdjunto {

    private String nombreArchivo;
    private Date fechaCarga;

    public DocumentoAdjunto(String nombreArchivo, Date fechaCarga) {
        this.nombreArchivo = nombreArchivo;
        this.fechaCarga = fechaCarga;
    }

    public String getNombreArchivo() { return nombreArchivo; }
    public Date getFechaCarga() { return fechaCarga; }
}
