import json
import re
from pathlib import Path
from urllib.parse import unquote

root = Path(__file__).resolve().parent
metadata = json.loads((root / "navigation.json").read_text(encoding="utf-8"))
identifiers = {page["id"] for page in metadata["pages"]}
if len(identifiers) != len(metadata["pages"]):
    raise SystemExit("Идентификаторы страниц должны быть уникальными")

pages = []
headings = {}
for entry in metadata["pages"]:
    path = root / "pages" / (entry["id"] + ".md")
    text = path.read_text(encoding="utf-8")
    if not text.startswith("# "):
        raise SystemExit("Нет заголовка статьи: " + str(path))
    fence = None
    anchors = set()
    for line in text.splitlines():
        marker = re.match(r"^(~~~|`{3})", line)
        if marker:
            if fence is None:
                fence = marker[1]
            elif fence == marker[1]:
                fence = None
        elif fence is None and re.match(r"^#{2,3} ", line):
            title = re.sub(r"^#{2,3} ", "", line)
            base = re.sub(r"[^a-zа-яё0-9]+", "-", title.lower()).strip("-")
            anchor = base
            suffix = 2
            while anchor in anchors:
                anchor = base + "-" + str(suffix)
                suffix += 1
            anchors.add(anchor)
    if fence:
        raise SystemExit("Незакрытый блок кода: " + str(path))
    headings[entry["id"]] = anchors
    pages.append({**entry, "markdown": text})

for page in pages:
    for target in re.findall(r"\]\((#/[^)]+)\)", page["markdown"]):
        segments = target[2:].split("/")
        if segments[0] not in identifiers:
            raise SystemExit("Неизвестная статья: " + target)
        if len(segments) > 1 and unquote(segments[1]) not in headings[segments[0]]:
            raise SystemExit("Неизвестный раздел: " + target)

for asset in ("index.html", "assets/site.css", "assets/site.js", "assets/wordmark.png"):
    if not (root / asset).is_file():
        raise SystemExit("Отсутствует ресурс: " + asset)

output = {**metadata, "pages": pages}
(root / "content.json").write_text(
    json.dumps(output, ensure_ascii=False, indent=2) + "\n",
    encoding="utf-8",
)
print("Документация собрана: " + str(len(pages)) + " статей")
