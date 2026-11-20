import os
import socket
import struct
import sys

def command(text):
    connection = socket.create_connection(("127.0.0.1", 25580), timeout=10)
    def send(identifier, kind, body):
        data = body.encode()
        connection.sendall(struct.pack("<iii", len(data) + 10, identifier, kind) + data + b"\0\0")
    def read(count):
        data = b""
        while len(data) < count:
            chunk = connection.recv(count - len(data))
            if not chunk:
                raise EOFError("Сервер закрыл соединение")
            data += chunk
        return data
    def receive():
        size = struct.unpack("<i", read(4))[0]
        packet = read(size)
        return struct.unpack("<ii", packet[:8]), packet[8:-2].decode()
    send(1, 3, os.environ.get("BENCH_RCON_PASSWORD", "benchmark-local-only"))
    while True:
        (identifier, kind), body = receive()
        if identifier == -1:
            raise RuntimeError("Неверный пароль RCON")
        if kind == 2:
            break
    send(2, 2, text)
    try:
        while True:
            (identifier, kind), body = receive()
            if identifier == 2:
                return body
    finally:
        connection.close()

if __name__ == "__main__":
    print(command(" ".join(sys.argv[1:])))
