from pathlib import Path

import pyodbc
from dotenv import dotenv_values


def conectar():
    ruta_env = Path.cwd() / ".env"

    if not ruta_env.is_file():
        raise ValueError(
            "No se encontró .env. Ejecuta el comando dentro de la carpeta api."
        )

    configuracion = dotenv_values(
        ruta_env,
        interpolate=False
    )

    campos = [
        "DB_SERVER",
        "DB_NAME",
        "DB_USER",
        "DB_PASSWORD",
        "DB_DRIVER",
    ]

    for campo in campos:
        if not configuracion.get(campo):
            raise ValueError(
                f"Falta configurar {campo} en el archivo .env"
            )

    def proteger(valor):
        return "{" + str(valor).replace("}", "}}") + "}"

    cadena = (
        f"DRIVER={proteger(configuracion['DB_DRIVER'])};"
        f"SERVER={proteger(configuracion['DB_SERVER'])};"
        f"DATABASE={proteger(configuracion['DB_NAME'])};"
        f"UID={proteger(configuracion['DB_USER'])};"
        f"PWD={proteger(configuracion['DB_PASSWORD'])};"
        "Encrypt=yes;"
        "TrustServerCertificate=yes;"
    )

    return pyodbc.connect(cadena, timeout=5)


def probar_conexion():
    conexion = conectar()

    try:
        cursor = conexion.cursor()
        cursor.execute("SELECT DB_NAME()")
        nombre = cursor.fetchone()[0]

        print(
            f"Conexión correcta con la base de datos: {nombre}"
        )
    finally:
        conexion.close()