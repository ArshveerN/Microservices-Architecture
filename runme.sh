#!/usr/bin/env bash

set -e  # stop on first error

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
SRC_DIR="$ROOT_DIR/src"
BIN_DIR="$ROOT_DIR/compiled"
CONFIG="$ROOT_DIR/config.json"

USER_PKG="UserService"
PRODUCT_PKG="ProductService"
ORDER_PKG="OrderService"

ISCS_PY="$SRC_DIR/ISCS/ISCS.py"
WORKLOAD_PARSER="$ROOT_DIR/src/WorkloadParser.py"

mkdir -p "$BIN_DIR"

compile() {
    echo "[INFO] Compiling Java services..."

    rm -rf "$BIN_DIR"/*
    mkdir -p "$BIN_DIR"

    javac -d "$BIN_DIR" "$SRC_DIR/$USER_PKG"/*.java
    javac -d "$BIN_DIR" "$SRC_DIR/$PRODUCT_PKG"/*.java
    javac -d "$BIN_DIR" "$SRC_DIR/$ORDER_PKG"/*.java

    echo "[INFO] Compilation successful."
}

start_user() {
    echo "[INFO] Starting UserService..."
    java -cp "$BIN_DIR" UserService.UserServer "$CONFIG"
}

start_product() {
    echo "[INFO] Starting ProductService..."
    java -cp "$BIN_DIR" ProductService.ProductServer "$CONFIG"
}

start_order() {
    echo "[INFO] Starting OrderService..."
    java -cp "$BIN_DIR" OrderService.OrderServer "$CONFIG"
}

start_iscs() {
    echo "[INFO] Starting ISCS..."
    python3 "$ISCS_PY" "$CONFIG"
}

start_workload() {
    if [ -z "$2" ]; then
        echo "[ERROR] Missing workload file"
        exit 1
    fi

    echo "[INFO] Running workload parser on $2"
    python3 "$WORKLOAD_PARSER" "$2"
}

case "$1" in
    -c)
        compile
        ;;
    -u)
        start_user
        ;;
    -p)
        start_product
        ;;
    -o)
        start_order
        ;;
    -i)
        start_iscs
        ;;
    -w)
        start_workload "$@"
        ;;
    *)
        echo "Usage:"
        echo "  ./runme.sh -c              Compile all services"
        echo "  ./runme.sh -u              Start UserService"
        echo "  ./runme.sh -p              Start ProductService"
        echo "  ./runme.sh -i              Start ISCS"
        echo "  ./runme.sh -o              Start OrderService"
        echo "  ./runme.sh -w workload.txt Run workload parser"
        exit 1
        ;;
esac
