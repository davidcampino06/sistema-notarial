package notaria.modelo;

import notaria.logica.Configuracion;

/** Poder especial: tiene una tarifa fija. */
public class PoderEspecial extends Tramite {

    private String objetoPoder;

    public PoderEspecial(String objetoPoder) {
        if (objetoPoder == null || objetoPoder.isBlank()) {
            throw new IllegalArgumentException("El objeto del poder es obligatorio");
        }
        this.objetoPoder = objetoPoder;
    }

    @Override
    public double calcularDerechos() {
        return Configuracion.getInstancia().getTarifa(Configuracion.TARIFA_PODER);
    }

    @Override
    public String getTipo() { return "Poder especial"; }

    @Override
    public String getDetalle() { return "Objeto del poder: " + objetoPoder; }

    public String getObjetoPoder() { return objetoPoder; }
}
