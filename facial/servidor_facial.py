# -*- coding: utf-8 -*-
"""
Servicio local de reconocimiento facial para SIGELAB (Python + OpenCV).

Java lo consume por HTTP en 127.0.0.1:5001. El rostro es SOLO un segundo factor del login:
el servicio no conoce contraseñas ni roles, solo responde si el rostro frente a la cámara
corresponde al usuario indicado.

Uso:
    python servidor_facial.py                 -> inicia el servicio (comando por defecto)
    python servidor_facial.py registrar 1     -> registra el rostro del usuario con id 1
    python servidor_facial.py verificar 1     -> prueba la verificación desde la consola

Endpoints (todos responden JSON UTF-8 con "exito" y "mensaje"):
    GET  /salud                  -> el servicio está activo
    GET  /estado?usuario=ID      -> indica si el usuario ya tiene rostro registrado
    POST /registrar?usuario=ID   -> captura fotos con la cámara y guarda el rostro
    POST /verificar?usuario=ID   -> captura con la cámara y compara con el rostro registrado
"""
import argparse
import json
import os
import shutil
import sys
import time
import traceback
from http.server import BaseHTTPRequestHandler, HTTPServer
from urllib.parse import parse_qs, urlparse

import cv2
import numpy as np

if not hasattr(cv2, "face"):
    sys.exit("Falta el módulo cv2.face. Instale: pip install opencv-contrib-python numpy")

HOST = "127.0.0.1"           # solo escucha en esta computadora
PUERTO = 5001
CARPETA_ROSTROS = os.path.join(os.path.dirname(os.path.abspath(__file__)), "rostros")

TAMANO_ROSTRO = (200, 200)
UMBRAL_CONFIANZA = 70.0      # LBPH: menor valor = más parecido. Suba el umbral si rechaza al usuario correcto
FOTOS_REGISTRO = 20
FOTOS_MINIMAS_REGISTRO = 12
SEGUNDOS_MAXIMOS_REGISTRO = 20.0
SEGUNDOS_VERIFICACION = 4.0
CUADROS_MINIMOS_VERIFICACION = 5
PORCENTAJE_ACEPTACION = 0.6  # proporción de cuadros que deben coincidir con el usuario
NOMBRE_VENTANA = "SIGELAB - Reconocimiento facial"

detector_rostros = cv2.CascadeClassifier(cv2.data.haarcascades + "haarcascade_frontalface_default.xml")


# ---------------------------------------------------------------- cámara y detección

def abrir_camara():
    camara = cv2.VideoCapture(0, cv2.CAP_DSHOW) if os.name == "nt" else cv2.VideoCapture(0)
    if not camara.isOpened():
        camara.release()
        camara = cv2.VideoCapture(0)
    if not camara.isOpened():
        camara.release()
        return None
    return camara


def detectar_rostro(cuadro):
    """Devuelve (rostro recortado en gris 200x200, caja) del rostro más grande, o (None, None)."""
    gris = cv2.cvtColor(cuadro, cv2.COLOR_BGR2GRAY)
    gris = cv2.equalizeHist(gris)
    rostros = detector_rostros.detectMultiScale(gris, scaleFactor=1.2, minNeighbors=5, minSize=(80, 80))
    if len(rostros) == 0:
        return None, None
    x, y, ancho, alto = max(rostros, key=lambda r: r[2] * r[3])
    recorte = cv2.resize(gris[y:y + alto, x:x + ancho], TAMANO_ROSTRO)
    return recorte, (int(x), int(y), int(ancho), int(alto))


class VistaPrevia:
    """Ventana opcional con lo que ve la cámara. Si no hay entorno gráfico, se desactiva sola."""

    def __init__(self):
        self.activa = True

    def mostrar(self, cuadro, texto, caja=None):
        if not self.activa:
            return
        try:
            imagen = cuadro.copy()
            if caja is not None:
                x, y, ancho, alto = caja
                cv2.rectangle(imagen, (x, y), (x + ancho, y + alto), (0, 200, 0), 2)
            cv2.putText(imagen, texto, (10, 30), cv2.FONT_HERSHEY_SIMPLEX, 0.7, (0, 255, 0), 2)
            cv2.imshow(NOMBRE_VENTANA, imagen)
            cv2.waitKey(1)
        except cv2.error:
            self.activa = False

    def cerrar(self):
        if self.activa:
            try:
                cv2.destroyWindow(NOMBRE_VENTANA)
                cv2.waitKey(1)
            except cv2.error:
                pass


