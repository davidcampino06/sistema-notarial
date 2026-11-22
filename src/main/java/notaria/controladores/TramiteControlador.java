package notaria.controladores;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import notaria.logica.Nucleo;
import notaria.logica.Sesion;
import notaria.modelo.*;
import org.springframework.web.bind.annotation.*;

/**
 * Componente "Gestión de Trámites" de la vista funcional.
 * Lo usan VentanaPrincipal (bandeja y detalle), VentanaRadicacion,
 * VentanaRevision y VentanaConsulta (pública).
 */
@RestController
@RequestMapping("/api")
public class TramiteControlador extends Controlador {

    public TramiteControlador(Nucleo nucleo) {
        super(nucleo);
    }

    /** GET /api/tramites → todos los trámites (cualquier funcionario). */
    @GetMapping("/tramites")
    public List<Tramite> listarTramites(@RequestHeader(value = "Authorization", required = false) String autorizacion) {
        sesion(autorizacion);
        return nucleo.getTramites();
    }

    /** GET /api/tramites/{numeroRadicado} → detalle de un trámite. */
    @GetMapping("/tramites/{numeroRadicado}")
    public Tramite abrirTramite(@RequestHeader(value = "Authorization", required = false) String autorizacion,
                                @PathVariable String numeroRadicado) {
        sesion(autorizacion);
        return nucleo.buscarTramite(numeroRadicado);
    }

    record DatosCliente(String numeroDocumento, String nombres, String telefono, String tipoDocumento, String correo) {}

    record SolicitudRadicacion(String tipo, Integer numeroFolios, String contenido, String objetoPoder,
                               Double valorInmueble, List<DatosCliente> clientes, List<String> documentos) {}

    /** POST /api/tramites → radica un trámite (solo Recepcionista). */
    @PostMapping("/tramites")
    public Tramite radicarTramite(@RequestHeader(value = "Authorization", required = false) String autorizacion,
                                  @RequestBody SolicitudRadicacion solicitud) {
        sesion(autorizacion, Rol.RECEPCIONISTA);

        // Según el tipo escogido se crea un objeto de una subclase distinta (POLIMORFISMO)
        Tramite tramite;
        String tipo = solicitud.tipo() == null ? "" : solicitud.tipo();
        switch (tipo) {
            case "Autenticacion":
                tramite = new Autenticacion(solicitud.numeroFolios() == null ? 0 : solicitud.numeroFolios());
                break;
            case "DeclaracionExtrajuicio":
                tramite = new DeclaracionExtrajuicio(solicitud.contenido());
                break;
            case "PoderEspecial":
                tramite = new PoderEspecial(solicitud.objetoPoder());
                break;
            case "EscrituraCompraventa":
                tramite = new EscrituraCompraventa(solicitud.valorInmueble() == null ? 0 : solicitud.valorInmueble());
                break;
            default:
                throw new IllegalArgumentException("Escoja un tipo de trámite");
        }

        // Si el cliente ya existe se usa el guardado; si no, se crea con los datos enviados
        if (solicitud.clientes() != null) {
            for (DatosCliente datos : solicitud.clientes()) {
                if (datos.numeroDocumento() == null || datos.numeroDocumento().isBlank()) {
                    continue;
                }
                Cliente cliente = nucleo.getListaClientes().buscar(datos.numeroDocumento().trim());
                if (cliente == null) {
                    cliente = new Cliente(datos.numeroDocumento(), datos.nombres(), datos.telefono(),
                            datos.tipoDocumento(), datos.correo());
                }
                tramite.agregarCliente(cliente);
            }
        }

        // Documentos adjuntos: nombre del archivo y fecha de carga (DocumentoAdjunto)
        if (solicitud.documentos() != null) {
            for (String nombre : solicitud.documentos()) {
                if (nombre != null && !nombre.isBlank()) {
                    tramite.agregarDocumento(new DocumentoAdjunto(nombre.trim(), new Date()));
                }
            }
        }
        if (tramite.getDocumentos().isEmpty()) {
            throw new IllegalArgumentException("Debe adjuntar al menos un documento");
        }
        return nucleo.radicarTramite(tramite);
    }

    record CambioEstado(String nuevo, String observaciones, String digitador) {}

    /**
     * POST /api/tramites/{numeroRadicado}/estado → cambia el estado.
     * Lo usan asignar digitador, enviar a revisión, aprobar, devolver,
     * firmar, entregar y anular. Las reglas están en Nucleo.cambiarEstado().
     */
    @PostMapping("/tramites/{numeroRadicado}/estado")
    public Tramite cambiarEstado(@RequestHeader(value = "Authorization", required = false) String autorizacion,
                                 @PathVariable String numeroRadicado, @RequestBody CambioEstado cambio) {
        Sesion sesion = sesion(autorizacion);
        Tramite tramite = nucleo.buscarTramite(numeroRadicado);
        Usuario digitador = cambio.digitador() == null ? null : nucleo.buscarUsuario(cambio.digitador());
        nucleo.cambiarEstado(sesion.getUsuarioActual(), tramite, EstadoTramite.valueOf(cambio.nuevo()),
                cambio.observaciones(), digitador);
        return tramite;
    }

    /** GET /api/digitadores → nombres de los digitadores activos (para asignar). */
    @GetMapping("/digitadores")
    public List<String> digitadores(@RequestHeader(value = "Authorization", required = false) String autorizacion) {
        sesion(autorizacion, Rol.RECEPCIONISTA, Rol.NOTARIO);
        List<String> nombres = new ArrayList<>();
        for (Usuario u : nucleo.getUsuarios()) {
            if (u.isActivo() && u.getRol().es(Rol.DIGITADOR)) {
                nombres.add(u.getNombreUsuario());
            }
        }
        return nombres;
    }

    /**
     * GET /api/consulta?radicado=...&documento=... → consulta PÚBLICA (sin sesión).
     * Solo responde si el documento es de uno de los clientes del trámite.
     */
    @GetMapping("/consulta")
    public Map<String, Object> consultarEstado(@RequestParam String radicado, @RequestParam String documento) {
        Tramite tramite;
        try {
            tramite = nucleo.buscarTramite(radicado.trim());
        } catch (IllegalArgumentException e) {
            tramite = null;
        }
        if (tramite != null) {
            for (Cliente c : tramite.getClientes()) {
                if (c.getNumeroDocumento().equals(documento.trim())) {
                    return Map.of("numeroRadicado", tramite.getNumeroRadicado(),
                            "tipo", tramite.getTipo(),
                            "fechaRadicacion", tramite.getFechaRadicacion(),
                            "estado", tramite.getEstado());
                }
            }
        }
        throw new IllegalArgumentException("No encontramos un trámite con ese radicado y ese documento. Revise los datos.");
    }
}
