package gt.edu.umg.sigelab.mobile

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.widget.EditText
import android.widget.FrameLayout
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL

class LoginActivity : AppCompatActivity() {

    private lateinit var usuario: TextInputEditText
    private lateinit var contrasena: TextInputEditText
    private lateinit var contenedorUsuario: TextInputLayout
    private lateinit var contenedorContrasena: TextInputLayout
    private lateinit var estado: TextView
    private lateinit var continuar: MaterialButton

    private var enviando = false

    private val abrirCamara = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { resultado ->
        if (resultado.resultCode == RESULT_OK) {
            val token = resultado.data
                ?.getStringExtra("token")
                .orEmpty()

            val servidor = resultado.data
                ?.getStringExtra("servidor")
                .orEmpty()

            if (token.isNotBlank() && servidor.isNotBlank()) {
                consultarSesion(servidor, token)
            } else {
                mostrarMensaje(
                    "No se recibió una sesión válida. " +
                            "Vuelve a ingresar tus credenciales."
                )
            }
        } else {
            mostrarMensaje(
                "No se completó la verificación facial. " +
                        "Ingresa nuevamente tu contraseña."
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Si ya hay una sesión válida en memoria, ir directamente al menú.
        val sesion = SesionMovil.actual
        if (sesion != null && sesion.token.isNotBlank() &&
            sesion.servidor.trimEnd('/') == servidorConfigurado()
        ) {
            abrirMenuPrincipal()
            return
        }

        enableEdgeToEdge()

        window.addFlags(
            WindowManager.LayoutParams.FLAG_SECURE
        )

        setContentView(R.layout.activity_login)

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

        configurarMargenes()

        contrasena.isSaveEnabled = false

        continuar.setOnClickListener {
            comprobarCredenciales()
        }

        // El acceso es la primera pantalla; aquí también se puede configurar la API.
        findViewById<MaterialButton>(R.id.btnVolverLogin).apply {
            text = "Configurar conexión"
            setOnClickListener {
                if (!enviando) mostrarConfiguracionServidor()
            }
        }

        if (servidorConfigurado().isBlank()) {
            estado.text = "Configura la conexión con la laptop antes de iniciar sesión."
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

    private fun configurarMargenes() {
        val principal = findViewById<View>(
            R.id.loginPrincipal
        )

        val izquierda = principal.paddingLeft
        val arriba = principal.paddingTop
        val derecha = principal.paddingRight
        val abajo = principal.paddingBottom

        ViewCompat.setOnApplyWindowInsetsListener(
            principal
        ) { vista, insets ->
            val barras = insets.getInsets(
                WindowInsetsCompat.Type.systemBars()
            )

            val teclado = insets.getInsets(
                WindowInsetsCompat.Type.ime()
            )

            vista.setPadding(
                izquierda + barras.left,
                arriba + barras.top,
                derecha + barras.right,
                abajo + maxOf(barras.bottom, teclado.bottom)
            )

            insets
        }
    }

    private fun comprobarCredenciales() {
        if (enviando) return

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

        val servidor = servidorConfigurado()

        if (servidor.isBlank()) {
            estado.text =
                "Configura la conexión con la laptop antes de iniciar sesión."
            return
        }

        val solicitud = JSONObject().apply {
            put("usuario", nombreUsuario)
            put("contrasena", clave)
        }.toString().toByteArray(Charsets.UTF_8)

        contrasena.text?.clear()
        SesionMovil.limpiar()

        cambiarEstado(
            true,
            "Comprobando usuario y contraseña…"
        )

        Thread {
            try {
                val datos = solicitar(
                    servidor = servidor,
                    ruta = "/autenticacion/credenciales",
                    cuerpo = solicitud
                )

                val desafio = datos.optString("desafio")

                if (
                    datos.optString("estado") != "pendiente_facial" ||
                    !datos.has("sesion_abierta") ||
                    datos.getBoolean("sesion_abierta") ||
                    desafio.isBlank()
                ) {
                    throw ErrorAcceso(
                        "El servidor no entregó una solicitud facial " +
                                "válida. Comprueba que la API esté actualizada."
                    )
                }

                runOnUiThread {
                    if (pantallaDisponible()) {
                        try {
                            estado.text =
                                "Credenciales correctas. Abriendo cámara…"

                            val pantalla = Intent(
                                this,
                                VerificacionFacialActivity::class.java
                            ).apply {
                                putExtra("servidor", servidor)
                                putExtra("desafio", desafio)
                            }

                            abrirCamara.launch(pantalla)

                        } catch (_: Exception) {
                            mostrarMensaje(
                                "No se pudo abrir la pantalla facial. " +
                                        "Comprueba que VerificacionFacialActivity " +
                                        "esté registrada en AndroidManifest.xml."
                            )
                        }
                    }
                }

            } catch (error: Exception) {
                mostrarMensaje(describirError(error))
            } finally {
                solicitud.fill(0)
            }
        }.start()
    }

    private fun consultarSesion(
        servidor: String,
        token: String
    ) {
        cambiarEstado(
            true,
            "Rostro verificado. Consultando tu sesión…"
        )

        Thread {
            try {
                val respuesta = solicitar(
                    servidor = servidor,
                    ruta = "/autenticacion/sesion",
                    token = token
                )

                if (
                    respuesta.optString("estado") != "autenticado"
                ) {
                    throw ErrorAcceso(
                        "El servidor no confirmó la sesión."
                    )
                }

                val datosUsuario = respuesta.getJSONObject(
                    "usuario"
                )

                val id = datosUsuario.getInt("id_usuario")
                val rol = datosUsuario.getString("rol")

                val nombre = listOf(
                    datosUsuario.optString("nombre"),
                    datosUsuario.optString("apellido")
                ).filter { it.isNotBlank() }
                    .joinToString(" ")
                    .ifBlank {
                        datosUsuario.optString("nombre_usuario")
                    }

                if (id <= 0 || rol.isBlank()) {
                    throw ErrorAcceso(
                        "La información de la sesión está incompleta."
                    )
                }

                runOnUiThread {
                    if (pantallaDisponible()) {
                        if (servidorConfigurado() != servidor) {
                            mostrarMensaje(
                                "Cambió el servidor configurado. " +
                                        "Inicia sesión nuevamente."
                            )
                            return@runOnUiThread
                        }

                        SesionMovil.iniciar(
                            SesionMovil.Datos(
                                servidor = servidor,
                                token = token,
                                idUsuario = id,
                                nombre = nombre,
                                rol = rol
                            )
                        )

                        Toast.makeText(
                            this,
                            "Bienvenido, $nombre",
                            Toast.LENGTH_LONG
                        ).show()

                        abrirMenuPrincipal()
                    }
                }

            } catch (error: Exception) {
                mostrarMensaje(describirError(error))
            }
        }.start()
    }

    private fun abrirMenuPrincipal() {
        startActivity(
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        )
        finish()
    }

    private fun mostrarConfiguracionServidor() {
        val campo = EditText(this).apply {
            hint = "http://192.168.1.10:8000"
            inputType = InputType.TYPE_CLASS_TEXT or
                    InputType.TYPE_TEXT_VARIATION_URI
            setSingleLine(true)
            setText(servidorConfigurado())
        }

        val margen = dp(24)
        val contenedor = FrameLayout(this).apply {
            setPadding(margen, 0, margen, 0)
            addView(campo)
        }

        val dialogo = MaterialAlertDialogBuilder(this)
            .setTitle("Configurar conexión")
            .setMessage(
                "Escribe la dirección del servidor de SIGELAB. " +
                        "El teléfono y la laptop deben estar en la misma red Wi-Fi."
            )
            .setView(contenedor)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Guardar", null)
            .create()

        dialogo.setOnShowListener {
            dialogo.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener {
                    val direccion = campo.text.toString().trim().trimEnd('/')
                    if (!direccionValida(direccion)) {
                        campo.error = "Ejemplo: http://192.168.1.10:8000"
                        return@setOnClickListener
                    }

                    getSharedPreferences("sigelab_configuracion", MODE_PRIVATE)
                        .edit()
                        .putString("servidor", direccion)
                        .apply()

                    estado.text = "Servidor configurado. Ingresa tus credenciales."
                    Toast.makeText(this, "Dirección guardada", Toast.LENGTH_SHORT)
                        .show()
                    dialogo.dismiss()
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

    private fun dp(valor: Int): Int {
        return (valor * resources.displayMetrics.density).toInt()
    }

    private fun servidorConfigurado(): String {
        return getSharedPreferences(
            "sigelab_configuracion",
            MODE_PRIVATE
        ).getString("servidor", "")
            .orEmpty()
            .trim()
            .trimEnd('/')
    }

    private fun solicitar(
        servidor: String,
        ruta: String,
        cuerpo: ByteArray? = null,
        token: String? = null
    ): JSONObject {
        val conexion = URL(
            "$servidor$ruta"
        ).openConnection() as HttpURLConnection

        try {
            conexion.requestMethod =
                if (cuerpo == null) "GET" else "POST"

            conexion.connectTimeout = 5000
            conexion.readTimeout = 15000
            conexion.instanceFollowRedirects = false
            conexion.useCaches = false

            conexion.setRequestProperty(
                "Accept",
                "application/json"
            )

            if (token != null) {
                conexion.setRequestProperty(
                    "Authorization",
                    "Bearer $token"
                )
            }

            if (cuerpo != null) {
                conexion.doOutput = true

                conexion.setRequestProperty(
                    "Content-Type",
                    "application/json; charset=UTF-8"
                )

                conexion.setFixedLengthStreamingMode(
                    cuerpo.size
                )

                conexion.outputStream.use {
                    it.write(cuerpo)
                }
            }

            val codigo = conexion.responseCode

            val flujo = if (codigo in 200..299) {
                conexion.inputStream
            } else {
                conexion.errorStream
            }

            val contenido = flujo
                ?.bufferedReader(Charsets.UTF_8)
                ?.use { it.readText() }
                .orEmpty()

            val datos = runCatching {
                JSONObject(contenido)
            }.getOrElse {
                JSONObject()
            }

            if (codigo != 200) {
                val mensaje = when (codigo) {
                    401, 403 -> {
                        val detalle = datos.opt("detail")

                        if (
                            detalle is String &&
                            detalle.isNotBlank()
                        ) {
                            detalle
                        } else {
                            "Acceso denegado. Revisa tus credenciales."
                        }
                    }

                    400, 422 ->
                        "Revisa los datos ingresados."

                    404 ->
                        "Esta función no está disponible en el servidor. " +
                                "Comprueba que la API esté actualizada."

                    429 ->
                        "Se hicieron demasiados intentos. " +
                                "Espera un momento antes de volver a intentar."

                    503 ->
                        "El servicio no está disponible. " +
                                "Revisa la API y la conexión con la base de datos."

                    else ->
                        "El servidor no pudo completar la solicitud " +
                                "(HTTP $codigo)."
                }

                throw ErrorAcceso(mensaje)
            }

            if (datos.length() == 0) {
                throw ErrorAcceso(
                    "El servidor devolvió una respuesta no válida."
                )
            }

            return datos

        } finally {
            conexion.disconnect()
        }
    }

    private fun describirError(error: Exception): String {
        return when (error) {
            is ErrorAcceso ->
                error.message ?: "No se pudo iniciar sesión."

            is java.net.SocketTimeoutException ->
                "El servidor tardó demasiado. Vuelve a intentarlo."

            else ->
                "No se pudo completar el acceso. " +
                        "Comprueba la conexión y que la API esté funcionando."
        }
    }

    private fun mostrarMensaje(mensaje: String) {
        runOnUiThread {
            if (pantallaDisponible()) {
                cambiarEstado(false, mensaje)
            }
        }
    }

    private fun cambiarEstado(
        ocupado: Boolean,
        mensaje: String
    ) {
        enviando = ocupado
        continuar.isEnabled = !ocupado
        usuario.isEnabled = !ocupado
        contrasena.isEnabled = !ocupado
        estado.text = mensaje
    }

    private fun pantallaDisponible(): Boolean {
        return !isFinishing && !isDestroyed
    }

    private class ErrorAcceso(
        mensaje: String
    ) : Exception(mensaje)
}