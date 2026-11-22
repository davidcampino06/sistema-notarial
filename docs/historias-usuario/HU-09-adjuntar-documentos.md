# HU-09: Adjuntar documentos

**Como** recepcionista
**Quiero** registrar los documentos que entrega el cliente
**Para** tener la lista del expediente en el trámite.

| Campo | Valor |
|---|---|
| Módulo | Radicación de trámites |
| Requerimientos | RF-14, RF-15 |
| Usuarios con acceso | Recepcionista |
| Prioridad | Alta |
| Dependencias | HU-08 |
| Repositorios | sistema-notarial-frontend, sistema-notarial-backend |

**Trazabilidad con el diseño (diagramas en Visual Paradigm)**

| Vista | Elementos |
|---|---|
| Contexto (flujo) | Solicitud y documentos |
| Funcional (componente) | Gestión de Trámites |
| Lógica | DocumentoAdjunto (nombreArchivo, fechaCarga) · Tramite 1..* DocumentoAdjunto |
| Desarrollo | VentanaRadicacion.adjuntarDocumento() → TramiteControlador.radicarTramite() → Tramite.agregarDocumento() |
| Mockup | P05 Radicar trámite, P20 Radicación sin documentos |

**Validación INVEST**
- I: ✅ Se puede construir y probar por separado de las demás historias.
- N: ✅ Describe el resultado, no la tecnología.
- V: ✅ Le sirve directamente a: recepcionista.
- E: ✅ Las reglas y los mensajes están definidos.
- S: ✅ Se limita a «adjuntar documentos».
- T: ✅ Cada escenario tiene un resultado que se puede comprobar en la aplicación.

**Criterios de aceptación (Gherkin)**
```gherkin
# language: es
Característica: Adjuntar documentos

  Escenario: Adjuntar un documento
    Dado que la recepcionista está radicando
    Cuando escribe el nombre del archivo y pulsa «Adjuntar documento»
    Entonces el documento aparece en la lista
    Y al radicar queda guardado con su fecha de carga

  Escenario: Quitar un documento
    Dado que hay documentos en la lista
    Cuando pulsa «quitar» en uno
    Entonces el documento sale de la lista

  Escenario: Sin documentos
    Dado que la lista está vacía
    Cuando intenta radicar
    Entonces el sistema no radica
    Y muestra «Debe adjuntar al menos un documento»
```

**Requisitos no funcionales**
- Mensajes claros que dicen cómo corregir (RNF-12).

**Supuestos / notas**
- Se guarda el nombre del archivo y la fecha, como en el diagrama (DocumentoAdjunto); el archivo en sí no se sube.
