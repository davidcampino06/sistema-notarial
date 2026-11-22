# Backlog de historias de usuario · Sistema de Gestión Notarial

25 historias que describen lo que hace el sistema, alineadas con los diagramas en Visual Paradigm
(vistas de contexto, funcional, lógica, desarrollo y despliegue) y con el código de
`sistema-notarial-backend` (Spring Boot) y `sistema-notarial-frontend` (React).

| ID | Historia | Módulo | Prioridad | Usuarios con acceso | Requerimientos | Mockups |
|---|---|---|---|---|---|---|
| [HU-01](HU-01-iniciar-sesion.md) | Iniciar sesión | Acceso y usuarios | Alta | Recepcionista, digitador, notario, cajero y administrador | RF-01, RF-03 | P01, P18, P04 |
| [HU-02](HU-02-cerrar-sesion.md) | Cerrar sesión | Acceso y usuarios | Alta | Todos los funcionarios | RF-01 | P04, P01 |
| [HU-03](HU-03-recuperar-contrasena.md) | Recuperar contraseña | Acceso y usuarios | Media | Todos los funcionarios | RF-04 | P02 |
| [HU-04](HU-04-gestionar-usuarios.md) | Gestionar usuarios | Acceso y usuarios | Alta | Administrador | RF-02 | P16 |
| [HU-05](HU-05-controlar-el-acceso-por-rol.md) | Controlar el acceso por rol | Acceso y usuarios | Alta | Todos los funcionarios | RF-03 | P19, P04 |
| [HU-06](HU-06-buscar-cliente.md) | Buscar cliente | Gestión de clientes | Alta | Recepcionista | RF-07 | P05 |
| [HU-07](HU-07-registrar-cliente-al-radicar.md) | Registrar cliente al radicar | Gestión de clientes | Alta | Recepcionista | RF-06, RF-10 | P05 |
| [HU-08](HU-08-radicar-tramite.md) | Radicar trámite | Radicación de trámites | Alta | Recepcionista | RF-11, RF-12 | P05, P06 |
| [HU-09](HU-09-adjuntar-documentos.md) | Adjuntar documentos | Radicación de trámites | Alta | Recepcionista | RF-14, RF-15 | P05, P20 |
| [HU-10](HU-10-consultar-y-filtrar-tramites.md) | Consultar y filtrar trámites | Flujo y seguimiento del trámite | Alta | Todos los funcionarios | RF-20 | P04, P21 |
| [HU-11](HU-11-ver-detalle-del-tramite.md) | Ver detalle del trámite | Flujo y seguimiento del trámite | Media | Todos los funcionarios | RF-20 | P06, P13, P15 |
| [HU-12](HU-12-asignar-digitador.md) | Asignar digitador | Flujo y seguimiento del trámite | Alta | Recepcionista y notario | RF-17 | P06 |
| [HU-13](HU-13-enviar-tramite-a-revision.md) | Enviar trámite a revisión | Flujo y seguimiento del trámite | Alta | Digitador | RF-16 | P07 |
| [HU-14](HU-14-revisar-tramite-aprobar-o-devolver.md) | Revisar trámite (aprobar o devolver) | Flujo y seguimiento del trámite | Alta | Notario | RF-18 | P08 |
| [HU-15](HU-15-corregir-tramite-devuelto.md) | Corregir trámite devuelto | Flujo y seguimiento del trámite | Alta | Digitador | RF-16, RF-18 | P09 |
| [HU-16](HU-16-firmar-tramite.md) | Firmar trámite | Flujo y seguimiento del trámite | Alta | Notario | RF-16 | P08 |
| [HU-17](HU-17-entregar-tramite.md) | Entregar trámite | Flujo y seguimiento del trámite | Alta | Recepcionista | RF-16 | P13 |
| [HU-18](HU-18-anular-tramite.md) | Anular trámite | Flujo y seguimiento del trámite | Media | Notario | RF-16 | P14, P15 |
| [HU-19](HU-19-avisar-al-cliente-por-correo.md) | Avisar al cliente por correo | Flujo y seguimiento del trámite | Media | Cliente | RF-21 | — |
| [HU-20](HU-20-consultar-estado-en-linea.md) | Consultar estado en línea | Flujo y seguimiento del trámite | Baja | Cliente (sin iniciar sesión) | RF-22 | P03 |
| [HU-21](HU-21-liquidar-tramite.md) | Liquidar trámite | Liquidación y caja | Alta | Cajero | RF-23, RF-24 | P10 |
| [HU-22](HU-22-registrar-pago-y-generar-recibo.md) | Registrar pago y generar recibo | Liquidación y caja | Alta | Cajero | RF-25 | P10, P11 |
| [HU-23](HU-23-hacer-cierre-de-caja.md) | Hacer cierre de caja | Liquidación y caja | Media | Cajero | RF-26 | P12 |
| [HU-24](HU-24-configurar-tarifas.md) | Configurar tarifas | Administración y reportes | Alta | Administrador | RF-27 | P16 |
| [HU-25](HU-25-generar-reportes.md) | Generar reportes | Administración y reportes | Media | Administrador | RF-28 | P17 |

## Roles

- **Recepcionista:** radica trámites, registra clientes, asigna digitador y entrega.
- **Digitador:** elabora los trámites que tiene asignados y los envía a revisión.
- **Notario:** asigna, aprueba, devuelve, firma y anula.
- **Cajero:** liquida, registra pagos, imprime recibos y hace el cierre de caja.
- **Administrador:** gestiona usuarios, tarifas y reportes.
- **Cliente:** recibe avisos por correo y consulta su trámite en la web, sin cuenta.

## Flujo del trámite

Radicado → En elaboración → En revisión → Aprobado → Firmado (con pago) → Entregado.
Desde En revisión puede pasar a Devuelto y volver a En elaboración. Cualquier estado menos Firmado y Entregado puede pasar a Anulado (con motivo).

## Requerimientos que el sistema cumple en parte

Algunos requerimientos del documento inicial piden cosas que no quedaron en los diagramas, así que el sistema
no las tiene: bloqueo tras cinco intentos y cierre por inactividad (RF-01, RF-05), persona jurídica con
representante (RF-06), historial de estados (RF-19), aportes y tabla por rangos (RF-24), recibo con consecutivo
(RF-25), bloqueo tras el cierre de caja (RF-26), tipos de trámite configurables (RF-27) y exportar a Excel (RF-28).
Cada historia lo indica en sus notas.
