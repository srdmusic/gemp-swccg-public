#!/usr/bin/env python3
"""Download and verify every image used by the local GEMP web clients."""

from __future__ import annotations

import argparse
import fcntl
import hashlib
import json
import os
import re
import shutil
import subprocess
import sys
from concurrent.futures import ThreadPoolExecutor, as_completed
from datetime import datetime, timezone
from pathlib import Path, PurePosixPath
from urllib.parse import urlsplit


CDN_ROOT = "https://res.starwarsccg.org/"
LOCAL_ROOT = "/gemp-swccg/localimage/"
IMAGE_SUFFIXES = {".gif", ".jpg", ".jpeg", ".png", ".svg", ".webp"}
TEXT_SUFFIXES = {".css", ".html", ".java", ".js", ".json"}
REMOTE_URL = re.compile(r"https://res\.starwarsccg\.org/[A-Za-z0-9_./!&%+\-]+")
LOCAL_URL = re.compile(r"/gemp-swccg/localimage/[A-Za-z0-9_./!&%+\-]+")
MANIFEST_NAME = ".offline-card-images.json"
LOCK_NAME = ".offline-card-images.lock"


def image_urls(source_roots: list[Path]) -> list[str]:
    urls: set[str] = set()
    for source_root in source_roots:
        for source in source_root.rglob("*"):
            if not source.is_file() or source.suffix.lower() not in TEXT_SUFFIXES:
                continue
            try:
                text = source.read_text(encoding="utf-8")
            except UnicodeDecodeError:
                continue
            text = "\n".join(
                line for line in text.splitlines() if not line.lstrip().startswith("//")
            )
            urls.update(REMOTE_URL.findall(text))
            for local_url in LOCAL_URL.findall(text):
                urls.add(CDN_ROOT + local_url.removeprefix(LOCAL_ROOT))

    return sorted(url for url in urls if Path(urlsplit(url).path).suffix.lower() in IMAGE_SUFFIXES)


def relative_path(url: str) -> Path:
    parsed = urlsplit(url)
    if parsed.scheme != "https" or parsed.netloc != "res.starwarsccg.org":
        raise ValueError(f"Unexpected image host: {url}")
    relative = PurePosixPath(parsed.path.lstrip("/"))
    if not relative.parts or any(part in {"", ".", ".."} for part in relative.parts):
        raise ValueError(f"Unsafe image path: {url}")
    return Path(*relative.parts)


def valid_signature(path: Path) -> bool:
    if not path.is_file() or path.stat().st_size == 0:
        return False
    with path.open("rb") as handle:
        head = handle.read(4096)
    return (
        head.startswith((b"GIF87a", b"GIF89a"))
        or head.startswith(b"\x89PNG\r\n\x1a\n")
        or head.startswith(b"\xff\xd8\xff")
        or head.startswith(b"BM")
        or (head.startswith(b"RIFF") and head[8:12] == b"WEBP")
        or b"<svg" in head.lower()
    )


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def download_one(
    url: str, asset_root: Path, retries: int, expected: dict | None
) -> tuple[str, int, str | None]:
    destination = asset_root / relative_path(url)
    if valid_signature(destination):
        matches_manifest = expected is None or (
            destination.stat().st_size == expected.get("bytes")
            and sha256(destination) == expected.get("sha256")
        )
        if matches_manifest:
            return "kept", destination.stat().st_size, None

    destination.parent.mkdir(parents=True, exist_ok=True)
    part = destination.with_name(f".{destination.name}.part")
    part.unlink(missing_ok=True)
    command = [
        "curl",
        "--fail",
        "--location",
        "--silent",
        "--show-error",
        "--retry", str(retries),
        "--retry-all-errors",
        "--retry-delay", "2",
        "--connect-timeout", "20",
        "--max-time", "300",
        "--user-agent", "Mozilla/5.0 (compatible; GEMP-Offline-Images/1.0)",
        "--referer", "https://gemp.starwarsccg.org/",
        "--header", "Accept: image/gif,image/png,image/jpeg,image/svg+xml,image/*,*/*",
        "--output", str(part),
        url,
    ]
    result = subprocess.run(command, stdout=subprocess.DEVNULL, stderr=subprocess.PIPE, text=True)
    if result.returncode != 0:
        return "failed", 0, result.stderr.strip() or f"curl exited {result.returncode}"
    if not valid_signature(part):
        part.unlink(missing_ok=True)
        return "failed", 0, "response was not a valid image"
    os.replace(part, destination)
    return "downloaded", destination.stat().st_size, None


def write_manifest(urls: list[str], asset_root: Path) -> Path:
    assets = {}
    total_bytes = 0
    for url in urls:
        path = asset_root / relative_path(url)
        size = path.stat().st_size
        total_bytes += size
        assets[url] = {
            "path": relative_path(url).as_posix(),
            "bytes": size,
            "sha256": sha256(path),
        }

    manifest = {
        "generated_utc": datetime.now(timezone.utc).isoformat(),
        "source": CDN_ROOT,
        "count": len(assets),
        "bytes": total_bytes,
        "assets": assets,
    }
    destination = asset_root / MANIFEST_NAME
    temporary = destination.with_suffix(".tmp")
    temporary.write_text(json.dumps(manifest, indent=2, sort_keys=True) + "\n", encoding="utf-8")
    os.replace(temporary, destination)
    return destination


