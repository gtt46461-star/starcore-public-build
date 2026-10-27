"""Import the exact user-supplied R25 archive; never execute code from the archive.
GitHub Actions entry: python tools/r25_upload/import_full_archive.py
Offline validation: python import_full_archive.py --archive FILE --verify-only
"""
from __future__ import annotations
import argparse, hashlib, json, os, shutil, stat, subprocess, tempfile, zipfile
from pathlib import Path, PurePosixPath

REPO = "gtt46461-star/starcore-public-build"
TAG = "r25-full-20261026"
EXPECTED_SHA256 = "78e1ff7e24ea8e9cf36e49cd781c690f32695cd5e0a85e866408c78977e6fb98"
EXPECTED_BYTES = 133671781
INNER_SHA256 = "082504220d21d6bcc56722a50d74bd9bd6132b7136c0fc9c70d9e611bb4a4588"
PAYLOAD = "STARCORE_R25_20261026"
DEST = "construction/" + PAYLOAD


def sha256(path: Path) -> str:
    h = hashlib.sha256()
    with path.open("rb") as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            h.update(chunk)
    return h.hexdigest()


def run(args: list[str], cwd: Path | None = None) -> str:
    result = subprocess.run(args, cwd=cwd, check=True, stdout=subprocess.PIPE,
                            stderr=subprocess.PIPE, text=True)
    return result.stdout.strip()


