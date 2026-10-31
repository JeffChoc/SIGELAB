package gt.edu.umg.sigelab.mobile

import android.os.Bundle
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class LoginActivity : AppCompatActivity() {

    private lateinit var usuario: TextInputEditText
    private lateinit var contrasena: TextInputEditText

    private lateinit var contenedorUsuario: TextInputLayout
    private lateinit var contenedorContrasena: TextInputLayout

    private lateinit var estado: TextView
    private lateinit var continuar: MaterialButton

    private var enviando = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )

        setContentView(R.layout.activity_login)

        ViewCompat.setOnApplyWindowInsetsListener(
            findViewById(R.id.loginPrincipal)
        ) { vista, insets ->
            val barras = insets.getInsets(
                WindowInsetsCompat.Type.systemBars()
            )

            val teclado = insets.getInsets(
                WindowInsetsCompat.Type.ime()
            )

            vista.setPadding(
                barras.left,
                barras.top,
                barras.right,
                maxOf(barras.bottom, teclado.bottom)
            )

            insets
        }

        usuario = findViewById(R.id.txtUsuario)
        contrasena = findViewById(R.id.txtContrasena)

        contenedorUsuario = findViewById(
            R.id.contenedorUsuario
        )

        contenedorContrasena = findViewById(
            R.id.contenedorContrasena
        )

        estado = findViewById(R.id.txtEstadoLogin)
        continuar = findViewById(R.id.btnContinuarLogin)

        contrasena.isSaveEnabled = false

        continuar.setOnClickListener {
            comprobarCredenciales()
        }

        findViewById<MaterialButton>(
            R.id.btnVolverLogin
        ).setOnClickListener {
            contrasena.text?.clear()
            finish()
        }

        contrasena.setOnEditorActionListener { _, accion, _ ->
            if (accion == EditorInfo.IME_ACTION_DONE) {
                comprobarCredenciales()
                true
            } else {
                false
            }
        }
    }

    private fun comprobarCredenciales() {
        if (enviando) {
            return
        }

        contenedorUsuario.error = null
        contenedorContrasena.error = null

        val nombreUsuario = usuario.text
            ?.toString()
            .orEmpty()
            .trim()

        val clave = contrasena.text
            ?.toString()
            .orEmpty()

        if (nombreUsuario.isBlank()) {
            contenedorUsuario.error = "Ingresa tu usuario"
            usuario.requestFocus()
            return
        }

        if (clave.isEmpty()) {
            contenedorContrasena.error = "Ingresa tu contraseña"
            contrasena.requestFocus()
            return
        }

        val servidor = getSharedPreferences(
            "sigelab_configuracion",
            MODE_PRIVATE
        ).getString("servidor", "").orEmpty()

        if (servidor.isBlank()) {
            estado.text =
                "Regresa al inicio y configura la conexión."
            return
        }

        val solicitud = JSONObject().apply {
            put("usuario", nombreUsuario)
            put("contrasena", clave)
        }.toString().toByteArray(Charsets.UTF_8)

        contrasena.text?.clear()

        enviando = true
        continuar.isEnabled = false
        estado.text = "Comprobando credenciales…"

        Thread {
            var conexion: HttpURLConnection? = null

            val mensaje = try {
                val http = URL(
                    "$servidor/autenticacion/credenciales"
                ).openConnection() as HttpURLConnection

                conexion = http

                http.apply {
                    requestMethod = "POST"
                    connectTimeout = 5000
                    readTimeout = 10000
                    instanceFollowRedirects = false
                    useCaches = false
                    doOutput = true

                    setRequestProperty(
                        "Content-Type",
                        "application/json; charset=UTF-8"
                    )

                    setRequestProperty(
                        "Accept",
                        "application/json"
                    )

                    setFixedLengthStreamingMode(solicitud.size)
                }

                http.outputStream.use {
                    it.write(solicitud)
                }

                val codigo = http.responseCode

                when (codigo) {
                    200 -> {
                        val contenido = http.inputStream
                            .bufferedReader(Charsets.UTF_8)
                            .use { it.readText() }

                        val datos = JSONObject(contenido)

                        if (
                            datos.optString("estado") ==
                            "pendiente_facial" &&
                            datos.has("sesion_abierta") &&
                            !datos.getBoolean("sesion_abierta")
                        ) {
                            "Usuario y contraseña correctos.\n\n" +
                                    "Falta completar la verificación facial " +
                                    "para iniciar sesión."
                        } else {
                            "El servidor devolvió una respuesta inesperada."
                        }
                    }

                    401 -> {
                        val contenido = http.errorStream
                            ?.bufferedReader(Charsets.UTF_8)
                            ?.use { it.readText() }
                            .orEmpty()

                        val detalle = runCatching {
                            JSONObject(contenido).optString("detail")
                        }.getOrDefault("")

                        detalle.ifBlank {
                            "Usuario o contraseña incorrectos."
                        }
                    }

                    400 -> {
                        "Revisa los datos ingresados."
                    }

                    503 -> {
                        "No se pudo validar el acceso. " +
                                "Inténtalo más tarde."
                    }

                    404 -> {
                        "Actualiza el servidor de SIGELAB " +
                                "y vuelve a intentarlo."
                    }

                    else -> {
                        "No se pudo comprobar el acceso (HTTP $codigo)."
                    }
                }
            } catch (_: java.net.SocketTimeoutException) {
                "El servidor tardó demasiado. Vuelve a intentarlo."
            } catch (_: Exception) {
                "No se pudo completar la comprobación. " +
                        "Revisa la conexión con el servidor."
            } finally {
                solicitud.fill(0)
                conexion?.disconnect()
            }

            runOnUiThread {
                if (!isFinishing && !isDestroyed) {
                    enviando = false
                    continuar.isEnabled = true
                    estado.text = mensaje
                }
            }
        }.start()
    }
}