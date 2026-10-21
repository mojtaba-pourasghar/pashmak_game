#!/usr/bin/env python3
"""A local content server for development and for the download test.

    python3 content_tools/serve_content.py [--dir dist] [--port 8777]

It serves the packed chapters over plain HTTP on the loopback interface only.
That is why ContentEndpoints.allow_unsigned_content() refuses to accept
unsigned packages from anything but 127.0.0.1 — this server is a stand-in for
a CDN, never a substitute for one.
"""

from __future__ import annotations

import argparse
import functools
import http.server
import socketserver
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent


class QuietHandler(http.server.SimpleHTTPRequestHandler):
    def log_message(self, fmt, *args):  # noqa: A003
        pass


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("--dir", default="dist")
    ap.add_argument("--port", type=int, default=8777)
    args = ap.parse_args()

    serve_dir = Path(args.dir)
    if not serve_dir.is_absolute():
        serve_dir = ROOT / serve_dir
    serve_dir.mkdir(parents=True, exist_ok=True)

    handler = functools.partial(QuietHandler, directory=str(serve_dir))
    socketserver.TCPServer.allow_reuse_address = True
    with socketserver.TCPServer(("127.0.0.1", args.port), handler) as httpd:
        print(f"serving {serve_dir} at http://127.0.0.1:{args.port} (ctrl-c to stop)",
              flush=True)
        httpd.serve_forever()
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
