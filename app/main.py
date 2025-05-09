from fastapi import FastAPI
import socket
import requests


ip_address = socket.gethostbyname(socket.gethostname())
recursos = {
  "uno": "uno",
  "dos": "dos",
}
tamano_anillo = 6

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
      data=f"http://{ip_address}:8000",  # texto plano
      headers={"Contestatus_codent-Type": "text/plain"}  # explícitamente text/plain
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
      data=f"http://{ip_address}:8000",  # texto plano
      headers={"Content-Type": "text/plain"}  # explícitamente text/plain
  )
  return {
    "status_code": result.status_code,
    "text": result.text
  }


@app.get("/estabilizar")
def estabilizar(ip: str):
  url = f"http://{ip}:8080/ChordSimpleImplementation/nodo/estabilizar"
  result = requests.get(url)
  return "Done"

@app.get("/status")
def sucesores():
  urls = [
    "http://192.168.1.134:8080/ChordSimpleImplementation/nodo/status",
    "http://192.168.1.130:8080/ChordSimpleImplementation/nodo/status",
    "http://192.168.1.133:8080/ChordSimpleImplementation/nodo/status",
  ]

  response = []
  for url in urls:
    try:
      result = requests.get(url)
      if result.status_code == 200:
        response.append(result.text)
    except requests.exceptions.RequestException as e:
      print(f"Error al conectar con {url}: {e}")
  
  return response