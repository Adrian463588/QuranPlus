"""Build a verified hadith embedding index.

This command is intentionally fail-closed. It refuses the supplied reference
until an external approval manifest marks every collection complete, graded,
licensed, and checksum-matched. No output is written on a blocked run.

The vector table uses sqlite-vec's ``vec0`` format and all embeddings must be
384-dimensional ONNX output from the supplied tokenizer/model pair.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import os
import re
import sqlite3
import unicodedata
from pathlib import Path
from typing import Iterable


EXPECTED_COLLECTIONS = 17
EMBEDDING_DIMENSION = 384
CHUNK_TOKEN_COUNT = 512
CHUNK_OVERLAP = 50
MODEL_SEQUENCE_LENGTH = 512
TOKEN_PATTERN = re.compile(r"\[[^\]]+\]|[^\s\[\]]+")


class BlockedCorpus(RuntimeError):
    pass


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest()


def load_approval(path: Path) -> dict[str, dict[str, object]]:
    document = json.loads(path.read_text(encoding="utf-8"))
    collections = document.get("collections")
    if not isinstance(collections, list) or len(collections) != EXPECTED_COLLECTIONS:
        raise BlockedCorpus("Approval manifest must contain exactly 17 collections")
    result: dict[str, dict[str, object]] = {}
    for item in collections:
        collection_id = str(item.get("collection_id", ""))
        if not collection_id or collection_id in result:
            raise BlockedCorpus("Approval manifest contains an invalid or duplicate collection id")
        result[collection_id] = item
    return result


def load_vocab(path: Path) -> dict[str, int]:
    if not path.is_file():
        raise BlockedCorpus(f"Tokenizer vocabulary is unavailable: {path}")
    vocabulary = {
        token: index
        for index, token in enumerate(path.read_text(encoding="utf-8").splitlines())
    }
    for required in ("[CLS]", "[SEP]", "[UNK]"):
        if required not in vocabulary:
            raise BlockedCorpus(f"Tokenizer vocabulary is missing {required}")
    return vocabulary


def word_piece(word: str, vocabulary: dict[str, int]) -> list[str]:
    if not word:
        return []
    pieces: list[str] = []
    start = 0
    while start < len(word):
        end = len(word)
        match: str | None = None
        while start < end:
            candidate = word[start:end] if start == 0 else "##" + word[start:end]
            if candidate in vocabulary:
                match = candidate
                break
            end -= 1
        if match is None:
            return ["[UNK]"]
        pieces.append(match)
        start = end
    return pieces


def split_word(word: str) -> list[str]:
    result: list[str] = []
    current: list[str] = []
    for char in word.lower():
        if char.isspace() or unicodedata.category(char).startswith("P"):
            if current:
                result.append("".join(current))
                current.clear()
            if not char.isspace():
                result.append(char)
        else:
            current.append(char)
    if current:
        result.append("".join(current))
    return result


def tokenized_words(text: str, vocabulary: dict[str, int]) -> list[tuple[str, list[str]]]:
    result: list[tuple[str, list[str]]] = []
    for source_word in text.split():
        pieces: list[str] = []
        for word in split_word(source_word):
            pieces.extend(word_piece(word, vocabulary))
        if pieces:
            result.append((source_word, pieces))
    return result


def chunks(text: str, vocabulary: dict[str, int]) -> Iterable[tuple[int, str, list[str]]]:
    words = tokenized_words(text, vocabulary)
    start = 0
    chunk_index = 0
    while start < len(words):
        end = start
        piece_count = 0
        while end < len(words):
            next_count = len(words[end][1])
            if piece_count + next_count > CHUNK_TOKEN_COUNT - 2 and end > start:
                break
            if piece_count + next_count > CHUNK_TOKEN_COUNT - 2:
                raise BlockedCorpus("A single hadith token exceeds the verified 512-token model window")
            piece_count += next_count
            end += 1
        selected = words[start:end]
        token_list = [piece for _, pieces in selected for piece in pieces]
        yield chunk_index, " ".join(word for word, _ in selected), token_list
        chunk_index += 1
        if end == len(words):
            break
        overlap = 0
        next_start = end
        while next_start > start and overlap + len(words[next_start - 1][1]) <= CHUNK_OVERLAP:
            next_start -= 1
            overlap += len(words[next_start][1])
        start = next_start if next_start < end else end


def encode(tokens: list[str], vocabulary: dict[str, int]) -> tuple[list[int], list[int]]:
    content = tokens[: MODEL_SEQUENCE_LENGTH - 2]
    ids = [vocabulary["[CLS]"]]
    ids.extend(vocabulary.get(token, vocabulary["[UNK]"]) for token in content)
    ids.append(vocabulary["[SEP]"])
    attention = [1] * len(ids)
    while len(ids) < MODEL_SEQUENCE_LENGTH:
        ids.append(vocabulary.get("[PAD]", 0))
        attention.append(0)
    return ids, attention


def embed(session, numpy, tokens: list[str], vocabulary: dict[str, int]) -> list[float]:
    ids, attention = encode(tokens, vocabulary)
    inputs = {
        "input_ids": numpy.asarray([ids], dtype=numpy.int64),
        "attention_mask": numpy.asarray([attention], dtype=numpy.int64),
        "token_type_ids": numpy.zeros((1, MODEL_SEQUENCE_LENGTH), dtype=numpy.int64),
    }
    output = session.run(None, inputs)[0]
    rows = output[0]
    active = rows[: sum(attention)]
    if active.shape[-1] != EMBEDDING_DIMENSION:
        raise BlockedCorpus(f"ONNX output dimension is {active.shape[-1]}, expected {EMBEDDING_DIMENSION}")
    pooled = active.mean(axis=0)
    norm = float(numpy.linalg.norm(pooled))
    if not norm or not numpy.isfinite(norm):
        raise BlockedCorpus("ONNX embedding is not finite")
    return (pooled / norm).astype(numpy.float32).tolist()


def verify_sources(root: Path, approval: dict[str, dict[str, object]]) -> list[tuple[str, dict, dict]]:
    files = sorted((root / "db" / "by_book").glob("**/*.json"))
    if len(files) != EXPECTED_COLLECTIONS:
        raise BlockedCorpus(f"Expected {EXPECTED_COLLECTIONS} source collections, found {len(files)}")
    result = []
    for path in files:
        document = json.loads(path.read_text(encoding="utf-8"))
        collection_id = path.stem
        manifest = approval.get(collection_id)
        if manifest is None:
            raise BlockedCorpus(f"Approval manifest is missing {collection_id}")
        if (
            manifest.get("source_sha256") != sha256(path)
            or manifest.get("license_status") != "verified"
            or manifest.get("grade_status") != "verified"
            or manifest.get("is_complete") is not True
            or manifest.get("bundle_allowed") is not True
        ):
            raise BlockedCorpus(f"Collection is not approved for production indexing: {collection_id}")
        result.append((collection_id, document, manifest))
    return result


def build(args: argparse.Namespace) -> dict[str, object]:
    try:
        import numpy
        import onnxruntime
        import sqlite_vec
    except ImportError as error:
        raise BlockedCorpus(f"Verified indexing dependencies are unavailable: {error}") from error

    source_root = args.source_root.resolve()
    approval = load_approval(args.approved_manifest.resolve())
    sources = verify_sources(source_root, approval)
    vocab = load_vocab(args.vocab.resolve())
    if args.model_sha256 and sha256(args.model.resolve()) != args.model_sha256.lower():
        raise BlockedCorpus("ONNX model SHA-256 does not match the supplied manifest")
    session = onnxruntime.InferenceSession(str(args.model.resolve()), providers=["CPUExecutionProvider"])
    input_names = {item.name for item in session.get_inputs()}
    required_inputs = {"input_ids", "attention_mask", "token_type_ids"}
    if not required_inputs.issubset(input_names):
        raise BlockedCorpus(f"ONNX inputs are missing: {sorted(required_inputs - input_names)}")

    output = args.output.resolve()
    output.parent.mkdir(parents=True, exist_ok=True)
    temporary = output.with_name(output.name + ".tmp")
    if temporary.exists():
        temporary.unlink()
    total_chunks = 0
    try:
        with sqlite3.connect(temporary) as database:
            database.enable_load_extension(True)
            sqlite_vec.load(database)
            database.enable_load_extension(False)
            version = database.execute("SELECT vec_version()").fetchone()[0]
            database.execute("CREATE TABLE index_manifest (key TEXT PRIMARY KEY, value TEXT NOT NULL)")
            database.execute(
                """
                CREATE VIRTUAL TABLE hadith_vectors USING vec0(
                    embedding float[384],
                    collection_id TEXT,
                    hadith_id TEXT,
                    chunk_index INTEGER,
                    source_revision TEXT,
                    source_sha256 TEXT,
                    +text TEXT,
                    +reference TEXT
                )
                """
            )
            for collection_id, document, manifest in sources:
                for record in document.get("hadiths", []):
                    english = record.get("english") or {}
                    text = f"{record.get('arabic', '')}\n{english.get('narrator', '')}\n{english.get('text', '')}".strip()
                    if not text or not record.get("id") or not record.get("chapterId"):
                        raise BlockedCorpus(f"Incomplete hadith record: {collection_id}:{record.get('id')}")
                    reference = f"{collection_id}:{record.get('idInBook')}"
                    for chunk_index, chunk_text, token_list in chunks(text, vocab):
                        embedding = embed(session, numpy, token_list, vocab)
                        database.execute(
                            """
                            INSERT INTO hadith_vectors (
                                embedding, collection_id, hadith_id, chunk_index,
                                source_revision, source_sha256, text, reference
                            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                            """,
                            (
                                sqlite_vec.serialize_float32(embedding),
                                collection_id,
                                str(record["id"]),
                                chunk_index,
                                str(manifest["source_revision"]),
                                str(manifest["source_sha256"]),
                                chunk_text,
                                reference,
                            ),
                        )
                        total_chunks += 1
            database.executemany(
                "INSERT INTO index_manifest(key, value) VALUES (?, ?)",
                [
                    ("source_revision", "1.3.0"),
                    ("collection_count", str(len(sources))),
                    ("embedding_dimension", str(EMBEDDING_DIMENSION)),
                    ("chunk_token_count", str(CHUNK_TOKEN_COUNT)),
                    ("chunk_overlap", str(CHUNK_OVERLAP)),
                    ("sqlite_vec_version", str(version)),
                    ("record_count", str(total_chunks)),
                ],
            )
            if database.execute("PRAGMA integrity_check").fetchone()[0] != "ok":
                raise BlockedCorpus("sqlite-vec index integrity check failed")
    except Exception:
        if temporary.exists():
            temporary.unlink()
        raise
    os.replace(temporary, output)
    return {"status": "indexed", "collection_count": len(sources), "chunk_count": total_chunks}


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--source-root", type=Path, required=True)
    parser.add_argument("--approved-manifest", type=Path, required=True)
    parser.add_argument("--model", type=Path, required=True)
    parser.add_argument("--model-sha256", required=True)
    parser.add_argument("--vocab", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    try:
        print(json.dumps(build(args), ensure_ascii=False))
    except (BlockedCorpus, FileNotFoundError, json.JSONDecodeError) as error:
        print(json.dumps({"status": "blocked", "reason": str(error)}, ensure_ascii=False))
        raise SystemExit(2)


if __name__ == "__main__":
    main()
