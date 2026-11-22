# HU-14: Revisar trámite (aprobar o devolver)

**Como** notario
**Quiero** aprobar o devolver los trámites que reviso
**Para** garantizar que cumplen antes de firmarlos.

| Campo | Valor |
|---|---|
| Módulo | Flujo y seguimiento del trámite |
| Requerimientos | RF-18 |
| Usuarios con acceso | Notario |
| Prioridad | Alta |
| Dependencias | HU-13 |
| Repositorios | sistema-notarial-frontend, sistema-notarial-backend |

**Trazabilidad con el diseño (diagramas en Visual Paradigm)**

| Vista | Elementos |
|---|---|
| Contexto (flujo) | Radicaciones, revisiones y pagos |
| Funcional (componente) | Gestión de Trámites |
| Lógica | Tramite.avanzar(), observaciones · EstadoTramite |
| Desarrollo | VentanaRevision.aprobar(), devolver() → TramiteControlador.cambiarEstado() → Nucleo.cambiarEstado() |
| Mockup | P08 Revisión del notario |

**Validación INVEST**
- I: ✅ Se puede construir y probar por separado de las demás historias.
- N: ✅ Describe el resultado, no la tecnología.
- V: ✅ Le sirve directamente a: notario.
- E: ✅ Las reglas y los mensajes están definidos.
- S: ✅ Se limita a «revisar trámite (aprobar o devolver)».
- T: ✅ Cada escenario tiene un resultado que se puede comprobar en la aplicación.

**Criterios de aceptación (Gherkin)**
```gherkin
# language: es
Característica: Revisar trámite (aprobar o devolver)

  Escenario: Aprobar
    Dado que el trámite está En revisión
    Cuando el notario pulsa «Aprobar»
    Entonces el trámite pasa a Aprobado
    Y se avisa por correo a los clientes (HU-19)

  Escenario: Devolver con observaciones
    Dado que el trámite está En revisión
    Cuando el notario escribe observaciones y pulsa «Devolver al digitador»
    Entonces el trámite pasa a Devuelto con las observaciones

  Escenario: Devolver sin observaciones
    Dado que el notario no escribió observaciones
    Cuando intenta devolver
    Entonces el sistema no lo cambia
    Y muestra «Escriba las observaciones de la devolución»

  Escenario: Otro rol
    Dado que el usuario no es notario
    Cuando intenta aprobar o devolver
    Entonces el sistema no lo cambia
    Y muestra «Su rol (<rol>) no puede hacer esta acción»
```

**Supuestos / notas**
- Ninguno.
