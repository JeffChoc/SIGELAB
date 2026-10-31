package gt.edu.umg.sigelab.mobile

import android.os.Bundle
import android.widget.RadioGroup
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class DevolucionActivity : AppCompatActivity() {

    private lateinit var estado: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContentView(R.layout.activity_devolucion)

        ViewCompat.setOnApplyWindowInsetsListener(
            findViewById(R.id.devolucionPrincipal)
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

        estado = findViewById(R.id.txtEstadoDevolucion)

        findViewById<MaterialButton>(
            R.id.btnElegirPrestamo
        ).setOnClickListener {
            mostrarAccesoRequerido()
        }

        findViewById<RadioGroup>(
            R.id.grupoCondicion
        ).setOnCheckedChangeListener { _, opcion ->
            estado.text = when (opcion) {
                R.id.radioBueno -> {
                    "Los equipos seleccionados quedarán disponibles " +
                            "cuando se confirme su devolución."
                }

                R.id.radioDanado -> {
                    "Los equipos seleccionados quedarán dañados " +
                            "cuando se confirme su devolución."
                }

                else -> {
                    "Selecciona la condición de devolución."
                }
            }
        }

        // Se conectará al servidor después de integrar la sesión.
        findViewById<MaterialButton>(
            R.id.btnConfirmarDevolucion
        ).isEnabled = false

        findViewById<MaterialButton>(
            R.id.btnVolverDevolucion
        ).setOnClickListener {
            finish()
        }
    }

    private fun mostrarAccesoRequerido() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Inicio de sesión requerido")
            .setMessage(
                "Para consultar los préstamos y registrar devoluciones, " +
                        "inicia sesión y completa la verificación facial. " +
                        "Esta función está disponible para administradores " +
                        "y encargados."
            )
            .setPositiveButton("Entendido", null)
            .show()
    }
}