# ---------------------------------------------------------------- datos y modelo

def carpeta_usuario(id_usuario):
    return os.path.join(CARPETA_ROSTROS, str(id_usuario))


def tiene_rostro(id_usuario):
    carpeta = carpeta_usuario(id_usuario)
    if not os.path.isdir(carpeta):
        return False
    fotos = [f for f in os.listdir(carpeta) if f.lower().endswith(".png")]
    return len(fotos) >= FOTOS_MINIMAS_REGISTRO


def entrenar_modelo():
    """Entrena el reconocedor con los rostros registrados (son pocas imágenes, tarda milisegundos)."""
    imagenes, etiquetas = [], []
    if not os.path.isdir(CARPETA_ROSTROS):
        return None
    for nombre in os.listdir(CARPETA_ROSTROS):
        carpeta = os.path.join(CARPETA_ROSTROS, nombre)
        if not (nombre.isdigit() and os.path.isdir(carpeta)):
            continue
        for archivo in os.listdir(carpeta):
            if archivo.lower().endswith(".png"):
                imagen = cv2.imread(os.path.join(carpeta, archivo), cv2.IMREAD_GRAYSCALE)
                if imagen is not None:
                    imagenes.append(cv2.resize(imagen, TAMANO_ROSTRO))
                    etiquetas.append(int(nombre))
    if not imagenes:
        return None
    modelo = cv2.face.LBPHFaceRecognizer_create()
    modelo.train(imagenes, np.array(etiquetas, dtype=np.int32))
    return modelo


# ---------------------------------------------------------------- operaciones

def registrar_rostro(id_usuario):
    """Captura fotos del rostro y reemplaza el registro anterior solo si la captura fue buena."""
    camara = abrir_camara()
    if camara is None:
        return False, "No se pudo abrir la cámara.", None

    vista = VistaPrevia()
    recortes = []
    inicio = time.time()
    ultima_captura = 0.0
    try:
        while len(recortes) < FOTOS_REGISTRO and time.time() - inicio < SEGUNDOS_MAXIMOS_REGISTRO:
            correcto, cuadro = camara.read()
            if not correcto:
                continue
            recorte, caja = detectar_rostro(cuadro)
            vista.mostrar(cuadro, "Registrando %d/%d - mire a la camara" % (len(recortes), FOTOS_REGISTRO), caja)
            if recorte is not None and time.time() - ultima_captura >= 0.25:
                recortes.append(recorte)
                ultima_captura = time.time()
    finally:
        camara.release()
        vista.cerrar()

    if len(recortes) < FOTOS_MINIMAS_REGISTRO:
        return False, "No se detectó el rostro con claridad. Mejore la iluminación y mire a la cámara.", None

    carpeta = carpeta_usuario(id_usuario)
    if os.path.isdir(carpeta):
        shutil.rmtree(carpeta)
    os.makedirs(carpeta)
    for indice, recorte in enumerate(recortes):
        cv2.imwrite(os.path.join(carpeta, "%02d.png" % indice), recorte)
    return True, "Rostro registrado correctamente.", None


def verificar_rostro(id_usuario):
    """Devuelve (reconocido, mensaje, confianza media)."""
    if not tiene_rostro(id_usuario):
        return False, "El usuario no tiene un rostro registrado.", None
    modelo = entrenar_modelo()
    if modelo is None:
        return False, "No hay rostros registrados en el servicio.", None

    camara = abrir_camara()
    if camara is None:
        return False, "No se pudo abrir la cámara.", None

    vista = VistaPrevia()
    resultados = []
    inicio = time.time()
    try:
        while time.time() - inicio < SEGUNDOS_VERIFICACION:
            correcto, cuadro = camara.read()
            if not correcto:
                continue
            recorte, caja = detectar_rostro(cuadro)
            if recorte is not None:
                etiqueta, confianza = modelo.predict(recorte)
                resultados.append((etiqueta, confianza))
            vista.mostrar(cuadro, "Verificando - mire a la camara", caja)
    finally:
        camara.release()
        vista.cerrar()

    if len(resultados) < CUADROS_MINIMOS_VERIFICACION:
        return False, "No se detectó un rostro frente a la cámara.", None

    aciertos = [c for (etiqueta, c) in resultados if etiqueta == id_usuario and c <= UMBRAL_CONFIANZA]
    proporcion = len(aciertos) / len(resultados)
    confianza_media = sum(c for _, c in resultados) / len(resultados)
    if proporcion >= PORCENTAJE_ACEPTACION:
        return True, "Rostro reconocido.", round(confianza_media, 1)
    return False, "El rostro no corresponde al usuario.", round(confianza_media, 1)


