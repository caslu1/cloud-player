#!/usr/bin/env python3
"""Adiciona uma playlist M3U a um dispositivo no funplays.app.

Uso:
    python3 funplays_add.py --mac aa:bb:cc:dd:ee:ff --key 123456 \
        --url "http://servidor/get.php?username=...&password=...&type=m3u_plus" \
        [--name "Minha lista"] [--epg URL] [--list]

Fluxo (o mesmo que o site faz):
  1. GET  api/validate_mac      -> confere se o MAC existe
  2. POST api/login_by_mac      -> {mac, key} devolve um token
  3. POST api/playlist_with_mac -> envia nome, MAC e URL com o token
  4. GET  api/device            -> lista as playlists do aparelho

Usa apenas a biblioteca padrao do Python 3.
"""
import argparse
import json
import sys
import urllib.error
import urllib.parse
import urllib.request
import uuid

API = "https://api.funplays.app/api/"
UA = "Mozilla/5.0 (cloud-player funplays tool)"


class FunplaysError(Exception):
    pass


def _request(method, path, token=None, json_body=None, form=None):
    headers = {"User-Agent": UA, "Accept": "application/json"}
    data = None
    if token:
        headers["Authorization"] = "Bearer " + token
    if json_body is not None:
        data = json.dumps(json_body).encode()
        headers["Content-Type"] = "application/json"
    elif form is not None:
        boundary = uuid.uuid4().hex
        parts = []
        for k, v in form.items():
            parts.append(
                f'--{boundary}\r\nContent-Disposition: form-data; name="{k}"\r\n\r\n{v}\r\n'
            )
        parts.append(f"--{boundary}--\r\n")
        data = "".join(parts).encode()
        headers["Content-Type"] = "multipart/form-data; boundary=" + boundary
    req = urllib.request.Request(API + path, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req, timeout=30) as r:
            body = r.read().decode()
    except urllib.error.HTTPError as e:
        body = e.read().decode(errors="replace")
    try:
        payload = json.loads(body)
    except ValueError:
        raise FunplaysError(f"Resposta inesperada do servidor: {body[:200]}")
    if payload.get("error"):
        raise FunplaysError(str(payload.get("message")))
    return payload.get("message")


def normalize_mac(mac):
    mac = mac.strip().lower().replace("-", ":")
    if ":" not in mac and len(mac) == 12:
        mac = ":".join(mac[i:i + 2] for i in range(0, 12, 2))
    return mac


def validate_mac(mac):
    return _request("GET", "validate_mac?mac=" + urllib.parse.quote(mac))


def login(mac, key):
    return _request("POST", "login_by_mac", json_body={"mac": mac, "key": key.strip()})


def list_playlists(token):
    msg = _request("GET", "device", token=token)
    return msg.get("playlists", []) if isinstance(msg, dict) else msg


def add_playlist(token, mac, url, name, epg_url=None):
    form = {"name": name, "mac": mac, "url": url.strip(), "is_protected": "false"}
    if epg_url:
        form["epg_url"] = epg_url
    return _request("POST", "playlist_with_mac", token=token, form=form)


def main():
    p = argparse.ArgumentParser(description="Adiciona playlist M3U no funplays.app")
    p.add_argument("--mac", required=True)
    p.add_argument("--key", required=True, help="Device Key mostrada na TV")
    p.add_argument("--url", help="Link M3U")
    p.add_argument("--name", default="Playlist")
    p.add_argument("--epg", help="URL do EPG (opcional)")
    p.add_argument("--list", action="store_true", help="So listar as playlists")
    a = p.parse_args()

    mac = normalize_mac(a.mac)
    try:
        info = validate_mac(mac)
        print(f"Dispositivo encontrado: {info.get('model')} (id {info.get('id')})")
        token = login(mac, a.key)
        if a.url and not a.list:
            pl = add_playlist(token, mac, a.url, a.name, a.epg)
            print(f"OK: playlist '{pl.get('name')}' adicionada (id {pl.get('id')})")
        print("Playlists no aparelho:")
        for pl in list_playlists(token):
            print(f"  - {pl.get('name')} (id {pl.get('id')})")
    except FunplaysError as e:
        print("ERRO:", e, file=sys.stderr)
        sys.exit(1)


if __name__ == "__main__":
    main()
