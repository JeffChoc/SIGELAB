# Reparto del trabajo - SIGELAB

El proyecto se divide en siete áreas que coinciden con la rúbrica. Cada persona es responsable de **conocer, probar y poder
defender** su área: la guía del proyecto indica que cada estudiante debe explicar, localizar y modificar su propio código.

| # | Área | Responsable | Carpetas y archivos principales | Rúbrica | Casos de prueba |
|---|---|---|---|---|---|
| 1 | Base de datos y modelo | [nombre] | `sql/`, `docs/modelo_er.md`, `docs/diccionario_datos.md`, `model/`, `enums/` | Modelo de datos (15) | |
| 2 | Arquitectura, infraestructura y Git | [nombre] | `pom.xml`, `config/`, `util/`, `exception/`, `Main`, `README.md`, `.gitignore` | Arquitectura (10), Git y documentación (5) | |
| 3 | Login, usuarios, roles y auditoría | [nombre] | `AutenticacionService`, `UsuarioService`, `AuditoriaService`, `LoginView`, `UsuarioView`, `AuditoriaView` | Usuarios (10) y auditoría (5), en parte | CP-01, CP-02, PA-01, PA-02, PA-04, PA-08, PA-09 |
| 4 | Reconocimiento facial | [nombre] | `facial/`, `ReconocimientoFacialService` | Facial en el login (5) | CP-01, CP-02, PA-03 |
| 5 | Equipos, catálogos y mantenimiento | [nombre] | `Equipo*`, `Laboratorio*`, `TipoEquipo*`, `EstadoEquipoService`, `Mantenimiento*` | Equipos (10) y mantenimiento (10) | CP-03, CP-15, CP-16, CP-17, PA-05 |
| 6 | Préstamos y QR | [nombre] | `Prestamo*`, `QrService`, `PrestamoView` | Préstamos (15) y QR (5) | CP-04 a CP-10, PA-06, PA-07 |
| 7 | Devoluciones, panel principal y reportes | [nombre] | `Devolucion*`, `Dashboard*`, `Reporte*` | Devoluciones (15) y reportes (5), en parte | CP-11 a CP-14, CP-18, PA-10 |

Todos conocen además el flujo completo: la guía pide que cada integrante pueda explicarlo de principio a fin.

## Cómo se trabaja en Git

1. Todos son colaboradores del repositorio, y cada persona sube **desde su propia cuenta**.
2. Cada área trabaja en una rama propia (`feature/prestamos`, `feature/facial`...).
3. Los cambios entran a `main` con un *pull request* revisado por otra persona.
4. Los mensajes de commit siguen la convención `feat:`, `fix:`, `docs:`, `chore:`.
5. El historial **no se reescribe ni se reasigna**: refleja el trabajo que de verdad hizo cada quien.

## Qué debe hacer cada persona antes de la defensa

| # | Trabajo pendiente |
|---|---|
| 1 | Ejecutar el script en una base limpia, comprobar el diccionario de datos contra la base real y exportar el diagrama ER como imagen. |
| 2 | Revisar la lista de verificación, hacer la prueba de clonado, eliminar clases que ya no se usan y completar los datos pendientes del README. |
| 3 | Probar el menú con cada rol y los intentos fallidos de login; comprobar la pantalla de auditoría. |
| 4 | Registrar el rostro de los integrantes, ajustar el umbral con la luz del salón y preparar un archivo `.bat` que inicie el servicio. |
| 5 | Ejecutar sus casos de prueba y comprobar los cambios de estado y el historial de mantenimiento en la base. |
| 6 | Ejecutar sus casos de prueba y generar los QR de los equipos de prueba. |
| 7 | Ejecutar sus casos de prueba, provocar un préstamo vencido y comprobar el panel principal y los reportes. |

Cada persona anota el resultado de sus casos en `docs/matriz_pruebas.md`, corrige desde su cuenta lo que falle y guarda sus capturas en `docs/evidencias/`.
