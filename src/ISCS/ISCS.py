"""Inter-service communication (ISCS) HTTP proxy.

This module implements a small HTTP server that validates and forwards
requests between services (User, Product, Order). It exposes a
`BaseHTTPRequestHandler` subclass `MyHandler` and helper functions
to perform HTTP requests to the downstream services.

The module expects a `config.json` file in the current working
directory with service addresses.
"""

from http.server import BaseHTTPRequestHandler, HTTPServer
import json
import re
import requests
from typing import Union


with open("config.json") as f:
    config = json.load(f)

user_ip = config["UserService"]["ip"]
user_port = config["UserService"]["port"]

product_ip = config["ProductService"]["ip"]
product_port = config["ProductService"]["port"]

order_ip = config["OrderService"]["ip"]
order_port = config["OrderService"]["port"]

ISCS_IP = config["InterServiceCommunication"]["ip"]
ISCS_PORT = config["InterServiceCommunication"]["port"]

class MyHandler(BaseHTTPRequestHandler):
    """HTTP request handler for ISCS.

    The handler validates request paths and JSON payloads before
    forwarding them to the appropriate backend service using the
    helper functions `get_json` and `post_json`.
    """

    def log_message(self, format, *args):
        """Override BaseHTTPRequestHandler.log_message to silence access logs."""
        pass

    def do_GET(self):
        """Handle incoming GET requests.

        Valid paths: /user/<id> and /product/<id>. The method expects a
        JSON body containing an "id" field which must match the id in
        the path. On success the response body from the backend service
        is proxied back to the client with the same status code.
        """

        if re.match(r"^/user/\d+$", self.path):
            num = int(self.path.split("/")[-1])

            content_length = int(self.headers.get("Content-Length", 0))
            body = self.rfile.read(content_length)

            if content_length > 0 and body and body.decode("utf-8").strip():
                data = json.loads(body.decode("utf-8"))

                if not str(data["id"]).isdigit():
                    self.send_response(400)
                    self.send_header("Content-Type", "application/json")
                    self.end_headers()
                    return
                else:
                    id = int(data["id"])

                if id != num:
                    self.send_response(400)
                    self.send_header("Content-Type", "application/json")
                    self.end_headers()
                    return

            output_from_server = get_json(
                user_ip,
                user_port,
                f"user/{num}",
                {}
            )


            self.send_response(output_from_server["status_code"])
            self.send_header("Content-Type", "application/json")
            self.end_headers()
            self.wfile.write(output_from_server["response_json"].encode("utf-8"))

        elif re.match(r"^/product/\d+$", self.path):
            num = int(self.path.split("/")[-1])

            content_length = int(self.headers.get("Content-Length", 0))
            body = self.rfile.read(content_length)

            if content_length > 0 and body and body.decode("utf-8").strip():
                data = json.loads(body.decode("utf-8"))

                if not str(data["id"]).isdigit():
                    self.send_response(400)
                    self.send_header("Content-Type", "application/json")
                    self.end_headers()
                    return
                else:
                    id = int(data["id"])


                if id != num:
                    self.send_response(400)
                    self.send_header("Content-Type", "application/json")
                    self.end_headers()
                    return

            output_from_server = get_json(
                product_ip,
                product_port,
                f"product/{num}",
                {}
            )


            self.send_response(output_from_server["status_code"])
            self.send_header("Content-Type", "application/json")
            self.end_headers()
            self.wfile.write(output_from_server["response_json"].encode("utf-8"))


        else:
            self.send_response(404)
            self.end_headers()

    def do_POST(self):
        """Handle incoming POST requests.

        Supported endpoints: /user and /product. The method validates the
        JSON payload for required fields and forwards the payload to the
        corresponding backend service. The backend response is proxied
        back to the client.
        """
        if self.path == "/user":
            content_length = int(self.headers.get("Content-Length", 0))
            if content_length == 0:
                self.send_response(400)
                self.end_headers()
                return

            raw_body = self.rfile.read(content_length)

            body_str = raw_body.decode("utf-8")

            try:
                data = json.loads(body_str)
            except json.JSONDecodeError:
                self.send_response(400)
                self.end_headers()
                return
            command = data.get("command")
            id_val = data.get("id")

            if command is None or id_val is None:
                self.send_response(400)
                self.end_headers()
                return

            if not str(id_val).isdigit():
                self.send_response(400)
                self.end_headers()
                return
            if command in ["create", "delete"]:
                if not (data["username"] and data["email"] and data["password"]):
                    self.send_response(400)
                    self.end_headers()
                    return
            elif command == "update":
                pass
            else:
                self.send_response(400)
                self.end_headers()
                return
            result = post_json(
                user_ip,
                user_port,
                "/user",
                data
            )
            self.send_response(result["status_code"])
            self.send_header("Content-Type", "application/json")
            self.end_headers()
            self.wfile.write(result["response_json"].encode("utf-8"))

        elif self.path == "/product":
            content_length = int(self.headers.get("Content-Length", 0))
            if content_length == 0:
                self.send_response(400)
                self.end_headers()
                return

            raw_body = self.rfile.read(content_length)

            body_str = raw_body.decode("utf-8")

            try:
                data = json.loads(body_str)
            except json.JSONDecodeError:
                self.send_response(400)
                self.end_headers()
                return
            command = data.get("command")
            id_val = data.get("id")

            if command is None or id_val is None:
                self.send_response(400)
                self.end_headers()
                return

            if not str(id_val).isdigit():
                self.send_response(400)
                self.end_headers()
                return
            if command in ["create", "delete"]:
                if not (data["name"] and (command == "delete" or data["description"]) and data["price"] and data["quantity"]):
                    self.send_response(400)
                    self.end_headers()
                    return
            elif command == "update":
                pass
            else:
                self.send_response(400)
                self.end_headers()
                return
            result = post_json(
                product_ip,
                product_port,
                "/product",
                data
            )
            self.send_response(result["status_code"])
            self.send_header("Content-Type", "application/json")
            self.end_headers()
            self.wfile.write(result["response_json"].encode("utf-8"))

        elif self.path == "/order":
            pass

        else:
            self.send_response(404)
            self.end_headers()


