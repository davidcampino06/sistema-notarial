# HU-22: Registrar pago y generar recibo

**Como** cajero
**Quiero** registrar el pago y entregar un recibo
**Para** dejar constancia de lo que pagó el cliente.

| Campo | Valor |
|---|---|
| Módulo | Liquidación y caja |
| Requerimientos | RF-25 |
| Usuarios con acceso | Cajero |
| Prioridad | Alta |
| Dependencias | HU-21 |
| Repositorios | sistema-notarial-frontend, sistema-notarial-backend |

**Trazabilidad con el diseño (diagramas en Visual Paradigm)**

| Vista | Elementos |
|---|---|
| Contexto (flujo) | Bandeja de trámites y recibos |
| Funcional (componente) | Caja y Liquidación |
| Lógica | Pago (valor, medioPago, fecha) · MedioPago |
| Desarrollo | VentanaCaja.registrarPago(), generarRecibo() → CajaControlador.registrarPago() → Nucleo.registrarPago() |
| Mockup | P10 Caja: liquidar y registrar pago, P11 Recibo de pago |

**Validación INVEST**
- I: ✅ Se puede construir y probar por separado de las demás historias.
- N: ✅ Describe el resultado, no la tecnología.
- V: ✅ Le sirve directamente a: cajero.
- E: ✅ Las reglas y los mensajes están definidos.
- S: ✅ Se limita a «registrar pago y generar recibo».
- T: ✅ Cada escenario tiene un resultado que se puede comprobar en la aplicación.

**Criterios de aceptación (Gherkin)**
```gherkin
# language: es
Característica: Registrar pago y generar recibo

  Escenario: Registrar pago
    Dado que el trámite está liquidado y sin pago
    Cuando el cajero elige Efectivo, Tarjeta o Transferencia y pulsa «Registrar pago»
    Entonces el sistema guarda el pago con su fecha
    Y abre el recibo con radicado, trámite, clientes, derechos, IVA, total, medio, fecha y cajero

  Escenario: Imprimir recibo
    Dado que el trámite está pagado
    Cuando el cajero pulsa «Recibo» y luego «Imprimir»
    Entonces se imprime el recibo sin los botones

  Escenario: Sin liquidar
    Dado que el trámite no está liquidado
    Cuando se intenta registrar el pago
    Entonces el sistema lo rechaza con «Primero debe liquidar el trámite»

  Escenario: Pago doble
    Dado que el trámite ya fue pagado
    Cuando se intenta pagar otra vez
    Entonces el sistema lo rechaza con «Este trámite ya fue pagado»
```

**Supuestos / notas**
- El recibo se imprime desde el navegador (también se puede guardar como PDF); no lleva número consecutivo propio, usa el radicado.
