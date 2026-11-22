# HU-02: Cerrar sesión

**Como** funcionario
**Quiero** cerrar mi sesión cuando termino
**Para** que nadie use el sistema con mi usuario.

| Campo | Valor |
|---|---|
| Módulo | Acceso y usuarios |
| Requerimientos | RF-01 |
| Usuarios con acceso | Todos los funcionarios |
| Prioridad | Alta |
| Dependencias | HU-01 |
| Repositorios | sistema-notarial-frontend, sistema-notarial-backend |

**Trazabilidad con el diseño (diagramas en Visual Paradigm)**

| Vista | Elementos |
|---|---|
| Contexto (flujo) | Bandeja de trámites y recibos |
| Funcional (componente) | Gestión de Trámites |
| Lógica | Sesion |
| Desarrollo | PaginaInterna.cerrarSesion() (VentanaPrincipal) → AutenticacionControlador.cerrarSesion() → Sesion.cerrar() |
| Mockup | P04 Bandeja de trámites, P01 Inicio de sesión |

**Validación INVEST**
- I: ✅ Se puede construir y probar por separado de las demás historias.
- N: ✅ Describe el resultado, no la tecnología.
- V: ✅ Le sirve directamente a: todos los funcionarios.
- E: ✅ Las reglas y los mensajes están definidos.
- S: ✅ Se limita a «cerrar sesión».
- T: ✅ Cada escenario tiene un resultado que se puede comprobar en la aplicación.

**Criterios de aceptación (Gherkin)**
```gherkin
# language: es
Característica: Cerrar sesión

  Escenario: Cerrar sesión
    Dado que el funcionario está dentro del sistema
    Cuando pulsa «Cerrar sesión»
    Entonces el sistema vuelve a la pantalla de ingreso
    Y muestra «Sesión cerrada»

  Escenario: El token ya no sirve
    Dado que el funcionario cerró la sesión
    Cuando se intenta usar el mismo token en la API
    Entonces el sistema responde 401 con «Debe iniciar sesión»

  Escenario: Entrar a una página interna sin sesión
    Dado que no hay sesión abierta en el navegador
    Cuando se abre la dirección /principal
    Entonces el sistema lleva a la pantalla de ingreso
```

**Requisitos no funcionales**
- La interfaz está en español y no muestra mensajes técnicos (RNF-12).

**Supuestos / notas**
- El cierre automático por 15 minutos de inactividad (RF-05) no está en los diagramas y no se implementó.
