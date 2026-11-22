# HU-04: Gestionar usuarios

**Como** administrador
**Quiero** crear usuarios, activarlos o desactivarlos y cambiarles la contraseña
**Para** controlar quién trabaja en el sistema.

| Campo | Valor |
|---|---|
| Módulo | Acceso y usuarios |
| Requerimientos | RF-02 |
| Usuarios con acceso | Administrador |
| Prioridad | Alta |
| Dependencias | HU-01 |
| Repositorios | sistema-notarial-frontend, sistema-notarial-backend |

**Trazabilidad con el diseño (diagramas en Visual Paradigm)**

| Vista | Elementos |
|---|---|
| Contexto (flujo) | Usuarios, roles y tarifas |
| Funcional (componente) | Interfaz configurar sistema (administración) |
| Lógica | Usuario (nombreUsuario, correo, activo) · Rol |
| Desarrollo | VentanaAdministracion.gestionarUsuarios() → AdministracionControlador.crearUsuario(), gestionarUsuario() → Nucleo.agregarUsuario() → BaseDatos.guardar(usuario) |
| Mockup | P16 Administración: usuarios y tarifas |

**Validación INVEST**
- I: ✅ Se puede construir y probar por separado de las demás historias.
- N: ✅ Describe el resultado, no la tecnología.
- V: ✅ Le sirve directamente a: administrador.
- E: ✅ Las reglas y los mensajes están definidos.
- S: ✅ Se limita a «gestionar usuarios».
- T: ✅ Cada escenario tiene un resultado que se puede comprobar en la aplicación.

**Criterios de aceptación (Gherkin)**
```gherkin
# language: es
Característica: Gestionar usuarios

  Escenario: Crear un usuario
    Dado que el administrador está en Administración
    Cuando escribe nombre de usuario, correo, rol y contraseña y pulsa «Crear usuario»
    Entonces el sistema guarda el usuario
    Y lo muestra en la tabla de usuarios
    Y muestra «Usuario <nombre> creado»

  Escenario: Nombre de usuario repetido
    Dado que ya existe un usuario con ese nombre
    Cuando el administrador intenta crear otro con el mismo nombre
    Entonces el sistema no lo guarda
    Y muestra «Ya existe el usuario <nombre>»

  Escenario: Contraseña muy corta
    Dado que el administrador escribe una contraseña de menos de 4 caracteres
    Cuando intenta crear el usuario o cambiar su contraseña
    Entonces el sistema no lo guarda
    Y muestra «La contraseña debe tener al menos 4 caracteres»

  Escenario: Desactivar y activar
    Dado que un usuario está activo
    Cuando el administrador pulsa «Desactivar»
    Entonces el usuario queda «Inactivo» y ya no puede iniciar sesión
    Y al pulsar «Activar» vuelve a poder entrar

  Escenario: No desactivarse a sí mismo
    Dado que el administrador está en su propia fila
    Cuando pulsa «Desactivar»
    Entonces el sistema no lo desactiva
    Y muestra «No puede desactivarse a sí mismo»

  Escenario: Cambiar contraseña
    Dado que el administrador escribe una contraseña nueva en la fila de un usuario
    Cuando pulsa «Cambiar contraseña»
    Entonces el usuario entra desde ese momento con la contraseña nueva
```

**Requisitos no funcionales**
- Las contraseñas se guardan cifradas (RNF-01).

**Supuestos / notas**
- Los usuarios no se borran, se desactivan: sus trámites conservan el nombre del digitador.
- Si la base está vacía, el sistema crea admin, recepcion, digitador, notario y cajero.