def get_json(ip: str, port: int, endpoint: str, params: dict, timeout=10)-> Union[dict, str]:
    """Perform an HTTP GET to a backend service and return a dict.

    Returns a dictionary with keys ``status_code`` and ``response_json`` on
    success. On network errors returns a string describing the error.

    :param ip: target host
    :param port: target port
    :param endpoint: path on the target (without leading slash is tolerated)
    :param params: query parameters or body payload
    :param timeout: request timeout in seconds
    :return: dict or error string
    """
    url = f"http://{ip}:{port}/{endpoint}"
    try:
        response = requests.get(
            url,
            params=params,
            timeout=timeout
        )
        return {
            "status_code": response.status_code,
            "response_json": response.text
        }
    except requests.exceptions.RequestException as e:
        return "error: " + str(e)

def post_json(ip: str, port: int, endpoint: str, payload: dict, timeout=10):
    """Perform an HTTP POST to a backend service and return a dict.

    Returns a dictionary with keys ``status_code`` and ``response_json`` on
    success. On network errors returns None.

    :param ip: target host
    :param port: target port
    :param endpoint: path on the target (should start with '/')
    :param payload: JSON-serializable payload to POST
    :param timeout: request timeout in seconds
    :return: dict or None on network error
    """
    url = f"http://{ip}:{port}{endpoint}"
    try:
        response = requests.post(url, json=payload, timeout=timeout)
        return {
            "status_code": response.status_code,
            "response_json": response.text
        }
    except requests.exceptions.RequestException as e:
        return None
    
def post_raw_json(ip: str, port: int, endpoint: str, raw_body: bytes, timeout=10):
    url = f"http://{ip}:{port}{endpoint}"
    headers = {"Content-Type": "application/json"}
    try:
        response = requests.post(url, data=raw_body, headers=headers, timeout=timeout)
        return {
            "status_code": response.status_code,
            "response_json": response.text
        }
    except requests.exceptions.RequestException as e:
        return None


def run():
    """Start the HTTP server and serve requests forever."""
    server = HTTPServer((ISCS_IP, ISCS_PORT), MyHandler)
    print(f"Server running on http://{ISCS_IP}:{ISCS_PORT}")
    server.serve_forever()


run()
