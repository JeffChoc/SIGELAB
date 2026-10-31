package gt.edu.umg.sigelab.mobile

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class PrestamoActivity : AppCompatActivity() {

    private var fechaElegida: Calendar? = null

    private val seleccionados = linkedMapOf<Int, JSONObject>()

    private lateinit var textoFecha: TextView
    private lateinit var resumen: TextView
    private lateinit var estado: TextView
    private lateinit var lista: LinearLayout
    private lateinit var botonEquipos: MaterialButton

    private var consultando = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_prestamo)

        ViewCompat.setOnApplyWindowInsetsListener(
            findViewById(R.id.prestamoPrincipal)
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

        textoFecha = findViewById(R.id.txtFechaDevolucion)
        resumen = findViewById(R.id.txtResumenEquipos)
        estado = findViewById(R.id.txtEstadoPrestamo)
        lista = findViewById(R.id.listaEquiposPrestamo)
        botonEquipos = findViewById(R.id.btnSeleccionarEquipos)

        findViewById<MaterialButton>(
            R.id.btnFechaDevolucion
        ).setOnClickListener {
            elegirFecha()
        }

        botonEquipos.setOnClickListener {
            consultarDisponibles()
        }

        findViewById<MaterialButton>(
            R.id.btnVolverPrestamo
        ).setOnClickListener {
            finish()
        }

        // Se habilitará después de integrar la sesión completa.
        findViewById<MaterialButton>(
            R.id.btnConfirmarPrestamo
        ).isEnabled = false

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
    }

    private fun elegirFecha() {
        val inicial = fechaElegida
            ?: Calendar.getInstance().apply {
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
                            estado.text =
                                "Selecciona una fecha y hora futuras."
                        } else {
                            fechaElegida = elegida
                            mostrarFecha()
                            mostrarAvisoSesion()
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
        val fecha = fechaElegida

        textoFecha.text = if (fecha == null) {
            "Sin fecha seleccionada"
        } else {
            val formato = SimpleDateFormat(
                "dd/MM/yyyy 'a las' HH:mm",
                Locale.getDefault()
            )

            "Devolución: ${formato.format(fecha.time)}"
        }
    }

    private fun consultarDisponibles() {
        if (consultando) {
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

        consultando = true
        botonEquipos.isEnabled = false
        estado.text = "Consultando equipos disponibles…"

        Thread {
            var conexion: HttpURLConnection? = null

            try {
                val http = URL("$servidor/equipos")
                    .openConnection() as HttpURLConnection

                conexion = http

                http.apply {
                    requestMethod = "GET"
                    connectTimeout = 5000
                    readTimeout = 10000
                    instanceFollowRedirects = false
                    useCaches = false
                    setRequestProperty("Accept", "application/json")
                }

                if (http.responseCode != 200) {
                    throw IllegalStateException()
                }

                val contenido = http.inputStream
                    .bufferedReader(Charsets.UTF_8)
                    .use { it.readText() }

                val equipos = JSONObject(contenido)
                    .getJSONArray("equipos")

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
                        botonEquipos.isEnabled = true
                        abrirSelector(disponibles)
                    }
                }
            } catch (_: Exception) {
                runOnUiThread {
                    if (!isFinishing && !isDestroyed) {
                        consultando = false
                        botonEquipos.isEnabled = true
                        estado.text =
                            "No se pudieron consultar los equipos. " +
                                    "Revisa el servidor y vuelve a intentarlo."
                    }
                }
            } finally {
                conexion?.disconnect()
            }
        }.start()
    }

    private fun abrirSelector(disponibles: List<JSONObject>) {
        val idsDisponibles = disponibles.map {
            it.getInt("id")
        }.toSet()

        val retirados = seleccionados.keys.filter {
            it !in idsDisponibles
        }

        retirados.forEach {
            seleccionados.remove(it)
        }

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
            seleccionados.containsKey(
                disponibles[it].getInt("id")
            )
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Equipos disponibles")
            .setMultiChoiceItems(
                etiquetas,
                marcados
            ) { _, posicion, marcado ->
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
                mostrarAvisoSesion()
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
                text =
                    "${equipo.optString("codigo")} · " +
                            "${equipo.optString("nombre")}\nQuitar del préstamo"

                isAllCaps = false

                setOnClickListener {
                    seleccionados.remove(equipo.getInt("id"))
                    mostrarSeleccionados()
                }
            }

            lista.addView(boton)
        }
    }

    private fun mostrarAvisoSesion() {
        estado.text =
            "Para confirmar necesitas iniciar sesión " +
                    "y completar la verificación facial."
    }

    override fun onSaveInstanceState(outState: Bundle) {
        fechaElegida?.let {
            outState.putLong("fecha", it.timeInMillis)
        }

        val equipos = JSONArray()

        seleccionados.values.forEach {
            equipos.put(it)
        }

        outState.putString("equipos", equipos.toString())

        super.onSaveInstanceState(outState)
    }
}