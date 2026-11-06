import base64
import binascii

import pyodbc
from fastapi import (
    APIRouter,
    Depends,
    HTTPException,
    Request,
    Response
)
from pydantic import (
    BaseModel,
    ConfigDict,
    Field,
    SecretStr,
    ValidationError
)
from starlette.concurrency import run_in_threadpool

from autenticacion import (
    ErrorAutenticacion,
    validar_credenciales
)
from conexion import conectar
from motor_facial_movil import verificar_capturas
from sesiones import (
    consumir_desafio,
    crear_desafio,
    crear_sesion,
    eliminar_sesion,
    exigir_usuario,
    seguridad
)


router = APIRouter(
    prefix="/autenticacion",
    tags=["Autenticación"]
)


class Credenciales(BaseModel):
    model_config = ConfigDict(extra="forbid")

    usuario: str = Field(min_length=1, max_length=40)

    contrasena: SecretStr = Field(
        min_length=1,
        max_length=1024
    )


class CapturasFaciales(BaseModel):
    model_config = ConfigDict(extra="forbid")

    desafio: SecretStr = Field(
        min_length=20,
        max_length=200
    )

    imagenes: list[str] = Field(
        min_length=5,
        max_length=10
    )


async def leer_solicitud(request, modelo, limite):
    contenido = bytearray()

    async for fragmento in request.stream():
        if len(contenido) + len(fragmento) > limite:
            raise HTTPException(
                status_code=413,
                detail="La solicitud es demasiado grande."
            )

        contenido.extend(fragmento)

    try:
        return modelo.model_validate_json(bytes(contenido))

    except (ValidationError, ValueError):
        raise HTTPException(
            status_code=400,
            detail="Los datos enviados no tienen el formato esperado."
        ) from None

    finally:
        contenido.clear()


def registrar_auditoria(id_usuario, accion, descripcion):
    conexion = conectar()

    try:
        conexion.cursor().execute(
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
            id_usuario,
            accion,
            "Usuario",
            id_usuario,
            descripcion[:300]
        )

        conexion.commit()

    except Exception:
        conexion.rollback()
        raise

    finally:
        conexion.close()


def completar_verificacion(datos):
    capturas = []

    for imagen in datos.imagenes:
        if not imagen or len(imagen) > 2000000:
            raise HTTPException(
                status_code=400,
                detail="Una captura supera el tamaño permitido."
            )

        try:
            contenido = base64.b64decode(
                imagen,
                validate=True
            )
        except (binascii.Error, ValueError):
            raise HTTPException(
                status_code=400,
                detail="Una captura no tiene un formato válido."
            ) from None

        capturas.append(contenido)

    # La identidad procede del desafío creado por el servidor.
    # El teléfono no puede elegir el id de otro usuario.
    id_usuario = consumir_desafio(
        datos.desafio.get_secret_value()
    )

    try:
        resultado = verificar_capturas(
            id_usuario,
            capturas
        )
    except ValueError:
        registrar_auditoria(
            id_usuario,
            "LOGIN_FALLIDO",
            "No se pudo completar la verificación facial móvil."
        )

        raise HTTPException(
            status_code=401,
            detail=(
                "No se pudo verificar el rostro. "
                "Ingresa nuevamente tus credenciales e inténtalo otra vez."
            )
        ) from None

    if resultado.get("exito") is not True:
        registrar_auditoria(
            id_usuario,
            "LOGIN_FALLIDO",
            "Verificación facial móvil rechazada."
        )

        raise HTTPException(
            status_code=401,
            detail=(
                resultado.get("mensaje", "Rostro no reconocido.")
                + " Ingresa nuevamente tus credenciales."
            )
        )

    # Se revisa otra vez el estado después de comparar el rostro.
    conexion = conectar()

    try:
        cursor = conexion.cursor()

        cursor.execute(
            """
            SELECT estado
            FROM dbo.Usuario
            WHERE id_usuario = ?
            """,
            id_usuario
        )

        usuario = cursor.fetchone()

        if usuario is None or usuario.estado != "ACTIVO":
            raise HTTPException(
                status_code=401,
                detail="El usuario ya no tiene acceso."
            )

    finally:
        conexion.close()

    registrar_auditoria(
        id_usuario,
        "LOGIN_EXITOSO",
        "Inicio de sesión móvil con verificación facial."
    )

    token = crear_sesion(id_usuario)

    return {
        "estado": "autenticado",
        "sesion_abierta": True,
        "token": token,
        "tipo": "Bearer",
        "vence_en": 3600
    }


@router.post("/credenciales")
async def comprobar_credenciales(
    request: Request,
    response: Response
):
    response.headers["Cache-Control"] = "no-store"

    datos = await leer_solicitud(
        request,
        Credenciales,
        16384
    )

    try:
        usuario = await run_in_threadpool(
            validar_credenciales,
            datos.usuario,
            datos.contrasena.get_secret_value()
        )

    except ErrorAutenticacion as error:
        raise HTTPException(
            status_code=401,
            detail=str(error)
        ) from None

    except (pyodbc.Error, ValueError):
        raise HTTPException(
            status_code=503,
            detail="No se pudo validar el acceso."
        ) from None

    return {
        "estado": "pendiente_facial",
        "sesion_abierta": False,
        "desafio": crear_desafio(usuario["id_usuario"]),
        "vence_en": 120,
        "mensaje": "Captura tu rostro para completar el acceso."
    }


@router.post("/rostro")
async def comprobar_rostro(
    request: Request,
    response: Response
):
    response.headers["Cache-Control"] = "no-store"

    datos = await leer_solicitud(
        request,
        CapturasFaciales,
        21000000
    )

    try:
        return await run_in_threadpool(
            completar_verificacion,
            datos
        )

    except pyodbc.Error:
        raise HTTPException(
            status_code=503,
            detail=(
                "No se pudo completar el acceso. "
                "Ingresa nuevamente tus credenciales."
            )
        ) from None


@router.get("/sesion")
def consultar_sesion(
    response: Response,
    usuario=Depends(exigir_usuario)
):
    response.headers["Cache-Control"] = "no-store"

    return {
        "estado": "autenticado",
        "usuario": usuario
    }


@router.post("/cerrar")
def cerrar_sesion(
    usuario=Depends(exigir_usuario),
    credenciales=Depends(seguridad)
):
    eliminar_sesion(credenciales.credentials)

    return {
        "estado": "sesion_cerrada"
    }