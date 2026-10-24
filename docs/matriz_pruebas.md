# Matriz de pruebas - SIGELAB

Casos de prueba obligatorios CP-01 a CP-18 de la guía del proyecto (sección 10), con sus pasos, el resultado esperado y
espacio para registrar el resultado real y la evidencia. Al final hay pruebas adicionales y el escenario de defensa.

## 1. Preparación (hacer antes de empezar)

1. **Base de datos limpia:** ejecute `sql/sigelab_bd.sql` completo. Deja los datos de prueba de partida:

   | Equipo | Estado inicial | | Usuario | Contraseña | Estado |
   |---|---|---|---|---|---|
   | `CAM-001`, `CAM-002`, `LAP-001`, `LAP-002`, `PRY-001`, `RTR-001`, `SWT-001` | DISPONIBLE | | `admin` | `Admin123*` | Activo |
   | `TAB-001` | DANADO | | `encargado1` | `Encargado123*` | Activo |
   | `MON-001` | MANTENIMIENTO (con un mantenimiento abierto) | | `docente1` | `Docente123*` | Inactivo |
   | `KIT-001` | BAJA | | | | |

2. **Servicio facial encendido** (`python servidor_facial.py` dentro de `facial/`) y con el rostro de los usuarios que van a
   iniciar sesión ya registrado.
3. **Aplicación abierta** desde IntelliJ (`Main`), con `config.properties` configurado.
4. **Evidencias:** guarde las capturas de pantalla en `docs/evidencias/` con el nombre del caso (por ejemplo `CP-04.png`).

> **Orden de ejecución.** Varios casos usan el estado que dejó un caso anterior (por ejemplo, CP-05 necesita que CP-04
> haya dejado `CAM-001` como PRESTADO). Ejecútelos en el orden de la columna **Orden** de la tabla siguiente, sobre la
> misma base de datos. Si se repite una prueba, vuelva a ejecutar el script SQL para restaurar el estado de partida.

## 2. Resumen

| Orden | Caso | Módulo | Prueba | Resultado esperado | Área | Estado |
|---|---|---|---|---|---|---|
| 1 | CP-01 | Login | Usuario válido, contraseña válida y rostro correcto | Acceso permitido | Login y facial | ☐ |
| 2 | CP-02 | Login | Usuario válido, contraseña válida y rostro incorrecto | Acceso denegado | Login y facial | ☐ |
| 3 | CP-03 | Equipo | Registrar un segundo equipo con el mismo código | Registro rechazado | Equipos y mantenimiento | ☐ |
| 4 | CP-06 | Préstamo | Prestar un equipo DANADO | Operación rechazada | Préstamos y QR | ☐ |
| 5 | CP-07 | Préstamo | Prestar un equipo en MANTENIMIENTO | Operación rechazada | Préstamos y QR | ☐ |
| 6 | CP-08 | Préstamo | Prestar un equipo en BAJA | Operación rechazada | Préstamos y QR | ☐ |
| 7 | CP-09 | QR | Leer el QR de un equipo DISPONIBLE | Equipo identificado y disponible para agregar | Préstamos y QR | ☐ |
| 8 | CP-04 | Préstamo | Prestar un equipo DISPONIBLE | Préstamo creado; el equipo pasa a PRESTADO | Préstamos y QR | ☐ |
| 9 | CP-05 | Préstamo | Intentar prestar de nuevo el mismo equipo | Operación rechazada | Préstamos y QR | ☐ |
| 10 | CP-10 | QR | Leer el QR de un equipo PRESTADO | Equipo identificado, pero bloqueado | Préstamos y QR | ☐ |
| 11 | CP-11 | Devolución | Devolver un equipo en condición BUENO | El equipo pasa a DISPONIBLE | Devoluciones y reportes | ☐ |
| 12 | CP-13 | Devolución | Intentar devolver de nuevo un equipo ya devuelto | Operación rechazada | Devoluciones y reportes | ☐ |
| 13 | CP-14 | Devolución | Devolver 2 de 3 equipos | El préstamo sigue activo con 1 pendiente | Devoluciones y reportes | ☐ |
| 14 | CP-12 | Devolución | Devolver un equipo como DANADO | El equipo pasa a DANADO | Devoluciones y reportes | ☐ |
| 15 | CP-15 | Mantenimiento | Enviar un equipo DANADO a mantenimiento | El equipo pasa a MANTENIMIENTO | Equipos y mantenimiento | ☐ |
| 16 | CP-16 | Mantenimiento | Cerrar el mantenimiento como reparado | El equipo pasa a DISPONIBLE | Equipos y mantenimiento | ☐ |
| 17 | CP-17 | Mantenimiento | Cerrar el mantenimiento como no reparable | El equipo pasa a BAJA | Equipos y mantenimiento | ☐ |
| 18 | CP-18 | Reportes | Fecha esperada vencida con un equipo pendiente | El préstamo aparece como vencido | Devoluciones y reportes | ☐ |

