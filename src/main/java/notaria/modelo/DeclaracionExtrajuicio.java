package notaria.modelo;

import notaria.Configuracion;

/** Declaración extrajuicio: tiene una tarifa fija. */
public class DeclaracionExtrajuicio extends Tramite {

    private String contenido;

    public DeclaracionExtrajuicio(String contenido) {
        if (contenido == null || contenido.isBlank()) {
            throw new IllegalArgumentException("El contenido de la declaración es obligatorio");
        }
        this.contenido = contenido;
    }

    @Override
    public double calcularDerechos() {
        return Configuracion.getInstancia().getTarifa(Configuracion.TARIFA_DECLARACION);
    }

    @Override
    public String getTipo() { return "Declaración extrajuicio"; }

    @Override
    public String getDetalle() { return "Contenido: " + contenido; }

    public String getContenido() { return contenido; }
}
