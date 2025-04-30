import requests

result = requests.post(
    "http://localhost:8080/ChordSimpleImplementation/nodo/join",
    data="http://localhost:8000",  # texto plano
    headers={"Content-Type": "text/plain"}  # explícitamente text/plain
)

print(result.status_code)
print(result.text)