## 3. Detalle de cada caso

Cada caso tiene tres campos para llenar a mano: **Resultado real**, **Evidencia** y **Estado** (☐ Aprobado / ☐ Falló), con el nombre de quien
probó y la fecha.

---

### CP-01 - Login con rostro correcto
- **Módulo / área:** Login / Login y facial. **Reglas:** RN-01, RN-02.
- **Precondición:** servicio facial encendido; el rostro de `admin` registrado.
- **Pasos:**
  1. Ejecute `Main`.
  2. Escriba `admin` y `Admin123*`, y pulse **Ingresar**.
  3. Cuando se abra la cámara, mire de frente.
- **Resultado esperado:** se abre el panel principal con «Bienvenido, Administrador Sistema (ADMIN)».
- **Verificación en BD:** `SELECT TOP 1 * FROM Auditoria WHERE accion = 'LOGIN_EXITOSO' ORDER BY id_auditoria DESC;` debe mostrar el ingreso.
- **Resultado real:** ______________________  **Evidencia:** `docs/evidencias/CP-01.png`
- **Estado:** ☐ Aprobado ☐ Falló  **Probado por:** __________  **Fecha:** ________

### CP-02 - Login con rostro incorrecto
- **Módulo / área:** Login / Login y facial. **Reglas:** RN-02.
- **Precondición:** igual que CP-01.
- **Pasos:**
  1. Escriba `admin` y `Admin123*` (credenciales válidas).
  2. Pulse **Ingresar** y, frente a la cámara, que se ponga **otra persona** (o tape la cámara).
- **Resultado esperado:** no entra. Aparece un mensaje que empieza con «Acceso denegado.» y la ventana de login sigue abierta.
- **Verificación en BD:** `SELECT TOP 1 * FROM Auditoria WHERE accion = 'LOGIN_FALLIDO' ORDER BY id_auditoria DESC;` debe indicar «Rostro no reconocido».
- **Resultado real:** ______________________  **Evidencia:** `docs/evidencias/CP-02.png`
- **Estado:** ☐ Aprobado ☐ Falló  **Probado por:** __________  **Fecha:** ________

### CP-03 - Código de equipo duplicado
- **Módulo / área:** Equipo / Equipos y mantenimiento. **Reglas:** RN-04.
- **Precondición:** sesión como `admin`; el equipo `CAM-001` existe.
- **Pasos:**
  1. **Catálogos → Equipos** y pulse **Nuevo**.
  2. Código `CAM-001`, nombre «Cámara repetida», elija un tipo y un laboratorio.
  3. Pulse **Guardar**.
- **Resultado esperado:** mensaje «Ya existe un equipo con el código CAM-001.» No se crea ninguna fila nueva.
- **Verificación en BD:** `SELECT COUNT(*) FROM Equipo WHERE codigo_inventario = 'CAM-001';` debe dar 1.
- **Resultado real:** ______________________  **Evidencia:** `docs/evidencias/CP-03.png`
- **Estado:** ☐ Aprobado ☐ Falló  **Probado por:** __________  **Fecha:** ________

### CP-06 - Prestar un equipo DANADO
- **Módulo / área:** Préstamo / Préstamos y QR. **Reglas:** RN-07, RN-08.
- **Precondición:** `TAB-001` en estado DANADO (estado de partida).
- **Pasos:**
  1. **Operaciones → Préstamos**, pestaña **Nuevo préstamo**.
  2. Escriba `TAB-001` y pulse **Agregar equipo**.
- **Resultado esperado:** mensaje «El equipo TAB-001 no se puede prestar: su estado es DANADO.» El equipo no queda en la lista.
- **Resultado real:** ______________________  **Evidencia:** `docs/evidencias/CP-06.png`
- **Estado:** ☐ Aprobado ☐ Falló  **Probado por:** __________  **Fecha:** ________

