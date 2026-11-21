# Sistema de Gestión Notarial (SGN)

Aplicación web en **Java** (programación orientada a objetos) con base de datos **MongoDB Atlas**.
Está hecha **solo con lo que aparece en los diagramas** del proyecto (vista de contexto, funcional,
lógica, de desarrollo y de despliegue).

---

## 1. De los diagramas al código

### Vista lógica → paquete `notaria.modelo`

| Clase del diagrama | Archivo | Qué hace |
|---|---|---|
| `Tramite` (abstracta) | `Tramite.java` | Datos comunes, `avanzar()` y el método abstracto `calcularDerechos()` |
| `Autenticacion` | `Autenticacion.java` | `numeroFolios`; derechos = folios × tarifa por folio |
| `DeclaracionExtrajuicio` | `DeclaracionExtrajuicio.java` | `contenido`; derechos = tarifa fija |
| `PoderEspecial` | `PoderEspecial.java` | `objetoPoder`; derechos = tarifa fija |
| `EscrituraCompraventa` | `EscrituraCompraventa.java` | `valorInmueble`; derechos = % del valor |
| `Cliente` | `Cliente.java` | `numeroDocumento`, `nombres`, `telefono`, `tipoDocumento` |
| `Usuario` / `Rol` | `Usuario.java`, `Rol.java` | Usuario * — 1 Rol. La contraseña se guarda cifrada |
| `DocumentoAdjunto` | `DocumentoAdjunto.java` | `nombreArchivo`, `fechaCarga` (composición con Trámite) |
| `Liquidacion` | `Liquidacion.java` | `valorDerechos`, `iva`, `total`, `calcularTotal()` |
| `Pago` | `Pago.java` | `valor`, `medioPago`, `fecha` |
| `EstadoTramite`, `MedioPago` | enums | Las enumeraciones del diagrama |

Asociaciones: Cliente 1..* — * Trámite (`clientes`), Trámite * — 0..1 Usuario digitador (`digitador`),
Trámite contiene 0..* DocumentoAdjunto (`documentos`), Trámite genera 0..1 Liquidación,
Liquidación se paga con 0..1 Pago.

### Vista de desarrollo → paquetes `notaria` y `notaria.ventanas`

| Clase del diagrama | Métodos implementados |
|---|---|
| `Main` | `inicializarNucleo()`, `abrirVentanaLogin()` |
| `Nucleo` | `iniciarSesion()`, `radicarTramite()`, `cambiarEstado()`, `registrarPago()`, `liquidar()`, `guardarBaseDatos()`, `leerBaseDatos()` |
| `Sesion` | `usuarioActual`, `rol` |
| `Configuracion` | `tarifasVigentes`, `servidorCorreo` |
| `ListaClientes` | `clientes` |
| `VentanaLogin` | `iniciarSesion()`, `recuperarContrasena()` |
| `VentanaPrincipal` | `abrirTramite()`, `filtrarTramites()`, `cerrarSesion()` (+ `listaTramites`, `filtros`, `menuRol`) |
| `VentanaRadicacion` | `radicar()`, `adjuntarDocumento()` (+ `selectorTipo`, `buscadorCliente`, `listaDocumentos`) |
| `VentanaRevision` | `aprobar()`, `devolver()`, `firmar()` (+ `visorDocumento`, `campoObservaciones`) |
| `VentanaCaja` | `registrarPago()`, `generarRecibo()`, `cierreCaja()` (+ `detalleLiquidacion`, `selectorMedioPago`) |
| `VentanaAdministracion` | `gestionarUsuarios()`, `configurarTarifas()` (+ `tablaUsuarios`, `tablaTarifas`) |

Como el despliegue dice **Servidor Web + Navegador (HTML5)**, cada “Ventana” es una clase Java que
dibuja su página HTML y atiende los botones de esa página.

### Vista funcional → componentes

* **Gestión de Clientes** → `Cliente`, `ListaClientes`, buscador de `VentanaRadicacion`.
* **Gestión de Trámites** → `Tramite` y subclases, `Nucleo.cambiarEstado()`, `VentanaPrincipal`, `VentanaRevision`.
  Interfaz *consultar trámite* = `abrirTramite()`; *configurar sistema* (Administrador) = `VentanaAdministracion`.
