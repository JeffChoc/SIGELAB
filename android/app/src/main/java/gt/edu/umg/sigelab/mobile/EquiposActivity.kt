package gt.edu.umg.sigelab.mobile

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class EquiposActivity : AppCompatActivity() {

    private lateinit var lista: LinearLayout
    private lateinit var estado: TextView
    private lateinit var actualizar: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val pantalla = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#F3F6FB"))
            setPadding(dp(20), dp(20), dp(20), dp(20))
        }

        setContentView(pantalla)

        ViewCompat.setOnApplyWindowInsetsListener(pantalla) { vista, insets ->
            val barras = insets.getInsets(
                WindowInsetsCompat.Type.systemBars()
            )

            vista.setPadding(
                dp(20) + barras.left,
                dp(20) + barras.top,
                dp(20) + barras.right,
                dp(20) + barras.bottom
            )

            insets
        }

        pantalla.addView(texto("Equipos del laboratorio", 26f, true))
        pantalla.addView(texto("Consulta su disponibilidad actual.", 15f))

        actualizar = MaterialButton(this).apply {
            text = "Actualizar equipos"
            isAllCaps = false
            setOnClickListener { cargarEquipos() }
        }

        pantalla.addView(actualizar)

        estado = texto("", 14f)
        pantalla.addView(estado)

        lista = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val desplazamiento = ScrollView(this).apply {
            addView(lista)
        }

        pantalla.addView(
            desplazamiento,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        cargarEquipos()
    }

    private fun cargarEquipos() {
        val servidor = getSharedPreferences(
            "sigelab_configuracion",
            MODE_PRIVATE
        ).getString("servidor", "").orEmpty()

        if (servidor.isBlank()) {
            estado.text = "Regresa al inicio y configura la conexión."
            return
        }

        actualizar.isEnabled = false
        estado.text = "Consultando equipos…"
        lista.removeAllViews()

        Thread {
            var conexion: HttpURLConnection? = null

            try {
                conexion = URL("$servidor/equipos")
                    .openConnection() as HttpURLConnection

                conexion.apply {
                    connectTimeout = 5000
                    readTimeout = 10000
                    requestMethod = "GET"
                    instanceFollowRedirects = false
                    useCaches = false
                    setRequestProperty("Accept", "application/json")
                }

                if (conexion.responseCode != 200) {
                    throw IllegalStateException(
                        "El servidor no pudo entregar los equipos."
                    )
                }

                val respuesta = conexion.inputStream
                    .bufferedReader(Charsets.UTF_8)
                    .use { it.readText() }

                val equipos = JSONObject(respuesta).getJSONArray("equipos")

                runOnUiThread {
                    if (!isFinishing && !isDestroyed) {
                        actualizar.isEnabled = true

                        estado.text = if (equipos.length() == 0) {
                            "No hay equipos registrados."
                        } else {
                            "${equipos.length()} equipos encontrados"
                        }

                        for (indice in 0 until equipos.length()) {
                            agregarTarjeta(equipos.getJSONObject(indice))
                        }
                    }
                }
            } catch (_: Exception) {
                runOnUiThread {
                    if (!isFinishing && !isDestroyed) {
                        actualizar.isEnabled = true
                        estado.text =
                            "No se pudieron cargar los equipos. " +
                                    "Revisa el servidor y el Wi-Fi, y toca Actualizar."
                    }
                }
            } finally {
                conexion?.disconnect()
            }
        }.start()
    }

    private fun agregarTarjeta(equipo: JSONObject) {
        val tarjeta = MaterialCardView(this).apply {
            radius = dp(18).toFloat()
            cardElevation = 0f
            setCardBackgroundColor(Color.WHITE)
            strokeWidth = dp(1)
            strokeColor = Color.parseColor("#D5DFED")

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(12)
            }
        }

        val contenido = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(18), dp(18), dp(18))
        }

        contenido.addView(texto(equipo.optString("nombre"), 19f, true))

        contenido.addView(
            texto("Código: ${equipo.optString("codigo")}", 14f)
        )

        contenido.addView(
            texto("Tipo: ${equipo.optString("tipo")}", 14f)
        )

        val descripcion = listOf(
            equipo.optString("marca"),
            equipo.optString("modelo")
        ).filter { it.isNotBlank() }.joinToString(" ")

        if (descripcion.isNotBlank()) {
            contenido.addView(texto(descripcion, 14f))
        }

        contenido.addView(
            texto("Laboratorio: ${equipo.optString("laboratorio")}", 14f)
        )

        contenido.addView(
            texto("Estado: ${equipo.optString("estado")}", 15f, true)
        )

        tarjeta.addView(contenido)
        lista.addView(tarjeta)
    }

    private fun texto(
        contenido: String,
        tamano: Float,
        negrita: Boolean = false
    ): TextView {
        return TextView(this).apply {
            text = contenido
            textSize = tamano
            setTextColor(Color.parseColor("#142D4E"))
            setPadding(0, dp(4), 0, dp(4))

            if (negrita) {
                setTypeface(typeface, android.graphics.Typeface.BOLD)
            }

            textAlignment = View.TEXT_ALIGNMENT_VIEW_START
        }
    }

    private fun dp(valor: Int): Int {
        return (valor * resources.displayMetrics.density).toInt()
    }
}