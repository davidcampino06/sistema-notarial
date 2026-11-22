# Sistema de Gestión Notarial — Backend (Spring Boot)

API REST en **Java 17 + Spring Boot 3.5** con base de datos **MongoDB Atlas**.
La parte visual está en el repositorio **sistema-notarial-frontend** (React).

```
React (Vercel)  ──HTTPS/JSON──>  Spring Boot (Render)  ──TCP/TLS:27017──>  MongoDB Atlas
   ventanas                      controladores → Núcleo → BaseDatos
```

## 1. Carpetas (cómo explicar el código)

```
src/main/java/notaria/
├── Main.java              arranca Spring Boot (y permite que React lo llame: CORS)
├── controladores/         RECIBEN las peticiones de React (API REST)
│   ├── Controlador.java           clase padre: revisa la sesión y el rol
│   ├── AutenticacionControlador   iniciar sesión, cerrar sesión, recuperar contraseña
│   ├── TramiteControlador         Gestión de Trámites (radicar, cambiar estado, consulta pública)
│   ├── ClienteControlador         Gestión de Clientes (buscar cliente)
│   ├── CajaControlador            Caja y Liquidación (liquidar, registrar pago)
│   ├── AdministracionControlador  configurar sistema (usuarios y tarifas)
│   └── ManejadorErrores           convierte los errores en {"mensaje": "..."}
├── logica/                REGLAS DEL NEGOCIO (vista de desarrollo)
│   ├── Nucleo.java                radicar, cambiarEstado, liquidar, registrarPago, correos…
│   ├── Sesion.java                quién inició sesión y con qué rol
│   ├── Configuracion.java         tarifas vigentes y servidor de correo
│   └── ListaClientes.java         lista de clientes
├── modelo/                CLASES DE LA VISTA LÓGICA
│   └── Tramite (abstracta) y sus 4 tipos, Cliente, Usuario, Rol, DocumentoAdjunto,
│       Liquidacion, Pago, EstadoTramite, MedioPago
└── basedatos/             MONGODB
    └── BaseDatos.java             conexión a Atlas, guardar y leer cada colección
```

**Cómo fluye una acción** (ejemplo: el notario aprueba un trámite):

1. **React** — `VentanaRevision.aprobar()` envía `POST /api/tramites/{radicado}/estado`.
2. **controladores** — `TramiteControlador.cambiarEstado()` revisa la sesión.
3. **logica** — `Nucleo.cambiarEstado()` revisa el rol y el estado; `Tramite.avanzar()` cambia el estado.
4. **basedatos** — `BaseDatos.guardar(tramite)` lo guarda en MongoDB Atlas.
5. **logica** — `Nucleo.notificarCliente()` envía el correo al cliente (si hay correo configurado).
6. **React** recibe el trámite actualizado y lo muestra.

## 2. API REST

| Método y ruta | Quién | Qué hace |
|---|---|---|
| `POST /api/sesion` | todos | iniciar sesión → `{token, nombreUsuario, rol}` |
| `DELETE /api/sesion` | con sesión | cerrar sesión |
| `POST /api/sesion/recuperar` | todos | contraseña temporal por correo |
| `GET /api/tramites` | con sesión | todos los trámites |
| `GET /api/tramites/{radicado}` | con sesión | un trámite |
| `POST /api/tramites` | Recepcionista | radicar |
| `POST /api/tramites/{radicado}/estado` | según la regla | asignar, revisar, aprobar, devolver, firmar, entregar, anular |
| `GET /api/digitadores` | Recepcionista, Notario | digitadores activos |
| `GET /api/consulta?radicado=&documento=` | público | estado del trámite para el cliente |
| `GET /api/clientes/{documento}` | Recepcionista | buscar cliente |
| `GET /api/clientes` | Administrador | lista de clientes (reportes) |
| `POST /api/caja/{radicado}/liquidar` | Cajero | liquidar (derechos + IVA) |
| `POST /api/caja/{radicado}/pago` | Cajero | registrar pago |
| `GET/POST /api/usuarios`, `PUT /api/usuarios/{nombre}` | Administrador | gestionar usuarios |
| `GET/PUT /api/tarifas` | Administrador | configurar tarifas |

React envía el token en el encabezado `Authorization: Bearer <token>`.

## 3. Base de datos (MongoDB Atlas)

La capa de base de datos es la clase `basedatos/BaseDatos.java`. Las colecciones se crean solas:

| Colección | `_id` | Campos | Clase |
|---|---|---|---|
| `usuarios` | nombreUsuario | correo, contrasena (cifrada PBKDF2), activo, rol | `Usuario` + `Rol` |
| `clientes` | numeroDocumento | nombres, telefono, tipoDocumento, correo | `Cliente` |
| `tramites` | numeroRadicado | tipo, fechaRadicacion, estado, observaciones, digitador, clientes (documentos), documentos [{nombreArchivo, fechaCarga}], dato del tipo (numeroFolios / contenido / objetoPoder / valorInmueble), liquidacion {valorDerechos, iva, total, pago {valor, medioPago, fecha}} | `Tramite` y subclases, `DocumentoAdjunto`, `Liquidacion`, `Pago` |
| `configuracion` | "tarifas" | valores {nombre de tarifa: valor} | `Configuracion` |

## 4. Usuarios iniciales

Si la base está vacía se crean `admin`, `recepcion`, `digitador`, `notario` y `cajero`, todos con
contraseña `clave123`. Cámbielas en Administración.

## 5. Ejecutar en VS Code

1. JDK 17+ y la extensión **Extension Pack for Java**.
2. Copie `.env.example` como `.env` y ponga su `MONGODB_URI` de Atlas.
3. **F5** → *Backend Sistema Notarial*. Queda en <http://localhost:8080/api/...>
4. Para verlo con pantallas, ejecute también el frontend (ver su README).

## 6. Desplegar en Render

Web Service con **Language: Docker**. Variables de entorno:

| Variable | Valor |
|---|---|
| `MONGODB_URI` | cadena de conexión de Atlas |
| `MONGODB_DB` | `notaria` |
| `FRONTEND_URL` | dirección del frontend en Vercel (para CORS) |
| `BREVO_API_KEY`, `CORREO_REMITENTE` | opcional, para enviar correos |

## 7. Agregar o quitar una función

* **Nuevo tipo de trámite:** clase en `modelo/` que extienda `Tramite`; su `case` en
  `TramiteControlador.radicarTramite()` y en `BaseDatos` (`guardar(Tramite)` y `leerTramite()`).
* **Nueva acción:** la regla en `Nucleo`, un método en el controlador que corresponda
  (`@PostMapping`…) y el botón en la ventana de React.
* **Quitar una acción:** borrar el método del controlador y el botón en React.

## 8. Historias de usuario y mockups

* [docs/historias-usuario](docs/historias-usuario/00-resumen.md): 25 historias de usuario con criterios de
  aceptación en Gherkin. Cada una también está como **issue** del repositorio (etiqueta `historia de usuario`).
* [docs/mockups](docs/mockups/README.md): 21 pantallas (P01–P21) de la aplicación, relacionadas con las historias.
