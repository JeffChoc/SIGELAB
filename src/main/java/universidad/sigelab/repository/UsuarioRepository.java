package universidad.sigelab.repository;

import universidad.sigelab.config.DatabaseConnection;
import universidad.sigelab.enums.EstadoRegistro;
import universidad.sigelab.enums.RolUsuario;
import universidad.sigelab.exception.DataAccessException;
import universidad.sigelab.model.Rol;
import universidad.sigelab.model.Usuario;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UsuarioRepository {

    private static final String SELECT_BASE = """
            SELECT u.id_usuario, u.nombre, u.apellido, u.nombre_usuario, u.contrasena_hash,
                   u.correo, u.estado, u.referencia_facial,
                   r.id_rol, r.nombre AS rol_nombre, r.descripcion AS rol_descripcion
            FROM Usuario u
            JOIN Rol r ON r.id_rol = u.id_rol
            """;

    public List<Usuario> listarTodos() {
        List<Usuario> usuarios = new ArrayList<>();
        try (Connection conexion = DatabaseConnection.getConnection();
             PreparedStatement sentencia = conexion.prepareStatement(SELECT_BASE + " ORDER BY u.nombre_usuario");
             ResultSet resultado = sentencia.executeQuery()) {
            while (resultado.next()) {
                usuarios.add(mapear(resultado));
            }
            return usuarios;
        } catch (SQLException e) {
            throw new DataAccessException("Error al listar los usuarios", e);
        }
    }

    public Optional<Usuario> buscarPorNombreUsuario(String nombreUsuario) {
        try (Connection conexion = DatabaseConnection.getConnection();
             PreparedStatement sentencia = conexion.prepareStatement(SELECT_BASE + " WHERE u.nombre_usuario = ?")) {
            sentencia.setString(1, nombreUsuario);
            try (ResultSet resultado = sentencia.executeQuery()) {
                return resultado.next() ? Optional.of(mapear(resultado)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error al buscar el usuario " + nombreUsuario, e);
        }
    }

    public Optional<Usuario> buscarPorId(int idUsuario) {
        try (Connection conexion = DatabaseConnection.getConnection();
             PreparedStatement sentencia = conexion.prepareStatement(SELECT_BASE + " WHERE u.id_usuario = ?")) {
            sentencia.setInt(1, idUsuario);
            try (ResultSet resultado = sentencia.executeQuery()) {
                return resultado.next() ? Optional.of(mapear(resultado)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error al buscar el usuario " + idUsuario, e);
        }
    }

    public boolean existeNombreUsuario(String nombreUsuario, int idExcluido) {
        return contar("SELECT COUNT(*) FROM Usuario WHERE nombre_usuario = ? AND id_usuario <> ?",
                nombreUsuario, idExcluido) > 0;
    }

    public boolean existeCorreo(String correo, int idExcluido) {
        return contar("SELECT COUNT(*) FROM Usuario WHERE correo = ? AND id_usuario <> ?",
                correo, idExcluido) > 0;
    }

    /**
     * Indica si, excluyendo al usuario indicado, queda al menos otro ADMIN activo.
     * Se usa para no dejar el sistema sin administradores.
     */
    public boolean existeOtroAdministradorActivo(int idExcluido) {
        String sql = """
                SELECT COUNT(*)
                FROM Usuario u
                JOIN Rol r ON r.id_rol = u.id_rol
                WHERE r.nombre = ? AND u.estado = ? AND u.id_usuario <> ?
                """;
        try (Connection conexion = DatabaseConnection.getConnection();
             PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setString(1, RolUsuario.ADMIN.name());
            sentencia.setString(2, EstadoRegistro.ACTIVO.name());
            sentencia.setInt(3, idExcluido);
            try (ResultSet resultado = sentencia.executeQuery()) {
                return resultado.next() && resultado.getInt(1) > 0;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error al contar los administradores activos", e);
        }
    }

    /** Inserta usando el hash que trae el objeto usuario. Devuelve el id generado. */
    public int insertar(Usuario usuario) {
        try (Connection conexion = DatabaseConnection.getConnection()) {
            return insertar(conexion, usuario);
        } catch (SQLException e) {
            throw new DataAccessException("Error al insertar el usuario " + usuario.getNombreUsuario(), e);
        }
    }

    /** Variante para usar dentro de una transacción. */
    public int insertar(Connection conexion, Usuario usuario) {
        String sql = """
                INSERT INTO Usuario (nombre, apellido, nombre_usuario, contrasena_hash, correo,
                                     id_rol, estado, referencia_facial)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement sentencia = conexion.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            sentencia.setString(1, usuario.getNombre());
            sentencia.setString(2, usuario.getApellido());
            sentencia.setString(3, usuario.getNombreUsuario());
            sentencia.setString(4, usuario.getContrasenaHash());
            sentencia.setString(5, usuario.getCorreo());
            sentencia.setInt(6, usuario.getRol().getIdRol());
            sentencia.setString(7, usuario.getEstado().name());
            sentencia.setString(8, usuario.getReferenciaFacial());
            sentencia.executeUpdate();
            try (ResultSet claves = sentencia.getGeneratedKeys()) {
                if (claves.next()) {
                    return claves.getInt(1);
                }
                throw new DataAccessException("No se obtuvo el id generado del usuario", null);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error al insertar el usuario " + usuario.getNombreUsuario(), e);
        }
    }

    /** No toca ni la contraseña ni el estado: tienen sus propios métodos. */
    public void actualizar(Usuario usuario) {
        try (Connection conexion = DatabaseConnection.getConnection()) {
            actualizar(conexion, usuario);
        } catch (SQLException e) {
            throw new DataAccessException("Error al actualizar el usuario " + usuario.getIdUsuario(), e);
        }
    }

    /** Variante para usar dentro de una transacción. */
    public void actualizar(Connection conexion, Usuario usuario) {
        String sql = """
                UPDATE Usuario
                SET nombre = ?, apellido = ?, nombre_usuario = ?, correo = ?, id_rol = ?, referencia_facial = ?
                WHERE id_usuario = ?
                """;
        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setString(1, usuario.getNombre());
            sentencia.setString(2, usuario.getApellido());
            sentencia.setString(3, usuario.getNombreUsuario());
            sentencia.setString(4, usuario.getCorreo());
            sentencia.setInt(5, usuario.getRol().getIdRol());
            sentencia.setString(6, usuario.getReferenciaFacial());
            sentencia.setInt(7, usuario.getIdUsuario());
            sentencia.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Error al actualizar el usuario " + usuario.getIdUsuario(), e);
        }
    }

    public void actualizarContrasena(int idUsuario, String contrasenaHash) {
        try (Connection conexion = DatabaseConnection.getConnection()) {
            actualizarContrasena(conexion, idUsuario, contrasenaHash);
        } catch (SQLException e) {
            throw new DataAccessException("Error al cambiar la contraseña del usuario " + idUsuario, e);
        }
    }

    /** Variante para usar dentro de una transacción. */
    public void actualizarContrasena(Connection conexion, int idUsuario, String contrasenaHash) {
        String sql = "UPDATE Usuario SET contrasena_hash = ? WHERE id_usuario = ?";
        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setString(1, contrasenaHash);
            sentencia.setInt(2, idUsuario);
            sentencia.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Error al cambiar la contraseña del usuario " + idUsuario, e);
        }
    }

    public void actualizarEstado(int idUsuario, EstadoRegistro estado) {
        try (Connection conexion = DatabaseConnection.getConnection()) {
            actualizarEstado(conexion, idUsuario, estado);
        } catch (SQLException e) {
            throw new DataAccessException("Error al cambiar el estado del usuario " + idUsuario, e);
        }
    }

    /** Variante para usar dentro de una transacción. */
    public void actualizarEstado(Connection conexion, int idUsuario, EstadoRegistro estado) {
        String sql = "UPDATE Usuario SET estado = ? WHERE id_usuario = ?";
        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setString(1, estado.name());
            sentencia.setInt(2, idUsuario);
            sentencia.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Error al cambiar el estado del usuario " + idUsuario, e);
        }
    }

    private int contar(String sql, String texto, int idExcluido) {
        try (Connection conexion = DatabaseConnection.getConnection();
             PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setString(1, texto);
            sentencia.setInt(2, idExcluido);
            try (ResultSet resultado = sentencia.executeQuery()) {
                return resultado.next() ? resultado.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error al verificar duplicados de usuario", e);
        }
    }

    private Usuario mapear(ResultSet resultado) throws SQLException {
        Rol rol;
        EstadoRegistro estado;
        try {
            rol = new Rol(
                    resultado.getInt("id_rol"),
                    RolUsuario.desde(resultado.getString("rol_nombre")),
                    resultado.getString("rol_descripcion"));
            estado = EstadoRegistro.valueOf(resultado.getString("estado"));
        } catch (IllegalArgumentException e) {
            // Un rol o estado escrito de forma distinta en la base no debe romper toda la pantalla.
            throw new DataAccessException(
                    "La base contiene un rol o estado de usuario no reconocido.", e);
        }

        Usuario usuario = new Usuario();
        usuario.setIdUsuario(resultado.getInt("id_usuario"));
        usuario.setNombre(resultado.getString("nombre"));
        usuario.setApellido(resultado.getString("apellido"));
        usuario.setNombreUsuario(resultado.getString("nombre_usuario"));
        usuario.setContrasenaHash(resultado.getString("contrasena_hash"));
        usuario.setCorreo(resultado.getString("correo"));
        usuario.setRol(rol);
        usuario.setEstado(estado);
        usuario.setReferenciaFacial(resultado.getString("referencia_facial"));
        return usuario;
    }
}
