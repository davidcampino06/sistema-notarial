# HU-03: Recuperar contraseña

**Como** funcionario
**Quiero** recibir una contraseña temporal en mi correo
**Para** volver a entrar sin esperar al administrador.

| Campo | Valor |
|---|---|
| Módulo | Acceso y usuarios |
| Requerimientos | RF-04 |
| Usuarios con acceso | Todos los funcionarios |
| Prioridad | Media |
| Dependencias | HU-04 (el usuario debe tener correo) |
| Repositorios | sistema-notarial-frontend, sistema-notarial-backend |

**Trazabilidad con el diseño (diagramas en Visual Paradigm)**

| Vista | Elementos |
|---|---|
| Contexto (flujo) | Notificaciones al cliente · Confirmación de envío (servidor de correo) |
| Funcional (componente) | Gestión de Trámites (correo por Brevo) |
| Lógica | Usuario.cambiarContrasena() |
| Desarrollo | VentanaLogin.recuperarContrasena() → AutenticacionControlador.recuperarContrasena() → Nucleo.enviarCorreo() → Configuracion |
| Mockup | P02 Recuperar contraseña |

**Validación INVEST**
- I: ✅ Se puede construir y probar por separado de las demás historias.
- N: ✅ Describe el resultado, no la tecnología.
- V: ✅ Le sirve directamente a: todos los funcionarios.
- E: ✅ Las reglas y los mensajes están definidos.
- S: ✅ Se limita a «recuperar contraseña».
- T: ✅ Cada escenario tiene un resultado que se puede comprobar en la aplicación.

**Criterios de aceptación (Gherkin)**
```gherkin
# language: es
Característica: Recuperar contraseña

  Escenario: Usuario con correo registrado
    Dado que el usuario existe, tiene correo y el servidor de correo está configurado
    Cuando escribe su usuario y pulsa «Enviar contraseña temporal»
    Entonces el sistema le envía por correo una contraseña temporal de 10 caracteres
    Y muestra «Se envió una contraseña temporal a su correo»

  Escenario: Usuario sin correo o inexistente
    Dado que el usuario no existe o no tiene correo registrado
    Cuando pide la contraseña temporal
    Entonces el sistema no cambia ninguna contraseña
    Y muestra «El usuario no existe o no tiene correo registrado. Hable con el administrador»

  Escenario: Correo no configurado
    Dado que el servidor de correo no está configurado
    Cuando el usuario pide la contraseña temporal
    Entonces el sistema no cambia la contraseña
    Y muestra «El servidor de correo no está configurado. Pida al administrador una contraseña nueva»

  Escenario: Falla el envío
    Dado que el servidor de correo responde con error
    Cuando el usuario pide la contraseña temporal
    Entonces la contraseña anterior sigue funcionando
    Y el sistema muestra el error
```

**Requisitos no funcionales**
- El correo se envía por la API de Brevo sobre HTTPS.

**Supuestos / notas**
- En lugar del enlace de 30 minutos de RF-04, el sistema envía una contraseña temporal; así quedó en los diagramas (servidor de correo y Nucleo.enviarCorreo()).
