package universidad.sigelab.config;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Abre conexiones a SQL Server. Los datos de conexión NO están en el código:
 * se leen de config.properties, que Git ignora (ver config.properties.example).
 */
public final class DatabaseConnection {

    private static final String ARCHIVO_CONFIGURACION = "config.properties";
    private static final String[] PROPIEDADES_OBLIGATORIAS = {"db.url", "db.usuario", "db.contrasena"};

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

    /** Lee el archivo una sola vez. Se busca en la carpeta desde donde se ejecuta el programa. */
    private static synchronized Properties cargarConfiguracion() throws SQLException {
        if (configuracion != null) {
            return configuracion;
        }
        Path ruta = Paths.get(ARCHIVO_CONFIGURACION);
        if (!Files.exists(ruta)) {
            throw new SQLException("No se encontró el archivo " + ruta.toAbsolutePath()
                    + ". Copie config.properties.example como config.properties y complete sus datos.");
        }

        Properties leidas = new Properties();
        try (Reader lector = Files.newBufferedReader(ruta, StandardCharsets.UTF_8)) {
            leidas.load(lector);
        } catch (IOException e) {
            throw new SQLException("No se pudo leer el archivo " + ruta.toAbsolutePath(), e);
        }

        for (String propiedad : PROPIEDADES_OBLIGATORIAS) {
            String valor = leidas.getProperty(propiedad);
            if (valor == null || valor.isBlank()) {
                throw new SQLException("Falta la propiedad '" + propiedad + "' en config.properties.");
            }
        }
        configuracion = leidas;
        return configuracion;
    }
}
