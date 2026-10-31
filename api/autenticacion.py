import hashlib
import hmac

from conexion import conectar


class ErrorAutenticacion(Exception):
    pass


def validar_credenciales(nombre_usuario: str, contrasena: str):
    """
    Valida las credenciales y el estado del usuario.

    No abre una sesión ni autoriza operaciones.
    Después debe completarse la verificación facial.
    """

    nombre_usuario = nombre_usuario.strip()

    if not nombre_usuario or not contrasena:
        raise ErrorAutenticacion(
            "Ingresa tu usuario y tu contraseña."
        )

    if len(nombre_usuario) > 40:
        raise ErrorAutenticacion(
            "Usuario o contraseña incorrectos."
        )

    hash_ingresado = hashlib.sha256(
        contrasena.encode("utf-8")
    ).hexdigest().upper()

    conexion = conectar()

    try:
        cursor = conexion.cursor()

        cursor.execute(
            """
            SELECT
                u.id_usuario,
                u.nombre,
                u.apellido,
                u.nombre_usuario,
                u.contrasena_hash,
                u.estado,
                r.nombre AS rol
            FROM dbo.Usuario AS u
            INNER JOIN dbo.Rol AS r
                ON r.id_rol = u.id_rol
            WHERE u.nombre_usuario = ?
            """,
            nombre_usuario
        )

        usuario = cursor.fetchone()

        hash_guardado = (
            str(usuario.contrasena_hash or "").upper()
            if usuario is not None
            else "0" * 64
        )

        coincide = hmac.compare_digest(
            hash_ingresado.encode("utf-8"),
            hash_guardado.encode("utf-8")
        )

        if usuario is None or not coincide:
            id_usuario = (
                usuario.id_usuario
                if usuario is not None
                else None
            )

            registrar_fallo(
                cursor,
                id_usuario,
                "Credenciales incorrectas en el acceso móvil."
            )

            conexion.commit()

            raise ErrorAutenticacion(
                "Usuario o contraseña incorrectos."
            )

        if usuario.estado != "ACTIVO":
            registrar_fallo(
                cursor,
                usuario.id_usuario,
                "Intento de acceso móvil con usuario inactivo."
            )

            conexion.commit()

            raise ErrorAutenticacion(
                "El usuario está inactivo. "
                "Comunícate con el administrador."
            )

        # Datos internos para el siguiente paso de autenticación.
        # No se devuelve la contraseña ni su hash.
        return {
            "id_usuario": usuario.id_usuario,
            "nombre": usuario.nombre,
            "apellido": usuario.apellido,
            "nombre_usuario": usuario.nombre_usuario,
            "rol": usuario.rol
        }

    except Exception:
        conexion.rollback()
        raise

    finally:
        conexion.close()


def registrar_fallo(cursor, id_usuario, descripcion):
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
        id_usuario,
        "LOGIN_FALLIDO",
        "Usuario",
        id_usuario,
        descripcion
    )