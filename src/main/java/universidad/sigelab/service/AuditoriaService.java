package universidad.sigelab.service;

import universidad.sigelab.enums.AccionAuditoria;
import universidad.sigelab.model.Auditoria;
import universidad.sigelab.model.ResultadoReporte;
import universidad.sigelab.model.Usuario;
import universidad.sigelab.repository.AuditoriaRepository;
import universidad.sigelab.util.SesionUsuario;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * RN-18: las operaciones relevantes dejan rastro de auditoría.
 */
public class AuditoriaService {

    private static final int LARGO_MAXIMO_DESCRIPCION = 300;

    private final AuditoriaRepository repositorioAuditoria =
            new AuditoriaRepository();

    /**
     * Registra una auditoría.
     */
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
     * Registra una acción a nombre del usuario de la sesión actual.
     */
    public void registrarComoUsuarioActual(
            AccionAuditoria accion,
            String entidad,
            Integer idRegistro,
            String descripcion) {

        Usuario usuario = SesionUsuario.getUsuarioActual();

        Integer idUsuario = usuario == null
                ? null
                : usuario.getIdUsuario();

        registrar(
                idUsuario,
                accion,
                entidad,
                idRegistro,
                descripcion
        );
    }

    /**
     * Consulta auditorías por rango de fechas, acción y usuario.
     */
    public List<Auditoria> consultar(
            LocalDate fechaInicio,
            LocalDate fechaFin,
            AccionAuditoria accion,
            String usuario) {

        return repositorioAuditoria.consultar(
                fechaInicio,
                fechaFin,
                accion,
                usuario
        );
    }

    /**
     * Genera un reporte listo para mostrar en una tabla.
     */
    public ResultadoReporte generarReporte(
            LocalDate fechaInicio,
            LocalDate fechaFin,
            AccionAuditoria accion,
            String usuario) {

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
                    auditoria.getIdUsuario(),
                    auditoria.getFechaHora(),
                    auditoria.getAccion(),
                    auditoria.getEntidad(),
                    auditoria.getIdRegistro(),
                    auditoria.getDescripcion()
            });
        }

        return new ResultadoReporte(columnas, filas);
    }

    /**
     * Limita la longitud de la descripción a 300 caracteres.
     */
    private String recortar(String texto) {

        if (texto == null
                || texto.length() <= LARGO_MAXIMO_DESCRIPCION) {
            return texto;
        }

        return texto.substring(0, LARGO_MAXIMO_DESCRIPCION);
    }
}
