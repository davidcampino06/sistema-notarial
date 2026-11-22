# HU-13: Enviar trámite a revisión

**Como** digitador
**Quiero** enviar al notario el trámite que elaboré
**Para** que lo revise antes de firmarlo.

| Campo | Valor |
|---|---|
| Módulo | Flujo y seguimiento del trámite |
| Requerimientos | RF-16 |
| Usuarios con acceso | Digitador |
| Prioridad | Alta |
| Dependencias | HU-12 |
| Repositorios | sistema-notarial-frontend, sistema-notarial-backend |

**Trazabilidad con el diseño (diagramas en Visual Paradigm)**

| Vista | Elementos |
|---|---|
| Contexto (flujo) | Radicaciones, revisiones y pagos |
| Funcional (componente) | Gestión de Trámites |
| Lógica | Tramite.avanzar() · EstadoTramite |
| Desarrollo | DetalleTramitePagina.cambiarEstado('EN_REVISION') → TramiteControlador.cambiarEstado() → Nucleo.cambiarEstado() |
| Mockup | P07 Trámite en elaboración: enviar a revisión |

**Validación INVEST**
- I: ✅ Se puede construir y probar por separado de las demás historias.
- N: ✅ Describe el resultado, no la tecnología.
- V: ✅ Le sirve directamente a: digitador.
- E: ✅ Las reglas y los mensajes están definidos.
- S: ✅ Se limita a «enviar trámite a revisión».
- T: ✅ Cada escenario tiene un resultado que se puede comprobar en la aplicación.

**Criterios de aceptación (Gherkin)**
```gherkin
# language: es
Característica: Enviar trámite a revisión

  Escenario: Enviar a revisión
    Dado que el trámite está En elaboración y asignado al digitador
    Cuando pulsa «Enviar a revisión»
    Entonces el trámite pasa a En revisión
    Y aparece en la ventana Revisión del notario

  Escenario: Trámite de otro digitador
    Dado que el trámite está asignado a otro digitador
    Cuando el digitador intenta enviarlo
    Entonces el sistema no lo cambia
    Y muestra «Este trámite no está asignado a usted»
```

**Supuestos / notas**
- El botón solo aparece al digitador asignado.
