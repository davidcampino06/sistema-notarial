package notaria.modelo;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Clase ABSTRACTA: no existe un "trámite genérico", siempre es uno de los
 * 4 tipos (Autenticacion, DeclaracionExtrajuicio, PoderEspecial,
 * EscrituraCompraventa). Cada tipo hereda de aquí y define su propio
 * calcularDerechos() -> eso es POLIMORFISMO.
 */
public abstract class Tramite {

    private String numeroRadicado;
    private Date fechaRadicacion;
    private EstadoTramite estado;

    // Asociaciones de la vista lógica
    private List<Cliente> clientes = new ArrayList<>();              // Cliente 1..* solicita Tramite
    private Usuario digitador;                                       // asignado a 0..1 Usuario
    private List<DocumentoAdjunto> documentos = new ArrayList<>();   // contiene 0..* documentos
    private Liquidacion liquidacion;                                 // genera 0..1 Liquidacion

    // Texto que escribe el notario al devolver (campoObservaciones de VentanaRevision)
    // o el motivo al anular.
    private String observaciones;

    protected Tramite() {
        this.estado = EstadoTramite.RADICADO;
    }

    /** Cada tipo de trámite calcula sus derechos notariales a su manera. */
    public abstract double calcularDerechos();

    /** Nombre del tipo de trámite para mostrar en pantalla. */
    public abstract String getTipo();

    /** Dato propio del tipo (folios, contenido, objeto, valor) para mostrar en pantalla. */
    public abstract String getDetalle();

    /**
     * Pasa el trámite al siguiente estado del flujo normal:
     * Radicado -> En elaboración -> En revisión -> Aprobado -> Firmado -> Entregado
     * Devuelto -> En elaboración (cuando el digitador corrige)
     */
    public void avanzar() {
        switch (estado) {
            case RADICADO:
            case DEVUELTO:
                estado = EstadoTramite.EN_ELABORACION;
                break;
            case EN_ELABORACION:
                estado = EstadoTramite.EN_REVISION;
                break;
            case EN_REVISION:
                estado = EstadoTramite.APROBADO;
                break;
            case APROBADO:
                estado = EstadoTramite.FIRMADO;
                break;
            case FIRMADO:
                estado = EstadoTramite.ENTREGADO;
                break;
            default:
                throw new IllegalStateException("Un trámite " + estado.getTexto() + " no puede avanzar");
        }
    }

    public void agregarCliente(Cliente cliente) {
        clientes.add(cliente);
    }

    public void agregarDocumento(DocumentoAdjunto documento) {
        documentos.add(documento);
    }

    // ----- getters y setters -----

    public String getNumeroRadicado() { return numeroRadicado; }
    public void setNumeroRadicado(String numeroRadicado) { this.numeroRadicado = numeroRadicado; }
    public Date getFechaRadicacion() { return fechaRadicacion; }
    public void setFechaRadicacion(Date fechaRadicacion) { this.fechaRadicacion = fechaRadicacion; }
    public EstadoTramite getEstado() { return estado; }
    public void setEstado(EstadoTramite estado) { this.estado = estado; }
    public List<Cliente> getClientes() { return clientes; }
    public Usuario getDigitador() { return digitador; }
    public void setDigitador(Usuario digitador) { this.digitador = digitador; }
    public List<DocumentoAdjunto> getDocumentos() { return documentos; }
    public Liquidacion getLiquidacion() { return liquidacion; }
    public void setLiquidacion(Liquidacion liquidacion) { this.liquidacion = liquidacion; }
    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }
}
