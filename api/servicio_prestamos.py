from datetime import datetime

from conexion import conectar
from modelos_prestamo import SolicitudPrestamo


class ErrorPrestamo(Exception):
    pass


def registrar_prestamo(
    solicitud: SolicitudPrestamo,
    id_usuario_sesion: int
):
    """
    Uso interno del servidor.

    id_usuario_sesion debe proceder de una sesión autenticada
    con verificación facial. Nunca del formulario de Android.
    """

    conexion = conectar()

    try:
        conexion.autocommit = False
        cursor = conexion.cursor()

        cursor.execute(
            """
            SELECT
                u.estado,
                r.nombre AS rol
            FROM dbo.Usuario AS u WITH (HOLDLOCK)
            INNER JOIN dbo.Rol AS r WITH (HOLDLOCK)
                ON r.id_rol = u.id_rol
            WHERE u.id_usuario = ?
            """,
            id_usuario_sesion
        )

        usuario = cursor.fetchone()

        if usuario is None:
            raise ErrorPrestamo(
                "El usuario de la sesión no existe."
            )

        if usuario.estado != "ACTIVO":
            raise ErrorPrestamo(
                "El usuario está inactivo."
            )

        if usuario.rol not in ("ADMIN", "ENCARGADO"):
            raise ErrorPrestamo(
                "No tienes permiso para registrar préstamos."
            )

        if solicitud.fecha_esperada <= datetime.now():
            raise ErrorPrestamo(
                "La devolución esperada debe ser una fecha futura."
            )

        equipos_actuales = []

        # Se bloquean en orden para coordinar operaciones simultáneas.
        for id_equipo in sorted(solicitud.equipos):
            cursor.execute(
                """
                SELECT
                    id_equipo,
                    codigo_inventario,
                    estado
                FROM dbo.Equipo WITH (UPDLOCK, HOLDLOCK)
                WHERE id_equipo = ?
                """,
                id_equipo
            )

            equipo = cursor.fetchone()

            if equipo is None:
                raise ErrorPrestamo(
                    f"El equipo con identificador {id_equipo} ya no existe."
                )

            if equipo.estado != "DISPONIBLE":
                raise ErrorPrestamo(
                    f"El equipo {equipo.codigo_inventario} "
                    f"no puede prestarse: está {equipo.estado}."
                )

            equipos_actuales.append(equipo)

        cursor.execute(
            """
            SET NOCOUNT ON;

            INSERT INTO dbo.Prestamo (
                solicitante_nombre,
                solicitante_identificacion,
                fecha_esperada_devolucion,
                id_usuario_registra,
                observaciones
            )
            VALUES (?, ?, ?, ?, ?);

            SELECT CAST(SCOPE_IDENTITY() AS int);
            """,
            solicitud.solicitante,
            solicitud.identificacion,
            solicitud.fecha_esperada,
            id_usuario_sesion,
            solicitud.observaciones
        )

        fila = cursor.fetchone()

        if fila is None or fila[0] is None:
            raise ErrorPrestamo(
                "No se pudo obtener el identificador del préstamo."
            )

        id_prestamo = int(fila[0])

        for equipo in equipos_actuales:
            cursor.execute(
                """
                INSERT INTO dbo.DetallePrestamo (
                    id_prestamo,
                    id_equipo
                )
                VALUES (?, ?)
                """,
                id_prestamo,
                equipo.id_equipo
            )

            cursor.execute(
                """
                UPDATE dbo.Equipo
                SET estado = 'PRESTADO'
                WHERE id_equipo = ?
                  AND estado = 'DISPONIBLE'
                """,
                equipo.id_equipo
            )

            if cursor.rowcount != 1:
                raise ErrorPrestamo(
                    f"El equipo {equipo.codigo_inventario} "
                    "dejó de estar disponible. "
                    "Se canceló el préstamo completo."
                )

        codigos = ", ".join(
            equipo.codigo_inventario
            for equipo in equipos_actuales
        )

        descripcion = (
            f"Préstamo a {solicitud.solicitante} "
            f"con {len(equipos_actuales)} equipo(s): {codigos}"
        )[:300]

        cursor.execute(
            """
            INSERT INTO dbo.Auditoria (
                id_usuario,
                accion,
                entidad,
                id_registro,
                descripcion
            )
            VALUES (?, ?, ?, ?, ?)
            """,
            id_usuario_sesion,
            "PRESTAMO_CREADO",
            "Prestamo",
            id_prestamo,
            descripcion
        )

        conexion.commit()

        return {
            "id_prestamo": id_prestamo,
            "mensaje": "Préstamo registrado correctamente",
            "total_equipos": len(equipos_actuales)
        }

    except Exception:
        conexion.rollback()
        raise

    finally:
        conexion.close()