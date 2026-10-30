from conexion import conectar
from modelos_devolucion import SolicitudDevolucion


class ErrorDevolucion(Exception):
    pass


def registrar_devolucion(
    solicitud: SolicitudDevolucion,
    id_usuario_sesion: int
):
    """
    Uso interno del servidor.

    El usuario debe proceder de una sesión autenticada
    con verificación facial, nunca del formulario Android.
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

        if usuario is None or usuario.estado != "ACTIVO":
            raise ErrorDevolucion(
                "El usuario no existe o está inactivo."
            )

        if usuario.rol not in ("ADMIN", "ENCARGADO"):
            raise ErrorDevolucion(
                "No tienes permiso para registrar devoluciones."
            )

        # Coordina devoluciones simultáneas del mismo préstamo.
        cursor.execute(
            """
            SELECT estado
            FROM dbo.Prestamo WITH (UPDLOCK, HOLDLOCK)
            WHERE id_prestamo = ?
            """,
            solicitud.id_prestamo
        )

        prestamo = cursor.fetchone()

        if prestamo is None:
            raise ErrorDevolucion(
                "El préstamo seleccionado no existe."
            )

        if prestamo.estado != "ACTIVO":
            raise ErrorDevolucion(
                "El préstamo ya está finalizado."
            )

        seleccionados = []

        for id_detalle in sorted(solicitud.detalles):
            cursor.execute(
                """
                SELECT
                    d.id_detalle,
                    e.id_equipo,
                    e.codigo_inventario,
                    e.estado
                FROM dbo.DetallePrestamo AS d WITH (UPDLOCK, HOLDLOCK)
                INNER JOIN dbo.Equipo AS e WITH (UPDLOCK, HOLDLOCK)
                    ON e.id_equipo = d.id_equipo
                WHERE d.id_detalle = ?
                  AND d.id_prestamo = ?
                """,
                id_detalle,
                solicitud.id_prestamo
            )

            detalle = cursor.fetchone()

            if detalle is None:
                raise ErrorDevolucion(
                    "Un equipo seleccionado no pertenece al préstamo."
                )

            cursor.execute(
                """
                SELECT id_devolucion
                FROM dbo.Devolucion WITH (UPDLOCK, HOLDLOCK)
                WHERE id_detalle = ?
                """,
                id_detalle
            )

            if cursor.fetchone() is not None:
                raise ErrorDevolucion(
                    f"El equipo {detalle.codigo_inventario} "
                    "ya fue devuelto."
                )

            if detalle.estado != "PRESTADO":
                raise ErrorDevolucion(
                    f"El equipo {detalle.codigo_inventario} "
                    "no está en estado PRESTADO."
                )

            seleccionados.append(detalle)

        estado_nuevo = (
            "DISPONIBLE"
            if solicitud.condicion == "BUENO"
            else "DANADO"
        )

        for detalle in seleccionados:
            cursor.execute(
                """
                INSERT INTO dbo.Devolucion (
                    id_detalle,
                    condicion,
                    observaciones,
                    id_usuario_recibe
                )
                VALUES (?, ?, ?, ?)
                """,
                detalle.id_detalle,
                solicitud.condicion,
                solicitud.observaciones,
                id_usuario_sesion
            )

            cursor.execute(
                """
                UPDATE dbo.Equipo
                SET estado = ?
                WHERE id_equipo = ?
                  AND estado = 'PRESTADO'
                """,
                estado_nuevo,
                detalle.id_equipo
            )

            if cursor.rowcount != 1:
                raise ErrorDevolucion(
                    "El estado de un equipo cambió. "
                    "Se canceló la devolución completa."
                )

            descripcion = (
                f"Equipo {detalle.codigo_inventario} "
                f"devuelto como {solicitud.condicion} "
                f"(préstamo N.º {solicitud.id_prestamo})"
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
                "EQUIPO_DEVUELTO",
                "Equipo",
                detalle.id_equipo,
                descripcion
            )

        cursor.execute(
            """
            SELECT COUNT(*)
            FROM dbo.DetallePrestamo AS d
            WHERE d.id_prestamo = ?
              AND NOT EXISTS (
                  SELECT 1
                  FROM dbo.Devolucion AS dv
                  WHERE dv.id_detalle = d.id_detalle
              )
            """,
            solicitud.id_prestamo
        )

        pendientes = int(cursor.fetchone()[0])

        if pendientes == 0:
            cursor.execute(
                """
                UPDATE dbo.Prestamo
                SET estado = 'FINALIZADO'
                WHERE id_prestamo = ?
                  AND estado = 'ACTIVO'
                """,
                solicitud.id_prestamo
            )

            if cursor.rowcount != 1:
                raise ErrorDevolucion(
                    "No se pudo finalizar el préstamo."
                )

        conexion.commit()

        return {
            "id_prestamo": solicitud.id_prestamo,
            "equipos_devueltos": len(seleccionados),
            "equipos_pendientes": pendientes,
            "estado": "FINALIZADO" if pendientes == 0 else "ACTIVO",
            "mensaje": "Devolución registrada correctamente"
        }

    except Exception:
        conexion.rollback()
        raise

    finally:
        conexion.close()