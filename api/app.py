import logging

import pyodbc
from fastapi import FastAPI, HTTPException, Query

from conexion import conectar
from rutas_autenticacion import router as router_autenticacion


app = FastAPI(
    title="SIGELAB API",
    description="Servidor para la aplicación móvil de SIGELAB.",
    version="1.0.0"
)

app.include_router(router_autenticacion)

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