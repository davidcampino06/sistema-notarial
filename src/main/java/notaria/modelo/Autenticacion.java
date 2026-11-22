package notaria.modelo;

import notaria.logica.Configuracion;

/** Autenticación de documentos: se cobra por cada folio (hoja). */
public class Autenticacion extends Tramite {

    private int numeroFolios;

    public Autenticacion(int numeroFolios) {
        if (numeroFolios <= 0) {
            throw new IllegalArgumentException("El número de folios debe ser mayor que 0");
        }
        this.numeroFolios = numeroFolios;
    }

    @Override
    public double calcularDerechos() {
        double tarifaPorFolio = Configuracion.getInstancia().getTarifa(Configuracion.TARIFA_AUTENTICACION_FOLIO);
        return numeroFolios * tarifaPorFolio;
    }

    @Override
    public String getTipo() { return "Autenticación"; }

    @Override
    public String getDetalle() { return "Folios: " + numeroFolios; }

    public int getNumeroFolios() { return numeroFolios; }
}
