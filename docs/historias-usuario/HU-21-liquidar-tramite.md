# HU-21: Liquidar trámite

**Como** cajero
**Quiero** que el sistema calcule cuánto debe pagar el cliente
**Para** cobrar el valor correcto sin hacer cuentas a mano.

| Campo | Valor |
|---|---|
| Módulo | Liquidación y caja |
| Requerimientos | RF-23, RF-24 |
| Usuarios con acceso | Cajero |
| Prioridad | Alta |
| Dependencias | HU-08, HU-24 |
| Repositorios | sistema-notarial-frontend, sistema-notarial-backend |

**Trazabilidad con el diseño (diagramas en Visual Paradigm)**

| Vista | Elementos |
|---|---|
| Contexto (flujo) | Radicaciones, revisiones y pagos |
| Funcional (componente) | Caja y Liquidación |
| Lógica | Tramite.calcularDerechos() (polimorfismo) · Liquidacion (valorDerechos, iva, total) |
| Desarrollo | VentanaCaja.liquidar → CajaControlador.liquidar() → Nucleo.liquidar() → Tramite.calcularDerechos() |
| Mockup | P10 Caja: liquidar y registrar pago |

**Validación INVEST**
- I: ✅ Se puede construir y probar por separado de las demás historias.
- N: ✅ Describe el resultado, no la tecnología.
- V: ✅ Le sirve directamente a: cajero.
- E: ✅ Las reglas y los mensajes están definidos.
- S: ✅ Se limita a «liquidar trámite».
- T: ✅ Cada escenario tiene un resultado que se puede comprobar en la aplicación.

**Criterios de aceptación (Gherkin)**
```gherkin
# language: es
Característica: Liquidar trámite

  Escenario: Según el tipo
    Dado que el trámite no está liquidado
    Cuando el cajero pulsa «Liquidar»
    Entonces Autenticación: folios × tarifa por folio
    Y Declaración extrajuicio y Poder especial: tarifa fija
    Y Escritura de compraventa: porcentaje del valor del inmueble

  Escenario: Desglose
    Dado que el trámite se liquidó
    Cuando el cajero mira la liquidación
    Entonces ve los derechos, el IVA del 19 % y el total
    Y muestra «Trámite <radicado> liquidado por <total>»

  Escenario: Ya liquidado
    Dado que el trámite ya tiene liquidación
    Cuando se intenta liquidar otra vez
    Entonces el sistema lo rechaza con «El trámite ya está liquidado»

  Escenario: Tarifas cambian después
    Dado que el trámite ya se liquidó
    Cuando el administrador cambia las tarifas
    Entonces la liquidación guardada conserva sus valores
```

**Requisitos no funcionales**
- Se calcula con polimorfismo: cada subclase de Tramite tiene su calcularDerechos().

**Supuestos / notas**
- No hay aportes ni tabla por rangos de la resolución; se usa el porcentaje configurable (RF-23 y RF-24 simplificados).
