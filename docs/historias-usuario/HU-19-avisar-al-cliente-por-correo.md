# HU-19: Avisar al cliente por correo

**Como** cliente
**Quiero** recibir un correo cuando mi trámite avance
**Para** no tener que llamar a preguntar.

| Campo | Valor |
|---|---|
| Módulo | Flujo y seguimiento del trámite |
| Requerimientos | RF-21 |
| Usuarios con acceso | Cliente |
| Prioridad | Media |
| Dependencias | HU-07, HU-14, HU-16 |
| Repositorios | sistema-notarial-frontend, sistema-notarial-backend |

**Trazabilidad con el diseño (diagramas en Visual Paradigm)**

| Vista | Elementos |
|---|---|
| Contexto (flujo) | Notificaciones al cliente · Confirmación de envío |
| Funcional (componente) | Gestión de Trámites (servidor de correo Brevo) |
| Lógica | Cliente.tieneCorreo() |
| Desarrollo | Nucleo.cambiarEstado() → Nucleo.notificarCliente() → Nucleo.enviarCorreo() → API de Brevo |
| Mockup | Sin pantalla (aviso por correo) |

**Validación INVEST**
- I: ✅ Se puede construir y probar por separado de las demás historias.
- N: ✅ Describe el resultado, no la tecnología.
- V: ✅ Le sirve directamente a: cliente.
- E: ✅ Las reglas y los mensajes están definidos.
- S: ✅ Se limita a «avisar al cliente por correo».
- T: ✅ Cada escenario tiene un resultado que se puede comprobar en la aplicación.

**Criterios de aceptación (Gherkin)**
```gherkin
# language: es
Característica: Avisar al cliente por correo

  Escenario: Trámite aprobado o firmado
    Dado que el cliente tiene correo y el correo está configurado
    Cuando el trámite pasa a Aprobado o a Firmado
    Entonces el cliente recibe un correo con el radicado y el estado nuevo
    Y si está Firmado, el correo dice que ya puede reclamarlo

  Escenario: Cliente sin correo
    Dado que el cliente no tiene correo
    Cuando el trámite cambia de estado
    Entonces no se envía aviso
    Y el trámite avanza igual

  Escenario: Falla el correo
    Dado que el servidor de correo responde con error
    Cuando el trámite cambia de estado
    Entonces el cambio de estado se guarda igual
    Y el error queda en el registro del servidor
```

**Requisitos no funcionales**
- Se usa la API de Brevo por HTTPS porque Render gratis bloquea SMTP.

**Supuestos / notas**
- Se avisa también al aprobar, no solo al firmar (RF-21 ampliado según el diagrama de contexto).
