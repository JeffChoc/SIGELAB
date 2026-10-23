# Servicio de reconocimiento facial de SIGELAB

Componente en Python + OpenCV que Java consume por HTTP (127.0.0.1:5001). El rostro es solo el
segundo factor del login: el servicio no conoce contraseñas ni roles.

## Requisitos
- Python 3.9 o superior
- Una cámara web
- `pip install -r requirements.txt` (opencv-contrib-python y numpy)

## Uso
1. Registrar el rostro de un usuario (el número es su `id_usuario` en la base de datos; `admin` es 1):
   `python servidor_facial.py registrar 1`
2. Iniciar el servicio y dejarlo abierto mientras se usa SIGELAB:
   `python servidor_facial.py`
3. Abrir SIGELAB e iniciar sesión: tras usuario y contraseña se abre la cámara.

Opcional, para probar sin abrir SIGELAB: `python servidor_facial.py verificar 1`

## Ajustes (constantes al inicio de servidor_facial.py)
- `UMBRAL_CONFIANZA` (70): si rechaza al usuario correcto, súbalo (por ejemplo 80); si acepta a
  otra persona, bájelo (por ejemplo 55). Menor valor = más parecido.
- Si la dirección cambia, agregue `facial.url=http://127.0.0.1:5001` a `config.properties`.

## Datos biométricos
Las fotos se guardan en `facial/rostros/<id_usuario>/`. Esa carpeta está en el `.gitignore`
y NO debe subirse al repositorio.
