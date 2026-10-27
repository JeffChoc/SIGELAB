# Guía de estudio para la defensa - SIGELAB

La defensa no es una presentación: el catedrático puede operar el sistema, pedir escenarios que no se ensayaron y preguntar a
cada integrante por su propio código. La guía del proyecto lo dice así: «eso lo generó la herramienta» no sustituye el dominio del
proyecto. Esta guía sirve para **entender** el sistema, no para memorizar respuestas: lea el código que se menciona en cada
respuesta, ejecútelo y explíquelo con sus palabras.

## 1. Las diez preguntas de la guía

### 1. ¿Dónde valida que un equipo prestado no pueda prestarse nuevamente?
En `PrestamoService.validarDisponible`, que usa `EstadoEquipo.puedePrestarse()` (solo `DISPONIBLE` puede prestarse). Se llama en dos
momentos: al agregar el equipo a la lista (`identificarParaPrestamo`) y al confirmar el préstamo (`confirmarPrestamo`).

Hay una segunda barrera en la base de datos: `EquipoRepository.actualizarEstado` ejecuta `UPDATE Equipo SET estado = ... WHERE
id_equipo = ... AND estado = 'DISPONIBLE'`. Si otra persona prestó el equipo en ese instante, la actualización no cambia nada y
`confirmarPrestamo` cancela todo el préstamo.

### 2. ¿Por qué esa validación pertenece al Service?
Porque las reglas no pueden depender de la pantalla (RN-20). Un botón se puede ocultar, deshabilitar o saltar; el `Service` es
el punto por el que pasan todas las operaciones, así que la regla se aplica igual venga de donde venga. El `Repository` solo guarda
y consulta datos, y el `Controller` solo coordina.

### 3. ¿Qué función cumple DetallePrestamo?
Es la tabla de detalle de la relación entre préstamos y equipos: un préstamo tiene muchos equipos, y un equipo participa en muchos
préstamos a lo largo del tiempo. Evita un diseño con columnas `equipo1`, `equipo2`... Tiene `UNIQUE (id_prestamo, id_equipo)` para
que un equipo no se repita en el mismo préstamo (RN-06). Cada línea tiene como máximo una fila en `Devolucion`: una línea sin
devolución es un equipo **pendiente**.

### 4. ¿Cómo se actualiza el estado del equipo?
Con `EquipoRepository.actualizarEstado(conexion, idEquipo, estadoEsperado, estadoNuevo)`, y solo desde operaciones de negocio:
`PrestamoService` (a PRESTADO), `DevolucionService` (a DISPONIBLE o DANADO) y `EstadoEquipoService` (daño y mantenimiento).
El formulario de edición nunca cambia el estado: `EquipoService.modificar` conserva el que ya tenía.

### 5. ¿Qué ocurre si falla una operación SQL en medio de un préstamo?
`Transaccion.ejecutar` abre la conexión sin `autocommit`. Si cualquier paso lanza una excepción, hace `rollback` y la vuelve a
lanzar: no queda el encabezado, ni el detalle, ni los estados cambiados, ni la auditoría. El usuario ve un mensaje comprensible
(`ManejadorErrores`), nunca la `SQLException`.

### 6. ¿Cómo se obtiene un equipo a partir del QR?
`QrService.leerCodigo` decodifica la imagen con ZXing y devuelve el texto, por ejemplo `CAM-001`. Después, `PrestamoView.leerQr`
lo pone en el campo y llama a `agregarEquipo`, que recorre `PrestamoController.identificarEquipo` →
`PrestamoService.identificarParaPrestamo` → `EquipoService.buscarPorCodigo` → `EquipoRepository.buscarPorCodigo` (un `SELECT` con
`JOIN`). El QR solo identifica: el equipo real se consulta en la base de datos y el estado no cambia (RN-19).

