from datetime import datetime

from pydantic import BaseModel, ConfigDict, Field, field_validator


class SolicitudPrestamo(BaseModel):
    model_config = ConfigDict(
        extra="forbid"
    )

    solicitante: str = Field(
        min_length=1,
        max_length=120
    )

    identificacion: str | None = Field(
        default=None,
        max_length=30
    )

    fecha_esperada: datetime

    observaciones: str | None = Field(
        default=None,
        max_length=300
    )

    equipos: list[int] = Field(
        min_length=1
    )

    @field_validator(
        "solicitante",
        "identificacion",
        "observaciones",
        mode="before"
    )
    @classmethod
    def limpiar_textos(cls, valor):
        if isinstance(valor, str):
            valor = valor.strip()

            if not valor:
                return None

        return valor

    @field_validator("fecha_esperada")
    @classmethod
    def validar_fecha(cls, valor):
        if valor.tzinfo is not None:
            raise ValueError(
                "Envía la fecha y hora local del laboratorio, sin zona horaria."
            )

        if valor <= datetime.now():
            raise ValueError(
                "La devolución esperada debe ser posterior a la fecha actual."
            )

        return valor

    @field_validator("equipos")
    @classmethod
    def validar_equipos(cls, valores):
        if any(numero <= 0 for numero in valores):
            raise ValueError(
                "Los identificadores de los equipos deben ser positivos."
            )

        if len(valores) != len(set(valores)):
            raise ValueError(
                "No se puede repetir un equipo en el préstamo."
            )

        return valores