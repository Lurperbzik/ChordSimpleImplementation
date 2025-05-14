from fastapi import FastAPI, APIRouter
import socket
import requests
from pydantic import BaseModel


app = FastAPI()


class Resources(BaseModel):
  id: int 
  name: str
  hostId: str
  
recursos_disponibles = []


ip_address = socket.gethostbyname(socket.gethostname())
tamano_anillo = 6

routerRecursos = APIRouter(tags=["Recursos"])
routerNodos = APIRouter(tags=["Nodos"])
routerAnillo = APIRouter(tags=["Anillo"])

"""
ENDPOINTS DE RECURSOS SI USAR
"""
@routerRecursos.post("/alta")
def alta(nombreRecurso: str):
  url = "http://localhost:8080/ChordSimpleImplementation/nodo/alta"
  result = requests.post(
      url,
      json=nombreRecurso, 
      headers={"Content-Type": "application/json"}  
  )
  return result.text

@routerRecursos.delete("/baja")
def baja(nombreRecurso: str):
  url = "http://localhost:8080/ChordSimpleImplementation/nodo/baja"
  result = requests.delete(
      url,
      data=nombreRecurso,
      headers={"Content-Type": "text/plain"}  )  

  return result.text

@routerRecursos.get("/descarga")
def descarga(idRec: int):
  url = "http://localhost:8080/ChordSimpleImplementation/nodo/descarga"
  result = requests.get(
      url,
      params={"idRec": idRec}
  )
  return result.json()



@routerRecursos.get("/recursos")
def get_recursos():
  return recursos_disponibles

""" 
ENDPOINTS DE RECURSOS NO USAR -> LOS USAN LOS NODOS
"""

@routerNodos.post("/recursos")
def add_recurso(recurso: Resources):
  recursos_disponibles.append(recurso)
  return "ok"

@routerNodos.delete("/recursos/{recurso_id}")
def delete_recurso(recurso_id: int):
  for recurso in recursos_disponibles:
    if recurso.id == recurso_id:
      recursos_disponibles.remove(recurso)
      return "ok"
    
  
  return "Recurso no encontrado"


@routerAnillo.get("/Anillo")
def anillo():
  return {
    "ipEntrada": ip_address,
    "tamanoAnillo": tamano_anillo
  }

@routerAnillo.post("/creaAnillo")
def crea_anillo():
  result = requests.post(
      "http://localhost:8080/ChordSimpleImplementation/nodo/join",
      data=f"http://{ip_address}:8000",  # texto plano
      headers={"Content-Type": "text/plain"}  # explícitamente text/plain
  )

  return {
    "status_code": result.status_code,
    "text": result.text
  }

@routerAnillo.post("/join")
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


@routerAnillo.get("/estabilizar")
def estabilizar(ip: str):
  url = f"http://{ip}:8080/ChordSimpleImplementation/nodo/estabilizar"
  result = requests.get(url)
  return "Done"

@routerAnillo.get("/status")
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

app.include_router(routerRecursos)
app.include_router(routerNodos)
app.include_router(routerAnillo)