### CP-07 - Prestar un equipo en MANTENIMIENTO
- **Módulo / área:** Préstamo / Préstamos y QR. **Reglas:** RN-08, RN-14.
- **Precondición:** `MON-001` en MANTENIMIENTO (estado de partida).
- **Pasos:** igual que CP-06, con el código `MON-001`.
- **Resultado esperado:** «El equipo MON-001 no se puede prestar: su estado es MANTENIMIENTO.»
- **Resultado real:** ______________________  **Evidencia:** `docs/evidencias/CP-07.png`
- **Estado:** ☐ Aprobado ☐ Falló  **Probado por:** __________  **Fecha:** ________

### CP-08 - Prestar un equipo en BAJA
- **Módulo / área:** Préstamo / Préstamos y QR. **Reglas:** RN-08.
- **Precondición:** `KIT-001` en BAJA (estado de partida).
- **Pasos:** igual que CP-06, con el código `KIT-001`.
- **Resultado esperado:** «El equipo KIT-001 no se puede prestar: su estado es BAJA.»
- **Resultado real:** ______________________  **Evidencia:** `docs/evidencias/CP-08.png`
- **Estado:** ☐ Aprobado ☐ Falló  **Probado por:** __________  **Fecha:** ________

### Preparación para los casos de QR
Con sesión de `admin` o `encargado1`: en **Catálogos → Equipos**, seleccione `CAM-001`, pulse **Generar QR** y guarde la imagen como
`CAM-001.png`. Repita con `CAM-002` (`CAM-002.png`).

### CP-09 - Leer el QR de un equipo DISPONIBLE
- **Módulo / área:** QR / Préstamos y QR. **Reglas:** RN-19.
- **Precondición:** `CAM-002` en DISPONIBLE y su imagen QR generada.
- **Pasos:**
  1. **Operaciones → Préstamos**, pestaña **Nuevo préstamo**.
  2. Pulse **Leer QR desde imagen** y elija `CAM-002.png`.
  3. Pulse **Limpiar** (no confirme el préstamo).
- **Resultado esperado:** `CAM-002` aparece en la lista de equipos del préstamo. Su estado **sigue DISPONIBLE** hasta confirmar un préstamo.
- **Verificación en BD:** `SELECT codigo_inventario, estado FROM Equipo WHERE codigo_inventario = 'CAM-002';` debe decir DISPONIBLE.
- **Resultado real:** ______________________  **Evidencia:** `docs/evidencias/CP-09.png`
- **Estado:** ☐ Aprobado ☐ Falló  **Probado por:** __________  **Fecha:** ________

### CP-04 - Prestar un equipo DISPONIBLE
- **Módulo / área:** Préstamo / Préstamos y QR. **Reglas:** RN-05, RN-09, RN-18.
- **Precondición:** `CAM-001` en DISPONIBLE.
- **Pasos:**
  1. **Operaciones → Préstamos**, pestaña **Nuevo préstamo**.
  2. Solicitante «Ana García»; fecha de devolución para mañana.
  3. Agregue `CAM-001` y pulse **Confirmar préstamo**.
- **Resultado esperado:** mensaje «Préstamo N.º ... registrado con 1 equipo(s).» `CAM-001` pasa a PRESTADO. Anote el número del préstamo (**préstamo A**).
- **Verificación en BD:**
  ```sql
  SELECT codigo_inventario, estado FROM Equipo WHERE codigo_inventario = 'CAM-001';
  SELECT * FROM Prestamo ORDER BY id_prestamo DESC;
  SELECT * FROM DetallePrestamo ORDER BY id_detalle DESC;
  SELECT TOP 1 * FROM Auditoria WHERE accion = 'PRESTAMO_CREADO' ORDER BY id_auditoria DESC;
  ```
- **Resultado real:** ______________________  **Evidencia:** `docs/evidencias/CP-04.png`
- **Estado:** ☐ Aprobado ☐ Falló  **Probado por:** __________  **Fecha:** ________

### CP-05 - Prestar de nuevo el mismo equipo
- **Módulo / área:** Préstamo / Préstamos y QR. **Reglas:** RN-07, RN-08.
- **Precondición:** `CAM-001` en PRESTADO (resultado de CP-04).
- **Pasos:** en un préstamo nuevo, escriba `CAM-001` y pulse **Agregar equipo**.
- **Resultado esperado:** «El equipo CAM-001 no se puede prestar: su estado es PRESTADO.»
- **Resultado real:** ______________________  **Evidencia:** `docs/evidencias/CP-05.png`
- **Estado:** ☐ Aprobado ☐ Falló  **Probado por:** __________  **Fecha:** ________

