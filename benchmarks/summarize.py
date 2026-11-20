import collections
import json
import math
import statistics
import sys
from pathlib import Path

source = Path(sys.argv[1])
data = json.loads(source.read_text(encoding="utf-8"))
if data["failures"]:
    raise SystemExit("Замеры содержат ошибки: " + str(data["failures"]))
groups = collections.defaultdict(list)
rounds = collections.defaultdict(list)
for sample in data["samples"]:
    if sample["phase"] not in ("round1", "round2", "round3"):
        continue
    groups[(sample["menu"], sample["metric"])].append(sample["nanos"] / 1_000_000)
    rounds[(sample["phase"], sample["menu"], sample["metric"])].append(sample["nanos"] / 1_000_000)

def summarize(values):
    ordered = sorted(values)
    return {
        "count": len(values),
        "mean_ms": statistics.fmean(values),
        "median_ms": statistics.median(values),
        "p95_ms": ordered[math.ceil(len(ordered) * .95) - 1],
        "p99_ms": ordered[math.ceil(len(ordered) * .99) - 1],
        "min_ms": ordered[0],
        "max_ms": ordered[-1],
    }

summary = {
    "metric_units": "milliseconds",
    "percentile_method": "nearest rank; median is arithmetic mean of middle pair",
    "aggregate": {menu + "/" + metric: summarize(values) for (menu, metric), values in sorted(groups.items())},
    "rounds": {phase + "/" + menu + "/" + metric: summarize(values) for (phase, menu, metric), values in sorted(rounds.items())},
}
for name, values in summary["aggregate"].items():
    if values["count"] != 1200:
        raise SystemExit("Неполный набор замеров: " + name + " " + str(values["count"]))
target = source.with_name("summary.json")
target.write_text(json.dumps(summary, indent=2), encoding="utf-8")
for key, value in summary["aggregate"].items():
    print(key, "n=" + str(value["count"]), "median=" + format(value["median_ms"], ".4f"), "p95=" + format(value["p95_ms"], ".4f"))
