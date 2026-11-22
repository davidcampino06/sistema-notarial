# HU-24: Configurar tarifas

**Como** administrador
**Quiero** cambiar las tarifas vigentes
**Para** que las liquidaciones usen los valores correctos.

| Campo | Valor |
|---|---|
| Módulo | Administración y reportes |
| Requerimientos | RF-27 |
| Usuarios con acceso | Administrador |
| Prioridad | Alta |
| Dependencias | HU-01 |
| Repositorios | sistema-notarial-frontend, sistema-notarial-backend |

**Trazabilidad con el diseño (diagramas en Visual Paradigm)**

| Vista | Elementos |
|---|---|
| Contexto (flujo) | Usuarios, roles y tarifas |
| Funcional (componente) | Interfaz configurar sistema (administración) |
| Lógica | Configuracion (Singleton): tarifasVigentes |
| Desarrollo | VentanaAdministracion.configurarTarifas() → AdministracionControlador.configurarTarifas() → Configuracion.setTarifa() → BaseDatos.guardar(configuracion) |
| Mockup | P16 Administración: usuarios y tarifas |

**Validación INVEST**
- I: ✅ Se puede construir y probar por separado de las demás historias.
- N: ✅ Describe el resultado, no la tecnología.
- V: ✅ Le sirve directamente a: administrador.
- E: ✅ Las reglas y los mensajes están definidos.
- S: ✅ Se limita a «configurar tarifas».
- T: ✅ Cada escenario tiene un resultado que se puede comprobar en la aplicación.

**Criterios de aceptación (Gherkin)**
```gherkin
# language: es
Característica: Configurar tarifas

  Escenario: Guardar tarifas
    Dado que el administrador está en Administración
    Cuando cambia el valor por folio, las tarifas fijas o el porcentaje de compraventa y pulsa «Guardar tarifas»
    Entonces el sistema guarda los valores
    Y muestra «Tarifas guardadas»
    Y las liquidaciones nuevas usan esos valores

  Escenario: Valor negativo
    Dado que el administrador escribe un valor negativo
    Cuando guarda
    Entonces el sistema no lo guarda
    Y muestra «La tarifa no puede ser negativa»
```

**Supuestos / notas**
- Los tipos de trámite son clases del programa (RNF-15), no se crean desde la pantalla; no hay histórico de resoluciones (RF-27 parcial).
