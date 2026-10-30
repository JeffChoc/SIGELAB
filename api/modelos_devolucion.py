from typing import Literal

from pydantic import BaseModel, ConfigDict, Field, field_validator


class SolicitudDevolucion(BaseModel):
    model_config = ConfigDict(extra="forbid")

    id_prestamo: int = Field(gt=0)

    detalles: list[int] = Field(min_length=1)

    condicion: Literal["BUENO", "DANADO"]

    observaciones: str | None = Field(
        default=None,
        max_length=300
    )

    @field_validator("observaciones", mode="before")
    @classmethod
    def limpiar_observaciones(cls, valor):
        if isinstance(valor, str):
            return valor.strip() or None

        return valor

    @field_validator("detalles")
    @classmethod
    def validar_detalles(cls, valores):
        if any(numero <= 0 for numero in valores):
            raise ValueError(
                "Los identificadores deben ser positivos."
            )

        # Cada detalle se procesa una sola vez.
        return list(dict.fromkeys(valores))