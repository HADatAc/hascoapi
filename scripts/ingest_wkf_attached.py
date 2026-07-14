#!/usr/bin/env python3
"""Ingest a WKF spreadsheet through HASCOAPI generic MT endpoints.

Workflow implemented:
1) Create DataFile metadata
2) Create WKF metadata linked to the DataFile
3) Upload .xlsx file
4) Trigger generic MT ingestion for wkf
5) Poll ingestion log

Usage examples:
  python3 scripts/ingest_wkf_attached.py \
    --file /path/to/WKF-ASPIRACAO_SECRECOES_PSMR_0001_CTT.xlsx

  python3 scripts/ingest_wkf_attached.py \
    --file /path/to/file.xlsx \
    --base-url http://localhost:9000 \
    --manager-email user@example.org
"""

from __future__ import annotations

import argparse
import json
import mimetypes
import os
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
from pathlib import Path
from typing import Dict, Tuple


def _join_url(base_url: str, path: str) -> str:
    return base_url.rstrip("/") + path


def _http_json(method: str, url: str, payload: Dict | None = None) -> Dict:
    data = None
    headers = {"Accept": "application/json"}
    if payload is not None:
        data = json.dumps(payload).encode("utf-8")
        headers["Content-Type"] = "application/json"

    req = urllib.request.Request(url=url, data=data, method=method, headers=headers)
    try:
        with urllib.request.urlopen(req, timeout=30) as resp:
            body = resp.read().decode("utf-8")
            return json.loads(body) if body else {}
    except urllib.error.HTTPError as err:
        body = err.read().decode("utf-8", errors="replace")
        raise RuntimeError(f"HTTP {err.code} for {url}: {body}") from err
    except urllib.error.URLError as err:
        raise RuntimeError(f"Connection error for {url}: {err}") from err


def _http_multipart_json(url: str, field_name: str, file_path: Path) -> Dict:
    boundary = f"----hascoapi-wkf-{int(time.time() * 1000)}"
    mime_type = mimetypes.guess_type(str(file_path))[0] or "application/octet-stream"

    with open(file_path, "rb") as f:
        file_bytes = f.read()

    preamble = (
        f"--{boundary}\r\n"
        f"Content-Disposition: form-data; name=\"{field_name}\"; filename=\"{file_path.name}\"\r\n"
        f"Content-Type: {mime_type}\r\n\r\n"
    ).encode("utf-8")
    epilogue = f"\r\n--{boundary}--\r\n".encode("utf-8")
    body = preamble + file_bytes + epilogue

    headers = {
        "Content-Type": f"multipart/form-data; boundary={boundary}",
        "Accept": "application/json",
    }

    req = urllib.request.Request(url=url, data=body, method="POST", headers=headers)
    try:
        with urllib.request.urlopen(req, timeout=180) as resp:
            payload = resp.read().decode("utf-8")
            return json.loads(payload) if payload else {}
    except urllib.error.HTTPError as err:
        body = err.read().decode("utf-8", errors="replace")
        raise RuntimeError(f"HTTP {err.code} for {url}: {body}") from err
    except urllib.error.URLError as err:
        raise RuntimeError(f"Connection error for {url}: {err}") from err


def _http_multipart_file(url: str, field_name: str, file_path: Path) -> Dict:
    boundary = f"----hascoapi-wkf-{int(time.time() * 1000)}"
    mime_type = mimetypes.guess_type(str(file_path))[0] or "application/octet-stream"

    with open(file_path, "rb") as f:
        file_bytes = f.read()

    preamble = (
        f"--{boundary}\r\n"
        f"Content-Disposition: form-data; name=\"{field_name}\"; filename=\"{file_path.name}\"\r\n"
        f"Content-Type: {mime_type}\r\n\r\n"
    ).encode("utf-8")
    epilogue = f"\r\n--{boundary}--\r\n".encode("utf-8")
    body = preamble + file_bytes + epilogue

    headers = {
        "Content-Type": f"multipart/form-data; boundary={boundary}",
        "Accept": "application/json",
    }

    req = urllib.request.Request(url=url, data=body, method="POST", headers=headers)
    try:
        with urllib.request.urlopen(req, timeout=120) as resp:
            payload = resp.read().decode("utf-8")
            return json.loads(payload) if payload else {}
    except urllib.error.HTTPError as err:
        body = err.read().decode("utf-8", errors="replace")
        raise RuntimeError(f"HTTP {err.code} for {url}: {body}") from err
    except urllib.error.URLError as err:
        raise RuntimeError(f"Connection error for {url}: {err}") from err


