# Diccionario de datos - SIGELAB

Base de datos **SIGELAB** (Microsoft SQL Server). Generado a partir de `sql/sigelab_bd.sql`, por lo que
coincide con el script entregado. Los textos de estado se guardan en mayúsculas y están protegidos con `CHECK`.

## Tablas

- [Rol](#rol): Clasifica a los usuarios y define el alcance del menú y de los permisos.
- [Usuario](#usuario): Personas que acceden al sistema: credenciales, rol y estado.
- [Laboratorio](#laboratorio): Ubicación lógica de los equipos.
- [TipoEquipo](#tipoequipo): Catálogo de categorías de equipo.
- [Equipo](#equipo): Activo principal que se administra. Su estado refleja el ciclo de vida.
- [Prestamo](#prestamo): Encabezado de un préstamo de uno o varios equipos.
- [DetallePrestamo](#detalleprestamo): Relaciona cada equipo con su préstamo (relación uno a muchos).
- [Devolucion](#devolucion): Registro de la devolución de cada equipo y de su condición. Conserva el historial.
- [Mantenimiento](#mantenimiento): Historial de intervenciones de un equipo.
- [Auditoria](#auditoria): Trazabilidad de las operaciones importantes del sistema.

## Rol

Clasifica a los usuarios y define el alcance del menú y de los permisos.

| Columna | Tipo | ¿Nulo? | Restricciones | Descripción |
|---|---|---|---|---|
| `id_rol` | INT | No | Autoincremental, PK | Identificador del rol. |
| `nombre` | NVARCHAR(30) | No | Único | Nombre del rol: ADMIN, ENCARGADO o DOCENTE. |
| `descripcion` | NVARCHAR(150) | Sí | - | Descripción del alcance del rol. |

## Usuario

Personas que acceden al sistema: credenciales, rol y estado.

| Columna | Tipo | ¿Nulo? | Restricciones | Descripción |
|---|---|---|---|---|
| `id_usuario` | INT | No | Autoincremental, PK | Identificador del usuario. |
| `nombre` | NVARCHAR(60) | No | - | Nombres de la persona. |
| `apellido` | NVARCHAR(60) | No | - | Apellidos de la persona. |
| `nombre_usuario` | NVARCHAR(40) | No | Único | Nombre para iniciar sesión. Único. |
| `contrasena_hash` | VARCHAR(64) | No | - | Contraseña como hash SHA-256 en hexadecimal. Nunca se guarda en texto plano. |
| `correo` | NVARCHAR(120) | No | Único | Correo electrónico. Único y con formato válido. |
| `id_rol` | INT | No | FK → Rol(id_rol) | Rol del usuario. |
| `estado` | VARCHAR(10) | No | Por defecto 'ACTIVO' | ACTIVO o INACTIVO. Los usuarios se desactivan, no se borran, para conservar el historial. |
| `referencia_facial` | NVARCHAR(200) | Sí | - | Campo reservado. Los rostros los guarda el servicio facial en facial/rostros/<id_usuario>; el sistema no depende de este campo. |
| `fecha_creacion` | DATETIME2(0) | No | Por defecto SYSDATETIME() | Fecha y hora en que se creó el usuario. |

**Reglas `CHECK`:**

- `CK_Usuario_estado`: `estado IN ('ACTIVO','INACTIVO')`
- `CK_Usuario_correo`: `correo LIKE '%_@_%._%'`

## Laboratorio

Ubicación lógica de los equipos.

| Columna | Tipo | ¿Nulo? | Restricciones | Descripción |
|---|---|---|---|---|
| `id_laboratorio` | INT | No | Autoincremental, PK | Identificador del laboratorio. |
| `codigo` | NVARCHAR(20) | No | Único | Código del laboratorio. Único (RN-03). |
| `nombre` | NVARCHAR(100) | No | - | Nombre del laboratorio. |
| `ubicacion` | NVARCHAR(150) | No | - | Ubicación física, por ejemplo edificio y nivel. |
| `capacidad` | INT | No | - | Capacidad del laboratorio. Debe ser mayor que cero. |
| `estado` | VARCHAR(10) | No | Por defecto 'ACTIVO' | ACTIVO o INACTIVO. Solo los activos aceptan equipos nuevos. |

**Reglas `CHECK`:**

- `CK_Laboratorio_capacidad`: `capacidad > 0`
- `CK_Laboratorio_estado`: `estado IN ('ACTIVO','INACTIVO')`

## TipoEquipo

Catálogo de categorías de equipo.

| Columna | Tipo | ¿Nulo? | Restricciones | Descripción |
|---|---|---|---|---|
| `id_tipo_equipo` | INT | No | Autoincremental, PK | Identificador del tipo de equipo. |
| `nombre` | NVARCHAR(60) | No | Único | Nombre del tipo (Laptop, Cámara, Proyector...). Único. |
| `descripcion` | NVARCHAR(150) | Sí | - | Descripción del tipo. |

## Equipo

Activo principal que se administra. Su estado refleja el ciclo de vida.

| Columna | Tipo | ¿Nulo? | Restricciones | Descripción |
|---|---|---|---|---|
| `id_equipo` | INT | No | Autoincremental, PK | Identificador del equipo. |
| `codigo_inventario` | NVARCHAR(30) | No | Único | Código único de inventario (RN-04). Es el valor que contiene el QR. |
| `nombre` | NVARCHAR(100) | No | - | Nombre del equipo. |
| `id_tipo_equipo` | INT | No | FK → TipoEquipo(id_tipo_equipo) | Tipo de equipo (catálogo). |
| `marca` | NVARCHAR(60) | Sí | - | Marca. |
| `modelo` | NVARCHAR(60) | Sí | - | Modelo. |
| `numero_serie` | NVARCHAR(80) | Sí | - | Número de serie. Único cuando está informado. |
| `id_laboratorio` | INT | No | FK → Laboratorio(id_laboratorio) | Laboratorio donde se encuentra el equipo. |
| `estado` | VARCHAR(15) | No | Por defecto 'DISPONIBLE' | DISPONIBLE, PRESTADO, DANADO, MANTENIMIENTO o BAJA. Cambia solo por operaciones de negocio. |
| `observaciones` | NVARCHAR(300) | Sí | - | Notas libres. |

**Reglas `CHECK`:**

- `CK_Equipo_estado`: `estado IN ('DISPONIBLE','PRESTADO','DANADO','MANTENIMIENTO','BAJA')`

**Índices:**

- `UX_Equipo_numero_serie` sobre (numero_serie) (único) cuando numero_serie IS NOT NULL
- `IX_Equipo_estado` sobre (estado)
- `IX_Equipo_laboratorio` sobre (id_laboratorio)

## Prestamo

Encabezado de un préstamo de uno o varios equipos.

| Columna | Tipo | ¿Nulo? | Restricciones | Descripción |
|---|---|---|---|---|
| `id_prestamo` | INT | No | Autoincremental, PK | Identificador del préstamo. |
| `solicitante_nombre` | NVARCHAR(120) | No | - | Nombre de quien recibe los equipos. |
| `solicitante_identificacion` | NVARCHAR(30) | Sí | - | Carné o código del solicitante (opcional). |
| `fecha_prestamo` | DATETIME2(0) | No | Por defecto SYSDATETIME() | Fecha y hora en que se registró el préstamo. |
| `fecha_esperada_devolucion` | DATETIME2(0) | No | - | Fecha límite de devolución. No puede ser anterior a la del préstamo. |
| `id_usuario_registra` | INT | No | FK → Usuario(id_usuario) | Usuario que registró el préstamo. |
| `estado` | VARCHAR(12) | No | Por defecto 'ACTIVO' | ACTIVO mientras quede algún equipo pendiente; FINALIZADO cuando se devolvieron todos. El estado «vencido» no se guarda: se calcula. |
| `observaciones` | NVARCHAR(300) | Sí | - | Notas libres. |

**Reglas `CHECK`:**

- `CK_Prestamo_estado`: `estado IN ('ACTIVO','FINALIZADO')`
- `CK_Prestamo_fechas`: `fecha_esperada_devolucion >= fecha_prestamo`

**Índices:**

- `IX_Prestamo_estado` sobre (estado)
- `IX_Prestamo_fechas` sobre (fecha_prestamo, fecha_esperada_devolucion)

## DetallePrestamo

Relaciona cada equipo con su préstamo (relación uno a muchos).

| Columna | Tipo | ¿Nulo? | Restricciones | Descripción |
|---|---|---|---|---|
| `id_detalle` | INT | No | Autoincremental, PK | Identificador de la línea del préstamo. |
| `id_prestamo` | INT | No | FK → Prestamo(id_prestamo) | Préstamo al que pertenece la línea. |
| `id_equipo` | INT | No | FK → Equipo(id_equipo) | Equipo prestado. El mismo equipo no puede repetirse en un préstamo (RN-06). |

**Índices:**

- `IX_Detalle_equipo` sobre (id_equipo)

## Devolucion

Registro de la devolución de cada equipo y de su condición. Conserva el historial.

| Columna | Tipo | ¿Nulo? | Restricciones | Descripción |
|---|---|---|---|---|
| `id_devolucion` | INT | No | Autoincremental, PK | Identificador de la devolución. |
| `id_detalle` | INT | No | Único, FK → DetallePrestamo(id_detalle) | Línea del préstamo que se devuelve. Único: un equipo no se devuelve dos veces (RN-11). |
| `fecha_devolucion` | DATETIME2(0) | No | Por defecto SYSDATETIME() | Fecha y hora de la devolución. |
| `condicion` | VARCHAR(10) | No | - | BUENO o DANADO: cómo regresó el equipo. |
| `observaciones` | NVARCHAR(300) | Sí | - | Notas de la devolución. |
| `id_usuario_recibe` | INT | No | FK → Usuario(id_usuario) | Usuario que recibió el equipo. |

**Reglas `CHECK`:**

- `CK_Devolucion_condicion`: `condicion IN ('BUENO','DANADO')`

## Mantenimiento

Historial de intervenciones de un equipo.

| Columna | Tipo | ¿Nulo? | Restricciones | Descripción |
|---|---|---|---|---|
| `id_mantenimiento` | INT | No | Autoincremental, PK | Identificador del mantenimiento. |
| `id_equipo` | INT | No | FK → Equipo(id_equipo) | Equipo en mantenimiento. |
| `fecha_ingreso` | DATETIME2(0) | No | Por defecto SYSDATETIME() | Fecha y hora en que el equipo entró a mantenimiento. |
| `tipo` | VARCHAR(12) | No | - | PREVENTIVO o CORRECTIVO. |
| `descripcion` | NVARCHAR(300) | No | - | Trabajo a realizar. |
| `responsable` | NVARCHAR(100) | No | - | Persona responsable del mantenimiento. |
| `fecha_salida` | DATETIME2(0) | Sí | - | Fecha y hora de salida. Vacía mientras esté EN_PROCESO. |
| `resultado` | VARCHAR(15) | Sí | - | REPARADO (el equipo queda DISPONIBLE) o NO_REPARABLE (queda en BAJA). Vacío mientras esté EN_PROCESO. |
| `estado` | VARCHAR(12) | No | Por defecto 'EN_PROCESO' | EN_PROCESO o FINALIZADO. |
| `observaciones` | NVARCHAR(300) | Sí | - | Notas del cierre. |
| `id_usuario_registra` | INT | No | FK → Usuario(id_usuario) | Usuario que envió el equipo a mantenimiento. |

**Reglas `CHECK`:**

- `CK_Mant_tipo`: `tipo IN ('PREVENTIVO','CORRECTIVO')`
- `CK_Mant_estado`: `estado IN ('EN_PROCESO','FINALIZADO')`
- `CK_Mant_resultado`: `resultado IS NULL OR resultado IN ('REPARADO','NO_REPARABLE')`
- `CK_Mant_fechas`: `fecha_salida IS NULL OR fecha_salida >= fecha_ingreso`
- `CK_Mant_cierre`: `(estado = 'EN_PROCESO' AND fecha_salida IS NULL AND resultado IS NULL) OR (estado = 'FINALIZADO' AND fecha_salida IS NOT NULL AND resultado IS NOT NULL)`

**Índices:**

- `IX_Mant_equipo` sobre (id_equipo)
- `UX_Mant_un_abierto_por_equipo` sobre (id_equipo) (único) cuando estado = 'EN_PROCESO'

## Auditoria

Trazabilidad de las operaciones importantes del sistema.

| Columna | Tipo | ¿Nulo? | Restricciones | Descripción |
|---|---|---|---|---|
| `id_auditoria` | INT | No | Autoincremental, PK | Identificador del registro de auditoría. |
| `id_usuario` | INT | Sí | FK → Usuario(id_usuario) | Usuario que realizó la acción. Vacío en un LOGIN_FALLIDO de un usuario inexistente. |
| `fecha_hora` | DATETIME2(0) | No | Por defecto SYSDATETIME() | Momento de la acción. |
| `accion` | NVARCHAR(40) | No | - | Acción realizada, por ejemplo LOGIN_EXITOSO o PRESTAMO_CREADO. |
| `entidad` | NVARCHAR(40) | No | - | Tabla o entidad afectada. |
| `id_registro` | INT | Sí | - | Identificador del registro afectado. |
| `descripcion` | NVARCHAR(300) | Sí | - | Descripción legible de lo ocurrido. |

**Índices:**

- `IX_Auditoria_fecha` sobre (fecha_hora)
- `IX_Auditoria_usuario` sobre (id_usuario)

## Vistas

| Vista | Para qué sirve |
|---|---|
| `vw_prestamos_con_pendientes` | Cada préstamo con su total de equipos y cuántos siguen pendientes (`JOIN`, `COUNT`, `GROUP BY`). |
| `vw_prestamos_vencidos` | Préstamos cuya fecha esperada ya pasó y todavía tienen equipos pendientes (RN-17). |
| `vw_dashboard` | Una sola fila con los contadores del panel principal. |
