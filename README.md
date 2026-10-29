# SIGELAB - Sistema de Gestión y Control de Laboratorios Universitarios

Proyecto final del curso **Programación II** (Universidad Mariano Gálvez de Guatemala,
Facultad de Ingeniería en Sistemas, cuarto semestre 2026). Catedrático: Ing. Alejandro Díaz.

- **Integrantes:** [Jeffery Elias Choc Hilario               3590-25-5427
                   Yeremi Anderson Alvarez Galeano          3590-25-18649
                   William David Choc Arrecis               3590-25-15860
                   Irad Aroldo De León Alvarado             3590-25-23576
                   Nidelson Ubaldi Villatoro Castillo       3590-25-24408
                   Lester David Mejia Rios                  3590-25-14664
                   Irving David Concohá Garrido             3590-24-21346]
- **Repositorio:** https://github.com/JeffChoc/SIGELAB

Aplicación de escritorio en Java Swing con base de datos Microsoft SQL Server. Controla el ciclo de vida de
los equipos de los laboratorios: disponible, préstamo, devolución, daño, mantenimiento y baja.

## Arquitectura

```
View → Controller → Service → Repository → JDBC → SQL Server
```

| Capa | Responsabilidad |
|---|---|
| `view` | Pantallas Swing. Capturan y muestran datos; no contienen SQL ni reglas. |
| `controller` | Coordina las acciones de la pantalla y llama al servicio. |
| `service` | Reglas de negocio (RN-01 a RN-20), validaciones, permisos por rol y transacciones. |
| `repository` | Todo el SQL. |
| `config`, `util` | Conexión a la base, sesión del usuario, transacciones, utilidades. |
| `model`, `enums`, `exception` | Entidades, estados y excepciones (`BusinessException`, `DataAccessException`). |

## Requisitos

| Componente | Versión                                                                                                                                                     |
|---|-------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Java (JDK) | **JDK 26** (la versión configurada en el `pom.xml`). Con un JDK anterior el proyecto no compila.                                                            |
| Maven | El que trae IntelliJ IDEA.                                                                                                                                  |
| IDE | **IntelliJ IDEA**, en una versión que soporte Java 26 (desarrollado con la 2026.1). Los formularios usan su diseñador de interfaces; ver la nota más abajo. |
| Microsoft SQL Server | 2022, con autenticación SQL Server y TCP/IP habilitado (puerto 1433).                                                                                       |
| Python | 3.9 o superior, solo para el reconocimiento facial.                                                                                                         |
| Cámara web | Para el reconocimiento facial.                                                                                                                              |

> **IntelliJ IDEA.** Los formularios (`*.form`) los compila IntelliJ. Abra el proyecto con IntelliJ y, en
> *Settings → Editor → GUI Designer*, deje **Generate GUI into: Java source code**. Si se compila fuera de
> IntelliJ (por ejemplo `mvn package` en una terminal), las pantallas no se construyen correctamente.

## Instalación paso a paso

### 1. Obtener el proyecto
```
git clone https://github.com/JeffChoc/SIGELAB.git
```
Abra la carpeta con IntelliJ IDEA y espere a que Maven descargue las dependencias. Si IntelliJ pide un SDK,
elija el **JDK 26** (*File → Project Structure → Project → SDK*).

### 2. Crear la base de datos
1. Abra `sql/sigelab_bd.sql` en DBeaver o en SQL Server Management Studio.
2. Ejecútelo **completo**: en DBeaver, *Execute script* (Alt+X); en SSMS, F5.

El script crea la base `SIGELAB` si no existe, crea las tablas, las vistas y los datos de prueba. Se puede
volver a ejecutar: borra y recrea las tablas.

### 3. Configurar la conexión
En la carpeta del `pom.xml`, copie `config.properties.example` como `config.properties` y complete sus datos:

```
db.url=jdbc:sqlserver://localhost:1433;databaseName=SIGELAB;encrypt=true;trustServerCertificate=true
db.usuario=su_usuario_sql
db.contrasena=su_contrasena
```

`config.properties` contiene credenciales y **no se sube a Git**. Si usa una instancia con nombre, por
ejemplo SQLEXPRESS, la URL es `jdbc:sqlserver://localhost;instanceName=SQLEXPRESS;databaseName=SIGELAB;...`.

### 4. Preparar el reconocimiento facial
El login exige una segunda validación con el rostro (RN-02). Está en la carpeta `facial/` (Python + OpenCV).
En una terminal, dentro de `facial/`:

```
python -m pip install -r requirements.txt
python servidor_facial.py registrar 1
```

El segundo comando abre la cámara y registra el rostro del usuario `admin` (su `id_usuario` es 1). Mire de
frente con buena luz hasta que termine. El rostro de los demás usuarios se registra desde el sistema
(pantalla *Usuarios*, botón *Registrar rostro*).

Después, inicie el servicio y **déjelo abierto mientras use SIGELAB**:

```
python servidor_facial.py
```

Debe imprimir `Servicio facial de SIGELAB activo en http://127.0.0.1:5001`. Más detalles en `facial/LEEME.md`.

