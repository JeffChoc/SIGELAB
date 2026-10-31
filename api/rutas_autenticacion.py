import json

import pyodbc
from fastapi import APIRouter, HTTPException, Request
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


router = APIRouter(
    prefix="/autenticacion",
    tags=["Autenticación"]
)


class Credenciales(BaseModel):
    model_config = ConfigDict(extra="forbid")

    usuario: str = Field(
        min_length=1,
        max_length=40
    )

    contrasena: SecretStr = Field(
        min_length=1,
        max_length=1024
    )


@router.post("/credenciales")
async def comprobar_credenciales(request: Request):
    try:
        contenido = await request.body()

        if len(contenido) > 16384:
            raise HTTPException(
                status_code=413,
                detail="La solicitud es demasiado grande."
            )

        datos = Credenciales.model_validate_json(contenido)

    except (ValidationError, ValueError, json.JSONDecodeError):
        # No se incluye la contraseña en los mensajes de error.
        raise HTTPException(
            status_code=400,
            detail="Envía un usuario y una contraseña válidos."
        ) from None

    try:
        await run_in_threadpool(
            validar_credenciales,
            datos.usuario,
            datos.contrasena.get_secret_value()
        )

    except ErrorAutenticacion as error:
        raise HTTPException(
            status_code=401,
            detail=str(error)
        ) from None

    except pyodbc.Error:
        raise HTTPException(
            status_code=503,
            detail="No se pudo validar el acceso. Inténtalo más tarde."
        ) from None

    return {
        "estado": "pendiente_facial",
        "mensaje": (
            "Credenciales correctas. "
            "Falta completar la verificación facial."
        ),
        "sesion_abierta": False
    }