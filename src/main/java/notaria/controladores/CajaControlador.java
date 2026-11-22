package notaria.controladores;

import notaria.logica.Nucleo;
import notaria.logica.Sesion;
import notaria.modelo.MedioPago;
import notaria.modelo.Rol;
import notaria.modelo.Tramite;
import org.springframework.web.bind.annotation.*;

/**
 * Componente "Caja y Liquidación" de la vista funcional (interfaz
 * "liquidar trámite"). Lo usa VentanaCaja: liquidar y registrarPago().
 */
@RestController
@RequestMapping("/api/caja")
public class CajaControlador extends Controlador {

    public CajaControlador(Nucleo nucleo) {
        super(nucleo);
    }

    /** POST /api/caja/{numeroRadicado}/liquidar → crea la liquidación (derechos + IVA). */
    @PostMapping("/{numeroRadicado}/liquidar")
    public Tramite liquidar(@RequestHeader(value = "Authorization", required = false) String autorizacion,
                            @PathVariable String numeroRadicado) {
        Sesion sesion = sesion(autorizacion, Rol.CAJERO);
        Tramite tramite = nucleo.buscarTramite(numeroRadicado);
        nucleo.liquidar(sesion.getUsuarioActual(), tramite);
        return tramite;
    }

    record DatosPago(String medioPago) {}

    /** POST /api/caja/{numeroRadicado}/pago → registra el pago de la liquidación. */
    @PostMapping("/{numeroRadicado}/pago")
    public Tramite registrarPago(@RequestHeader(value = "Authorization", required = false) String autorizacion,
                                 @PathVariable String numeroRadicado, @RequestBody DatosPago datos) {
        Sesion sesion = sesion(autorizacion, Rol.CAJERO);
        Tramite tramite = nucleo.buscarTramite(numeroRadicado);
        MedioPago medio = datos.medioPago() == null ? null : MedioPago.valueOf(datos.medioPago());
        nucleo.registrarPago(sesion.getUsuarioActual(), tramite, medio);
        return tramite;
    }
}
