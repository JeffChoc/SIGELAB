package universidad.sigelab.service;

import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeWriter;
import universidad.sigelab.enums.RolUsuario;
import universidad.sigelab.exception.BusinessException;
import universidad.sigelab.model.Equipo;
import universidad.sigelab.util.SesionUsuario;
import universidad.sigelab.util.TextoUtil;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class QrService {

    private static final int TAMANO_IMAGEN = 400;

    private static final Path CARPETA_QR =
            Path.of(System.getProperty("user.dir"), "codigos_qr");

    private final EquipoService servicioEquipo = new EquipoService();

    /**
     * Devuelve la carpeta central de códigos QR y la crea si no existe.
     */
    public File obtenerCarpetaQr() {
        try {
            Files.createDirectories(CARPETA_QR);
            return CARPETA_QR.toFile();
        } catch (IOException e) {
            throw new BusinessException(
                    "No se pudo crear la carpeta de códigos QR."
            );
        }
    }

    /**
     * Abre la carpeta de códigos QR en el Explorador de Windows.
     */
    public void abrirCarpetaQr() {
        File carpeta = obtenerCarpetaQr();

        if (!Desktop.isDesktopSupported()) {
            throw new BusinessException(
                    "No se puede abrir el explorador de archivos en este equipo."
            );
        }

        try {
            Desktop.getDesktop().open(carpeta);
        } catch (IOException e) {
            throw new BusinessException(
                    "No se pudo abrir la carpeta de códigos QR."
            );
        }
    }

    /**
     * Genera el QR de un equipo existente y lo guarda en codigos_qr.
     */
    public File generarQrDeEquipo(String codigoInventario) {
        SesionUsuario.exigirRol(
                RolUsuario.ADMIN,
                RolUsuario.ENCARGADO
        );

        Equipo equipo = servicioEquipo.buscarPorCodigo(codigoInventario);

        String codigo = TextoUtil.limpiar(
                equipo.getCodigoInventario()
        );

        if (codigo == null || codigo.isBlank()) {
            throw new BusinessException(
                    "El equipo no tiene un código de inventario válido."
            );
        }

        File carpeta = obtenerCarpetaQr();

        // Evita que el código genere rutas fuera de la carpeta QR.
        String nombreArchivo = codigo.replaceAll(
                "[^a-zA-Z0-9._-]",
                "_"
        );

        File destino = new File(carpeta, nombreArchivo + ".png");

        Map<EncodeHintType, Object> pistas =
                new EnumMap<>(EncodeHintType.class);

        pistas.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        pistas.put(EncodeHintType.MARGIN, 2);

        try {
            BitMatrix matriz = new QRCodeWriter().encode(
                    codigo,
                    BarcodeFormat.QR_CODE,
                    TAMANO_IMAGEN,
                    TAMANO_IMAGEN,
                    pistas
            );

            MatrixToImageWriter.writeToPath(
                    matriz,
                    "PNG",
                    destino.toPath()
            );

            return destino;

        } catch (WriterException e) {
            throw new IllegalStateException(
                    "No se pudo generar el QR del equipo " + codigo,
                    e
            );
        } catch (IOException e) {
            throw new BusinessException(
                    "No se pudo guardar la imagen QR. "
                            + "Verifique los permisos de la carpeta."
            );
        }
    }

    /**
     * Conserva la firma anterior por compatibilidad con el controlador.
     * El archivo destino recibido se utiliza directamente.
     */
    public void generarQrDeEquipo(
            String codigoInventario,
            File destino) {

        SesionUsuario.exigirRol(
                RolUsuario.ADMIN,
                RolUsuario.ENCARGADO
        );

        if (destino == null) {
            throw new BusinessException(
                    "Seleccione dónde guardar la imagen del QR."
            );
        }

        Equipo equipo = servicioEquipo.buscarPorCodigo(
                codigoInventario
        );

        Map<EncodeHintType, Object> pistas =
                new EnumMap<>(EncodeHintType.class);

        pistas.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        pistas.put(EncodeHintType.MARGIN, 2);

        try {
            BitMatrix matriz = new QRCodeWriter().encode(
                    equipo.getCodigoInventario(),
                    BarcodeFormat.QR_CODE,
                    TAMANO_IMAGEN,
                    TAMANO_IMAGEN,
                    pistas
            );

            Path ruta = destino.toPath();
            Path carpeta = ruta.toAbsolutePath().getParent();

            if (carpeta != null) {
                Files.createDirectories(carpeta);
            }

            MatrixToImageWriter.writeToPath(
                    matriz,
                    "PNG",
                    ruta
            );

        } catch (WriterException e) {
            throw new IllegalStateException(
                    "No se pudo codificar el QR del equipo "
                            + codigoInventario,
                    e
            );
        } catch (IOException e) {
            throw new BusinessException(
                    "No se pudo guardar la imagen QR."
            );
        }
    }

    /**
     * Lee un QR desde una imagen. No modifica el estado del equipo.
     */
    public String leerCodigo(File archivoImagen) {
        SesionUsuario.exigirSesion();

        if (archivoImagen == null) {
            throw new BusinessException(
                    "Seleccione la imagen del código QR."
            );
        }

        try {
            BufferedImage imagen = ImageIO.read(archivoImagen);

            if (imagen == null) {
                throw new BusinessException(
                        "El archivo seleccionado no es una imagen válida."
                );
            }

            LuminanceSource fuente =
                    new BufferedImageLuminanceSource(imagen);

            BinaryBitmap mapaBits =
                    new BinaryBitmap(new HybridBinarizer(fuente));

            Map<DecodeHintType, Object> pistas =
                    new EnumMap<>(DecodeHintType.class);

            pistas.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
            pistas.put(
                    DecodeHintType.POSSIBLE_FORMATS,
                    List.of(BarcodeFormat.QR_CODE)
            );

            Result resultado =
                    new MultiFormatReader().decode(mapaBits, pistas);

            String codigo = TextoUtil.limpiar(resultado.getText());

            if (codigo == null || codigo.isBlank()) {
                throw new BusinessException(
                        "El código QR está vacío."
                );
            }

            return codigo.toUpperCase(Locale.ROOT);

        } catch (NotFoundException e) {
            throw new BusinessException(
                    "No se encontró un código QR legible en la imagen. "
                            + "Pruebe con una imagen más nítida."
            );
        } catch (IOException e) {
            throw new BusinessException(
                    "No se pudo abrir la imagen seleccionada."
            );
        }
    }
}
