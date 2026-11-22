# HU-10: Consultar y filtrar trámites

**Como** funcionario
**Quiero** ver y filtrar la bandeja de trámites
**Para** encontrar rápido los que debo atender.

| Campo | Valor |
|---|---|
| Módulo | Flujo y seguimiento del trámite |
| Requerimientos | RF-20 |
| Usuarios con acceso | Todos los funcionarios |
| Prioridad | Alta |
| Dependencias | HU-01 |
| Repositorios | sistema-notarial-frontend, sistema-notarial-backend |

**Trazabilidad con el diseño (diagramas en Visual Paradigm)**

| Vista | Elementos |
|---|---|
| Contexto (flujo) | Bandeja de trámites y recibos |
| Funcional (componente) | Gestión de Trámites |
| Lógica | Tramite · EstadoTramite |
| Desarrollo | VentanaPrincipal.filtrarTramites(), abrirTramite() → TramiteControlador.listarTramites() |
| Mockup | P04 Bandeja de trámites, P21 Vista en celular |

**Validación INVEST**
- I: ✅ Se puede construir y probar por separado de las demás historias.
- N: ✅ Describe el resultado, no la tecnología.
- V: ✅ Le sirve directamente a: todos los funcionarios.
- E: ✅ Las reglas y los mensajes están definidos.
- S: ✅ Se limita a «consultar y filtrar trámites».
- T: ✅ Cada escenario tiene un resultado que se puede comprobar en la aplicación.

**Criterios de aceptación (Gherkin)**
```gherkin
# language: es
Característica: Consultar y filtrar trámites

  Escenario: Ver la bandeja
    Dado que el funcionario inició sesión
    Cuando abre «Trámites»
    Entonces ve radicado, tipo, clientes, fecha, digitador y estado de cada trámite, del más nuevo al más antiguo

  Escenario: Filtrar
    Dado que hay trámites de varios tipos y estados
    Cuando elige un estado, un tipo o escribe un radicado, documento o nombre
    Entonces la lista muestra solo los trámites que cumplen todos los filtros

  Escenario: Sin resultados
    Dado que ningún trámite cumple los filtros
    Cuando aplica los filtros
    Entonces la tabla muestra «No hay trámites.»

  Escenario: Abrir un trámite
    Dado que el trámite está en la lista
    Cuando pulsa «Abrir»
    Entonces el sistema muestra el detalle del trámite (HU-11)
```

**Requisitos no funcionales**
- La página se adapta a celular (P21).

**Supuestos / notas**
- Todos los roles ven la bandeja completa.
