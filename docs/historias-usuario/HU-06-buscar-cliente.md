# HU-06: Buscar cliente

**Como** recepcionista
**Quiero** buscar un cliente por su número de documento
**Para** no volver a escribir sus datos si ya vino antes.

| Campo | Valor |
|---|---|
| Módulo | Gestión de clientes |
| Requerimientos | RF-07 |
| Usuarios con acceso | Recepcionista |
| Prioridad | Alta |
| Dependencias | HU-01 |
| Repositorios | sistema-notarial-frontend, sistema-notarial-backend |

**Trazabilidad con el diseño (diagramas en Visual Paradigm)**

| Vista | Elementos |
|---|---|
| Contexto (flujo) | Solicitud y documentos |
| Funcional (componente) | Gestión de Clientes |
| Lógica | Cliente · ListaClientes.buscar() |
| Desarrollo | VentanaRadicacion.buscadorCliente → ClienteControlador.buscarCliente() → ListaClientes.buscar() |
| Mockup | P05 Radicar trámite |

**Validación INVEST**
- I: ✅ Se puede construir y probar por separado de las demás historias.
- N: ✅ Describe el resultado, no la tecnología.
- V: ✅ Le sirve directamente a: recepcionista.
- E: ✅ Las reglas y los mensajes están definidos.
- S: ✅ Se limita a «buscar cliente».
- T: ✅ Cada escenario tiene un resultado que se puede comprobar en la aplicación.

**Criterios de aceptación (Gherkin)**
```gherkin
# language: es
Característica: Buscar cliente

  Escenario: Cliente que ya existe
    Dado que el cliente ya tiene un trámite en la notaría
    Cuando la recepcionista escribe su documento y pulsa «Buscar»
    Entonces el sistema llena el bloque «Cliente principal» con sus datos
    Y muestra «Cliente encontrado: <nombres>»

  Escenario: Cliente nuevo
    Dado que no hay ningún cliente con ese documento
    Cuando la recepcionista lo busca
    Entonces el sistema deja el documento escrito en el bloque del cliente
    Y indica que llene sus datos y que se registrará al radicar
```

**Requisitos no funcionales**
- La búsqueda responde en menos de 2 segundos (RNF-07).

**Supuestos / notas**
- La búsqueda es por documento exacto; la búsqueda por nombre se hace en la bandeja (HU-10).
