package gt.edu.umg.sigelab.mobile

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.RadioGroup
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.util.Locale

class DevolucionActivity : AppCompatActivity() {

    private data class RespuestaHttp(val codigo: Int, val texto: String)

    private lateinit var estado: TextView
    private lateinit var seleccionActual: TextView
    private lateinit var resumen: TextView
    private lateinit var contenedorEquipos: LinearLayout
    private lateinit var condiciones: RadioGroup
    private lateinit var observaciones: TextInputEditText
    private lateinit var botonElegir: MaterialButton
    private lateinit var botonConfirmar: MaterialButton

    private var cargando = false
    private var enviando = false
    private var idPrestamo: Int? = null
    private val equiposPendientes = mutableListOf<JSONObject>()
    private val detallesSeleccionados = mutableSetOf<Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_devolucion)

        ViewCompat.setOnApplyWindowInsetsListener(
            findViewById(R.id.devolucionPrincipal)
        ) { vista, insets ->
            val barras = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val teclado = insets.getInsets(WindowInsetsCompat.Type.ime())
            vista.setPadding(
                barras.left, barras.top, barras.right,
                maxOf(barras.bottom, teclado.bottom)
            )
            insets
        }

        estado = findViewById(R.id.txtEstadoDevolucion)
        seleccionActual = findViewById(R.id.txtPrestamoSeleccionado)
        resumen = findViewById(R.id.txtResumenDevolucion)
        contenedorEquipos = findViewById(R.id.listaEquiposDevolucion)
        condiciones = findViewById(R.id.grupoCondicion)
        observaciones = findViewById(R.id.txtObservacionesDevolucion)
        botonElegir = findViewById(R.id.btnElegirPrestamo)
        botonConfirmar = findViewById(R.id.btnConfirmarDevolucion)

        botonElegir.setOnClickListener { buscarPrestamosActivos() }
        botonConfirmar.setOnClickListener { confirmarDevolucion() }
        condiciones.setOnCheckedChangeListener { _, _ -> actualizarBotones() }
        findViewById<MaterialButton>(R.id.btnVolverDevolucion)
            .setOnClickListener { finish() }

        // El XML deja el boton desactivado; se habilita al seleccionar
        // prestamo, equipos y condicion con una sesion valida.
        actualizarBotones()
    }

    override fun onResume() {
        super.onResume()
        if (::estado.isInitialized && !cargando && !enviando) {
            actualizarBotones()
        }
    }

    private fun servidorConfigurado(): String =
        getSharedPreferences("sigelab_configuracion", MODE_PRIVATE)
            .getString("servidor", "")
            .orEmpty()
            .trim()
            .trimEnd('/')

    private fun sesionValida(): SesionMovil.Datos? {
        val sesion = SesionMovil.actual ?: return null
        if (sesion.token.isBlank()) return null
        if (sesion.rol.uppercase(Locale.ROOT) !in setOf("ADMIN", "ENCARGADO")) return null
        val servidor = servidorConfigurado()
        if (servidor.isBlank() || sesion.servidor.trim().trimEnd('/') != servidor) return null
        return sesion
    }

    private fun mensajeSesion(): String {
        val sesion = SesionMovil.actual
            ?: return "No hay sesion activa en memoria. Inicia sesion y completa Face ID nuevamente."
        if (sesion.token.isBlank()) return "La sesion no tiene token. Inicia sesion nuevamente."
        if (sesion.rol.uppercase(Locale.ROOT) !in setOf("ADMIN", "ENCARGADO")) {
            return "Tu usuario tiene rol ${sesion.rol} y no puede registrar devoluciones."
        }
        val servidor = servidorConfigurado()
        if (servidor.isBlank()) return "Configura la direccion del servidor en el inicio."
        if (sesion.servidor.trim().trimEnd('/') != servidor) {
            return "El servidor configurado cambio desde el inicio de sesion. Vuelve a iniciar sesion."
        }
        return "Sesion activa: ${sesion.nombre}."
    }

    private fun solicitarInicioSesion() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Sesion necesaria")
            .setMessage(mensajeSesion())
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Iniciar sesion") { _, _ ->
                startActivity(Intent(this, LoginActivity::class.java))
            }
            .show()
    }

    private fun actualizarBotones() {
        val sesion = sesionValida()
        // El boton de consulta queda habilitado para explicar el problema y
        // ofrecer acceso al inicio de sesion, incluso si falta la sesion.
        botonElegir.isEnabled = !cargando && !enviando
        botonConfirmar.isEnabled = !cargando && !enviando &&
                sesion != null && idPrestamo != null &&
                detallesSeleccionados.isNotEmpty() && condiciones.checkedRadioButtonId != -1

        if (sesion == null) {
            estado.text = mensajeSesion()
        } else if (idPrestamo == null && !cargando && !enviando) {
            estado.text = "Sesion activa: ${sesion.nombre}. Elige un prestamo activo."
        } else if (idPrestamo != null && !cargando && !enviando) {
            estado.text = if (botonConfirmar.isEnabled) {
                "Listo para devolver ${detallesSeleccionados.size} equipo(s)."
            } else {
                "Selecciona equipos y la condicion en que regresan."
            }
        }
    }

    private fun consultar(
        servidor: String,
        ruta: String,
        token: String,
        metodo: String = "GET",
        cuerpo: JSONObject? = null
    ): RespuestaHttp {
        val conexion = URL("$servidor$ruta").openConnection() as HttpURLConnection
        try {
            conexion.requestMethod = metodo
            conexion.connectTimeout = 5000
            conexion.readTimeout = 15000
            conexion.instanceFollowRedirects = false
            conexion.useCaches = false
            conexion.setRequestProperty("Accept", "application/json")
            conexion.setRequestProperty("Authorization", "Bearer $token")

            if (cuerpo != null) {
                conexion.doOutput = true
                conexion.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                conexion.outputStream.use { salida ->
                    salida.write(cuerpo.toString().toByteArray(Charsets.UTF_8))
                }
            }

            val codigo = conexion.responseCode
            val flujo = if (codigo in 200..299) conexion.inputStream else conexion.errorStream
            val respuesta = flujo?.bufferedReader(Charsets.UTF_8)
                ?.use { it.readText() }.orEmpty()
            return RespuestaHttp(codigo, respuesta)
        } finally {
            conexion.disconnect()
        }
    }

    private fun mensajeError(respuesta: RespuestaHttp): String = try {
        val valor = JSONObject(respuesta.texto).opt("detail")
        if (valor is String && valor.isNotBlank()) valor
        else "No se pudo realizar la operacion (HTTP ${respuesta.codigo})."
    } catch (_: Exception) {
        "No se pudo realizar la operacion (HTTP ${respuesta.codigo})."
    }

    private fun buscarPrestamosActivos() {
        val sesion = sesionValida()
        if (sesion == null) {
            solicitarInicioSesion()
            return
        }
        if (cargando || enviando) return
        cargando = true
        botonElegir.isEnabled = false
        botonConfirmar.isEnabled = false
        estado.text = "Buscando prestamos activos..."

        Thread {
            try {
                val respuesta = consultar(
                    sesion.servidor.trimEnd('/'), "/prestamos/activos", sesion.token
                )
                if (respuesta.codigo == 401) {
                    runOnUiThread {
                        SesionMovil.limpiar()
                        cargando = false
                        actualizarBotones()
                    }
                    return@Thread
                }
                if (respuesta.codigo != 200) throw IOException(mensajeError(respuesta))
                val array = JSONObject(respuesta.texto).getJSONArray("prestamos")
                val prestamos = (0 until array.length()).map { array.getJSONObject(it) }
                runOnUiThread {
                    if (isFinishing || isDestroyed) return@runOnUiThread
                    cargando = false
                    actualizarBotones()
                    if (prestamos.isEmpty()) {
                        estado.text = "No hay prestamos activos con equipos pendientes."
                        return@runOnUiThread
                    }
                    val etiquetas = prestamos.map { p ->
                        "#${p.getInt("id_prestamo")} · ${p.optString("solicitante")}" +
                                " (${p.optInt("pendientes")} pendiente(s))"
                    }.toTypedArray()
                    MaterialAlertDialogBuilder(this)
                        .setTitle("Elegir prestamo activo")
                        .setItems(etiquetas) { _, indice ->
                            val elegido = prestamos[indice]
                            cargarPendientes(elegido.getInt("id_prestamo"))
                        }
                        .setNegativeButton("Cancelar", null)
                        .show()
                }
            } catch (error: Exception) {
                runOnUiThread {
                    if (!isFinishing && !isDestroyed) {
                        cargando = false
                        actualizarBotones()
                        estado.text = error.message ?: "No se pudieron consultar los prestamos."
                    }
                }
            }
        }.start()
    }

    private fun cargarPendientes(prestamo: Int) {
        val sesion = sesionValida() ?: return
        if (cargando || enviando) return
        cargando = true
        estado.text = "Cargando equipos pendientes..."
        botonElegir.isEnabled = false
        botonConfirmar.isEnabled = false

        Thread {
            try {
                val respuesta = consultar(
                    sesion.servidor.trimEnd('/'),
                    "/prestamos/$prestamo/pendientes", sesion.token
                )
                if (respuesta.codigo != 200) {
                    if (respuesta.codigo == 401) SesionMovil.limpiar()
                    throw IOException(mensajeError(respuesta))
                }
                val datos = JSONObject(respuesta.texto)
                val array = datos.getJSONArray("equipos")
                val lista = (0 until array.length()).map { array.getJSONObject(it) }
                val nombre = datos.optString("solicitante")
                runOnUiThread {
                    if (isFinishing || isDestroyed) return@runOnUiThread
                    idPrestamo = prestamo
                    equiposPendientes.clear()
                    equiposPendientes.addAll(lista)
                    detallesSeleccionados.clear()
                    seleccionActual.text = "Prestamo #$prestamo · $nombre"
                    resumen.text = "${lista.size} equipo(s) pendiente(s). Selecciona los que regresan."
                    mostrarEquipos()
                    cargando = false
                    actualizarBotones()
                }
            } catch (error: Exception) {
                runOnUiThread {
                    if (!isFinishing && !isDestroyed) {
                        cargando = false
                        actualizarBotones()
                        estado.text = error.message ?: "No se pudieron cargar los equipos."
                    }
                }
            }
        }.start()
    }

    private fun mostrarEquipos() {
        contenedorEquipos.removeAllViews()
        equiposPendientes.forEach { equipo ->
            val idDetalle = equipo.getInt("id_detalle")
            val casilla = MaterialCheckBox(this).apply {
                text = "${equipo.optString("codigo")} · ${equipo.optString("nombre")}"
                isChecked = idDetalle in detallesSeleccionados
                isEnabled = !enviando
                setOnCheckedChangeListener { _, marcado ->
                    if (marcado) detallesSeleccionados.add(idDetalle)
                    else detallesSeleccionados.remove(idDetalle)
                    actualizarBotones()
                }
            }
            contenedorEquipos.addView(casilla)
        }
    }

    private fun confirmarDevolucion() {
        if (cargando || enviando) return
        val sesion = sesionValida() ?: run {
            solicitarInicioSesion()
            return
        }
        val prestamo = idPrestamo ?: return
        if (detallesSeleccionados.isEmpty()) {
            estado.text = "Selecciona al menos un equipo."
            return
        }
        val condicion = when (condiciones.checkedRadioButtonId) {
            R.id.radioBueno -> "BUENO"
            R.id.radioDanado -> "DANADO"
            else -> {
                estado.text = "Selecciona BUENO o DANADO."
                return
            }
        }
        val notas = observaciones.text?.toString()?.trim().orEmpty()
        if (notas.length > 300) {
            observaciones.error = "Maximo 300 caracteres."
            return
        }
        val cuerpo = JSONObject().apply {
            put("id_prestamo", prestamo)
            put("condicion", condicion)
            put("detalles", JSONArray().apply {
                detallesSeleccionados.sorted().forEach { put(it) }
            })
            if (notas.isNotBlank()) put("observaciones", notas)
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Confirmar devolucion")
            .setMessage(
                "Prestamo #$prestamo\n" +
                        "Equipos: ${detallesSeleccionados.size}\n" +
                        "Condicion: $condicion\n\n" +
                        "¿Deseas registrar la devolucion en SIGELAB?"
            )
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Registrar") { _, _ ->
                enviarDevolucion(sesion, cuerpo)
            }
            .show()
    }

    private fun enviarDevolucion(sesion: SesionMovil.Datos, cuerpo: JSONObject) {
        if (enviando) return
        enviando = true
        botonElegir.isEnabled = false
        botonConfirmar.isEnabled = false
        mostrarEquipos()
        estado.text = "Registrando devolucion en SQL Server..."

        Thread {
            var error: String? = null
            var exito: JSONObject? = null
            var sesionVencida = false
            try {
                val respuesta = consultar(
                    sesion.servidor.trimEnd('/'), "/devoluciones",
                    sesion.token, "POST", cuerpo
                )
                if (respuesta.codigo == 201) {
                    exito = JSONObject(respuesta.texto)
                } else {
                    sesionVencida = respuesta.codigo == 401
                    error = mensajeError(respuesta)
                }
            } catch (_: SocketTimeoutException) {
                error = "No hubo respuesta a tiempo. Comprueba en el escritorio si ya se registro antes de intentar otra vez."
            } catch (_: IOException) {
                error = "Fallo la conexion. Comprueba el prestamo en SIGELAB de escritorio antes de reintentar."
            } catch (_: Exception) {
                error = "Respuesta inesperada. Comprueba si ya se registro la devolucion antes de reintentar."
            }

            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                enviando = false
                if (exito != null) {
                    val restantes = exito!!.optInt("pendientes_restantes")
                    val devueltos = exito!!.optInt("equipos_devueltos")
                    idPrestamo = null
                    equiposPendientes.clear()
                    detallesSeleccionados.clear()
                    contenedorEquipos.removeAllViews()
                    seleccionActual.text = "Todavia no has seleccionado un prestamo."
                    resumen.text = "Elige otro prestamo para consultar equipos pendientes."
                    condiciones.clearCheck()
                    observaciones.text?.clear()
                    actualizarBotones()
                    estado.text = "Devolucion registrada correctamente."
                    MaterialAlertDialogBuilder(this)
                        .setTitle("Devolucion registrada")
                        .setMessage(
                            "$devueltos equipo(s) devuelto(s).\n" +
                                    "Pendientes restantes: $restantes.\n" +
                                    "Actualiza SIGELAB de escritorio para comprobarlo."
                        )
                        .setPositiveButton("Aceptar", null)
                        .show()
                } else {
                    if (sesionVencida) SesionMovil.limpiar()
                    actualizarBotones()
                    estado.text = error ?: "No se pudo registrar la devolucion."
                }
            }
        }.start()
    }
}