### 7. ¿Cómo se comunica Java con el reconocimiento facial?
`ReconocimientoFacialService` usa `java.net.http.HttpClient` para llamar a `POST http://127.0.0.1:5001/verificar?usuario=ID`. El
servicio de Python abre la cámara, detecta el rostro con OpenCV y lo compara con el registrado. Responde un JSON con `exito` y
`mensaje`. Java lo interpreta y `AutenticacionService.verificarRostro` deniega el acceso si `exito` es falso o si el servicio no
responde, y registra `LOGIN_FALLIDO` con el motivo.

### 8. ¿Qué FOREIGN KEY relaciona estas tablas?

| Tabla.columna | Referencia |
|---|---|
| `Usuario.id_rol` | `Rol` |
| `Equipo.id_tipo_equipo` | `TipoEquipo` |
| `Equipo.id_laboratorio` | `Laboratorio` |
| `Prestamo.id_usuario_registra` | `Usuario` |
| `DetallePrestamo.id_prestamo` | `Prestamo` |
| `DetallePrestamo.id_equipo` | `Equipo` |
| `Devolucion.id_detalle` | `DetallePrestamo` |
| `Devolucion.id_usuario_recibe` | `Usuario` |
| `Mantenimiento.id_equipo` | `Equipo` |
| `Mantenimiento.id_usuario_registra` | `Usuario` |
| `Auditoria.id_usuario` | `Usuario` (puede ser nulo) |

Están en `docs/diccionario_datos.md` y en el diagrama de `docs/modelo_er.md`.

### 9. ¿Cómo identifica su consulta un préstamo vencido?
La vista `vw_prestamos_vencidos` toma de `vw_prestamos_con_pendientes` los préstamos con `equipos_pendientes > 0` cuya
`fecha_esperada_devolucion` es menor que `SYSDATETIME()` (RN-17). `ReporteRepository.prestamosVencidos` usa lo mismo con `HAVING`.
El estado «vencido» **no se guarda** en la tabla: se calcula, porque cambia solo con el paso del tiempo.

### 10. ¿Qué hizo específicamente usted dentro del repositorio?
Responda con su área de `docs/reparto_trabajo.md`: qué módulo, qué archivos y qué decisiones tomó. Pruébelo con Git:
```
git log --author="su nombre" --oneline
git shortlog -sn
git blame src/main/java/universidad/sigelab/service/PrestamoService.java
```
Debe poder abrir uno de sus archivos y explicar el flujo completo de principio a fin.

## 2. Otras preguntas probables

| Pregunta | Respuesta breve |
|---|---|
| ¿Por qué usar enums para los estados? | Evitan textos escritos de formas distintas, y la base los protege además con un `CHECK`. |
| ¿Por qué una tabla `Devolucion` aparte? | Conserva el historial de cada equipo, permite devoluciones parciales, guarda la condición y quién recibió, y `UNIQUE (id_detalle)` impide devolver dos veces. |
| ¿Por qué no se borran usuarios ni laboratorios? | Se desactivan para conservar el historial (los préstamos y la auditoría los referencian). |
| ¿Cómo se protegen las contraseñas? | Se guardan como hash SHA-256 (`HashUtil`); el hash nunca sale del `Service`. Limitación: es SHA-256 sin sal, suficiente para el alcance académico. |
| ¿Qué diferencia hay entre `BusinessException` y `DataAccessException`? | La primera es una regla incumplida, con un mensaje para el usuario. La segunda es un fallo técnico de JDBC o SQL; su detalle va al log. |
| ¿Por qué `PreparedStatement`? | Evita la inyección SQL: los valores se envían como parámetros, no pegados al texto de la consulta. |
| ¿Para qué sirve `config.properties`? | Mantiene las credenciales de la base fuera del código y fuera de Git. |
| ¿Qué pasa si dos personas prestan el mismo equipo a la vez? | El `UPDATE ... WHERE estado = 'DISPONIBLE'` solo funciona para una; la otra recibe un mensaje y su préstamo completo se revierte. |
| ¿Cómo se audita? | `AuditoriaService` y `AuditoriaRepository`; en préstamos, devoluciones y mantenimiento se escribe dentro de la misma transacción. |
| ¿Cómo se validan los permisos? | `SesionUsuario.exigirRol` o `exigirSesion` al inicio de las operaciones del `Service`; el menú solo es una ayuda visual. |
| ¿Cómo se calculan las cantidades del panel principal? | `DashboardRepository` lee la vista `vw_dashboard`, que cuenta con subconsultas. |
| ¿Qué es LBPH? | Un reconocedor de OpenCV basado en histogramas de patrones binarios locales. Compara el rostro con las fotos registradas y devuelve una distancia; menor es más parecido. |
| ¿Qué limitaciones tiene el reconocimiento facial? | Depende de la luz y no distingue una foto de una persona. La guía deja fuera esa detección. |
| ¿Por qué el Controller es tan delgado? | Por la arquitectura: convierte datos de la pantalla (por ejemplo, un texto a número) y delega todo lo demás al `Service`. |
| ¿Qué pasa con un equipo DANADO? | No se puede prestar. Debe pasar a mantenimiento correctivo y finalizar como reparado para volver a DISPONIBLE. |

