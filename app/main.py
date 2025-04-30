from fastapi import FastAPI
import socket
import requests


ip_address = socket.gethostbyname(socket.gethostname())
recursos = {
  "uno": "uno",
  "dos": "dos",
}
tamano_anillo = 8

app = FastAPI()
@app.get("/Anillo")
def anillo():
  return {
    "ipEntrada": ip_address,
    "tamanoAnillo": tamano_anillo
  }

@app.post("/creaAnillo")
def crea_anillo():
  result = requests.post(
      "http://localhost:8080/ChordSimpleImplementation/nodo/join",
      data="http://localhost:8000",  # texto plano
      headers={"Content-Type": "text/plain"}  # explícitamente text/plain
  )

  return {
    "status_code": result.status_code,
    "text": result.text
  }

@app.post("/join")
def join(ip: str):
  url = f"http://{ip}:8080/ChordSimpleImplementation/nodo/join"
  result = requests.post(
      url,
      data="http://localhost:8000",  # texto plano
      headers={"Content-Type": "text/plain"}  # explícitamente text/plain
  )
  return {
    "status_code": result.status_code,
    "text": result.text
  }