### CP-10 - Leer el QR de un equipo PRESTADO
- **Módulo / área:** QR / Préstamos y QR. **Reglas:** RN-19.
- **Precondición:** `CAM-001` en PRESTADO y su imagen QR generada.
- **Pasos:** en **Nuevo préstamo**, pulse **Leer QR desde imagen** y elija `CAM-001.png`.
- **Resultado esperado:** el equipo se identifica, pero se rechaza con «...su estado es PRESTADO.» No se agrega a la lista y su estado no cambia.
- **Resultado real:** ______________________  **Evidencia:** `docs/evidencias/CP-10.png`
- **Estado:** ☐ Aprobado ☐ Falló  **Probado por:** __________  **Fecha:** ________

### CP-11 - Devolver un equipo en condición BUENO
- **Módulo / área:** Devolución / Devoluciones y reportes. **Reglas:** RN-10, RN-12, RN-16.
- **Precondición:** préstamo A activo con `CAM-001` pendiente (CP-04).
- **Pasos:**
  1. **Operaciones → Devoluciones**.
  2. Elija el préstamo A, seleccione `CAM-001`, condición **BUENO**, y pulse **Registrar devolución**.
- **Resultado esperado:** «Devolución registrada. El préstamo N.º ... quedó FINALIZADO.» `CAM-001` pasa a DISPONIBLE.
- **Verificación en BD:**
  ```sql
  SELECT codigo_inventario, estado FROM Equipo WHERE codigo_inventario = 'CAM-001';
  SELECT * FROM Devolucion ORDER BY id_devolucion DESC;
  SELECT id_prestamo, estado FROM Prestamo ORDER BY id_prestamo DESC;
  ```
- **Resultado real:** ______________________  **Evidencia:** `docs/evidencias/CP-11.png`
- **Estado:** ☐ Aprobado ☐ Falló  **Probado por:** __________  **Fecha:** ________

### CP-13 - Devolver de nuevo un equipo ya devuelto
- **Módulo / área:** Devolución / Devoluciones y reportes. **Reglas:** RN-10, RN-11.
- **Precondición:** `CAM-001` ya devuelto (CP-11).
- **Pasos:** en **Devoluciones**, escriba `CAM-001` en el buscador y pulse **Buscar**.
- **Resultado esperado:** «El equipo CAM-001 no está pendiente de devolución en ningún préstamo activo.» No hay forma de devolverlo otra vez.
- **Verificación en BD:** `SELECT COUNT(*) FROM Devolucion WHERE id_detalle = <id del detalle de CAM-001>;` debe dar 1.
- **Resultado real:** ______________________  **Evidencia:** `docs/evidencias/CP-13.png`
- **Estado:** ☐ Aprobado ☐ Falló  **Probado por:** __________  **Fecha:** ________

### CP-14 - Devolución parcial: 2 de 3 equipos
- **Módulo / área:** Devolución / Devoluciones y reportes. **Reglas:** RN-16.
- **Precondición:** `LAP-001`, `LAP-002` y `PRY-001` en DISPONIBLE.
- **Pasos:**
  1. Cree un préstamo (**préstamo B**) con los tres equipos y confírmelo.
  2. En **Devoluciones**, elija el préstamo B, seleccione `LAP-001` y `LAP-002` (Ctrl), condición **BUENO**, y pulse **Registrar devolución**.
- **Resultado esperado:** «Quedan 1 equipo(s) pendiente(s)...». El préstamo B sigue ACTIVO; en la lista de activos muestra Pendientes = 1. `PRY-001` sigue PRESTADO.
- **Verificación en BD:** `SELECT * FROM vw_prestamos_con_pendientes WHERE id_prestamo = <préstamo B>;` debe mostrar `equipos_pendientes = 1`.
- **Resultado real:** ______________________  **Evidencia:** `docs/evidencias/CP-14.png`
- **Estado:** ☐ Aprobado ☐ Falló  **Probado por:** __________  **Fecha:** ________

