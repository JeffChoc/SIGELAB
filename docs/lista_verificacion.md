# Lista de verificación de aceptación - SIGELAB

Es la lista de la sección 15 de la guía del proyecto, con la forma de comprobar cada punto y dónde queda la evidencia.
Márquela casilla por casilla antes de entregar.

## 1. Los 22 puntos de la guía

| # | Punto de la guía | Cómo comprobarlo | Evidencia | ☐ |
|---|---|---|---|---|
| 1 | El proyecto ejecuta correctamente desde el repositorio entregado | Prueba de clonado (sección 2) | Captura del login abierto en la copia clonada | ☐ |
| 2 | La base de datos puede crearse mediante el script | Ejecutar `sql/sigelab_bd.sql` completo en SQL Server | Captura de las 10 tablas y de `SELECT * FROM vw_dashboard` | ☐ |
| 3 | Login valida usuario, contraseña, estado y rostro | CP-01, CP-02, PA-01, PA-02, PA-03 | `docs/matriz_pruebas.md` | ☐ |
| 4 | Los menús respetan roles | PA-04 (entrar como `encargado1`) | `docs/matriz_pruebas.md` | ☐ |
| 5 | Laboratorios y equipos manejan códigos únicos | CP-03, PA-05 | `docs/matriz_pruebas.md` | ☐ |
| 6 | Los equipos utilizan los cinco estados obligatorios | Revisar el `CHECK` de `Equipo` y el enum `EstadoEquipo` | `docs/diccionario_datos.md` | ☐ |
| 7 | El QR identifica un equipo real de la base de datos | CP-09, CP-10 | `docs/matriz_pruebas.md` | ☐ |
| 8 | Un préstamo acepta varios equipos mediante detalle | CP-14 (préstamo con tres equipos) y `SELECT * FROM DetallePrestamo` | `docs/matriz_pruebas.md` | ☐ |
| 9 | No se pueden prestar equipos que no estén DISPONIBLES | CP-05, CP-06, CP-07, CP-08 | `docs/matriz_pruebas.md` | ☐ |
| 10 | Confirmar un préstamo cambia automáticamente los estados | CP-04 | `docs/matriz_pruebas.md` | ☐ |
| 11 | Las devoluciones pueden ser parciales | CP-14 | `docs/matriz_pruebas.md` | ☐ |
| 12 | BUENO devuelve el equipo a DISPONIBLE | CP-11 | `docs/matriz_pruebas.md` | ☐ |
| 13 | DAÑADO cambia el equipo a DAÑADO | CP-12 | `docs/matriz_pruebas.md` | ☐ |
| 14 | No se puede devolver dos veces el mismo equipo | CP-13 | `docs/matriz_pruebas.md` | ☐ |
| 15 | Mantenimiento bloquea préstamos | CP-07 | `docs/matriz_pruebas.md` | ☐ |
| 16 | Cerrar mantenimiento cambia a DISPONIBLE o BAJA | CP-16, CP-17 | `docs/matriz_pruebas.md` | ☐ |
| 17 | Existe auditoría de eventos importantes | **Reportes → Auditoría** después de las pruebas | Captura de la pantalla con varias acciones | ☐ |
| 18 | Existen consultas y reportes solicitados | **Reportes → Consultas y reportes**: los 6 reportes y el panel principal | Capturas | ☐ |
| 19 | Las reglas están en Service y el SQL en Repository | Revisión del código (sección 3) | Resultado de la búsqueda | ☐ |
| 20 | Git muestra trabajo progresivo | `git log --oneline` y el historial del repositorio en GitHub | Captura del historial | ☐ |
| 21 | README y documentación permiten ejecutar el proyecto | Prueba de clonado, siguiendo solo el README | Notas de la prueba | ☐ |
| 22 | Todos los integrantes pueden explicar el flujo completo | Ensayo de la defensa (`docs/guia_defensa.md`) | Ensayo realizado | ☐ |

## 2. Prueba de clonado (puntos 1, 2 y 21)

Es la prueba más importante: demuestra que el catedrático podrá ejecutar el proyecto. Hágala en **otra carpeta** y siga
**solo el README**, como lo haría alguien que no conoce el proyecto.

1. Abra una terminal en una carpeta vacía, por ejemplo `C:\prueba_clonado`.
2. `git clone https://github.com/JeffChoc/SIGELAB.git` y abra la carpeta resultante con IntelliJ IDEA.
3. Compruebe que el clon **no** contiene `config.properties` ni `facial/rostros/`. Si los contiene, hay datos que no debían subirse a Git.
4. Siga el README paso a paso: ejecutar el script SQL, copiar `config.properties.example` como `config.properties`,
   instalar y registrar el rostro con el servicio facial, y ejecutar `Main`.
5. Inicie sesión como `admin`.
6. Anote todo lo que **no** se pudo hacer con el README. Corríjalo en el README y haga un commit.

## 3. Revisión del código (punto 19)

Estas búsquedas se hacen en PowerShell, dentro de la carpeta del proyecto. Lo esperado es que **no muestren resultados**. Si algo aparece, revise ese archivo: puede ser solo un comentario.

SQL fuera del Repository (en pantallas, controladores o servicios):
```
Select-String -Path "src\main\java\universidad\sigelab\view\*.java","src\main\java\universidad\sigelab\controller\*.java","src\main\java\universidad\sigelab\service\*.java" -Pattern "SELECT |INSERT INTO|UPDATE |DELETE FROM|prepareStatement"
```

Datos de conexión escritos en el código (solo deben estar en `config.properties`):
```
Get-ChildItem src -Recurse -Filter *.java | Select-String -Pattern "jdbc:sqlserver"
```



## 4. Limpieza del repositorio

Busque restos que ya no se usan:
```
git ls-files | findstr /i "Frame PruebasConsola"
```
Debe aparecer, como máximo, `PrincipalFrame.java` y `ReporteFrame.java`, que sí se usan. Si aparecen otras (por ejemplo
`LoginFrame`, `UsuarioFrame`, `EquipoFrame`, `PrestamoFrame` o `PruebasConsola`), elimínelas con
`git rm <ruta>`, compruebe que el proyecto sigue compilando y haga un commit.

Compruebe también que no se subió información que no debía:
```
git ls-files | findstr /i "config.properties rostros .idea target"
```
Solo puede aparecer `config.properties.example`.

Y que ninguna contraseña real quedó en el historial (reemplace el texto por la contraseña que usó):
```
git log -p --all -S"su_contraseña" --oneline
```
No debe imprimir ningún commit.

## 5. Antes de la defensa

- [ ] La base de datos está restaurada con el script y el servicio facial enciende sin errores.
- [ ] El rostro de **todos los integrantes** está registrado y reconoce con la luz del salón (ajustar `UMBRAL_CONFIANZA` si hace falta).
- [ ] El proyecto abre y ejecuta en la computadora que se llevará a la defensa, con JDK 26.
- [ ] El escenario de defensa con `CAM-001` sale completo (sección 5 de `docs/matriz_pruebas.md`).
- [ ] El repositorio de GitHub tiene como colaborador al catedrático y su último `push` está al día.
- [ ] Cada integrante puede decir qué hizo, en qué archivos, y puede explicar el flujo completo.
