#!/usr/bin/env python3
"""
Bridge between an ESP32 + DHT11 sensor (Serial only, no WiFi/HTTP code) and the
backend's environmental telemetry endpoint:

    POST /api/v1/machines/{machineId}/environment

Reads the sketch's plain-text Serial output:
    Temperature: 28.50 \xc2\xb0C
    Humidity: 62.00 %

and forwards each complete (temperature, humidity) pair to an EXISTING
machine (MACHINE_ID must point at a machine that already exists in the
backend - this script never creates one).

Config (env vars, with CLI overrides):
    SERIAL_PORT     e.g. COM5 (Windows) or /dev/ttyUSB0 (Linux)
    BAUD_RATE       default 115200 (matches the sketch)
    BACKEND_URL     default http://localhost:8080
    MACHINE_ID      required - id of an existing Machine
    BRIDGE_USERNAME required - backend account used to authenticate posts
    BRIDGE_PASSWORD required

Usage:
    pip install pyserial requests
    MACHINE_ID=5 BRIDGE_USERNAME=... BRIDGE_PASSWORD=... SERIAL_PORT=COM5 \
        python arduino_bridge.py
"""

from __future__ import annotations

import argparse
import os
import re
import sys
import time

import requests
import serial

TEMP_RE = re.compile(r"Temperature:\s*([-\d.]+)")
HUMIDITY_RE = re.compile(r"Humidity:\s*([-\d.]+)")

RECONNECT_DELAY_SECONDS = 5
LOGIN_RETRY_DELAY_SECONDS = 5


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--port", default=os.environ.get("SERIAL_PORT"), help="Serial port, e.g. COM5")
    parser.add_argument("--baud", type=int, default=int(os.environ.get("BAUD_RATE", "115200")))
    parser.add_argument("--backend-url", default=os.environ.get("BACKEND_URL", "http://localhost:8080"))
    parser.add_argument("--machine-id", default=os.environ.get("MACHINE_ID"), help="Existing machine id")
    parser.add_argument("--username", default=os.environ.get("BRIDGE_USERNAME"))
    parser.add_argument("--password", default=os.environ.get("BRIDGE_PASSWORD"))
    args = parser.parse_args()

    missing = [name for name, val in [
        ("--port/SERIAL_PORT", args.port),
        ("--machine-id/MACHINE_ID", args.machine_id),
        ("--username/BRIDGE_USERNAME", args.username),
        ("--password/BRIDGE_PASSWORD", args.password),
    ] if not val]
    if missing:
        parser.error(f"Missing required config: {', '.join(missing)}")

    return args


def login(backend_url: str, username: str, password: str) -> str:
    while True:
        try:
            resp = requests.post(
                f"{backend_url}/api/v1/auth/login",
                json={"username": username, "password": password},
                timeout=10,
            )
            resp.raise_for_status()
            token = resp.json()["token"]
            print("[bridge] authenticated")
            return token
        except Exception as e:
            print(f"[bridge] login failed ({e}), retrying in {LOGIN_RETRY_DELAY_SECONDS}s")
            time.sleep(LOGIN_RETRY_DELAY_SECONDS)


def post_reading(backend_url: str, machine_id: str, token: str, temperature: float, humidity: float) -> bool:
    """Returns False if the token looks expired (caller should re-login)."""
    try:
        resp = requests.post(
            f"{backend_url}/api/v1/machines/{machine_id}/environment",
            json={"temperature": temperature, "humidity": humidity},
            headers={"Authorization": f"Bearer {token}"},
            timeout=10,
        )
        if resp.status_code == 401:
            print("[bridge] token expired, re-authenticating")
            return False
        resp.raise_for_status()
        print(f"[bridge] posted temperature={temperature} humidity={humidity} -> {resp.json().get('riskLevel')}")
        return True
    except Exception as e:
        print(f"[bridge] failed to post reading: {e}")
        return True  # don't force a re-login on a transient network error


def read_serial_loop(args: argparse.Namespace) -> None:
    token = login(args.backend_url, args.username, args.password)

    while True:
        try:
            with serial.Serial(args.port, args.baud, timeout=5) as ser:
                print(f"[bridge] connected to {args.port} @ {args.baud} baud")
                pending_temp = None

                while True:
                    line = ser.readline().decode(errors="replace").strip()
                    if not line:
                        continue

                    temp_match = TEMP_RE.search(line)
                    humidity_match = HUMIDITY_RE.search(line)

                    if temp_match:
                        pending_temp = float(temp_match.group(1))
                    elif humidity_match and pending_temp is not None:
                        humidity = float(humidity_match.group(1))
                        if not post_reading(args.backend_url, args.machine_id, token, pending_temp, humidity):
                            token = login(args.backend_url, args.username, args.password)
                            post_reading(args.backend_url, args.machine_id, token, pending_temp, humidity)
                        pending_temp = None

        except serial.SerialException as e:
            print(f"[bridge] serial connection lost ({e}), retrying in {RECONNECT_DELAY_SECONDS}s")
            time.sleep(RECONNECT_DELAY_SECONDS)


if __name__ == "__main__":
    try:
        read_serial_loop(parse_args())
    except KeyboardInterrupt:
        sys.exit(0)
