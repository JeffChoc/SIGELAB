import logging

import pyodbc
from fastapi import Depends, FastAPI, HTTPException, Query

from conexion import conectar
from rutas_autenticacion import router as router_autenticacion
from modelos_prestamo import SolicitudPrestamo
from servicio_prestamos import ErrorPrestamo, registrar_prestamo
from sesiones import exigir_usuario
from rutas_devoluciones import router as router_devoluciones


app = FastAPI(
    title="SIGELAB API",
    description="Servidor para la aplicación móvil de SIGELAB.",
    version="1.1.0"
)

app.include_router(router_autenticacion)
app.include_router(router_devoluciones)

logger = logging.getLogger("uvicorn.error")


@app.get("/")
def inicio():
    return {
        "aplicacion": "SIGELAB",
        "mensaje": "Servidor API funcionando"
    }


@app.get("/salud")
def comprobar_salud():
    conexion = None

    try:
        conexion = conectar()

        cursor = conexion.cursor()
        cursor.execute("SELECT 1")
        cursor.fetchone()

        return {
            "estado": "ok",
            "base_datos": "conectada"
        }

    except (pyodbc.Error, ValueError):
        logger.error(
            "No se pudo comprobar la conexión con SQL Server."
        )

        raise HTTPException(
            status_code=503,
            detail="La base de datos no está disponible."
        ) from None

    finally:
        if conexion is not None:
            conexion.close()


@app.get("/equipos")
def consultar_equipos(
    codigo: str | None = Query(
        default=None,
        min_length=1,
        max_length=100
    )
):
    conexion = None

    try:
        conexion = conectar()
        cursor = conexion.cursor()

        consulta = """
            SELECT
                e.id_equipo,
                e.codigo_inventario,
                e.nombre,
                t.nombre AS tipo,
                e.marca,
                e.modelo,
                l.nombre AS laboratorio,
                e.estado
            FROM dbo.Equipo AS e
            LEFT JOIN dbo.TipoEquipo AS t
                ON t.id_tipo_equipo = e.id_tipo_equipo
            LEFT JOIN dbo.Laboratorio AS l
                ON l.id_laboratorio = e.id_laboratorio
        """

        if codigo is not None:
            consulta += """
                WHERE LOWER(LTRIM(RTRIM(e.codigo_inventario))) = ?
            """

        consulta += """
            ORDER BY e.nombre, e.id_equipo
        """

        if codigo is not None:
            cursor.execute(
                consulta,
                codigo.strip().lower()
            )
        else:
            cursor.execute(consulta)

        equipos = []

        for fila in cursor.fetchall():
            equipos.append(
                {
                    "id": fila.id_equipo,
                    "codigo": fila.codigo_inventario,
                    "nombre": fila.nombre,
                    "tipo": fila.tipo or "Sin tipo",
                    "marca": fila.marca or "",
                    "modelo": fila.modelo or "",
                    "laboratorio": fila.laboratorio or "Sin asignar",
                    "estado": fila.estado
                }
            )

        return {
            "total": len(equipos),
            "equipos": equipos
        }

    except (pyodbc.Error, ValueError):
        logger.error(
            "No se pudo consultar el catálogo de equipos."
        )

        raise HTTPException(
            status_code=503,
            detail="No se pudieron consultar los equipos."
        ) from None

    finally:
        if conexion is not None:
            conexion.close()


@app.post("/prestamos", status_code=201)
def crear_prestamo(
    solicitud: SolicitudPrestamo,
    usuario: dict = Depends(exigir_usuario)
):
    """
    Registra un prestamo usando la sesion autenticada.

    El id del usuario se obtiene de la sesion del servidor,
    nunca del cuerpo de la peticion enviada por Android.
    """
    try:
        return registrar_prestamo(
            solicitud=solicitud,
            id_usuario_sesion=usuario["id_usuario"]
        )

    except ErrorPrestamo as error:
        mensaje = str(error)

        if mensaje == "No tienes permiso para registrar préstamos.":
            codigo = 403
        elif mensaje in (
            "El usuario de la sesión no existe.",
            "El usuario está inactivo."
        ):
            codigo = 401
        elif mensaje == "La devolución esperada debe ser una fecha futura.":
            codigo = 400
        else:
            codigo = 409

        raise HTTPException(
            status_code=codigo,
            detail=mensaje
        ) from None

    except (pyodbc.Error, ValueError):
        logger.exception(
            "Error de base de datos al registrar el préstamo."
        )
        raise HTTPException(
            status_code=503,
            detail=(
                "No se pudo completar el préstamo. "
                "Revisa la conexión con la base de datos."
            )
        ) from None