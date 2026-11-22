# HU-18: Anular trámite

**Como** notario
**Quiero** anular un trámite escribiendo el motivo
**Para** cancelar solicitudes que no pueden seguir.

| Campo | Valor |
|---|---|
| Módulo | Flujo y seguimiento del trámite |
| Requerimientos | RF-16 |
| Usuarios con acceso | Notario |
| Prioridad | Media |
| Dependencias | HU-08 |
| Repositorios | sistema-notarial-frontend, sistema-notarial-backend |

**Trazabilidad con el diseño (diagramas en Visual Paradigm)**

| Vista | Elementos |
|---|---|
| Contexto (flujo) | Radicaciones, revisiones y pagos |
| Funcional (componente) | Gestión de Trámites |
| Lógica | Tramite.observaciones · EstadoTramite.ANULADO |
| Desarrollo | DetalleTramitePagina.cambiarEstado('ANULADO') → TramiteControlador.cambiarEstado() → Nucleo.cambiarEstado() |
| Mockup | P14 Anular trámite, P15 Trámite anulado |

**Validación INVEST**
- I: ✅ Se puede construir y probar por separado de las demás historias.
- N: ✅ Describe el resultado, no la tecnología.
- V: ✅ Le sirve directamente a: notario.
- E: ✅ Las reglas y los mensajes están definidos.
- S: ✅ Se limita a «anular trámite».
- T: ✅ Cada escenario tiene un resultado que se puede comprobar en la aplicación.

**Criterios de aceptación (Gherkin)**
```gherkin
# language: es
Característica: Anular trámite

  Escenario: Anular con motivo
    Dado que el trámite no está Firmado, Entregado ni Anulado
    Cuando el notario escribe el motivo y pulsa «Anular trámite»
    Entonces el trámite pasa a Anulado
    Y el motivo queda visible en el detalle

  Escenario: Sin motivo
    Dado que el notario no escribió el motivo
    Cuando intenta anular
    Entonces el sistema no lo anula
    Y muestra «Escriba el motivo de la anulación»

  Escenario: Trámite firmado o entregado
    Dado que el trámite está Firmado o Entregado
    Cuando se intenta anular
    Entonces el sistema no lo anula
    Y muestra «Un trámite <estado> no se puede anular»

  Escenario: Anulado no cambia
    Dado que el trámite está Anulado
    Cuando se intenta liquidar o cambiar de estado
    Entonces el sistema lo rechaza, por ejemplo «No se puede liquidar un trámite anulado»
```

**Supuestos / notas**
- Ninguno.
