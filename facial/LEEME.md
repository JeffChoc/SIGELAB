# SIGELAB - Servicio de Reconocimiento Facial

## 1. Descripción

El servicio facial de SIGELAB es un componente desarrollado en Python que permite realizar el registro y la verificación de rostros como parte del proceso de autenticación del sistema.

Utiliza OpenCV y NumPy para procesar imágenes capturadas mediante una cámara web y se comunica con la aplicación Java a través de un servicio local HTTP.

**Características:**
- Registro de rostros asociados al ID de un usuario.
- Verificación de rostros registrados.
- Integración con la aplicación Java de SIGELAB.
- Ejecución independiente del sistema principal.

## 2. Requisitos

Cada integrante del equipo debe contar con:

- Git instalado.
- Python 3.9 de 64 bits.
- Una cámara web funcional.
- El proyecto SIGELAB clonado desde GitHub.
- IntelliJ IDEA para ejecutar la aplicación Java, si corresponde.

## 3. Ubicación del servicio

El servicio se encuentra dentro de la carpeta `facial` del proyecto:

```text
SIGELAB/
├── facial/
│   ├── servidor_facial.py
│   ├── requirements.txt
│   └── LEEME.md
├── src/
└── pom.xml
```

La estructura completa puede variar según la organización del proyecto.

## 4. Obtener el proyecto

Cada colaborador debe clonar el repositorio de SIGELAB desde GitHub o actualizar su copia local.

Si ya tiene el proyecto clonado, puede abrir una terminal en la carpeta del proyecto y ejecutar:

```bash
git pull
```

Si todavía no lo tiene, debe clonar el repositorio utilizando la URL oficial compartida por el equipo.

## 5. Preparar Python

Abrir una terminal en la carpeta `facial`.

Crear un entorno virtual:

```bash
py -3.9 -m venv .venv
```

Activar el entorno virtual en Windows PowerShell:

```powershell
.\.venv\Scripts\Activate.ps1
```

En Windows CMD:

```bat
.venv\Scripts\activate.bat
```

En Linux o macOS, si se utiliza un entorno compatible:

```bash
python3 -m venv .venv
source .venv/bin/activate
```

## 6. Instalar las dependencias

Con el entorno virtual activado, ejecutar:

```bash
python -m pip install --upgrade pip
python -m pip install -r requirements.txt
```

Para mantener una instalación compatible con la versión probada del proyecto, se puede instalar OpenCV 4.10.0.84:

```bash
python -m pip install opencv-contrib-python==4.10.0.84
```

El archivo `requirements.txt` del repositorio debe mantenerse actualizado con las versiones que el equipo haya probado.

Verificar la instalación:

```bash
python -c "import cv2, numpy; print('OpenCV:', cv2.__version__); print('NumPy:', numpy.__version__)"
```

## 7. Registrar un rostro

Cada persona debe utilizar el ID que le corresponde en la base de datos de SIGELAB.

Ejecutar:

```bash
python servidor_facial.py registrar ID_USUARIO
```

Por ejemplo:

```bash
python servidor_facial.py registrar 2
```

En este ejemplo, `2` es el ID de usuario de la base de datos, no un número que todos deban copiar.

Seguir las instrucciones que aparezcan en pantalla y completar el registro frente a la cámara.

**Importante:** antes de registrar un rostro, confirmar que el usuario existe en la base de datos y que el procedimiento de registro admite ese ID.

## 8. Actualizar un rostro existente

Para volver a registrar el rostro de un usuario, utilizar su mismo ID:

```bash
python servidor_facial.py registrar ID_USUARIO
```

Seguir las instrucciones que muestre el programa.

El comportamiento exacto al registrar un rostro que ya existe depende de la implementación del script. Si el registro anterior no se reemplaza automáticamente, revisar el código antes de eliminar archivos.

## 9. Iniciar el servicio facial

Con el entorno virtual activado y desde la carpeta `facial`, ejecutar:

```bash
python servidor_facial.py
```

Mantener abierta la terminal mientras se utilice SIGELAB.

El servicio está configurado para utilizar la dirección local:

```text
127.0.0.1:5001
```

Esta dirección representa la propia computadora de cada colaborador. No es una dirección pública ni una dirección compartida por todos los integrantes.

Para detener el servicio, presionar `Ctrl + C`.

## 10. Ejecutar SIGELAB

1. Activar el entorno virtual de Python.
2. Iniciar el servicio facial.
3. Mantener abierta la terminal.
4. Abrir SIGELAB desde IntelliJ IDEA.
5. Iniciar sesión con un usuario válido.
6. Completar la verificación facial cuando la aplicación la solicite.

La aplicación Java y el servicio Python deben ejecutarse en la computadora donde se está utilizando el sistema, salvo que el equipo configure expresamente una arquitectura diferente.

## 11. Datos biométricos y GitHub

Los registros faciales son datos biométricos y deben manejarse con cuidado.

- No subir fotografías ni registros faciales personales a GitHub.
- No compartir los registros de un usuario con otros integrantes.
- Cada integrante debe registrar sus propios datos de prueba cuando corresponda.
- La carpeta que contiene los rostros debe excluirse del control de versiones.
- No guardar contraseñas ni secretos en el repositorio.

## 12. Solución de problemas

**Python no se reconoce:** instalar Python y comprobar que esté disponible en la terminal.

**No se encuentra `.venv`:** crear el entorno virtual siguiendo las instrucciones de la sección 5.

**No se encuentra OpenCV:** activar el entorno virtual e instalar las dependencias.

**No abre la cámara:** comprobar los permisos, la conexión de la cámara y que ninguna otra aplicación la esté utilizando.

**No se reconoce el rostro:** comprobar que se haya registrado con el ID correcto y seguir las instrucciones de captura.

**Java no se comunica con Python:** verificar que el servicio esté iniciado y que la configuración de conexión de SIGELAB apunte al puerto correcto.

---

**Proyecto:** SIGELAB  
**Componente:** Servicio de reconocimiento facial  
**Tecnologías:** Python, OpenCV y NumPy  
**Uso:** Equipo de desarrollo de SIGELAB
