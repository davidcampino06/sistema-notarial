# HU-12: Asignar digitador

**Como** notario
**Quiero** asignar cada trámite radicado a un digitador
**Para** repartir el trabajo de la notaría.

| Campo | Valor |
|---|---|
| Módulo | Flujo y seguimiento del trámite |
| Requerimientos | RF-17 |
| Usuarios con acceso | Recepcionista y notario |
| Prioridad | Alta |
| Dependencias | HU-08 |
| Repositorios | sistema-notarial-frontend, sistema-notarial-backend |

**Trazabilidad con el diseño (diagramas en Visual Paradigm)**

| Vista | Elementos |
|---|---|
| Contexto (flujo) | Radicaciones, revisiones y pagos |
| Funcional (componente) | Gestión de Trámites |
| Lógica | Tramite.avanzar() · Usuario (digitador) · EstadoTramite |
| Desarrollo | DetalleTramitePagina.cambiarEstado() → TramiteControlador.digitadores(), cambiarEstado() → Nucleo.cambiarEstado() |
| Mockup | P06 Trámite radicado: asignar digitador |

**Validación INVEST**
- I: ✅ Se puede construir y probar por separado de las demás historias.
- N: ✅ Describe el resultado, no la tecnología.
- V: ✅ Le sirve directamente a: recepcionista y notario.
- E: ✅ Las reglas y los mensajes están definidos.
- S: ✅ Se limita a «asignar digitador».
- T: ✅ Cada escenario tiene un resultado que se puede comprobar en la aplicación.

**Criterios de aceptación (Gherkin)**
```gherkin
# language: es
Característica: Asignar digitador

  Escenario: Asignar
    Dado que el trámite está Radicado
    Cuando la recepcionista o el notario elige un digitador y pulsa «Asignar digitador»
    Entonces el trámite pasa a En elaboración con ese digitador
    Y muestra «El trámite quedó en estado: En elaboración»

  Escenario: Solo digitadores activos
    Dado que hay digitadores activos e inactivos
    Cuando se abre la lista de digitadores
    Entonces la lista muestra solo los digitadores activos

  Escenario: Estado no permitido
    Dado que el trámite ya no está Radicado
    Cuando se intenta asignar
    Entonces el sistema no lo cambia
    Y muestra «No se puede pasar de <estado> a En elaboración»
```

**Supuestos / notas**
- La reasignación a otro digitador no está en el flujo del diagrama de estados.
