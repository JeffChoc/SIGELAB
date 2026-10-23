# Manual de usuario - SIGELAB

SIGELAB sirve para controlar los equipos de los laboratorios de la universidad: saber qué equipos hay, quién los tiene
prestados, cuándo deben volver, en qué estado regresan y cuáles necesitan mantenimiento.

Para instalar el sistema vea el [README](../README.md).

## 1. Antes de empezar

### Roles

| Rol | Qué puede hacer |
|---|---|
| **ADMIN** | Todo: usuarios, laboratorios, tipos de equipo, equipos, préstamos, devoluciones, mantenimiento, reportes y auditoría. |
| **ENCARGADO** | Equipos, préstamos, devoluciones, mantenimiento, códigos QR y reportes. |
| **DOCENTE** | Consultar equipos y ver el panel principal. |

El menú solo muestra las opciones de su rol.

### Estados de un equipo

| Estado | Significado |
|---|---|
| **DISPONIBLE** | Se puede prestar. |
| **PRESTADO** | Está en un préstamo activo; no se puede prestar otra vez. |
| **DANADO** | Se reportó con un problema; no se puede prestar. |
| **MANTENIMIENTO** | Está en reparación o revisión; no se puede prestar. |
| **BAJA** | Ya no se usa ni se presta. |

## 2. Iniciar y cerrar sesión

1. Escriba su **usuario** y su **contraseña** y pulse **Ingresar** (o Enter).
2. El botón dirá «Verificando rostro...» y se abrirá la cámara. **Mire de frente a la cámara** durante unos segundos.
3. Si su rostro se reconoce, entra al panel principal.

Para que esto funcione, el servicio de reconocimiento facial debe estar encendido y su rostro debe estar registrado.

| Mensaje | Qué hacer |
|---|---|
| «Usuario o contraseña incorrectos.» | Revise los datos. |
| «El usuario está inactivo...» | Pida al administrador que lo active. |
| «El usuario no tiene un rostro registrado.» | Pida al administrador que registre su rostro. |
| «El rostro no corresponde al usuario.» / «No se detectó un rostro...» | Mire de frente, con buena luz, e inténtelo de nuevo. |
| «No se pudo conectar con el servicio de reconocimiento facial...» | Avise a quien administra el sistema: el servicio no está encendido. |

Para salir, use **Sesión → Cerrar sesión**.

## 3. Panel principal

Al entrar verá su nombre y su rol, y las cantidades actuales:

- **Equipos:** total, disponibles, prestados, dañados, en mantenimiento y de baja.
- **Préstamos:** activos y vencidos. Si hay vencidos, el número se muestra en rojo.

Se actualiza solo al volver a esta ventana. También puede pulsar **Actualizar**.

## 4. Equipos

Menú **Catálogos → Equipos**.

### Consultar
- **Filtrar por estado:** elija un estado en la lista de arriba. Con *TODOS* se ven todos.
- **Buscar por código:** escriba el código de inventario (por ejemplo `CAM-001`) y pulse **Buscar por código**.
- **Ver todos:** vuelve a la lista completa.
- Al hacer clic en una fila, sus datos aparecen en el formulario.

### Registrar un equipo nuevo (ADMIN y ENCARGADO)
1. Pulse **Nuevo**.
2. Complete los campos marcados con *: código de inventario, nombre, tipo y laboratorio. Marca, modelo, número de serie y observaciones son opcionales.
3. Elija el **estado inicial**: DISPONIBLE (lo normal) o DANADO, si el equipo llega con un problema.
4. Pulse **Guardar**.

El código de inventario no puede repetirse. Si ya existe, el sistema avisa «Ya existe un equipo con el código ...».

### Modificar un equipo
Seleccione el equipo, cambie los datos y pulse **Guardar**. El estado no se cambia aquí: cambia con préstamos,
devoluciones y mantenimiento. Un equipo dado de baja ya no se puede modificar.

### Cambiar el estado de un equipo registrado (ADMIN y ENCARGADO)
Seleccione el equipo y pulse **Cambiar estado**. Solo se ofrece lo que corresponde:

| Estado actual | Qué se puede hacer |
|---|---|
| **DISPONIBLE** | *Reportar daño* (describa el problema) o *Enviar a mantenimiento preventivo* (indique el trabajo y el responsable). |
| **DANADO** | *Enviar a mantenimiento correctivo* (indique el trabajo y el responsable). |
| **MANTENIMIENTO** | *Finalizar*: «Reparado» deja el equipo DISPONIBLE; «No reparable» lo deja en BAJA. |
| **PRESTADO** | Cambia cuando se devuelve (ver Devoluciones). |
| **BAJA** | Ya no cambia. |

### Generar el código QR de un equipo
Seleccione el equipo y pulse **Generar QR**. Elija dónde guardar la imagen y se mostrará una vista previa.
El QR contiene solo el código de inventario; se puede imprimir y pegar en el equipo.

## 5. Préstamos (ADMIN y ENCARGADO)

Menú **Operaciones → Préstamos**.

### Registrar un préstamo
1. En la pestaña **Nuevo préstamo**, escriba el **solicitante** (obligatorio) y, si quiere, su carné y observaciones.
2. Indique la **fecha y hora esperada de devolución**. Debe ser posterior a la actual.
3. Agregue los equipos, de una de estas formas:
   - Escriba el código de inventario y pulse **Agregar equipo** (o Enter). Un lector de códigos QR USB también funciona.
   - Pulse **Leer QR desde imagen** y elija la imagen del QR del equipo.
