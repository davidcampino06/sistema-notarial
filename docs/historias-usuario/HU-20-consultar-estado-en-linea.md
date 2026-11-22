# HU-20: Consultar estado en línea

**Como** cliente
**Quiero** consultar en la web el estado de mi trámite
**Para** saber cómo va sin ir a la notaría.

| Campo | Valor |
|---|---|
| Módulo | Flujo y seguimiento del trámite |
| Requerimientos | RF-22 |
| Usuarios con acceso | Cliente (sin iniciar sesión) |
| Prioridad | Baja |
| Dependencias | HU-08 |
| Repositorios | sistema-notarial-frontend, sistema-notarial-backend |

**Trazabilidad con el diseño (diagramas en Visual Paradigm)**

| Vista | Elementos |
|---|---|
| Contexto (flujo) | Radicado y estado del trámite (cliente) |
| Funcional (componente) | Gestión de Trámites (consulta pública) |
| Lógica | Tramite · Cliente |
| Desarrollo | VentanaConsulta.consultarEstado() → TramiteControlador.consultarEstado() (ruta pública) |
| Mockup | P03 Consulta pública del trámite |

**Validación INVEST**
- I: ✅ Se puede construir y probar por separado de las demás historias.
- N: ✅ Describe el resultado, no la tecnología.
- V: ✅ Le sirve directamente a: cliente (sin iniciar sesión).
- E: ✅ Las reglas y los mensajes están definidos.
- S: ✅ Se limita a «consultar estado en línea».
- T: ✅ Cada escenario tiene un resultado que se puede comprobar en la aplicación.

**Criterios de aceptación (Gherkin)**
```gherkin
# language: es
Característica: Consultar estado en línea

  Escenario: Datos correctos
    Dado que el cliente tiene su radicado y su documento
    Cuando los escribe y pulsa «Consultar»
    Entonces ve el tipo de trámite, el radicado, la fecha y el estado actual

  Escenario: Datos que no coinciden
    Dado que el radicado no existe o el documento no es de un cliente del trámite
    Cuando consulta
    Entonces el sistema muestra «No encontramos un trámite con ese radicado y ese documento. Revise los datos.», sin decir si el radicado existe

  Escenario: Sin datos personales
    Dado que el trámite tiene varios clientes
    Cuando uno de ellos consulta
    Entonces la respuesta no muestra nombres ni documentos de los otros clientes
```

**Requisitos no funcionales**
- No necesita sesión.

**Supuestos / notas**
- Ninguno.
