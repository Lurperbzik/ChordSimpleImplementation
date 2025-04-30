from fastapi import FastAPI
import socket

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
