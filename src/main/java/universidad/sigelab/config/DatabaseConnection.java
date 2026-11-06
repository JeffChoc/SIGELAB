package universidad.sigelab.config;

import java.io.IOException;
import java.io.Reader;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Abre conexiones a SQL Server. Los datos de conexión NO están en el código:
 * se leen de config.properties, que Git ignora (ver config.properties.example).
 *
 * El archivo se busca primero en la carpeta de trabajo y, si no está, subiendo desde la carpeta del
 * código compilado (target/classes) hasta encontrarlo. Así funciona aunque el IDE ejecute el programa
 * desde otra carpeta, siempre que config.properties esté en la carpeta del proyecto (junto al pom.xml).
 */
public final class DatabaseConnection {

    private static final String ARCHIVO_CONFIGURACION = "config.properties";
    private static final String[] PROPIEDADES_OBLIGATORIAS = {"db.url", "db.usuario", "db.contrasena"};
    private static final int NIVELES_HACIA_ARRIBA = 4;

    private static Properties configuracion;

    private DatabaseConnection() {
    }

    public static Connection getConnection() throws SQLException {
        Properties datos = cargarConfiguracion();
        return DriverManager.getConnection(
                datos.getProperty("db.url"),
                datos.getProperty("db.usuario"),
                datos.getProperty("db.contrasena"));
    }

    /** Lee el archivo una sola vez. */
    private static synchronized Properties cargarConfiguracion() throws SQLException {
        if (configuracion != null) {
            return configuracion;
        }
        List<Path> candidatos = rutasCandidatas();
        Path ruta = null;
        for (Path candidato : candidatos) {
            if (Files.isRegularFile(candidato)) {
                ruta = candidato;
                break;
            }
        }
        if (ruta == null) {
            StringBuilder buscadas = new StringBuilder();
            for (Path candidato : candidatos) {
                buscadas.append("\n  - ").append(candidato);
            }
            throw new SQLException("No se encontró el archivo " + ARCHIVO_CONFIGURACION
                    + ". Copie config.properties.example como config.properties (en la carpeta del pom.xml) "
                    + "y complete sus datos. Se buscó en:" + buscadas);
        }

        Properties leidas = new Properties();
        try (Reader lector = Files.newBufferedReader(ruta, StandardCharsets.UTF_8)) {
            leidas.load(lector);
        } catch (IOException e) {
            throw new SQLException("No se pudo leer el archivo " + ruta, e);
        }

        for (String propiedad : PROPIEDADES_OBLIGATORIAS) {
            String valor = leidas.getProperty(propiedad);
            if (valor == null || valor.isBlank()) {
                throw new SQLException("Falta la propiedad '" + propiedad + "' en " + ruta + ".");
            }
        }
        configuracion = leidas;
        return configuracion;
    }

    /** Carpeta de trabajo primero; después, desde target/classes hacia arriba hasta la carpeta del proyecto. */
    private static List<Path> rutasCandidatas() {
        List<Path> candidatos = new ArrayList<>();
        candidatos.add(Paths.get(ARCHIVO_CONFIGURACION).toAbsolutePath());
        try {
            Path codigo = Paths.get(DatabaseConnection.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI());
            Path carpeta = Files.isDirectory(codigo) ? codigo : codigo.getParent();
            for (int nivel = 0; nivel < NIVELES_HACIA_ARRIBA && carpeta != null; nivel++) {
                candidatos.add(carpeta.resolve(ARCHIVO_CONFIGURACION));
                carpeta = carpeta.getParent();
            }
        } catch (URISyntaxException | RuntimeException ignorada) {
            // si no se puede determinar la ubicación del código, solo se usa la carpeta de trabajo
        }
        return candidatos;
    }
}