def recorded_assets(asset_root: Path) -> dict:
    manifest_path = asset_root / MANIFEST_NAME
    if not manifest_path.is_file():
        return {}
    try:
        return json.loads(manifest_path.read_text(encoding="utf-8")).get("assets", {})
    except (OSError, ValueError):
        return {}


def acquire_download_lock(asset_root: Path):
    asset_root.mkdir(parents=True, exist_ok=True)
    lock = (asset_root / LOCK_NAME).open("a+", encoding="utf-8")
    try:
        fcntl.flock(lock, fcntl.LOCK_EX | fcntl.LOCK_NB)
    except BlockingIOError:
        lock.seek(0)
        owner = lock.read().strip() or "another process"
        lock.close()
        raise RuntimeError(f"image installer already running ({owner})")
    lock.seek(0)
    lock.truncate()
    lock.write(f"pid {os.getpid()}\n")
    lock.flush()
    return lock


def verify(urls: list[str], asset_root: Path) -> tuple[list[str], int]:
    manifest_path = asset_root / MANIFEST_NAME
    if not manifest_path.is_file():
        return [f"missing manifest: {manifest_path}"], 0
    manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
    recorded = manifest.get("assets", {})
    failures = []
    total_bytes = 0
    for url in urls:
        path = asset_root / relative_path(url)
        entry = recorded.get(url)
        if entry is None:
            failures.append(f"not recorded: {url}")
        elif not valid_signature(path):
            failures.append(f"missing or invalid: {path}")
        elif path.stat().st_size != entry.get("bytes") or sha256(path) != entry.get("sha256"):
            failures.append(f"integrity mismatch: {path}")
        else:
            total_bytes += path.stat().st_size
    return failures, total_bytes


def main() -> int:
    repo_root = Path(__file__).resolve().parents[1]
    default_web_root = repo_root / "src/gemp-swccg-async/src/main/web"
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--web-root", type=Path, default=default_web_root)
    parser.add_argument(
        "--java-root",
        action="append",
        type=Path,
        help="Java source root to scan (repeatable)",
    )
    parser.add_argument("--asset-root", type=Path)
    parser.add_argument("--workers", type=int, default=2)
    parser.add_argument("--retries", type=int, default=4)
    parser.add_argument("--verify-only", action="store_true")
    args = parser.parse_args()

    if shutil.which("curl") is None:
        print("curl is required to download the image library", file=sys.stderr)
        return 1

    web_root = args.web_root.resolve()
    java_roots = [root.resolve() for root in args.java_root] if args.java_root else [
        repo_root / "src/gemp-swccg-async/src/main/java",
        repo_root / "src/gemp-swccg-server/src/main/java",
    ]
    asset_root = (args.asset_root or (repo_root / ".local/card-images")).resolve()
    urls = image_urls([web_root, *java_roots])
    print(f"Inventory: {len(urls)} unique images", flush=True)
    if not urls:
        print("No image URLs found", file=sys.stderr)
        return 1

    if args.verify_only:
        failures, total_bytes = verify(urls, asset_root)
        if failures:
            for failure in failures[:20]:
                print(f"ERROR: {failure}", file=sys.stderr)
            if len(failures) > 20:
                print(f"ERROR: {len(failures) - 20} more failures", file=sys.stderr)
            return 1
        print(f"Verified: {len(urls)} images, {total_bytes / (1024 ** 3):.2f} GiB")
        return 0

    try:
        download_lock = acquire_download_lock(asset_root)
    except RuntimeError as error:
        print(str(error), file=sys.stderr)
        return 1

    downloaded = kept = total_bytes = 0
    failures: list[tuple[str, str]] = []
    existing_assets = recorded_assets(asset_root)
    with ThreadPoolExecutor(max_workers=max(1, args.workers)) as pool:
        futures = {
            pool.submit(
                download_one,
                url,
                asset_root,
                max(1, args.retries),
                existing_assets.get(url),
            ): url
            for url in urls
        }
        for completed, future in enumerate(as_completed(futures), start=1):
            url = futures[future]
            status, size, error = future.result()
            if status == "downloaded":
                downloaded += 1
            elif status == "kept":
                kept += 1
            else:
                failures.append((url, error or "unknown error"))
            total_bytes += size
            if completed % 100 == 0 or completed == len(urls):
                print(
                    f"Progress: {completed}/{len(urls)} "
                    f"({downloaded} downloaded, {kept} already valid, {len(failures)} failed)",
                    flush=True,
                )

    if failures:
        for url, error in failures[:20]:
            print(f"ERROR: {url}: {error}", file=sys.stderr)
        if len(failures) > 20:
            print(f"ERROR: {len(failures) - 20} more failures", file=sys.stderr)
        return 1

    manifest_path = write_manifest(urls, asset_root)
    verification_failures, verified_bytes = verify(urls, asset_root)
    if verification_failures:
        for failure in verification_failures[:20]:
            print(f"ERROR: {failure}", file=sys.stderr)
        return 1
    print(
        f"Complete: {len(urls)} images, {verified_bytes / (1024 ** 3):.2f} GiB, "
        f"manifest {manifest_path}",
        flush=True,
    )
    download_lock.close()
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
