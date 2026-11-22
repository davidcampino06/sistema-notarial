# HU-07: Registrar cliente al radicar

**Como** recepcionista
**Quiero** registrar los datos del cliente mientras radico su trámite
**Para** tenerlo asociado a sus trámites y poder avisarle.

| Campo | Valor |
|---|---|
| Módulo | Gestión de clientes |
| Requerimientos | RF-06, RF-10 |
| Usuarios con acceso | Recepcionista |
| Prioridad | Alta |
| Dependencias | HU-06 |
| Repositorios | sistema-notarial-frontend, sistema-notarial-backend |

**Trazabilidad con el diseño (diagramas en Visual Paradigm)**

| Vista | Elementos |
|---|---|
| Contexto (flujo) | Solicitud y documentos |
| Funcional (componente) | Gestión de Clientes |
| Lógica | Cliente (tipoDocumento, numeroDocumento, nombres, telefono, correo) |
| Desarrollo | VentanaRadicacion → TramiteControlador.radicarTramite() → Nucleo.radicarTramite() → ListaClientes.agregar() → BaseDatos.guardar(cliente) |
| Mockup | P05 Radicar trámite |

**Validación INVEST**
- I: ✅ Se puede construir y probar por separado de las demás historias.
- N: ✅ Describe el resultado, no la tecnología.
- V: ✅ Le sirve directamente a: recepcionista.
- E: ✅ Las reglas y los mensajes están definidos.
- S: ✅ Se limita a «registrar cliente al radicar».
- T: ✅ Cada escenario tiene un resultado que se puede comprobar en la aplicación.

**Criterios de aceptación (Gherkin)**
```gherkin
# language: es
Característica: Registrar cliente al radicar

  Escenario: Cliente nuevo con datos completos
    Dado que el cliente no está registrado
    Cuando la recepcionista escribe tipo de documento (CC, CE, NIT o PAS), número y nombres, y radica el trámite
    Entonces el sistema guarda el cliente
    Y lo asocia al trámite

  Escenario: Faltan datos obligatorios
    Dado que el cliente nuevo no tiene nombres
    Cuando la recepcionista intenta radicar
    Entonces el sistema no radica
    Y muestra «Los nombres del cliente son obligatorios»

  Escenario: Cliente repetido
    Dado que ya existe un cliente con ese número de documento
    Cuando se radica otro trámite con ese documento
    Entonces el sistema usa el cliente que ya estaba guardado
    Y no crea un cliente duplicado

  Escenario: Segundo cliente
    Dado que el trámite tiene dos partes, por ejemplo vendedor y comprador
    Cuando la recepcionista llena también el bloque «Segundo cliente»
    Entonces el trámite queda con los dos clientes
```

**Requisitos no funcionales**
- Teléfono y correo son opcionales; el correo sirve para los avisos (HU-19).

**Supuestos / notas**
- En los diagramas el cliente se registra al radicar, no en una ventana aparte; por eso no hay persona jurídica con representante legal (RF-06 parcial). El NIT se registra como tipo de documento.
