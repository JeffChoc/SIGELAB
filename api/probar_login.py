from getpass import getpass

import pyodbc

from autenticacion import (
    ErrorAutenticacion,
    validar_credenciales
)


def probar_login():
    print("Prueba de credenciales de SIGELAB")
    print("Usa tu usuario del programa, no el usuario de SQL Server.")
    print()

    usuario = input("Usuario: ")
    contrasena = getpass("Contraseña: ")

    try:
        validar_credenciales(usuario, contrasena)

        print()
        print("Usuario y contraseña correctos.")
        print("Usuario activo.")
        print("Acceso pendiente de verificación facial.")
        print("No se ha abierto una sesión.")

    except ErrorAutenticacion as error:
        print()
        print(str(error))

    except pyodbc.Error:
        print()
        print(
            "No se pudo completar la consulta a SQL Server. "
            "Revisa que esté funcionando y que .env sea correcto."
        )

    finally:
        contrasena = ""


probar_login()