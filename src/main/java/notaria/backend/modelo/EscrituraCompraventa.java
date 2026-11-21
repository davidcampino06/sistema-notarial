package notaria.backend.modelo;

import notaria.backend.Configuracion;

/** Escritura de compraventa: se cobra un porcentaje del valor del inmueble. */
public class EscrituraCompraventa extends Tramite {

    private double valorInmueble;

    public EscrituraCompraventa(double valorInmueble) {
        if (valorInmueble <= 0) {
            throw new IllegalArgumentException("El valor del inmueble debe ser mayor que 0");
        }
        this.valorInmueble = valorInmueble;
    }

    @Override
    public double calcularDerechos() {
        double porcentaje = Configuracion.getInstancia().getTarifa(Configuracion.TARIFA_COMPRAVENTA_PORCENTAJE);
        return Math.round(valorInmueble * porcentaje / 100.0);
    }

    @Override
    public String getTipo() { return "Escritura de compraventa"; }

    @Override
    public String getDetalle() { return "Valor del inmueble: $ " + String.format("%,.0f", valorInmueble); }

    public double getValorInmueble() { return valorInmueble; }
}