* **Caja y Liquidación** → `Liquidacion`, `Pago`, `VentanaCaja`. Interfaz *liquidar trámite* = `Nucleo.liquidar()`.

### Vista de despliegue

Servidor Web (Render) · Servidor de Base de Datos (MongoDB Atlas M0, TCP/TLS:27017) · Navegador (HTML5, HTTPS:443) · Servidor de correo (SMTP:587).


### Lo que NO está en el código (porque no tiene clase ni método en los diagramas)

* “Reportes y auditoría” (sale solo como flujo en la vista de contexto).
* Notificaciones por correo al cliente: `Cliente` no tiene atributo `correo` en la vista lógica.
* Subir el archivo real del documento: `DocumentoAdjunto` solo guarda `nombreArchivo` y `fechaCarga`.

El atributo `observaciones` de `Tramite` guarda lo que escribe el notario en `campoObservaciones`
al devolver (y el motivo al anular).

---

## 2. Reglas del negocio (dónde están)

Todas están en `Nucleo.cambiarEstado()`, `Nucleo.liquidar()` y `Nucleo.registrarPago()`:

* Flujo: Radicado → En elaboración (asignar digitador) → En revisión → Aprobado → Firmado → Entregado.
  En revisión → Devuelto → En elaboración. Cualquier estado antes de Firmado → Anulado (con motivo).
* Solo el **notario** aprueba, devuelve, firma y anula.
* No se puede **firmar sin pago** registrado.
* Un trámite **firmado o entregado no se puede anular**.
* Al devolver o anular es obligatorio escribir el motivo.
* Radicado con formato `AÑO-consecutivo` (ej. `2026-000001`); el consecutivo vuelve a 1 cada año.
* Liquidación: derechos (según el tipo de trámite) + IVA 19 % = total.

## 3. Dónde se ve la POO (para la sustentación)

* **Abstracción y herencia**: `Tramite` es abstracta; sus 4 tipos heredan de ella. `Ventana` es la clase padre de las 6 ventanas.
* **Polimorfismo**: `Nucleo.liquidar()` llama `tramite.calcularDerechos()` sin saber el tipo; cada subclase responde distinto.
* **Encapsulamiento**: atributos `private`; la contraseña solo cambia con `cambiarContrasena()` y se guarda cifrada.
* **Composición**: `Tramite` contiene su lista de `DocumentoAdjunto`.
* **Singleton**: `Configuracion.getInstancia()`.
* **Enumeraciones**: `EstadoTramite`, `MedioPago`.

Recorrido de una acción (ej. *Aprobar*): el navegador envía el formulario → `VentanaRevision.aprobar()` →
`Nucleo.cambiarEstado()` revisa rol y estado → `Tramite.avanzar()` cambia el estado →
`Nucleo.guardarBaseDatos(tramite)` lo guarda en MongoDB → la ventana muestra el resultado.

---

## 4. Usuarios iniciales

La primera vez que arranca con la base vacía se crean (contraseña `clave123` para todos):

| Usuario | Rol |
|---|---|
| `admin` | Administrador |
| `recepcion` | Recepcionista |
| `digitador` | Digitador |
| `notario` | Notario |
| `cajero` | Cajero |

Cambie esas contraseñas en **Administración** después de desplegar.

---

## 5. Crear la base de datos gratis (MongoDB Atlas)

1. Entre a <https://www.mongodb.com/cloud/atlas/register> y cree la cuenta.
2. Cree un clúster **gratis (M0 / Free)**.
3. **Database Access** → *Add New Database User* → usuario y contraseña (anótelos).
4. **Network Access** → *Add IP Address* → *Allow access from anywhere* (`0.0.0.0/0`). Es necesario para Render.
5. **Connect** → *Drivers* → copie la cadena `mongodb+srv://...` y cambie `<password>` por su contraseña.
   Si la contraseña tiene símbolos como `@` o `#`, use una sin símbolos para evitar problemas.

La base de datos `notaria` y sus colecciones (`usuarios`, `clientes`, `tramites`, `configuracion`)
se crean solas al arrancar el programa.

## 6. Ejecutar en VS Code

