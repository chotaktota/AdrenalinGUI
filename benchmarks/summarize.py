import collections
import json
import math
import statistics
import sys
from pathlib import Path

PHASES = ("round1", "round2", "round3")
MENUS = tuple(
    f"{engine}/{mode}"
    for engine in ("abstract", "agui", "bmenu")
    for mode in ("dynamic", "static")
)
METRICS = ("click_action", "open")
SAMPLES_PER_ROUND = 400

source = Path(sys.argv[1])
data = json.loads(source.read_text(encoding="utf-8"))
if data["failures"]:
    raise SystemExit("Замеры содержат ошибки: " + str(data["failures"]))
groups = collections.defaultdict(list)
rounds = collections.defaultdict(list)
for sample in data["samples"]:
    phase, menu, metric = sample["phase"], sample["menu"], sample["metric"]
    if phase not in PHASES:
        continue
    if menu not in MENUS or metric not in METRICS:
        raise SystemExit(f"Неизвестная группа замеров: {phase}/{menu}/{metric}")
    millis = sample["nanos"] / 1_000_000
    groups[(menu, metric)].append(millis)
    rounds[(phase, menu, metric)].append(millis)

allow_incomplete_actions = "--allow-incomplete-actions" in sys.argv
incomplete = {}
for menu in MENUS:
    for metric in METRICS:
        recorded = len(groups.get((menu, metric), ()))
        allow_missing = allow_incomplete_actions and metric == "click_action" and recorded > 0
        for phase in PHASES:
            count = len(rounds.get((phase, menu, metric), ()))
            if count == SAMPLES_PER_ROUND or (allow_missing and count < SAMPLES_PER_ROUND):
                continue
            raise SystemExit(
                f"Неполный набор замеров: {phase}/{menu}/{metric}: "
                f"ожидалось {SAMPLES_PER_ROUND}, получено {count}"
            )
        expected = SAMPLES_PER_ROUND * len(PHASES)
        if recorded != expected:
            incomplete[f"{menu}/{metric}"] = {"expected": expected, "recorded": recorded}

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
    "incomplete_actions": incomplete,
}
target = source.with_name("summary.json")
target.write_text(json.dumps(summary, indent=2), encoding="utf-8")
for key, value in summary["aggregate"].items():
    print(key, "n=" + str(value["count"]), "median=" + format(value["median_ms"], ".4f"), "p95=" + format(value["p95_ms"], ".4f"))
