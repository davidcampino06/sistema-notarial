# HU-05: Controlar el acceso por rol

**Como** administrador
**Quiero** que cada rol vea y use solo sus funciones
**Para** que nadie haga acciones que no le corresponden.

| Campo | Valor |
|---|---|
| Módulo | Acceso y usuarios |
| Requerimientos | RF-03 |
| Usuarios con acceso | Todos los funcionarios |
| Prioridad | Alta |
| Dependencias | HU-01, HU-04 |
| Repositorios | sistema-notarial-frontend, sistema-notarial-backend |

**Trazabilidad con el diseño (diagramas en Visual Paradigm)**

| Vista | Elementos |
|---|---|
| Contexto (flujo) | Radicaciones, revisiones y pagos |
| Funcional (componente) | Todos los componentes (revisión de sesión y rol) |
| Lógica | Rol.es() · Sesion |
| Desarrollo | menuRol() y PaginaInterna (React) → Controlador.sesion(autorizacion, roles) → 403 |
| Mockup | P19 Acceso denegado por rol, P04 Bandeja de trámites |

**Validación INVEST**
- I: ✅ Se puede construir y probar por separado de las demás historias.
- N: ✅ Describe el resultado, no la tecnología.
- V: ✅ Le sirve directamente a: todos los funcionarios.
- E: ✅ Las reglas y los mensajes están definidos.
- S: ✅ Se limita a «controlar el acceso por rol».
- T: ✅ Cada escenario tiene un resultado que se puede comprobar en la aplicación.

**Criterios de aceptación (Gherkin)**
```gherkin
# language: es
Característica: Controlar el acceso por rol

  Escenario: Menú según el rol
    Dado que un funcionario inició sesión
    Cuando mira el menú
    Entonces ve «Trámites» y solo la ventana de su rol: Radicación (recepcionista), Revisión (notario), Caja (cajero) o Administración (administrador)

  Escenario: Abrir una ventana de otro rol
    Dado que el cajero inició sesión
    Cuando escribe la dirección /administracion
    Entonces el sistema no muestra la ventana
    Y muestra «Su rol no tiene acceso a esta ventana.»

  Escenario: Llamar la API sin permiso
    Dado que el cajero tiene una sesión abierta
    Cuando llama directamente a GET /api/usuarios
    Entonces el sistema responde 403 con «Su rol no tiene acceso a esta acción»

  Escenario: Acción del flujo sin permiso
    Dado que un trámite está En revisión
    Cuando un usuario que no es notario intenta aprobarlo
    Entonces el sistema no lo cambia
    Y muestra «Su rol (<rol>) no puede hacer esta acción»
```

**Requisitos no funcionales**
- Un usuario sin permiso recibe acceso denegado aunque llame la API directamente (RNF-02).

**Supuestos / notas**
- El rol se asigna al crear el usuario (HU-04).
