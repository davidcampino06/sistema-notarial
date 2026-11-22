# HU-17: Entregar trámite

**Como** recepcionista
**Quiero** registrar la entrega del trámite firmado
**Para** cerrar su ciclo.

| Campo | Valor |
|---|---|
| Módulo | Flujo y seguimiento del trámite |
| Requerimientos | RF-16 |
| Usuarios con acceso | Recepcionista |
| Prioridad | Alta |
| Dependencias | HU-16 |
| Repositorios | sistema-notarial-frontend, sistema-notarial-backend |

**Trazabilidad con el diseño (diagramas en Visual Paradigm)**

| Vista | Elementos |
|---|---|
| Contexto (flujo) | Radicado y estado del trámite |
| Funcional (componente) | Gestión de Trámites |
| Lógica | Tramite.avanzar() · EstadoTramite.ENTREGADO |
| Desarrollo | DetalleTramitePagina.cambiarEstado('ENTREGADO') → TramiteControlador.cambiarEstado() → Nucleo.cambiarEstado() |
| Mockup | P13 Trámite firmado: entregar |

**Validación INVEST**
- I: ✅ Se puede construir y probar por separado de las demás historias.
- N: ✅ Describe el resultado, no la tecnología.
- V: ✅ Le sirve directamente a: recepcionista.
- E: ✅ Las reglas y los mensajes están definidos.
- S: ✅ Se limita a «entregar trámite».
- T: ✅ Cada escenario tiene un resultado que se puede comprobar en la aplicación.

**Criterios de aceptación (Gherkin)**
```gherkin
# language: es
Característica: Entregar trámite

  Escenario: Entregar
    Dado que el trámite está Firmado
    Cuando la recepcionista pulsa «Marcar como entregado»
    Entonces el trámite pasa a Entregado, que es un estado final

  Escenario: No firmado
    Dado que el trámite no está Firmado
    Cuando se intenta entregar
    Entonces el sistema no lo cambia
    Y muestra «No se puede pasar de <estado> a Entregado»
```

**Supuestos / notas**
- No se guarda quién recibe: no está en el diagrama de clases.