## 3. Ejercicios de modificación en vivo

El catedrático puede pedir que cambie algo del código delante de él. Practique estos cambios hasta hacerlos sin ayuda.

### Ejercicio 1: límite de 3 equipos por préstamo
Archivo: `PrestamoService`, método `confirmarPrestamo`, justo después de comprobar que la lista no está vacía.
```java
private static final int MAXIMO_EQUIPOS_POR_PRESTAMO = 3;
// ...
if (equiposSeleccionados.size() > MAXIMO_EQUIPOS_POR_PRESTAMO) {
    throw new BusinessException("Un préstamo no puede tener más de " + MAXIMO_EQUIPOS_POR_PRESTAMO + " equipos.");
}
```

### Ejercicio 2: contraseña de mínimo 10 caracteres
Archivo: `UsuarioService`, constante `LARGO_MINIMO_CONTRASENA`. Un solo número.

### Ejercicio 3: nueva acción de auditoría
1. Agregar el valor al enum `AccionAuditoria`. La columna `accion` es texto, así que no hace falta cambiar la base.
2. Llamar a `servicioAuditoria.registrarComoUsuarioActual(...)` desde el método del `Service` donde ocurre la operación.

### Ejercicio 4: nuevo campo en un equipo (por ejemplo, «responsable»)
Se recorren todas las capas, de abajo hacia arriba:
1. SQL: `ALTER TABLE Equipo ADD responsable NVARCHAR(100) NULL;` y el mismo cambio en `sql/sigelab_bd.sql`.
2. `Equipo` (campo, getter y setter).
3. `EquipoRepository`: el `SELECT`, el `INSERT`, el `UPDATE` y el método que convierte la fila en objeto.
4. `EquipoService` (validación del largo) y `EquipoController`.
5. `EquiposView.java` y su `.form` (un campo de texto nuevo).

### Ejercicio 5: nuevo reporte («equipos de un laboratorio»)
1. `ReporteRepository`: un método con el `SELECT` con `JOIN`.
2. `ReporteService` (con `exigirRol`) y `ReporteController`.
3. `ReporteFrame`: una opción más en el selector y su caso en `consultar()`.

### Ejercicio 6: más estricto o más permisivo el reconocimiento facial
Archivo: `facial/servidor_facial.py`, constante `UMBRAL_CONFIANZA`. Menor valor, más estricto.

## 4. Consejos para la defensa

- **Ensaye el escenario con `CAM-001`** (sección 5 de `docs/matriz_pruebas.md`) hasta poder hacerlo explicando cada paso.
- **Abra el código mientras explica.** Es mejor mostrar el método que describirlo de memoria.
- **Si no sabe algo, dígalo y recorra el código en voz alta.** Seguir el flujo desde la pantalla hasta la base de datos suele bastar para encontrar la respuesta.
- **Conozca el proyecto completo, no solo su parte.** Roten quién opera el sistema y quién explica durante los ensayos.
- **Lleve todo funcionando:** la base restaurada, el servicio facial encendido y el rostro de cada integrante registrado.
