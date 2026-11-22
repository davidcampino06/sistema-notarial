# HU-08: Radicar trámite

**Como** recepcionista
**Quiero** radicar un trámite eligiendo su tipo y sus clientes
**Para** iniciar formalmente la solicitud y darle un número.

| Campo | Valor |
|---|---|
| Módulo | Radicación de trámites |
| Requerimientos | RF-11, RF-12 |
| Usuarios con acceso | Recepcionista |
| Prioridad | Alta |
| Dependencias | HU-07, HU-09 |
| Repositorios | sistema-notarial-frontend, sistema-notarial-backend |

**Trazabilidad con el diseño (diagramas en Visual Paradigm)**

| Vista | Elementos |
|---|---|
| Contexto (flujo) | Solicitud y documentos · Radicado y estado del trámite |
| Funcional (componente) | Gestión de Trámites |
| Lógica | Tramite (abstracta) y Autenticacion, DeclaracionExtrajuicio, PoderEspecial, EscrituraCompraventa · EstadoTramite |
| Desarrollo | VentanaRadicacion.radicar() → TramiteControlador.radicarTramite() → Nucleo.radicarTramite() → BaseDatos.guardar(tramite) |
| Mockup | P05 Radicar trámite, P06 Trámite radicado: asignar digitador |

**Validación INVEST**
- I: ✅ Se puede construir y probar por separado de las demás historias.
- N: ✅ Describe el resultado, no la tecnología.
- V: ✅ Le sirve directamente a: recepcionista.
- E: ✅ Las reglas y los mensajes están definidos.
- S: ✅ Se limita a «radicar trámite».
- T: ✅ Cada escenario tiene un resultado que se puede comprobar en la aplicación.

**Criterios de aceptación (Gherkin)**
```gherkin
# language: es
Característica: Radicar trámite

  Escenario: Radicar con datos válidos
    Dado que la recepcionista eligió el tipo, llenó su dato, el cliente y adjuntó documentos
    Cuando pulsa «Radicar trámite»
    Entonces el sistema le da un número con formato AÑO-consecutivo, por ejemplo 2026-000001
    Y el trámite queda en estado Radicado con su fecha
    Y abre el trámite y muestra «Trámite radicado con el número <radicado>»

  Escenario: Primer trámite del año
    Dado que empieza un año nuevo
    Cuando se radica el primer trámite
    Entonces el consecutivo vuelve a 000001

  Escenario: Dato propio de cada tipo
    Dado que la recepcionista elige el tipo de trámite
    Cuando mira el formulario
    Entonces Autenticación pide número de folios, Declaración extrajuicio el contenido, Poder especial el objeto del poder y Escritura de compraventa el valor del inmueble

  Escenario: Dato del tipo no válido
    Dado que los folios o el valor del inmueble son 0, o el contenido o el objeto están vacíos
    Cuando intenta radicar
    Entonces el sistema no radica
    Y muestra el dato que falta, por ejemplo «El número de folios debe ser mayor que 0»

  Escenario: Sin cliente
    Dado que no se escribió el documento de ningún cliente
    Cuando intenta radicar
    Entonces el sistema no radica
    Y muestra «El trámite debe tener al menos un cliente»
```

**Requisitos no funcionales**
- Un tipo de trámite nuevo es una subclase de Tramite con su calcularDerechos() (RNF-15).

**Supuestos / notas**
- Las calidades del cliente (vendedor, comprador…) no están en el diagrama de clases; el formulario permite dos clientes.
