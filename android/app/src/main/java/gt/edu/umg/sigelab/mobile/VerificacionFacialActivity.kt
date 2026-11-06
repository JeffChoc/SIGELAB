package gt.edu.umg.sigelab.mobile

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

class VerificacionFacialActivity : AppCompatActivity() {

    private lateinit var vistaCamara: PreviewView
    private lateinit var textoEstado: TextView
    private lateinit var botonVerificar: MaterialButton
    private lateinit var botonCancelar: MaterialButton

    private val ejecutor = Executors.newSingleThreadExecutor()
    private val manejador = Handler(Looper.getMainLooper())

    private var proveedorCamara: ProcessCameraProvider? = null
    private var captura: ImageCapture? = null

    private val imagenes = ArrayList<String>()

    private var servidor = ""
    private var desafio = ""

    private var ocupado = false
    private var errorMostrado = false

    @Volatile
    private var destruida = false

    private val permisoCamara = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { autorizado ->
        if (autorizado) {
            iniciarCamara()
        } else {
            mostrarError(
                "Necesitamos permiso para usar la cámara frontal. " +
                        "Puedes habilitarlo en los permisos de la aplicación."
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        window.addFlags(
            WindowManager.LayoutParams.FLAG_SECURE
        )

        setContentView(R.layout.activity_verificacion_facial)

        vistaCamara = findViewById(R.id.vistaCamaraFacial)
        textoEstado = findViewById(R.id.txtEstadoFacial)
        botonVerificar = findViewById(R.id.btnVerificarRostro)
        botonCancelar = findViewById(R.id.btnCancelarFacial)

        configurarMargenes()

        servidor = intent.getStringExtra("servidor")
            ?.trim()
            ?.trimEnd('/')
            .orEmpty()

        desafio = intent.getStringExtra("desafio").orEmpty()

        botonVerificar.isEnabled = false

        botonCancelar.setOnClickListener {
            setResult(RESULT_CANCELED)
            finish()
        }

        botonVerificar.setOnClickListener {
            comenzarCapturas()
        }

        if (servidor.isBlank() || desafio.isBlank()) {
            mostrarError(
                "Primero debes ingresar tu usuario y contraseña."
            )
            return
        }

        val autorizado = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (autorizado) {
            iniciarCamara()
        } else {
            permisoCamara.launch(Manifest.permission.CAMERA)
        }
    }

    private fun configurarMargenes() {
        val principal = findViewById<View>(
            R.id.facialPrincipal
        )

        val izquierda = principal.paddingLeft
        val arriba = principal.paddingTop
        val derecha = principal.paddingRight
        val abajo = principal.paddingBottom

        ViewCompat.setOnApplyWindowInsetsListener(principal) { vista, insets ->
            val barras = insets.getInsets(
                WindowInsetsCompat.Type.systemBars()
            )

            vista.setPadding(
                izquierda + barras.left,
                arriba + barras.top,
                derecha + barras.right,
                abajo + barras.bottom
            )

            insets
        }
    }

    private fun iniciarCamara() {
        textoEstado.text = "Preparando cámara frontal…"

        val futuro = ProcessCameraProvider.getInstance(this)

        futuro.addListener(
            {
                if (!pantallaDisponible()) {
                    return@addListener
                }

                try {
                    val proveedor = futuro.get()
                    proveedorCamara = proveedor

                    val selector = CameraSelector.DEFAULT_FRONT_CAMERA

                    if (!proveedor.hasCamera(selector)) {
                        mostrarError(
                            "No se encontró una cámara frontal disponible."
                        )
                        return@addListener
                    }

                    val vistaPrevia = Preview.Builder()
                        .build()

                    vistaPrevia.setSurfaceProvider(
                        vistaCamara.surfaceProvider
                    )

                    val nuevaCaptura = ImageCapture.Builder()
                        .setCaptureMode(
                            ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY
                        )
                        .build()

                    proveedor.unbindAll()

                    proveedor.bindToLifecycle(
                        this,
                        selector,
                        vistaPrevia,
                        nuevaCaptura
                    )

                    captura = nuevaCaptura

                    textoEstado.text =
                        "Coloca tu rostro frente a la cámara y pulsa " +
                                "«Verificar mi rostro»."

                    botonVerificar.isEnabled = true

                } catch (_: Exception) {
                    mostrarError(
                        "No se pudo iniciar la cámara. " +
                                "Cierra otras aplicaciones que la estén usando."
                    )
                }
            },
            ContextCompat.getMainExecutor(this)
        )
    }

    private fun comenzarCapturas() {
        if (ocupado || captura == null || errorMostrado) {
            return
        }

        ocupado = true
        imagenes.clear()

        botonVerificar.isEnabled = false

        textoEstado.text =
            "Mira de frente y mantén el teléfono quieto."

        tomarSiguienteImagen()
    }

    private fun tomarSiguienteImagen() {
        if (!pantallaDisponible() || errorMostrado) {
            return
        }

        val capturador = captura

        if (capturador == null) {
            mostrarError("La cámara no está disponible.")
            return
        }

        vistaCamara.display?.let { pantalla ->
            capturador.targetRotation = pantalla.rotation
        }

        textoEstado.text =
            "Capturando imagen ${imagenes.size + 1} de 8…"

        capturador.takePicture(
            ejecutor,
            object : ImageCapture.OnImageCapturedCallback() {

                override fun onCaptureSuccess(imagen: ImageProxy) {
                    val resultado = try {
                        convertirImagen(imagen)
                    } catch (_: Exception) {
                        null
                    } finally {
                        imagen.close()
                    }

                    runOnUiThread {
                        if (!pantallaDisponible() || errorMostrado) {
                            return@runOnUiThread
                        }

                        if (resultado == null) {
                            mostrarError(
                                "No se pudo preparar la imagen de la cámara."
                            )
                            return@runOnUiThread
                        }

                        imagenes.add(resultado)

                        if (imagenes.size >= 8) {
                            enviarImagenes()
                        } else {
                            manejador.postDelayed(
                                { tomarSiguienteImagen() },
                                400
                            )
                        }
                    }
                }

                override fun onError(
                    excepcion: ImageCaptureException
                ) {
                    runOnUiThread {
                        mostrarError(
                            "No se pudo capturar tu rostro. " +
                                    "Vuelve a iniciar el acceso."
                        )
                    }
                }
            }
        )
    }

    private fun convertirImagen(imagen: ImageProxy): String {
        val original = imagen.toBitmap()

        var orientada: Bitmap? = null
        var reducida: Bitmap? = null

        try {
            val matriz = Matrix().apply {
                postRotate(
                    imagen.imageInfo.rotationDegrees.toFloat()
                )
            }

            val correcta = Bitmap.createBitmap(
                original,
                0,
                0,
                original.width,
                original.height,
                matriz,
                true
            )

            orientada = correcta

            val ladoMayor = maxOf(
                correcta.width,
                correcta.height
            )

            val final = if (ladoMayor > 960) {
                val escala = 960.0 / ladoMayor

                Bitmap.createScaledBitmap(
                    correcta,
                    (correcta.width * escala).toInt().coerceAtLeast(1),
                    (correcta.height * escala).toInt().coerceAtLeast(1),
                    true
                )
            } else {
                correcta
            }

            reducida = final

            return ByteArrayOutputStream().use { salida ->
                val correcto = final.compress(
                    Bitmap.CompressFormat.JPEG,
                    80,
                    salida
                )

                check(correcto) {
                    "No se pudo convertir la captura."
                }

                Base64.encodeToString(
                    salida.toByteArray(),
                    Base64.NO_WRAP
                )
            }

        } finally {
            reducida?.let {
                if (it !== orientada && it !== original) {
                    it.recycle()
                }
            }

            orientada?.let {
                if (it !== original) {
                    it.recycle()
                }
            }

            original.recycle()
        }
    }

    private fun enviarImagenes() {
        textoEstado.text =
            "Verificando tu identidad. Espera un momento…"

        val solicitud = JSONObject().apply {
            put("desafio", desafio)
            put("imagenes", JSONArray(imagenes))
        }.toString()

        imagenes.clear()

        ejecutor.execute {
            var conexion: HttpURLConnection? = null
            var tokenRecibido: String? = null
            var mensajeError: String? = null

            try {
                conexion = URL(
                    "$servidor/autenticacion/rostro"
                ).openConnection() as HttpURLConnection

                conexion.requestMethod = "POST"
                conexion.connectTimeout = 5000
                conexion.readTimeout = 60000
                conexion.instanceFollowRedirects = false
                conexion.useCaches = false
                conexion.doOutput = true

                conexion.setRequestProperty(
                    "Content-Type",
                    "application/json; charset=UTF-8"
                )

                conexion.setRequestProperty(
                    "Accept",
                    "application/json"
                )

                val cuerpo = solicitud.toByteArray(
                    Charsets.UTF_8
                )

                try {
                    conexion.setFixedLengthStreamingMode(
                        cuerpo.size
                    )

                    conexion.outputStream.use { salida ->
                        salida.write(cuerpo)
                    }
                } finally {
                    cuerpo.fill(0)
                }

                val codigo = conexion.responseCode

                val flujo = if (codigo in 200..299) {
                    conexion.inputStream
                } else {
                    conexion.errorStream
                }

                val texto = flujo?.bufferedReader(
                    Charsets.UTF_8
                )?.use { lector ->
                    lector.readText()
                }.orEmpty()

                val respuesta = try {
                    JSONObject(texto)
                } catch (_: Exception) {
                    JSONObject()
                }

                if (
                    codigo == 200 &&
                    respuesta.optString("estado") == "autenticado" &&
                    respuesta.optBoolean("sesion_abierta", false)
                ) {
                    val token = respuesta.optString("token")

                    if (token.isBlank()) {
                        mensajeError =
                            "El servidor no entregó una sesión válida."
                    } else {
                        tokenRecibido = token
                    }
                } else {
                    mensajeError = when (codigo) {
                        401 -> respuesta.optString("detail")
                            .takeIf { it.isNotBlank() }
                            ?: "No se confirmó la coincidencia facial."

                        400, 422 ->
                            "Las capturas no pudieron procesarse. " +
                                    "Vuelve a iniciar el acceso."

                        413 ->
                            "Las imágenes superan el tamaño permitido."

                        404 ->
                            "El servidor no tiene disponible " +
                                    "la verificación facial móvil."

                        503 ->
                            "El servicio facial o la base de datos " +
                                    "no están disponibles."

                        else ->
                            "No se pudo completar la verificación " +
                                    "(HTTP $codigo)."
                    }
                }

            } catch (_: java.net.SocketTimeoutException) {
                mensajeError =
                    "El servidor tardó demasiado en responder. " +
                            "Vuelve a ingresar tu usuario y contraseña."

            } catch (_: Exception) {
                mensajeError =
                    "No se pudo conectar con el servidor. " +
                            "Revisa la conexión y que la API esté funcionando."

            } finally {
                conexion?.disconnect()
            }

            val tokenFinal = tokenRecibido
            val errorFinal = mensajeError

            runOnUiThread {
                if (!pantallaDisponible()) {
                    return@runOnUiThread
                }

                if (!tokenFinal.isNullOrBlank()) {
                    textoEstado.text = "Identidad verificada."

                    val resultado = Intent().apply {
                        putExtra("token", tokenFinal)
                        putExtra("servidor", servidor)
                    }

                    setResult(RESULT_OK, resultado)
                    finish()
                } else {
                    mostrarError(
                        errorFinal
                            ?: "No se pudo verificar tu identidad."
                    )
                }
            }
        }
    }

    private fun mostrarError(mensaje: String) {
        if (!pantallaDisponible() || errorMostrado) {
            return
        }

        errorMostrado = true
        manejador.removeCallbacksAndMessages(null)

        botonVerificar.isEnabled = false
        textoEstado.text = mensaje

        MaterialAlertDialogBuilder(this)
            .setTitle("Verificación facial")
            .setMessage(mensaje)
            .setCancelable(false)
            .setPositiveButton("Volver al acceso") { _, _ ->
                setResult(RESULT_CANCELED)
                finish()
            }
            .show()
    }

    private fun pantallaDisponible(): Boolean {
        return !destruida && !isFinishing && !isDestroyed
    }

    override fun onDestroy() {
        destruida = true

        manejador.removeCallbacksAndMessages(null)
        proveedorCamara?.unbindAll()

        imagenes.clear()
        desafio = ""

        ejecutor.shutdown()

        super.onDestroy()
    }
}