4. Para sacar un equipo de la lista, selecciónelo y pulse **Quitar seleccionado**.
5. Pulse **Confirmar préstamo**.

Al confirmar, todos los equipos pasan a PRESTADO. Si algo falla, no se guarda nada.

| Mensaje | Significado |
|---|---|
| «El equipo ... no se puede prestar: su estado es ...» | Solo se prestan equipos DISPONIBLES. El equipo se identificó, pero está bloqueado. |
| «El equipo ... ya fue agregado a este préstamo.» | No se puede repetir un equipo. |
| «Agregue al menos un equipo al préstamo.» | Un préstamo necesita al menos un equipo. |
| «No existe un equipo con el código ...» | Revise el código. |

### Ver los préstamos
La pestaña **Préstamos** muestra el historial con el solicitante, las fechas, el estado, cuántos equipos tiene, cuántos siguen
pendientes y si está **vencido** (la fecha esperada ya pasó y todavía hay equipos pendientes).

## 6. Devoluciones (ADMIN y ENCARGADO)

Menú **Operaciones → Devoluciones**.

1. Elija un préstamo de la lista de **préstamos activos**. También puede escribir el código de un equipo y pulsar **Buscar**: el sistema encuentra su préstamo.
2. En **Equipos pendientes**, seleccione los que regresan (con Ctrl o Mayús se eligen varios).
3. Elija la **condición**: **BUENO** deja el equipo DISPONIBLE; **DANADO** lo deja DANADO.
4. Escriba una observación si quiere y pulse **Registrar devolución**.

Se puede devolver una parte del préstamo: el préstamo sigue activo mientras quede algún equipo pendiente y pasa a
FINALIZADO cuando regresan todos. Si necesita devolver equipos con condiciones distintas, haga una devolución por cada condición.
Un equipo no se puede devolver dos veces.

## 7. Laboratorios (solo ADMIN)

Menú **Catálogos → Laboratorios**.

- **Registrar:** pulse **Nuevo**, complete código, nombre, ubicación y capacidad (un número mayor que cero) y pulse **Guardar**. El código no puede repetirse.
- **Modificar:** seleccione el laboratorio, cambie los datos y guarde.
- **Activar o desactivar:** seleccione el laboratorio y pulse el botón, que dice **Desactivar** o **Activar** según su estado actual. Los laboratorios no se borran, para conservar el historial de sus equipos.

## 8. Tipos de equipo (solo ADMIN)

Menú **Catálogos → Tipos de equipo**. Pulse **Nuevo**, escriba el nombre (no puede repetirse) y una descripción opcional, y pulse **Guardar**.
Para cambiar uno, selecciónelo, edítelo y guarde.

## 9. Usuarios (solo ADMIN)

Menú **Catálogos → Usuarios**.

- **Registrar:** pulse **Nuevo** y complete nombre, apellido, nombre de usuario, correo, rol y contraseña (mínimo 8 caracteres). El usuario y el correo no pueden repetirse.
- **Modificar:** seleccione el usuario y cambie los datos. Deje la contraseña vacía para conservar la actual.
- **Desactivar o activar:** use los botones **Desactivar** y **Activar**. Un usuario inactivo no puede iniciar sesión. Los usuarios no se borran, para conservar su historial.
- **Registrar rostro:** seleccione el usuario y pulse **Registrar rostro**. Se abre la cámara; la persona debe estar frente a ella unos segundos. Sin esto, ese usuario no podrá iniciar sesión.

No es posible desactivar su propio usuario ni cambiar su propio rol.

## 10. Reportes (ADMIN y ENCARGADO)

Menú **Reportes → Consultas y reportes**. Elija un reporte, complete el dato que pida y pulse **Consultar**.

| Reporte | Dato que pide |
|---|---|
| Equipos por estado | Ninguno |
| Equipos por laboratorio y estado | Ninguno |
| Préstamos (activos, finalizados, vencidos) | El tipo de préstamos |
| Préstamos entre fechas | Fecha desde y fecha hasta (la inicial no puede ser posterior a la final) |
| Historial de préstamos de un equipo | Código del equipo |
| Historial de mantenimiento de un equipo | Código del equipo |

Debajo de la tabla se indica cuántos registros se encontraron.

## 11. Auditoría (solo ADMIN)

Menú **Reportes → Auditoría**. Muestra quién hizo qué y cuándo: inicios de sesión (exitosos y fallidos), equipos registrados y
modificados, préstamos, devoluciones, mantenimientos y cambios de usuarios.

Puede filtrar por acción, por usuario (parte del nombre) y por fechas (marque «Filtrar por fechas»). Se muestran como máximo los 500 registros más recientes.

## 12. Preguntas frecuentes

**¿Por qué no puedo prestar un equipo?** Solo se prestan los equipos en estado DISPONIBLE. Revise su estado en *Equipos*.

**Devolví un equipo dañado, ¿cómo vuelve a estar disponible?** Debe pasar por mantenimiento: *Cambiar estado → Enviar a mantenimiento
correctivo* y luego *Finalizar* como reparado.

**Me equivoqué al registrar un equipo.** Selecciónelo y corrija los datos. Si el error es de estado, use *Cambiar estado*.

**¿Puedo borrar un usuario, un laboratorio o un préstamo?** No. Se desactivan o se finalizan, para que el historial siempre
esté completo.

**El sistema muestra «Ocurrió un error inesperado» o un problema con la base de datos.** Intente de nuevo; si continúa, avise al administrador.
