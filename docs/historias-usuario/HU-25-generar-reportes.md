# HU-25: Generar reportes

**Como** administrador
**Quiero** ver reportes de trámites y de recaudo
**Para** controlar la operación de la notaría.

| Campo | Valor |
|---|---|
| Módulo | Administración y reportes |
| Requerimientos | RF-28 |
| Usuarios con acceso | Administrador |
| Prioridad | Media |
| Dependencias | HU-08, HU-22 |
| Repositorios | sistema-notarial-frontend, sistema-notarial-backend |

**Trazabilidad con el diseño (diagramas en Visual Paradigm)**

| Vista | Elementos |
|---|---|
| Contexto (flujo) | Reportes |
| Funcional (componente) | Gestión de Trámites · Gestión de Clientes · Caja y Liquidación |
| Lógica | Tramite · Cliente · Pago |
| Desarrollo | Reportes.generarReportes() (VentanaAdministracion) → TramiteControlador.listarTramites(), ClienteControlador.listarClientes() |
| Mockup | P17 Reportes |

**Validación INVEST**
- I: ✅ Se puede construir y probar por separado de las demás historias.
- N: ✅ Describe el resultado, no la tecnología.
- V: ✅ Le sirve directamente a: administrador.
- E: ✅ Las reglas y los mensajes están definidos.
- S: ✅ Se limita a «generar reportes».
- T: ✅ Cada escenario tiene un resultado que se puede comprobar en la aplicación.

**Criterios de aceptación (Gherkin)**
```gherkin
# language: es
Característica: Generar reportes

  Escenario: Resumen
    Dado que hay trámites y pagos
    Cuando el administrador abre «Ver reportes»
    Entonces ve trámites radicados, clientes registrados, trámites pagados y total recaudado

  Escenario: Por estado y por tipo
    Dado que hay trámites
    Cuando mira los reportes
    Entonces ve la cantidad de trámites por estado, y por tipo con lo recaudado

  Escenario: Por medio de pago
    Dado que hay pagos
    Cuando mira el recaudo
    Entonces ve el total por efectivo, tarjeta y transferencia, y el total general

  Escenario: Sin datos
    Dado que no hay trámites
    Cuando abre los reportes
    Entonces la tabla por tipo muestra «Aún no hay trámites.» y los totales en cero

  Escenario: Imprimir
    Dado que el administrador está en reportes
    Cuando pulsa «Imprimir»
    Entonces se imprime el reporte (o se guarda como PDF desde el navegador)
```

**Supuestos / notas**
- Los reportes son del total, sin rango de fechas, y no se exportan a Excel (RF-28 parcial).