def inspect_archive(archive: Path) -> tuple[dict[str, bytes], dict]:
    if archive.stat().st_size != EXPECTED_BYTES or sha256(archive) != EXPECTED_SHA256:
        raise ValueError("ARCHIVE_SIZE_OR_SHA256_MISMATCH")
    files: dict[str, bytes] = {}
    originals = []
    with zipfile.ZipFile(archive) as z:
        infos = [i for i in z.infolist() if not i.is_dir()]
        if len(infos) != 151 or len({i.filename for i in infos}) != 151:
            raise ValueError("ARCHIVE_ENTRY_COUNT_OR_DUPLICATES")
        for i in infos:
            p = PurePosixPath(i.filename)
            if p.is_absolute() or any(x in {"..", ".git"} for x in p.parts):
                raise ValueError("UNSAFE_ARCHIVE_PATH")
            if stat.S_ISLNK(i.external_attr >> 16) or i.flag_bits & 1:
                raise ValueError("SYMLINK_OR_ENCRYPTED_ENTRY")
            parts = p.parts
            if len(parts) >= 3 and parts[1] == PAYLOAD:
                rel = "/".join(parts[2:])
                if rel in files or i.file_size >= 100 * 1024 * 1024:
                    raise ValueError("DUPLICATE_OR_OVERSIZE_SOURCE_FILE")
                files[rel] = z.read(i)
            elif len(parts) == 3 and parts[1] == "ORIGINAL_UNCHANGED":
                h = hashlib.sha256()
                with z.open(i) as stream:
                    for chunk in iter(lambda: stream.read(1024 * 1024), b""):
                        h.update(chunk)
                if h.hexdigest() != INNER_SHA256:
                    raise ValueError("ORIGINAL_ARCHIVE_HASH_MISMATCH")
                originals.append({"archive_member": i.filename, "bytes": i.file_size,
                                  "sha256": h.hexdigest()})
            else:
                raise ValueError("UNEXPECTED_ARCHIVE_ENTRY")
    if len(files) != 150 or len(originals) != 1:
        raise ValueError("PAYLOAD_COUNT_MISMATCH")
    manifest = json.loads(files["MANIFEST_SHA256.json"])
    expected = {r["path"]: r for r in manifest["files"]}
    if len(expected) != 149 or set(expected) != set(files) - {"MANIFEST_SHA256.json"}:
        raise ValueError("MANIFEST_COVERAGE_MISMATCH")
    for name, record in expected.items():
        data = files[name]
        if len(data) != record["bytes"] or hashlib.sha256(data).hexdigest() != record["sha256"]:
            raise ValueError("MANIFEST_FILE_MISMATCH: " + name)
    return files, {"archive_bytes": EXPECTED_BYTES, "archive_sha256": EXPECTED_SHA256,
                   "payload_files_verified": len(files), "original_archives": originals}


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--archive", type=Path)
    parser.add_argument("--verify-only", action="store_true")
    args = parser.parse_args()
    with tempfile.TemporaryDirectory(prefix="r25_full_import_") as temp:
        temp_path = Path(temp)
        asset = None
        if args.archive:
            archive = args.archive
        else:
            if os.environ.get("GITHUB_REPOSITORY") != REPO:
                raise ValueError("WRONG_REPOSITORY")
            if os.environ.get("RELEASE_TAG") != TAG:
                raise ValueError("WRONG_RELEASE_TAG")
            release = json.loads(run(["gh", "api", f"repos/{REPO}/releases/tags/{TAG}"]))
            candidates = [a for a in release["assets"] if a["size"] == EXPECTED_BYTES
                          and a["name"].lower().endswith(".zip")]
            if len(candidates) != 1:
                raise ValueError("UPLOAD_EXACT_FULL_ZIP_TO_RELEASE_BEFORE_RUNNING")
            asset = candidates[0]
            if Path(asset["name"]).name != asset["name"] or any(c in asset["name"] for c in "*?[]\\"):
                raise ValueError("UNSAFE_ASSET_FILENAME")
            run(["gh", "release", "download", TAG, "--repo", REPO,
                 "--pattern", asset["name"], "--dir", str(temp_path)])
            archive = temp_path / asset["name"]
        files, verification = inspect_archive(archive)
        if args.verify_only:
            print(json.dumps({"status": "LOCAL_ARCHIVE_VERIFIED_NOT_UPLOADED", **verification},
                             ensure_ascii=False, indent=2))
            return
        if asset is None:
            raise ValueError("LOCAL_MODE_IS_VERIFY_ONLY; UPLOAD_TO_RELEASE_FOR_IMPORT")
        root = Path(run(["git", "rev-parse", "--show-toplevel"])).resolve()
        if run(["git", "status", "--porcelain"], root):
            raise ValueError("WORKTREE_NOT_CLEAN")
        destination = root / DEST
        # Do not follow any existing repository symlink, even within the destination.
        for relative in [DEST] + [DEST + "/" + x for x in files]:
            current = root
            for part in PurePosixPath(relative).parts:
                current /= part
                if current.is_symlink():
                    raise ValueError("EXISTING_SYMLINK: " + relative)
        for relative, content in files.items():
            target = destination / relative
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_bytes(content)
        # Preserve the 150 original payload files byte-for-byte, including their line endings.
        (destination / ".gitattributes").write_text("* -text\n", encoding="utf-8")
        status = {"scope": "150_ORIGINAL_FILES_IN_GIT_PLUS_UNCHANGED_FULL_ZIP_IN_RELEASE",
                  "repository": REPO, "branch": "main", "release_tag": TAG,
                  "complete_archive_uploaded": True, "original_files_uploaded": 150,
                  "archive_asset_id": asset["id"], "archive_download_url": asset["browser_download_url"],
                  "production_app_source_changed": False, **verification}
        (destination / "UPLOAD_STATUS.json").write_text(json.dumps(status, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
        (destination / "ORIGINAL_ARCHIVE_REFERENCE.json").write_text(json.dumps({
            "full_archive_download_url": asset["browser_download_url"],
            "full_archive_sha256": EXPECTED_SHA256,
            "original_zip_preserved_inside_full_archive": verification["original_archives"][0]
        }, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
        (destination / "README.md").write_text(
            "# R25 完整方案入库\n\n"
            "本目录包含完整保全包中的150个原始施工文件，已按原清单逐文件验证，保留原字节。\n\n"
            "完整ZIP未重新压缩，保存在本仓库Release；其中包含原始130,956,719字节的《10.26方案.zip》。"
            "大ZIP不作为普通Git文件提交，避免100 MiB单文件限制。\n\n"
            f"[下载完整原包]({asset['browser_download_url']})\n\n"
            "施工从[CODEX_START.md](CODEX_START.md)开始。旧README_FIRST及delivery_status保持原包历史记录，"
            "当前上传范围以UPLOAD_STATUS.json为准。此次只导入方案，不修改App源码，不构建APK。\n",
            encoding="utf-8")
        run(["git", "config", "core.autocrlf", "false"], root)
        run(["git", "add", "--force", "--", DEST], root)
        staged = [x for x in run(["git", "diff", "--cached", "--name-only", "-z"], root).split("\0") if x]
        if any(not name.startswith(DEST + "/") for name in staged):
            raise ValueError("OUT_OF_SCOPE_STAGED_CHANGE")
        for relative, data in files.items():
            expected_blob = hashlib.sha1(b"blob " + str(len(data)).encode() + b"\0" + data).hexdigest()
            if run(["git", "rev-parse", ":" + DEST + "/" + relative], root) != expected_blob:
                raise ValueError("GIT_INDEX_BYTES_MISMATCH: " + relative)
        if staged:
            run(["git", "config", "user.name", "github-actions[bot]"], root)
            run(["git", "config", "user.email", "41898282+github-actions[bot]@users.noreply.github.com"], root)
            run(["git", "commit", "-m", "docs: import complete verified R25 package (150 files) [skip ci]"], root)
            run(["git", "push", "origin", "HEAD:main"], root)  # Never force-push.
        head = run(["git", "rev-parse", "HEAD"], root)
        remote = run(["git", "ls-remote", "origin", "refs/heads/main"], root).split()[0]
        if remote != head:
            raise ValueError("REMOTE_HEAD_CHANGED; VERIFY_BEFORE_REPORTING_SUCCESS")
        receipt = {"status": "REMOTE_MAIN_VERIFIED", "commit": head, **status}
        print(json.dumps(receipt, ensure_ascii=False, indent=2))
        summary = os.environ.get("GITHUB_STEP_SUMMARY")
        if summary:
            with open(summary, "a", encoding="utf-8") as stream:
                stream.write("## R25 complete import verified\n\n```json\n" + json.dumps(receipt, ensure_ascii=False, indent=2) + "\n```\n")


if __name__ == "__main__":
    main()
