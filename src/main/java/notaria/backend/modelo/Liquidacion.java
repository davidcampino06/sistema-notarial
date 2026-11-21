package notaria.backend.modelo;

/**
 * Cuenta de cobro de un trámite (vista lógica).
 * Un Trámite genera 0..1 Liquidación y una Liquidación se paga con 0..1 Pago.
 */
public class Liquidacion {

    public static final double PORCENTAJE_IVA = 0.19;   // 19 % de IVA en Colombia

    private double valorDerechos;
    private double iva;
    private double total;
    private Pago pago;            // null mientras no se haya pagado

    /** Crea la liquidación a partir de los derechos notariales del trámite. */
    public Liquidacion(double valorDerechos) {
        this.valorDerechos = valorDerechos;
        this.iva = Math.round(valorDerechos * PORCENTAJE_IVA);
        calcularTotal();
    }

    /** Constructor usado al leer de la base de datos (los valores ya estaban calculados). */
    public Liquidacion(double valorDerechos, double iva, double total) {
        this.valorDerechos = valorDerechos;
        this.iva = iva;
        this.total = total;
    }

    /** total = derechos + IVA */
    public double calcularTotal() {
        total = valorDerechos + iva;
        return total;
    }

    public boolean estaPagada() {
        return pago != null;
    }

    public double getValorDerechos() { return valorDerechos; }
    public double getIva() { return iva; }
    public double getTotal() { return total; }
    public Pago getPago() { return pago; }
    public void setPago(Pago pago) { this.pago = pago; }
}