def _encode_json_path_arg(obj: Dict) -> str:
    raw = json.dumps(obj, separators=(",", ":"), ensure_ascii=True)
    return urllib.parse.quote(raw, safe="")


def _encode_path_arg(value: str) -> str:
    return urllib.parse.quote(value, safe="")


def _extract_body_text(api_response: Dict) -> str:
    if not isinstance(api_response, dict):
        return str(api_response)
    body = api_response.get("body", "")
    if isinstance(body, str):
        return body
    return json.dumps(body, ensure_ascii=True)


def ingest_wkf(
    file_path: Path,
    base_url: str,
    kb_prefix: str,
    manager_email: str,
    status_uri: str,
) -> Tuple[str, str]:
    ts = int(time.time() * 1000)
    dfl_uri = f"{kb_prefix.rstrip('/')}/DFL{ts}"
    wkf_uri = f"{kb_prefix.rstrip('/')}/WKF{ts}"

    datafile_payload = {
        "uri": dfl_uri,
        "typeUri": "http://hadatac.org/ont/hasco/DataFile",
        "hascoTypeUri": "http://hadatac.org/ont/hasco/DataFile",
        "label": file_path.name,
        "filename": file_path.name,
        "fileStatus": "UNPROCESSED",
        "hasSIRManagerEmail": manager_email,
        "namedGraph": dfl_uri,
    }

    wkf_payload = {
        "uri": wkf_uri,
        "typeUri": "http://hadatac.org/ont/hasco/WKF",
        "hascoTypeUri": "http://hadatac.org/ont/hasco/WKF",
        "label": file_path.stem,
        "comment": f"WKF created by ingest_wkf_attached.py for {file_path.name}",
        "hasDataFileUri": dfl_uri,
        "hasSIRManagerEmail": manager_email,
        "hasStatus": "DRAFT",
        "hasVersion": "1.0",
        "namedGraph": dfl_uri,
    }

    create_datafile_url = _join_url(
        base_url,
        "/hascoapi/api/datafile/create/" + _encode_json_path_arg(datafile_payload),
    )
    create_wkf_url = _join_url(
        base_url,
        "/hascoapi/api/wkf/create/" + _encode_json_path_arg(wkf_payload),
    )

    upload_url = _join_url(
        base_url,
        "/hascoapi/api/uploadFile/"
        + _encode_path_arg(dfl_uri)
        + "/"
        + _encode_path_arg(file_path.name),
    )

    ingest_url = _join_url(
        base_url,
        "/hascoapi/api/ingest/"
        + _encode_path_arg(status_uri)
        + "/wkf/"
        + _encode_path_arg(wkf_uri),
    )

    log_url = _join_url(
        base_url,
        "/hascoapi/api/ingestion/" + _encode_path_arg(dfl_uri) + "/log",
    )

    print(f"[1/5] Creating DataFile: {dfl_uri}")
    resp1 = _http_json("POST", create_datafile_url)
    if not resp1.get("isSuccessful", False):
        raise RuntimeError("DataFile creation failed: " + _extract_body_text(resp1))

    print(f"[2/5] Creating WKF: {wkf_uri}")
    resp2 = _http_json("POST", create_wkf_url)
    if not resp2.get("isSuccessful", False):
        raise RuntimeError("WKF creation failed: " + _extract_body_text(resp2))

    print(f"[3/5] Uploading file: {file_path}")
    resp3 = _http_multipart_file(upload_url, "file", file_path)

    if resp3.get("isSuccessful", False):
        print("[4/5] Triggering ingestion")
        resp4 = _http_json("POST", ingest_url, payload={})
        if not resp4.get("isSuccessful", False):
            raise RuntimeError("Ingest trigger failed: " + _extract_body_text(resp4))
    else:
        # In some deployments uploadFile multipart parsing can fail while ingest() multipart succeeds.
        print("[4/5] uploadFile failed, falling back to direct multipart ingest")
        print("      uploadFile response:", _extract_body_text(resp3))
        resp4 = _http_multipart_json(ingest_url, "file", file_path)
        if not resp4.get("isSuccessful", False):
            raise RuntimeError("Direct multipart ingest failed: " + _extract_body_text(resp4))

    print("[5/5] Polling ingestion log")
    latest_log_text = ""
    for _ in range(12):
        time.sleep(2)
        resp_log = _http_json("GET", log_url)
        latest_log_text = _extract_body_text(resp_log)
        if "PROCESSED" in latest_log_text or "ERROR" in latest_log_text or "Exception" in latest_log_text:
            break

    print("\n===== Summary =====")
    print(f"WKF URI: {wkf_uri}")
    print(f"DataFile URI: {dfl_uri}")
    print("Ingestion endpoint response:", _extract_body_text(resp4))
    print("\nLatest log excerpt:")
    print(latest_log_text[:4000])

    return wkf_uri, dfl_uri