# ---------------------------------------------------------------- servidor HTTP

class Manejador(BaseHTTPRequestHandler):
    # HTTPServer atiende una solicitud a la vez: la cámara no se usa en paralelo y la ventana
    # de vista previa siempre se abre desde el mismo hilo.

    def _responder(self, datos, codigo=200):
        cuerpo = json.dumps(datos, ensure_ascii=False).encode("utf-8")
        self.send_response(codigo)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(cuerpo)))
        self.end_headers()
        self.wfile.write(cuerpo)

    @staticmethod
    def _id_usuario(consulta):
        valor = consulta.get("usuario", [""])[0]
        return int(valor) if valor.isdigit() else None

    def do_GET(self):
        ruta = urlparse(self.path)
        consulta = parse_qs(ruta.query)
        if ruta.path == "/salud":
            self._responder({"exito": True, "mensaje": "Servicio facial activo."})
        elif ruta.path == "/estado":
            id_usuario = self._id_usuario(consulta)
            if id_usuario is None:
                self._responder({"exito": False, "mensaje": "Falta el parámetro usuario."}, 400)
            else:
                registrado = tiene_rostro(id_usuario)
                self._responder({"exito": True, "registrado": registrado,
                                 "mensaje": "Rostro registrado." if registrado else "Sin rostro registrado."})
        else:
            self._responder({"exito": False, "mensaje": "Ruta no encontrada."}, 404)

    def do_POST(self):
        ruta = urlparse(self.path)
        id_usuario = self._id_usuario(parse_qs(ruta.query))
        if ruta.path not in ("/registrar", "/verificar"):
            self._responder({"exito": False, "mensaje": "Ruta no encontrada."}, 404)
            return
        if id_usuario is None:
            self._responder({"exito": False, "mensaje": "Falta el parámetro usuario."}, 400)
            return
        try:
            operacion = registrar_rostro if ruta.path == "/registrar" else verificar_rostro
            exito, mensaje, confianza = operacion(id_usuario)
            respuesta = {"exito": exito, "mensaje": mensaje}
            if confianza is not None:
                respuesta["confianza"] = confianza
            self._responder(respuesta)
        except Exception:
            traceback.print_exc()
            self._responder({"exito": False, "mensaje": "Error interno del servicio facial."}, 500)

    def log_message(self, formato, *argumentos):
        print("[facial] " + formato % argumentos)


def iniciar_servidor():
    servidor = HTTPServer((HOST, PUERTO), Manejador)
    print("Servicio facial de SIGELAB activo en http://%s:%d  (Ctrl+C para detener)" % (HOST, PUERTO))
    try:
        servidor.serve_forever()
    except KeyboardInterrupt:
        print("\nServicio detenido.")
    finally:
        servidor.server_close()


def main():
    analizador = argparse.ArgumentParser(description="Servicio de reconocimiento facial de SIGELAB")
    subcomandos = analizador.add_subparsers(dest="comando")
    subcomandos.add_parser("servidor", help="inicia el servicio HTTP (por defecto)")
    registrar = subcomandos.add_parser("registrar", help="registra el rostro de un usuario")
    registrar.add_argument("usuario", type=int, help="id del usuario en la base de datos")
    verificar = subcomandos.add_parser("verificar", help="prueba la verificación de un usuario")
    verificar.add_argument("usuario", type=int, help="id del usuario en la base de datos")
    argumentos = analizador.parse_args()

    comando = argumentos.comando or "servidor"
    if comando == "registrar":
        exito, mensaje, _ = registrar_rostro(argumentos.usuario)
        print(("OK: " if exito else "ERROR: ") + mensaje)
    elif comando == "verificar":
        exito, mensaje, confianza = verificar_rostro(argumentos.usuario)
        print(("RECONOCIDO: " if exito else "NO RECONOCIDO: ") + mensaje
              + ("" if confianza is None else " (confianza media %s)" % confianza))
    else:
        iniciar_servidor()


if __name__ == "__main__":
    main()