### CP-12 - Devolver un equipo como DANADO
- **Módulo / área:** Devolución / Devoluciones y reportes. **Reglas:** RN-13.
- **Precondición:** `CAM-002` en DISPONIBLE.
- **Pasos:**
  1. Cree un préstamo (**préstamo C**) solo con `CAM-002` y confírmelo.
  2. En **Devoluciones**, elija el préstamo C, seleccione `CAM-002`, condición **DANADO**, observación «Pantalla rota», y pulse **Registrar devolución**.
- **Resultado esperado:** `CAM-002` pasa a DANADO. El préstamo C queda FINALIZADO.
- **Verificación en BD:** `SELECT codigo_inventario, estado FROM Equipo WHERE codigo_inventario = 'CAM-002';` debe decir DANADO.
- **Resultado real:** ______________________  **Evidencia:** `docs/evidencias/CP-12.png`
- **Estado:** ☐ Aprobado ☐ Falló  **Probado por:** __________  **Fecha:** ________

### CP-15 - Enviar un equipo DANADO a mantenimiento
- **Módulo / área:** Mantenimiento / Equipos y mantenimiento. **Reglas:** RN-18.
- **Precondición:** `CAM-002` en DANADO (CP-12).
- **Pasos:**
  1. **Catálogos → Equipos**, seleccione `CAM-002` y pulse **Cambiar estado**.
  2. Elija **Enviar a mantenimiento correctivo**; descripción «Cambio de pantalla», responsable «Técnico Juan Ramírez».
- **Resultado esperado:** `CAM-002` pasa a MANTENIMIENTO.
- **Verificación en BD:**
  ```sql
  SELECT codigo_inventario, estado FROM Equipo WHERE codigo_inventario = 'CAM-002';
  SELECT * FROM Mantenimiento WHERE id_equipo = (SELECT id_equipo FROM Equipo WHERE codigo_inventario = 'CAM-002');
  ```
  Debe haber un mantenimiento `EN_PROCESO`, tipo CORRECTIVO.
- **Resultado real:** ______________________  **Evidencia:** `docs/evidencias/CP-15.png`
- **Estado:** ☐ Aprobado ☐ Falló  **Probado por:** __________  **Fecha:** ________

### CP-16 - Cerrar un mantenimiento como reparado
- **Módulo / área:** Mantenimiento / Equipos y mantenimiento. **Reglas:** RN-15.
- **Precondición:** `CAM-002` en MANTENIMIENTO (CP-15).
- **Pasos:** seleccione `CAM-002`, pulse **Cambiar estado**, elija **Reparado (queda DISPONIBLE)** y escriba una observación.
- **Resultado esperado:** `CAM-002` pasa a DISPONIBLE. El mantenimiento queda FINALIZADO con resultado REPARADO.
- **Resultado real:** ______________________  **Evidencia:** `docs/evidencias/CP-16.png`
- **Estado:** ☐ Aprobado ☐ Falló  **Probado por:** __________  **Fecha:** ________

### CP-17 - Cerrar un mantenimiento como no reparable
- **Módulo / área:** Mantenimiento / Equipos y mantenimiento. **Reglas:** RN-15.
- **Precondición:** `MON-001` en MANTENIMIENTO (estado de partida, conservado desde CP-07).
- **Pasos:** seleccione `MON-001`, pulse **Cambiar estado** y elija **No reparable (queda BAJA)**.
- **Resultado esperado:** `MON-001` pasa a BAJA. El mantenimiento queda FINALIZADO con resultado NO_REPARABLE.
- **Resultado real:** ______________________  **Evidencia:** `docs/evidencias/CP-17.png`
- **Estado:** ☐ Aprobado ☐ Falló  **Probado por:** __________  **Fecha:** ________

### CP-18 - Préstamo vencido
- **Módulo / área:** Reportes / Devoluciones y reportes. **Reglas:** RN-17.
- **Precondición:** el préstamo B sigue activo con `PRY-001` pendiente (CP-14).
- **Pasos:**
  1. En DBeaver, adelante las fechas del préstamo B (cambie `<préstamo B>` por su número):
     ```sql
     UPDATE Prestamo
     SET fecha_prestamo = DATEADD(DAY, -2, SYSDATETIME()),
         fecha_esperada_devolucion = DATEADD(DAY, -1, SYSDATETIME())
     WHERE id_prestamo = <préstamo B>;
     ```
  2. En la ventana principal, pulse **Actualizar**.
  3. Abra **Reportes → Consultas y reportes**, elija *Préstamos (activos, finalizados, vencidos)* con filtro **VENCIDOS** y pulse **Consultar**.
  4. En **Operaciones → Préstamos**, pestaña **Préstamos**, revise la columna **Vencido**.
