package universidad.sigelab.service;

import universidad.sigelab.enums.AccionAuditoria;
import universidad.sigelab.enums.RolUsuario;
import universidad.sigelab.model.Auditoria;
import universidad.sigelab.model.ResultadoReporte;
import universidad.sigelab.model.Usuario;
import universidad.sigelab.repository.AuditoriaRepository;
import universidad.sigelab.util.SesionUsuario;

import java.sql.Connection;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * RN-18: las operaciones relevantes dejan rastro de auditoría.
 * La consulta del historial está reservada al rol ADMIN.
 */
public class AuditoriaService {

    private static final int LARGO_MAXIMO_DESCRIPCION = 300;
    private static final DateTimeFormatter FORMATO_FECHA =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final AuditoriaRepository repositorioAuditoria = new AuditoriaRepository();

    /** Registra una auditoría usando su propia conexión. */
    public void registrar(
            Integer idUsuario,
            AccionAuditoria accion,
            String entidad,
            Integer idRegistro,
            String descripcion) {

        repositorioAuditoria.insertar(
                new Auditoria(
                        idUsuario,
                        accion,
                        entidad,
                        idRegistro,
                        recortar(descripcion)
                )
        );
    }

    /**
     * Registra la auditoría dentro de una transacción existente.
     * Así la operación y su rastro se confirman o se revierten juntos.
     */
    public void registrar(
            Connection conexion,
            Integer idUsuario,
            AccionAuditoria accion,
            String entidad,
            Integer idRegistro,
            String descripcion) {

        repositorioAuditoria.insertar(
                conexion,
                new Auditoria(
                        idUsuario,
                        accion,
                        entidad,
                        idRegistro,
                        recortar(descripcion)
                )
        );
    }

    /** Registra una acción a nombre del usuario de la sesión actual. */
    public void registrarComoUsuarioActual(
            AccionAuditoria accion,
            String entidad,
            Integer idRegistro,
            String descripcion) {

        Integer idUsuario = idUsuarioActual();
        registrar(idUsuario, accion, entidad, idRegistro, descripcion);
    }

    /** Variante transaccional de registrarComoUsuarioActual. */
    public void registrarComoUsuarioActual(
            Connection conexion,
            AccionAuditoria accion,
            String entidad,
            Integer idRegistro,
            String descripcion) {

        registrar(conexion, idUsuarioActual(), accion, entidad, idRegistro, descripcion);
    }

    /**
     * Consulta auditorías por rango de fechas, acción y usuario.
     * Solo el ADMIN puede leer el historial completo del sistema.
     */
    public List<Auditoria> consultar(
            LocalDate fechaInicio,
            LocalDate fechaFin,
            AccionAuditoria accion,
            String usuario) {

        SesionUsuario.exigirRol(RolUsuario.ADMIN);
        return repositorioAuditoria.consultar(
                fechaInicio,
                fechaFin,
                accion,
                usuario
        );
    }

    /** Genera un reporte listo para mostrar en una tabla. */
    public ResultadoReporte generarReporte(
            LocalDate fechaInicio,
            LocalDate fechaFin,
            AccionAuditoria accion,
            String usuario) {

        // consultar() valida que la sesión sea de un administrador; el reporte
        // no se puede generar sin pasar por esa comprobación.
        List<Auditoria> auditorias = consultar(
                fechaInicio,
                fechaFin,
                accion,
                usuario
        );

        List<String> columnas = List.of(
                "ID",
                "Usuario",
                "Fecha y hora",
                "Acción",
                "Entidad",
                "ID registro",
                "Descripción"
        );

        List<Object[]> filas = new ArrayList<>();

        for (Auditoria auditoria : auditorias) {
            filas.add(new Object[]{
                    auditoria.getIdAuditoria(),
                    nombreUsuarioVisible(auditoria),
                    auditoria.getFechaHora() == null
                            ? ""
                            : FORMATO_FECHA.format(auditoria.getFechaHora()),
                    auditoria.getAccion(),
                    auditoria.getEntidad(),
                    auditoria.getIdRegistro(),
                    auditoria.getDescripcion()
            });
        }

        return new ResultadoReporte(columnas, filas);
    }

    private Integer idUsuarioActual() {
        Usuario usuario = SesionUsuario.getUsuarioActual();
        return usuario == null ? null : usuario.getIdUsuario();
    }

    /**
     * Muestra el nombre de usuario cuando existe. Si el registro es de un login fallido
     * con un usuario que no está en la base, se conserva el id (o "—" si tampoco lo hay).
     */
    private String nombreUsuarioVisible(Auditoria auditoria) {
        if (auditoria.getNombreUsuario() != null && !auditoria.getNombreUsuario().isBlank()) {
            return auditoria.getNombreUsuario();
        }
        if (auditoria.getIdUsuario() != null) {
            return "#" + auditoria.getIdUsuario();
        }
        return "—";
    }

    /** Limita la longitud de la descripción a 300 caracteres. */
    private String recortar(String texto) {
        if (texto == null || texto.length() <= LARGO_MAXIMO_DESCRIPCION) {
            return texto;
        }
        return texto.substring(0, LARGO_MAXIMO_DESCRIPCION);
    }
}