def _resolve_default_file(arg_path: str | None) -> Path:
    if arg_path:
        path = Path(arg_path).expanduser().resolve()
        if not path.exists():
            raise FileNotFoundError(f"WKF file does not exist: {path}")
        return path

    candidates = [
        Path.cwd() / "WKF-ASPIRACAO_SECRECOES_PSMR_0001_CTT.xlsx",
        Path("/Users/pp3223/git/wkf/WKF-ASPIRACAO_SECRECOES_PSMR_0001_CTT.xlsx"),
        Path("/Users/pp3223/git/wkf/wkf/WKF-ASPIRACAO_SECRECOES_PSMR_0001_CTT.xlsx"),
        Path("/Users/pp3223/Downloads/WKF-ASPIRACAO_SECRECOES_PSMR_0001_CTT.xlsx"),
    ]
    for candidate in candidates:
        if candidate.exists():
            return candidate.resolve()

    raise FileNotFoundError(
        "Could not auto-locate WKF file. Pass --file /absolute/path/to/WKF-ASPIRACAO_SECRECOES_PSMR_0001_CTT.xlsx"
    )


def main() -> int:
    parser = argparse.ArgumentParser(description="Ingest a WKF spreadsheet via HASCOAPI")
    parser.add_argument("--file", help="Absolute path to WKF .xlsx file")
    parser.add_argument("--base-url", default="http://localhost:9000", help="HASCOAPI base URL")
    parser.add_argument(
        "--kb-prefix",
        default="https://hadatac.org/ont/hadatac#",
        help="Knowledge graph base prefix for generated WKF/DFL URIs",
    )
    parser.add_argument("--manager-email", default="pp3223@example.org", help="Manager email saved in metadata")
    parser.add_argument(
        "--status-uri",
        default="http://hadatac.org/ont/vstoi#Draft",
        help="Status URI used by ingest endpoint",
    )
    args = parser.parse_args()

    try:
        file_path = _resolve_default_file(args.file)
        print(f"Using WKF file: {file_path}")
        ingest_wkf(
            file_path=file_path,
            base_url=args.base_url,
            kb_prefix=args.kb_prefix,
            manager_email=args.manager_email,
            status_uri=args.status_uri,
        )
        return 0
    except Exception as exc:
        print(f"ERROR: {exc}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
