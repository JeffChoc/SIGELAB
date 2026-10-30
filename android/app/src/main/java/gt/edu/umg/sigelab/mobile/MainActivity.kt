package gt.edu.umg.sigelab.mobile

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL

class MainActivity : AppCompatActivity() {

    private val preferencias by lazy {
        getSharedPreferences("sigelab_configuracion", MODE_PRIVATE)
    }

    private var numeroComprobacion = 0

    private val lectorQr = registerForActivityResult(
        ScanContract()
    ) { resultado ->
        val codigo = resultado.contents?.trim().orEmpty()

        when {
            codigo.isBlank() -> {
                Toast.makeText(
                    this,
                    "Escaneo cancelado",
                    Toast.LENGTH_SHORT
                ).show()
            }

            codigo.length > 100 -> {
                MaterialAlertDialogBuilder(this)
                    .setTitle("Código no compatible")
                    .setMessage(
                        "Escanea un QR de inventario de SIGELAB."
                    )
                    .setPositiveButton("Aceptar", null)
                    .show()
            }

            else -> {
                val pantalla = Intent(
                    this,
                    EquiposActivity::class.java
                ).apply {
                    putExtra("codigo_equipo", codigo)
                }

                startActivity(pantalla)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(
            findViewById(R.id.main)
        ) { vista, insets ->
            val barras = insets.getInsets(
                WindowInsetsCompat.Type.systemBars()
            )

            vista.setPadding(
                barras.left,
                barras.top,
                barras.right,
                barras.bottom
            )

            insets
        }

        findViewById<MaterialButton>(
            R.id.btnEquipos
        ).setOnClickListener {
            startActivity(
                Intent(this, EquiposActivity::class.java)
            )
        }

        findViewById<MaterialButton>(
            R.id.btnEscanearQr
        ).setOnClickListener {
            abrirLectorQr()
        }

        findViewById<MaterialButton>(
            R.id.btnConfigurarServidor
        ).setOnClickListener {
            mostrarConfiguracion()
        }

        findViewById<TextView>(
            R.id.txtEstadoConexion
        ).setOnClickListener {
            comprobarConexion()
        }

        comprobarConexion()
    }

    private fun abrirLectorQr() {
        val servidor = preferencias.getString(
            "servidor",
            ""
        ).orEmpty()

        if (servidor.isBlank()) {
            Toast.makeText(
                this,
                "Primero configura la conexión",
                Toast.LENGTH_LONG
            ).show()

            mostrarConfiguracion()
            return
        }

        val opciones = ScanOptions().apply {
            setDesiredBarcodeFormats(ScanOptions.QR_CODE)
            setPrompt("Escanea el código QR del equipo")
            setCameraId(0)
            setBeepEnabled(false)
            setBarcodeImageEnabled(false)
            setOrientationLocked(false)
        }

        lectorQr.launch(opciones)
    }

    private fun mostrarConfiguracion() {
        val campo = EditText(this).apply {
            hint = "http://192.168.1.10:8000"

            inputType = InputType.TYPE_CLASS_TEXT or
                    InputType.TYPE_TEXT_VARIATION_URI

            setSingleLine(true)
            setText(preferencias.getString("servidor", ""))
        }

        val margen = (
                24 * resources.displayMetrics.density
                ).toInt()

        val contenedor = FrameLayout(this).apply {
            setPadding(margen, 0, margen, 0)
            addView(campo)
        }

        val dialogo = MaterialAlertDialogBuilder(this)
            .setTitle("Configurar conexión")
            .setMessage(
                "Escribe la dirección del servidor de SIGELAB. " +
                        "El teléfono y la laptop deben estar " +
                        "en la misma red Wi-Fi."
            )
            .setView(contenedor)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Guardar", null)
            .create()

        dialogo.setOnShowListener {
            dialogo.getButton(
                AlertDialog.BUTTON_POSITIVE
            ).setOnClickListener {
                val direccion = campo.text.toString()
                    .trim()
                    .trimEnd('/')

                if (!direccionValida(direccion)) {
                    campo.error =
                        "Ejemplo: http://192.168.1.10:8000"

                    return@setOnClickListener
                }

                preferencias.edit()
                    .putString("servidor", direccion)
                    .apply()

                Toast.makeText(
                    this,
                    "Dirección guardada",
                    Toast.LENGTH_SHORT
                ).show()

                dialogo.dismiss()
                comprobarConexion()
            }
        }

        dialogo.show()
    }

    private fun direccionValida(direccion: String): Boolean {
        return try {
            val uri = URI(direccion)

            (uri.scheme == "http" || uri.scheme == "https") &&
                    !uri.host.isNullOrBlank() &&
                    uri.userInfo == null &&
                    uri.query == null &&
                    uri.fragment == null &&
                    (uri.path.isNullOrEmpty() || uri.path == "/") &&
                    (uri.port == -1 || uri.port in 1..65535)
        } catch (_: Exception) {
            false
        }
    }

    private fun comprobarConexion() {
        val servidor = preferencias.getString(
            "servidor",
            ""
        ).orEmpty()

        numeroComprobacion += 1
        val solicitudActual = numeroComprobacion
        val estado = findViewById<TextView>(R.id.txtEstadoConexion)

        if (servidor.isBlank()) {
            estado.text = "Sin servidor configurado"
            return
        }

        estado.text = "Comprobando conexión…"

        Thread {
            var conexion: HttpURLConnection? = null

            val mensaje = try {
                val http = URL("$servidor/salud")
                    .openConnection() as HttpURLConnection

                conexion = http

                http.apply {
                    requestMethod = "GET"
                    connectTimeout = 5000
                    readTimeout = 5000
                    instanceFollowRedirects = false
                    useCaches = false
                    setRequestProperty("Accept", "application/json")
                }

                val codigo = http.responseCode

                when (codigo) {
                    200 -> {
                        val respuesta = http.inputStream
                            .bufferedReader(Charsets.UTF_8)
                            .use { it.readText() }

                        val datos = JSONObject(respuesta)

                        if (
                            datos.optString("estado") == "ok" &&
                            datos.optString("base_datos") == "conectada"
                        ) {
                            "Conectado a SIGELAB\n" +
                                    "Base de datos disponible"
                        } else {
                            "El servidor respondió con un estado inesperado"
                        }
                    }

                    503 -> {
                        "Servidor disponible\n" +
                                "Base de datos no disponible"
                    }

                    else -> {
                        "No se pudo comprobar el servidor (HTTP $codigo)"
                    }
                }
            } catch (_: java.net.SocketTimeoutException) {
                "El servidor tardó demasiado en responder"
            } catch (_: org.json.JSONException) {
                "La dirección respondió, pero no es la API de SIGELAB"
            } catch (_: Exception) {
                "No se pudo conectar\n" +
                        "Revisa el servidor, la dirección y el Wi-Fi"
            } finally {
                conexion?.disconnect()
            }

            runOnUiThread {
                if (
                    !isFinishing &&
                    !isDestroyed &&
                    solicitudActual == numeroComprobacion
                ) {
                    estado.text =
                        "$mensaje\n\nToca aquí para comprobar otra vez"
                }
            }
        }.start()
    }
}