### 5. Ejecutar la aplicación
En IntelliJ, abra `src/main/java/universidad/sigelab/Main.java` y ejecute `Main`. El directorio de trabajo
debe ser la carpeta del `pom.xml` (donde está `config.properties`). La consola debe decir
`SIGELAB: conexión a SQL Server exitosa.` y se abre la ventana de inicio de sesión.

## Usuarios de prueba

| Usuario | Contraseña | Rol | Estado |
|---|---|---|---|
| `admin` | `Admin123*` | ADMIN | Activo |
| `encargado1` | `Encargado123*` | ENCARGADO | Activo |
| `docente1` | `Docente123*` | DOCENTE | **Inactivo** (sirve para probar RN-01) |

Para iniciar sesión, cada usuario necesita su rostro registrado (paso 4) y el servicio facial encendido.

## Roles

| Rol | Alcance |
|---|---|
| **ADMIN** | Todo: usuarios, laboratorios, tipos de equipo, equipos, préstamos, devoluciones, mantenimiento, reportes y auditoría. |
| **ENCARGADO** | Operación diaria: equipos, cambio de estado y mantenimiento, QR, préstamos, devoluciones y reportes. |
| **DOCENTE** | Consulta de equipos y panel principal. |

El menú se adapta al rol, pero los permisos se validan en la capa `service`, no en la pantalla.

## Funciones principales

- **Inicio de sesión** con usuario, contraseña y validación facial. Rechaza usuarios inactivos.
- **Usuarios y roles**, con desactivación lógica y registro del rostro.
- **Laboratorios y tipos de equipo**, con códigos únicos.
- **Equipos** con los cinco estados (DISPONIBLE, PRESTADO, DANADO, MANTENIMIENTO, BAJA).
  Desde la pantalla se reporta un daño, se envía a mantenimiento y se finaliza el mantenimiento.
- **Código QR:** se genera la imagen QR de un equipo (pantalla *Equipos*) y se lee desde una imagen al armar un
  préstamo. El QR contiene solo el código de inventario y nunca cambia estados.
- **Préstamos** de uno o varios equipos (encabezado y detalle). Confirmar un préstamo es una sola transacción.
- **Devoluciones** parciales o completas; cada equipo vuelve como BUENO o DANADO. El préstamo se finaliza
  cuando regresan todos los equipos.
- **Panel principal** con las cantidades de equipos y préstamos, calculadas desde la base de datos.
- **Reportes:** equipos por estado, por laboratorio y estado, préstamos (activos, finalizados, vencidos),
  préstamos entre fechas e historiales de préstamos y de mantenimiento de un equipo.
- **Auditoría** de las operaciones importantes, con consulta para el ADMIN.

## Documentación

- Modelo entidad-relación: [`docs/modelo_er.md`](docs/modelo_er.md)
- Diccionario de datos: [`docs/diccionario_datos.md`](docs/diccionario_datos.md)
- Script de la base de datos: [`sql/sigelab_bd.sql`](sql/sigelab_bd.sql)
- Servicio facial: [`facial/LEEME.md`](facial/LEEME.md)

## Dependencias externas

| Dependencia | Uso |
|---|---|
| `mssql-jdbc` 13.2.1 | Conexión JDBC a SQL Server |
| ZXing `core` y `javase` 3.5.3 | Generación y lectura de códigos QR |
| `opencv-contrib-python` 4.x y `numpy` | Servicio de reconocimiento facial (Python) |

Las dependencias de Java se descargan solas con Maven. Las de Python se instalan con `facial/requirements.txt`.
OpenCV debe ser de la serie 4.x: la 5.x no incluye el detector de rostros.

## Solución de problemas

| Síntoma | Causa y solución |
|---|---|
| `No se encontró el archivo ...config.properties` | Falta crearlo (paso 3) o el directorio de trabajo no es la carpeta del `pom.xml`. |
| Error de usuario o contraseña de SQL Server | Revise `db.usuario` y `db.contrasena`; la instancia debe permitir autenticación SQL Server. |
| `No se pudo conectar con el servicio de reconocimiento facial` | El servicio no está encendido: ejecute `python servidor_facial.py` (paso 4). |
| `El usuario no tiene un rostro registrado` | Registre el rostro (paso 4, o botón *Registrar rostro* en *Usuarios*). |
| `No se encontró el clasificador de rostros de OpenCV` | Tiene OpenCV 5.x. Ejecute `python -m pip uninstall -y opencv-python opencv-contrib-python` y luego `python -m pip install -r requirements.txt`. |
| `No se pudo abrir la cámara` | Otra aplicación usa la cámara, o Windows no le da permiso (*Configuración → Privacidad → Cámara*). |
| `NullPointerException` o `Cannot bind` al abrir una pantalla | Ábralo con IntelliJ, con *Generate GUI into: Java source code* activado, y haga *Build → Rebuild Project*. |
| El rostro correcto es rechazado | Mejore la iluminación o suba `UMBRAL_CONFIANZA` en `facial/servidor_facial.py` (ver `facial/LEEME.md`). |