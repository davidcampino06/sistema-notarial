# HU-23: Hacer cierre de caja

**Como** cajero
**Quiero** ver lo recaudado en un día por medio de pago
**Para** cuadrar la caja al final de la jornada.

| Campo | Valor |
|---|---|
| Módulo | Liquidación y caja |
| Requerimientos | RF-26 |
| Usuarios con acceso | Cajero |
| Prioridad | Media |
| Dependencias | HU-22 |
| Repositorios | sistema-notarial-frontend, sistema-notarial-backend |

**Trazabilidad con el diseño (diagramas en Visual Paradigm)**

| Vista | Elementos |
|---|---|
| Contexto (flujo) | Bandeja de trámites y recibos |
| Funcional (componente) | Caja y Liquidación |
| Lógica | Pago · MedioPago |
| Desarrollo | VentanaCaja.cierreCaja() (CierreCaja) → TramiteControlador.listarTramites() |
| Mockup | P12 Cierre de caja |

**Validación INVEST**
- I: ✅ Se puede construir y probar por separado de las demás historias.
- N: ✅ Describe el resultado, no la tecnología.
- V: ✅ Le sirve directamente a: cajero.
- E: ✅ Las reglas y los mensajes están definidos.
- S: ✅ Se limita a «hacer cierre de caja».
- T: ✅ Cada escenario tiene un resultado que se puede comprobar en la aplicación.

**Criterios de aceptación (Gherkin)**
```gherkin
# language: es
Característica: Hacer cierre de caja

  Escenario: Cierre del día
    Dado que hubo pagos hoy
    Cuando el cajero abre «Cierre de caja»
    Entonces ve cuántos pagos hubo, el total de efectivo, tarjeta y transferencia, y el total del día

  Escenario: Otro día
    Dado que el cajero quiere revisar un día anterior
    Cuando cambia la fecha
    Entonces los totales se recalculan para ese día
```

**Supuestos / notas**
- El cierre es una consulta e impresión; no bloquea pagos posteriores (RF-26 parcial, no está en los diagramas).
