# HU-16: Firmar trámite

**Como** notario
**Quiero** firmar los trámites aprobados y pagados
**Para** dejarlos listos para entregar.

| Campo | Valor |
|---|---|
| Módulo | Flujo y seguimiento del trámite |
| Requerimientos | RF-16 |
| Usuarios con acceso | Notario |
| Prioridad | Alta |
| Dependencias | HU-14, HU-22 |
| Repositorios | sistema-notarial-frontend, sistema-notarial-backend |

**Trazabilidad con el diseño (diagramas en Visual Paradigm)**

| Vista | Elementos |
|---|---|
| Contexto (flujo) | Radicaciones, revisiones y pagos |
| Funcional (componente) | Gestión de Trámites · Caja y Liquidación |
| Lógica | Tramite.avanzar() · Liquidacion.estaPagada() |
| Desarrollo | VentanaRevision.firmar() → TramiteControlador.cambiarEstado() → Nucleo.cambiarEstado() |
| Mockup | P08 Revisión del notario |

**Validación INVEST**
- I: ✅ Se puede construir y probar por separado de las demás historias.
- N: ✅ Describe el resultado, no la tecnología.
- V: ✅ Le sirve directamente a: notario.
- E: ✅ Las reglas y los mensajes están definidos.
- S: ✅ Se limita a «firmar trámite».
- T: ✅ Cada escenario tiene un resultado que se puede comprobar en la aplicación.

**Criterios de aceptación (Gherkin)**
```gherkin
# language: es
Característica: Firmar trámite

  Escenario: Firmar con pago
    Dado que el trámite está Aprobado y pagado
    Cuando el notario pulsa «Firmar»
    Entonces el trámite pasa a Firmado
    Y se avisa por correo a los clientes que ya pueden reclamarlo (HU-19)

  Escenario: Sin pago
    Dado que el trámite está Aprobado pero no se ha pagado
    Cuando el notario mira la lista «Aprobados»
    Entonces en vez del botón aparece «Debe pagar en caja»
    Y si se pide por la API, el sistema muestra «No se puede firmar: el trámite no tiene pago registrado»
```

**Supuestos / notas**
- Ninguno.
