# HU-15: Corregir trámite devuelto

**Como** digitador
**Quiero** ver las observaciones del notario y volver a elaborar el trámite
**Para** corregirlo y enviarlo de nuevo.

| Campo | Valor |
|---|---|
| Módulo | Flujo y seguimiento del trámite |
| Requerimientos | RF-16, RF-18 |
| Usuarios con acceso | Digitador |
| Prioridad | Alta |
| Dependencias | HU-14 |
| Repositorios | sistema-notarial-frontend, sistema-notarial-backend |

**Trazabilidad con el diseño (diagramas en Visual Paradigm)**

| Vista | Elementos |
|---|---|
| Contexto (flujo) | Radicaciones, revisiones y pagos |
| Funcional (componente) | Gestión de Trámites |
| Lógica | Tramite.observaciones · EstadoTramite |
| Desarrollo | DetalleTramitePagina.cambiarEstado('EN_ELABORACION') → TramiteControlador.cambiarEstado() → Nucleo.cambiarEstado() |
| Mockup | P09 Trámite devuelto: corregir |

**Validación INVEST**
- I: ✅ Se puede construir y probar por separado de las demás historias.
- N: ✅ Describe el resultado, no la tecnología.
- V: ✅ Le sirve directamente a: digitador.
- E: ✅ Las reglas y los mensajes están definidos.
- S: ✅ Se limita a «corregir trámite devuelto».
- T: ✅ Cada escenario tiene un resultado que se puede comprobar en la aplicación.

**Criterios de aceptación (Gherkin)**
```gherkin
# language: es
Característica: Corregir trámite devuelto

  Escenario: Ver observaciones
    Dado que el notario devolvió el trámite
    Cuando el digitador lo abre
    Entonces ve las observaciones resaltadas

  Escenario: Volver a elaboración
    Dado que el trámite está Devuelto y asignado al digitador
    Cuando pulsa «Corregir (volver a elaboración)»
    Entonces el trámite pasa a En elaboración
    Y puede enviarlo otra vez a revisión (HU-13)
```

**Supuestos / notas**
- Al aprobarlo, las observaciones se borran.