1. Instale **JDK 17 o superior** y en VS Code la extensión **Extension Pack for Java** (VS Code la sugiere al abrir la carpeta).
2. Abra la carpeta `sistema-notarial` en VS Code y espere a que descargue las dependencias de Maven (barra inferior).
3. Copie `.env.example` como `.env` y pegue su cadena de Atlas en `MONGODB_URI`.
4. Abra `src/main/java/notaria/Main.java` → **Run and Debug** → *Sistema Notarial* (o la tecla F5).
5. Abra <http://localhost:8080> en el navegador.

Sin `MONGODB_URI` también arranca, pero solo en memoria (los datos se pierden al cerrar).

## 7. Publicar en Render (gratis)

> Vercel no sirve para este proyecto: ejecuta sitios estáticos y funciones cortas (Node.js, Python, Go…), no un servidor Java que quede encendido.

1. Suba esta carpeta a un repositorio de GitHub (el `.env` no se sube; está en `.gitignore`).
2. En <https://render.com> → **New → Web Service** → conecte el repositorio.
3. *Language*: **Docker** · *Instance type*: **Free**.
4. En **Environment Variables** agregue `MONGODB_URI` (su cadena de Atlas) y `MONGODB_DB` = `notaria`.
5. **Create Web Service**. Cuando termine le da una dirección `https://....onrender.com`.

En el plan gratis el servicio se “duerme” tras 15 minutos sin uso; la primera visita tarda un poco
en despertarlo. Los datos no se pierden porque están en Atlas.

### Correo para “¿Olvidó su contraseña?” (opcional)

Agregue `SMTP_HOST`, `SMTP_PUERTO`, `SMTP_USUARIO`, `SMTP_CONTRASENA` (ver `.env.example`).
Funciona en local; **el plan gratis de Render bloquea los puertos SMTP**, así que allá el sistema muestra
el mensaje “pida al administrador una contraseña nueva” y el administrador la cambia en **Administración**.

---

## 8. Cómo agregar o quitar una función

### Agregar un nuevo tipo de trámite (ej. *Registro civil*)

1. Cree `modelo/RegistroCivil.java` que **extienda `Tramite`** e implemente `calcularDerechos()`, `getTipo()` y `getDetalle()`
   (copie `PoderEspecial.java` como guía).
2. Si cobra una tarifa nueva, agréguela en el constructor de `Configuracion` (aparece sola en Administración).
3. En `VentanaRadicacion`: agregue la opción en el `selectorTipo`, su campo y su `case` en `radicar()`.
4. En `Nucleo`: agregue su dato en `guardarBaseDatos(Tramite)` y su `case` en `leerTramite()`.

### Agregar un dato a una clase (ej. `correo` al `Cliente`)

1. Atributo + getter en `Cliente.java` y parámetro en el constructor.
2. Campo en `VentanaRadicacion.bloqueCliente()` y lectura en `agregarCliente()`.
3. Guardarlo/leerlo en `Nucleo.guardarBaseDatos(Cliente)` y `leerBaseDatos()`.

### Agregar una acción a una ventana

1. Método en la ventana (ej. `VentanaCaja.anularPago()`), con un `case` nuevo en `manejar()`.
2. Si cambia datos, la regla va en `Nucleo` y al final se llama `guardarBaseDatos(...)`.
3. El botón es un `<form method='post' action='/caja/...'>` en el HTML de esa ventana.

### Quitar una función

Borre el botón/formulario del HTML, su `case` en `manejar()` y el método. Si era la única que usaba un
método del `Nucleo`, bórrelo también. Para quitar una ventana completa: borre su clase, su línea
`createContext` en `Main.abrirVentanaLogin()` y su enlace en `VentanaPrincipal.menuRol()`.

---

## 9. Estructura

```
sistema-notarial/
├── pom.xml                 dependencias (MongoDB, correo) y empaquetado
├── Dockerfile              cómo se construye y ejecuta en Render
├── .env.example            plantilla de variables (MONGODB_URI, SMTP...)
└── src/main/java/notaria/
    ├── Main.java
    ├── Nucleo.java
    ├── Sesion.java
    ├── Configuracion.java
    ├── ListaClientes.java
    ├── modelo/             clases de la vista lógica
    └── ventanas/           Ventana (padre) + 6 ventanas de la vista de desarrollo
```
