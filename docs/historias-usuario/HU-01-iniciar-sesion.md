# HU-01: Iniciar sesión

**Como** funcionario de la notaría
**Quiero** iniciar sesión con mi usuario y mi contraseña
**Para** entrar solo a las funciones de mi rol.

| Campo | Valor |
|---|---|
| Módulo | Acceso y usuarios |
| Requerimientos | RF-01, RF-03 |
| Usuarios con acceso | Recepcionista, digitador, notario, cajero y administrador |
| Prioridad | Alta |
| Dependencias | HU-04 (debe existir el usuario) |
| Repositorios | sistema-notarial-frontend, sistema-notarial-backend |

**Trazabilidad con el diseño (diagramas en Visual Paradigm)**

| Vista | Elementos |
|---|---|
| Contexto (flujo) | Radicaciones, revisiones y pagos · Bandeja de trámites y recibos |
| Funcional (componente) | Gestión de Trámites (interfaz de ingreso) |
| Lógica | Usuario.verificarContrasena(), activo · Rol |
| Desarrollo | VentanaLogin.iniciarSesion() → AutenticacionControlador.iniciarSesion() → Nucleo.iniciarSesion() → Sesion.abrir() |
| Mockup | P01 Inicio de sesión, P18 Ingreso con datos incorrectos, P04 Bandeja de trámites |

**Validación INVEST**
- I: ✅ Se puede construir y probar por separado de las demás historias.
- N: ✅ Describe el resultado, no la tecnología.
- V: ✅ Le sirve directamente a: recepcionista, digitador, notario, cajero y administrador.
- E: ✅ Las reglas y los mensajes están definidos.
- S: ✅ Se limita a «iniciar sesión».
- T: ✅ Cada escenario tiene un resultado que se puede comprobar en la aplicación.

**Criterios de aceptación (Gherkin)**
```gherkin
# language: es
Característica: Iniciar sesión

  Escenario: Iniciar sesión con datos correctos
    Dado que el funcionario tiene un usuario activo
    Cuando escribe su usuario y su contraseña correctos y pulsa «Ingresar»
    Entonces el sistema le da acceso
    Y muestra la bandeja de trámites con el menú de su rol

  Escenario: Usuario o contraseña incorrectos
    Dado que el funcionario escribe un usuario que no existe o una contraseña equivocada
    Cuando pulsa «Ingresar»
    Entonces el sistema no le da acceso
    Y muestra «Usuario o contraseña incorrectos», sin decir cuál de los dos datos falló

  Escenario: Usuario desactivado
    Dado que el administrador desactivó al usuario
    Cuando el funcionario intenta ingresar con su contraseña correcta
    Entonces el sistema no le da acceso
    Y muestra «El usuario está inactivo. Hable con el administrador»

  Escenario: Pedir datos sin iniciar sesión
    Dado que nadie inició sesión en el navegador
    Cuando se pide la lista de trámites a la API
    Entonces el sistema responde 401 con «Debe iniciar sesión»
```

**Requisitos no funcionales**
- Las contraseñas se guardan cifradas con PBKDF2 y sal; nunca quedan legibles en la base de datos (RNF-01).
- La comunicación va por HTTPS (RNF-03).

**Supuestos / notas**
- No hay bloqueo tras cinco intentos fallidos: no está en los diagramas, así que no se implementó (RF-01 se cumple en lo demás).
- La sesión se identifica con un token que React envía en el encabezado Authorization.
