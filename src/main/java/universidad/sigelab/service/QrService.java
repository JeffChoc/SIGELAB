package universidad.sigelab.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.EncodeHintType;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.WriterException;
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
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * El QR tiene UNA sola finalidad: identificar un equipo (sección 6.6 de la guía).
 * Su contenido es únicamente el código de inventario, por ejemplo CAM-001.
 * RN-19: leer un QR identifica un equipo; nunca modifica su estado.
 * Para consultar el equipo real se usa luego EquipoService.buscarPorCodigo.
 */
public class QrService {

    private static final int TAMANO_IMAGEN = 400;

    private final EquipoService servicioEquipo = new EquipoService();

    /** Genera la imagen PNG del QR de un equipo que existe en la base de datos. */
    public void generarQrDeEquipo(String codigoInventario, File destino) {
        SesionUsuario.exigirRol(RolUsuario.ADMIN, RolUsuario.ENCARGADO);
        if (destino == null) {
            throw new BusinessException("Seleccione dónde guardar la imagen del QR.");
        }
        // Solo se genera QR de equipos reales: así el QR siempre apunta a un registro existente.
        Equipo equipo = servicioEquipo.buscarPorCodigo(codigoInventario);

        Map<EncodeHintType, Object> pistas = new EnumMap<>(EncodeHintType.class);
        pistas.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        pistas.put(EncodeHintType.MARGIN, 2);

        try {
            BitMatrix matriz = new QRCodeWriter().encode(equipo.getCodigoInventario(),
                    BarcodeFormat.QR_CODE, TAMANO_IMAGEN, TAMANO_IMAGEN, pistas);
            MatrixToImageWriter.writeToPath(matriz, "PNG", destino.toPath());
        } catch (WriterException e) {
            throw new IllegalStateException("No se pudo codificar el QR del equipo " + codigoInventario, e);
        } catch (IOException e) {
            throw new BusinessException("No se pudo guardar la imagen. "
                    + "Verifique que la carpeta exista y que tenga permisos de escritura.");
        }
    }

    /** Lee un QR desde un archivo de imagen y devuelve el código de inventario que contiene. */
    public String leerCodigo(File archivoImagen) {
        SesionUsuario.exigirSesion();
        if (archivoImagen == null) {
            throw new BusinessException("Seleccione la imagen del código QR.");
        }
        try {
            BufferedImage imagen = ImageIO.read(archivoImagen);
            if (imagen == null) {
                throw new BusinessException("El archivo seleccionado no es una imagen válida.");
            }
            LuminanceSource fuente = new BufferedImageLuminanceSource(imagen);
            BinaryBitmap mapaBits = new BinaryBitmap(new HybridBinarizer(fuente));

            Map<DecodeHintType, Object> pistas = new EnumMap<>(DecodeHintType.class);
            pistas.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
            pistas.put(DecodeHintType.POSSIBLE_FORMATS, List.of(BarcodeFormat.QR_CODE));

            Result resultado = new MultiFormatReader().decode(mapaBits, pistas);
            String codigo = TextoUtil.limpiar(resultado.getText());
            if (codigo == null) {
                throw new BusinessException("El código QR está vacío.");
            }
            return codigo.toUpperCase(Locale.ROOT);
        } catch (NotFoundException e) {
            throw new BusinessException("No se encontró un código QR legible en la imagen. "
                    + "Pruebe con una imagen más nítida.");
        } catch (IOException e) {
            throw new BusinessException("No se pudo abrir la imagen seleccionada.");
        }
    }
}
