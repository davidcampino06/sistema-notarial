# HU-11: Ver detalle del trámite

**Como** funcionario
**Quiero** ver todos los datos de un trámite
**Para** saber en qué va y qué me toca hacer.

| Campo | Valor |
|---|---|
| Módulo | Flujo y seguimiento del trámite |
| Requerimientos | RF-20 |
| Usuarios con acceso | Todos los funcionarios |
| Prioridad | Media |
| Dependencias | HU-10 |
| Repositorios | sistema-notarial-frontend, sistema-notarial-backend |

**Trazabilidad con el diseño (diagramas en Visual Paradigm)**

| Vista | Elementos |
|---|---|
| Contexto (flujo) | Bandeja de trámites y recibos |
| Funcional (componente) | Gestión de Trámites |
| Lógica | Tramite · Cliente · DocumentoAdjunto · Liquidacion · Pago |
| Desarrollo | DetalleTramitePagina (VentanaPrincipal) → TramiteControlador.abrirTramite() → Nucleo.buscarTramite() |
| Mockup | P06 Trámite radicado: asignar digitador, P13 Trámite firmado: entregar, P15 Trámite anulado |

**Validación INVEST**
- I: ✅ Se puede construir y probar por separado de las demás historias.
- N: ✅ Describe el resultado, no la tecnología.
- V: ✅ Le sirve directamente a: todos los funcionarios.
- E: ✅ Las reglas y los mensajes están definidos.
- S: ✅ Se limita a «ver detalle del trámite».
- T: ✅ Cada escenario tiene un resultado que se puede comprobar en la aplicación.

**Criterios de aceptación (Gherkin)**
```gherkin
# language: es
Característica: Ver detalle del trámite

  Escenario: Ver el detalle
    Dado que el funcionario abre un trámite
    Cuando se carga la página
    Entonces ve tipo, radicado, estado, dato del tipo, fecha, digitador, clientes, documentos y liquidación

  Escenario: Acciones según rol y estado
    Dado que el trámite está en un estado
    Cuando el funcionario mira la tarjeta «Acciones»
    Entonces solo aparecen las acciones que su rol puede hacer en ese estado
    Y si no tiene ninguna, la tarjeta no aparece

  Escenario: Observaciones
    Dado que el trámite fue devuelto o anulado
    Cuando se abre el detalle
    Entonces las observaciones o el motivo aparecen resaltados

  Escenario: Radicado que no existe
    Dado que se escribe un radicado inexistente en la dirección
    Cuando se abre la página
    Entonces el sistema muestra «No existe el trámite <radicado>»
```

**Requisitos no funcionales**
- Mensajes en español (RNF-12).

**Supuestos / notas**
- Ninguno.
