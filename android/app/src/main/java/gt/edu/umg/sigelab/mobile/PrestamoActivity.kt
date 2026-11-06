package gt.edu.umg.sigelab.mobile

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class PrestamoActivity : AppCompatActivity() {

    private var fechaElegida: Calendar? = null
    private val seleccionados = linkedMapOf<Int, JSONObject>()

    private lateinit var solicitante: TextInputEditText
    private lateinit var identificacion: TextInputEditText
    private lateinit var observaciones: TextInputEditText
    private lateinit var textoFecha: TextView
    private lateinit var resumen: TextView
    private lateinit var estado: TextView
    private lateinit var lista: LinearLayout
    private lateinit var botonEquipos: MaterialButton
    private lateinit var botonConfirmar: MaterialButton
    private lateinit var botonFecha: MaterialButton

    private var consultando = false
    private var enviando = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_prestamo)

        ViewCompat.setOnApplyWindowInsetsListener(
            findViewById(R.id.prestamoPrincipal)
        ) { vista, insets ->
            val barras = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val teclado = insets.getInsets(WindowInsetsCompat.Type.ime())

            vista.setPadding(
                barras.left,
                barras.top,
                barras.right,
                maxOf(barras.bottom, teclado.bottom)
            )
            insets
        }

        solicitante = findViewById(R.id.txtSolicitante)
        identificacion = findViewById(R.id.txtIdentificacion)
        observaciones = findViewById(R.id.txtObservaciones)
        textoFecha = findViewById(R.id.txtFechaDevolucion)
        resumen = findViewById(R.id.txtResumenEquipos)
        estado = findViewById(R.id.txtEstadoPrestamo)
        lista = findViewById(R.id.listaEquiposPrestamo)
        botonEquipos = findViewById(R.id.btnSeleccionarEquipos)
        botonConfirmar = findViewById(R.id.btnConfirmarPrestamo)
        botonFecha = findViewById(R.id.btnFechaDevolucion)

        botonFecha.setOnClickListener { elegirFecha() }
        botonEquipos.setOnClickListener { consultarDisponibles() }
        botonConfirmar.isEnabled = true
        botonConfirmar.setOnClickListener { confirmarPrestamo() }
        findViewById<MaterialButton>(R.id.btnVolverPrestamo)
            .setOnClickListener { finish() }

        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey("fecha")) {
                fechaElegida = Calendar.getInstance().apply {
                    timeInMillis = savedInstanceState.getLong("fecha")
                }
            }

            val guardados = JSONArray(
                savedInstanceState.getString("equipos") ?: "[]"
            )
            for (indice in 0 until guardados.length()) {
                val equipo = guardados.getJSONObject(indice)
                seleccionados[equipo.getInt("id")] = equipo
            }
        }

        mostrarFecha()
        mostrarSeleccionados()
        actualizarEstadoSesion()
    }

    override fun onResume() {
        super.onResume()
        if (::estado.isInitialized && !enviando && !consultando) {
            actualizarEstadoSesion()
        }
    }

    private fun servidorConfigurado(): String {
        return getSharedPreferences("sigelab_configuracion", MODE_PRIVATE)
            .getString("servidor", "")
            .orEmpty()
            .trim()
            .trimEnd('/')
    }

    private fun actualizarEstadoSesion() {
        val sesion = SesionMovil.actual
        estado.text = when {
            sesion == null ->
                "Inicia sesión y verifica tu rostro antes de confirmar el préstamo."
            sesion.rol.uppercase(Locale.ROOT) !in setOf("ADMIN", "ENCARGADO") ->
                "Tu sesión está activa, pero no tienes permiso para registrar préstamos."
            else ->
                "Sesión activa: ${sesion.nombre}. Completa el formulario y confirma."
        }
    }

    private fun elegirFecha() {
        if (enviando) return

        val inicial = fechaElegida ?: Calendar.getInstance().apply {
            add(Calendar.HOUR_OF_DAY, 1)
        }

        val selector = DatePickerDialog(
            this,
            { _, anio, mes, dia ->
                TimePickerDialog(
                    this,
                    { _, hora, minuto ->
                        val elegida = Calendar.getInstance().apply {
                            set(anio, mes, dia, hora, minuto, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        if (elegida.timeInMillis <= System.currentTimeMillis()) {
                            estado.text = "Selecciona una fecha y hora futuras."
                        } else {
                            fechaElegida = elegida
                            mostrarFecha()
                            actualizarEstadoSesion()
                        }
                    },
                    inicial.get(Calendar.HOUR_OF_DAY),
                    inicial.get(Calendar.MINUTE),
                    true
                ).show()
            },
            inicial.get(Calendar.YEAR),
            inicial.get(Calendar.MONTH),
            inicial.get(Calendar.DAY_OF_MONTH)
        )

        selector.datePicker.minDate = System.currentTimeMillis()
        selector.show()
    }

    private fun mostrarFecha() {
        textoFecha.text = fechaElegida?.let { fecha ->
            val formato = SimpleDateFormat("dd/MM/yyyy 'a las' HH:mm", Locale.getDefault())
            "Devolución: ${formato.format(fecha.time)}"
        } ?: "Sin fecha seleccionada"
    }

    private fun consultarDisponibles() {
        if (consultando || enviando) return

        val servidor = servidorConfigurado()
        if (servidor.isBlank()) {
            estado.text = "Regresa al inicio y configura la dirección del servidor."
            return
        }

        consultando = true
        botonEquipos.isEnabled = false
        estado.text = "Consultando equipos disponibles…"

        Thread {
            var conexion: HttpURLConnection? = null
            try {
                val http = URL("$servidor/equipos").openConnection() as HttpURLConnection
                conexion = http
                http.requestMethod = "GET"
                http.connectTimeout = 5000
                http.readTimeout = 10000
                http.instanceFollowRedirects = false
                http.useCaches = false
                http.setRequestProperty("Accept", "application/json")

                if (http.responseCode != 200) {
                    throw IOException("HTTP ${http.responseCode}")
                }

                val contenido = http.inputStream.bufferedReader(Charsets.UTF_8)
                    .use { it.readText() }
                val equipos = JSONObject(contenido).getJSONArray("equipos")
                val disponibles = mutableListOf<JSONObject>()

                for (indice in 0 until equipos.length()) {
                    val equipo = equipos.getJSONObject(indice)
                    if (equipo.optString("estado") == "DISPONIBLE") {
                        disponibles.add(equipo)
                    }
                }

                runOnUiThread {
                    if (!isFinishing && !isDestroyed) {
                        consultando = false
                        botonEquipos.isEnabled = !enviando
                        abrirSelector(disponibles)
                    }
                }
            } catch (_: Exception) {
                runOnUiThread {
                    if (!isFinishing && !isDestroyed) {
                        consultando = false
                        botonEquipos.isEnabled = !enviando
                        estado.text = "No se pudieron consultar los equipos. Revisa el servidor y el Wi-Fi."
                    }
                }
            } finally {
                conexion?.disconnect()
            }
        }.start()
    }

    private fun abrirSelector(disponibles: List<JSONObject>) {
        val idsDisponibles = disponibles.map { it.getInt("id") }.toSet()
        val retirados = seleccionados.keys.filter { it !in idsDisponibles }
        retirados.forEach { seleccionados.remove(it) }
        mostrarSeleccionados()

        if (disponibles.isEmpty()) {
            estado.text = "No hay equipos disponibles en este momento."
            return
        }

        estado.text = if (retirados.isNotEmpty()) {
            "Se retiraron equipos que ya no están disponibles."
        } else {
            "Selecciona los equipos que deseas agregar."
        }

        val etiquetas = disponibles.map {
            "${it.optString("codigo")} · ${it.optString("nombre")}"
        }.toTypedArray()
        val marcados = BooleanArray(disponibles.size) {
            seleccionados.containsKey(disponibles[it].getInt("id"))
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Equipos disponibles")
            .setMultiChoiceItems(etiquetas, marcados) { _, posicion, marcado ->
                marcados[posicion] = marcado
            }
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Agregar") { _, _ ->
                seleccionados.clear()
                disponibles.forEachIndexed { indice, equipo ->
                    if (marcados[indice]) {
                        seleccionados[equipo.getInt("id")] = equipo
                    }
                }
                mostrarSeleccionados()
                actualizarEstadoSesion()
            }
            .show()
    }

    private fun mostrarSeleccionados() {
        lista.removeAllViews()
        resumen.text = if (seleccionados.isEmpty()) {
            "Todavía no has seleccionado equipos."
        } else {
            "${seleccionados.size} equipo(s) seleccionado(s)"
        }

        seleccionados.values.forEach { equipo ->
            val boton = MaterialButton(this).apply {
                text = "${equipo.optString("codigo")} · ${equipo.optString("nombre")}\nQuitar del préstamo"
                isAllCaps = false
                isEnabled = !enviando
                setOnClickListener {
                    seleccionados.remove(equipo.getInt("id"))
                    mostrarSeleccionados()
                }
            }
            lista.addView(boton)
        }
    }

    private fun confirmarPrestamo() {
        if (enviando || consultando) return

        val sesion = SesionMovil.actual
        if (sesion == null) {
            MaterialAlertDialogBuilder(this)
                .setTitle("Inicia sesión")
                .setMessage("Para prestar equipos debes iniciar sesión y verificar tu rostro.")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Iniciar sesión") { _, _ ->
                    startActivity(Intent(this, LoginActivity::class.java))
                }
                .show()
            return
        }

        if (sesion.rol.uppercase(Locale.ROOT) !in setOf("ADMIN", "ENCARGADO")) {
            estado.text = "Tu usuario no tiene permiso para registrar préstamos."
            return
        }

        val servidor = servidorConfigurado()
        if (servidor.isBlank() || sesion.servidor.trimEnd('/') != servidor) {
            estado.text = "La dirección del servidor cambió. Configúrala e inicia sesión nuevamente."
            return
        }

        val nombre = solicitante.text?.toString()?.trim().orEmpty()
        val documento = identificacion.text?.toString()?.trim().orEmpty()
        val notas = observaciones.text?.toString()?.trim().orEmpty()
        val fecha = fechaElegida

        if (nombre.isBlank() || nombre.length > 120) {
            solicitante.error = "Ingresa el nombre del solicitante (máximo 120 caracteres)."
            solicitante.requestFocus()
            return
        }
        if (documento.length > 30) {
            identificacion.error = "Máximo 30 caracteres."
            return
        }
        if (notas.length > 300) {
            observaciones.error = "Máximo 300 caracteres."
            return
        }
        if (fecha == null || fecha.timeInMillis <= System.currentTimeMillis()) {
            estado.text = "Selecciona una fecha y hora de devolución futuras."
            return
        }
        if (seleccionados.isEmpty()) {
            estado.text = "Selecciona por lo menos un equipo disponible."
            return
        }

        val fechaApi = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
            .format(fecha.time)
        val cuerpo = JSONObject().apply {
            put("solicitante", nombre)
            if (documento.isNotEmpty()) put("identificacion", documento)
            put("fecha_esperada", fechaApi)
            if (notas.isNotEmpty()) put("observaciones", notas)
            put("equipos", JSONArray().apply {
                seleccionados.keys.forEach { put(it) }
            })
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Confirmar préstamo")
            .setMessage(
                "Solicitante: $nombre\n" +
                        "Equipos: ${seleccionados.size}\n" +
                        "Devolución: ${textoFecha.text}\n\n" +
                        "¿Deseas registrar el préstamo en SIGELAB?"
            )
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Registrar") { _, _ ->
                enviarPrestamo(servidor, sesion.token, cuerpo)
            }
            .show()
    }

    private fun cambiarEnviando(valor: Boolean) {
        enviando = valor
        botonConfirmar.isEnabled = !valor
        botonFecha.isEnabled = !valor
        botonEquipos.isEnabled = !valor && !consultando
        solicitante.isEnabled = !valor
        identificacion.isEnabled = !valor
        observaciones.isEnabled = !valor
        mostrarSeleccionados()
    }

    private fun enviarPrestamo(servidor: String, token: String, cuerpo: JSONObject) {
        if (enviando) return
        cambiarEnviando(true)
        estado.text = "Registrando préstamo en SQL Server…"
        Thread {
            var conexion: HttpURLConnection? = null
            var idPrestamo: Int? = null
            var mensajeError: String? = null
            var sesionVencida = false

            try {
                val http = URL("$servidor/prestamos").openConnection() as HttpURLConnection
                conexion = http
                http.requestMethod = "POST"
                http.connectTimeout = 5000
                http.readTimeout = 15000
                http.instanceFollowRedirects = false
                http.useCaches = false
                http.doOutput = true
                http.setRequestProperty("Accept", "application/json")
                http.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                http.setRequestProperty("Authorization", "Bearer $token")

                http.outputStream.use { salida ->
                    salida.write(cuerpo.toString().toByteArray(Charsets.UTF_8))
                }

                val codigo = http.responseCode
                val flujo = if (codigo in 200..299) http.inputStream else http.errorStream
                val textoRespuesta = flujo?.bufferedReader(Charsets.UTF_8)
                    ?.use { it.readText() }.orEmpty()

                if (codigo == 201) {
                    val respuesta = JSONObject(textoRespuesta)
                    idPrestamo = respuesta.getInt("id_prestamo")
                } else {
                    sesionVencida = codigo == 401
                    mensajeError = extraerError(textoRespuesta, codigo)
                }
            } catch (_: SocketTimeoutException) {
                mensajeError = "El servidor tardó demasiado. Verifica en SIGELAB de escritorio si se registró antes de reintentar."
            } catch (_: IOException) {
                mensajeError = "Se interrumpió la conexión. Comprueba en SIGELAB de escritorio si el préstamo se registró antes de reintentar."
            } catch (_: Exception) {
                mensajeError = "No se pudo interpretar la respuesta. Comprueba el préstamo en SIGELAB antes de reintentar."
            } finally {
                conexion?.disconnect()
            }

            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                cambiarEnviando(false)

                if (idPrestamo != null) {
                    val identificador = idPrestamo
                    solicitante.text?.clear()
                    identificacion.text?.clear()
                    observaciones.text?.clear()
                    fechaElegida = null
                    seleccionados.clear()
                    mostrarFecha()
                    mostrarSeleccionados()
                    estado.text = "Préstamo #$identificador registrado correctamente."

                    MaterialAlertDialogBuilder(this)
                        .setTitle("Préstamo registrado")
                        .setMessage(
                            "El préstamo #$identificador quedó registrado en SQL Server. " +
                                    "Comprueba su aparición al actualizar el sistema de escritorio."
                        )
                        .setPositiveButton("Aceptar", null)
                        .show()
                } else {
                    if (sesionVencida) SesionMovil.limpiar()
                    estado.text = mensajeError ?: "No se pudo completar el préstamo."
                }
            }
        }.start()
    }

    private fun extraerError(contenido: String, codigo: Int): String {
        return try {
            val detalle = JSONObject(contenido).opt("detail")
            when (detalle) {
                is String -> detalle
                is JSONArray -> {
                    val primero = detalle.optJSONObject(0)
                    primero?.optString("msg")?.takeIf { it.isNotBlank() }
                        ?: "Datos inválidos (HTTP $codigo)."
                }
                else -> "No se pudo registrar el préstamo (HTTP $codigo)."
            }
        } catch (_: Exception) {
            "No se pudo registrar el préstamo (HTTP $codigo)."
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        fechaElegida?.let { outState.putLong("fecha", it.timeInMillis) }
        val equipos = JSONArray()
        seleccionados.values.forEach { equipos.put(it) }
        outState.putString("equipos", equipos.toString())
        super.onSaveInstanceState(outState)
    }
}