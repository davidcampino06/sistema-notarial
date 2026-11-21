package notaria.modelo;

import java.util.Date;

/** Pago que hace el cliente de una liquidación (vista lógica). */
public class Pago {

    private double valor;
    private MedioPago medioPago;
    private Date fecha;

    public Pago(double valor, MedioPago medioPago, Date fecha) {
        this.valor = valor;
        this.medioPago = medioPago;
        this.fecha = fecha;
    }

    public double getValor() { return valor; }
    public MedioPago getMedioPago() { return medioPago; }
    public Date getFecha() { return fecha; }
}
