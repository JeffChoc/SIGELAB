package gt.edu.umg.sigelab.mobile

object SesionMovil {

    data class Datos(
        val servidor: String,
        val token: String,
        val idUsuario: Int,
        val nombre: String,
        val rol: String
    )

    @Volatile
    var actual: Datos? = null
        private set

    fun iniciar(datos: Datos) {
        actual = datos
    }

    fun limpiar() {
        actual = null
    }
}