- **Resultado esperado:** el panel principal muestra al menos 1 préstamo vencido, en rojo; el reporte lista el préstamo B; la columna Vencido dice «Sí».
- **Resultado real:** ______________________  **Evidencia:** `docs/evidencias/CP-18.png`
- **Estado:** ☐ Aprobado ☐ Falló  **Probado por:** __________  **Fecha:** ________

## 4. Pruebas adicionales

Cubren reglas que la guía no incluye entre los 18 casos y que el catedrático puede pedir en la defensa.

| Caso | Regla | Prueba | Resultado esperado | Estado |
|---|---|---|---|---|
| PA-01 | RN-01 | Iniciar sesión con `docente1` y su contraseña correcta | «El usuario está inactivo...». No se abre la cámara. | ☐ |
| PA-02 | - | Iniciar sesión con una contraseña incorrecta | «Usuario o contraseña incorrectos.» y un `LOGIN_FALLIDO` en la auditoría | ☐ |
| PA-03 | RN-02 | Apagar el servicio facial e iniciar sesión con credenciales válidas | «No se pudo conectar con el servicio de reconocimiento facial...». El acceso se deniega. | ☐ |
| PA-04 | RN-20 | Entrar como `encargado1` y revisar el menú | En Catálogos solo aparece Equipos; no aparecen Laboratorios, Tipos de equipo, Usuarios ni Auditoría | ☐ |
| PA-05 | RN-03 | En **Laboratorios**, registrar un laboratorio con el código `LAB-01` | «Ya existe un laboratorio con el código LAB-01.» | ☐ |
| PA-06 | RN-05 | Pulsar **Confirmar préstamo** sin agregar equipos | «Agregue al menos un equipo al préstamo.» | ☐ |
| PA-07 | RN-06 | Agregar el mismo código dos veces en un préstamo | «El equipo ... ya fue agregado a este préstamo.» | ☐ |
| PA-08 | - | Intentar desactivar al propio `admin` desde **Usuarios** | «No puede desactivar su propio usuario.» | ☐ |
| PA-09 | - | Crear un usuario con una contraseña de menos de 8 caracteres | Se rechaza con el mensaje del largo mínimo | ☐ |
| PA-10 | RN-17 | Con fechas válidas, consultar **Préstamos entre fechas** con un rango sin préstamos | «0 registro(s) encontrado(s).» | ☐ |

## 5. Escenario de defensa con CAM-001

Es el recorrido que propone la guía (sección 13). Conviene ensayarlo hasta poder hacerlo sin ayuda y explicar cada paso.
Empiece con la base restaurada (ejecute el script SQL) y haga cada paso en el sistema.

| Paso | Qué hacer | Qué debe pasar | ☐ |
|---|---|---|---|
| 1 | Buscar `CAM-001` en **Equipos** | Está DISPONIBLE | ☐ |
| 2 | Crear un préstamo para `CAM-001` | Préstamo creado; el equipo pasa a PRESTADO | ☐ |
| 3 | Intentar prestarla de nuevo | Se rechaza | ☐ |
| 4 | Devolverla como DANADO | El equipo pasa a DANADO | ☐ |
| 5 | Intentar prestarla | Se rechaza | ☐ |
| 6 | Enviarla a mantenimiento correctivo | Pasa a MANTENIMIENTO | ☐ |
| 7 | Finalizar el mantenimiento como reparada | Se cierra el mantenimiento | ☐ |
| 8 | Comprobar su estado | Vuelve a DISPONIBLE | ☐ |
| 9 | Crear un nuevo préstamo | Se crea correctamente | ☐ |

Al terminar, abra **Reportes → Auditoría** y muestre el rastro de todo lo hecho: `PRESTAMO_CREADO`, `EQUIPO_DEVUELTO`,
`EQUIPO_ENVIADO_MANTENIMIENTO` y `MANTENIMIENTO_FINALIZADO`.

## 6. Resumen de resultados

| | Cantidad |
|---|---|
| Casos obligatorios aprobados (de 18) | ____ |
| Pruebas adicionales aprobadas (de 10) | ____ |
| Escenario de defensa completo | ☐ Sí ☐ No |
| Defectos encontrados y corregidos (anote el commit) | ______________________ |
