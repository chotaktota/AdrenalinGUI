import json
from pathlib import Path
import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt

base = Path(__file__).resolve().parent
data = json.loads((base / "results/2026-11-20/summary.json").read_text())["aggregate"]
background, foreground, muted = "#19191b", "#e5e5e7", "#a1a1aa"
plt.rcParams.update({"font.family": "DejaVu Sans", "font.size": 10,
    "text.color": foreground, "axes.labelcolor": muted, "xtick.color": muted,
    "ytick.color": foreground, "axes.edgecolor": "#45454b", "svg.fonttype": "none"})
fig, axes = plt.subplots(1, 2, figsize=(11, 3.8), sharex=True, facecolor=background)
for ax, mode, title in zip(axes, ["static", "dynamic"], ["Static menu", "With placeholders"]):
    keys = [engine + "/" + mode + "/open" for engine in ["agui", "bmenu", "abstract"]]
    median = [data[key]["median_ms"] for key in keys]
    p95 = [data[key]["p95_ms"] for key in keys]
    ax.set_facecolor(background)
    ax.barh([0, 1, 2], median, height=.42, color=["#d55e57", "#777780", "#777780"], zorder=3)
    ax.errorbar(median, [0, 1, 2], xerr=[[0, 0, 0], [p-m for p,m in zip(p95,median)]],
        fmt="none", ecolor="#c2c2c8", capsize=4, elinewidth=1.2, zorder=4)
    ax.set_yticks([0, 1, 2], ["AdrenalinGUI", "BMenu 2.7", "AbstractMenus 1.18.0"])
    ax.invert_yaxis()
    ax.set_xlim(0, 1.1)
    ax.set_xticks([0, .25, .5, .75, 1.0])
    ax.set_title(title, loc="left", color=foreground, pad=18, fontsize=12)
    ax.set_xlabel("milliseconds")
    ax.grid(axis="x", color="#353539", linewidth=.7, zorder=0)
    ax.tick_params(axis="both", length=0, pad=8)
    for side in ["top", "right", "left"]:
        ax.spines[side].set_visible(False)
fig.suptitle("Server-side opening", x=.04, ha="left", y=.99, fontsize=16)
fig.text(.04, .035, "Bar: median   |   Whisker: p95     ·     1,200 opens per case     ·     Lower is better", color=muted, fontsize=9)
fig.subplots_adjust(left=.17, right=.98, top=.76, bottom=.23, wspace=.82)
target = base.parent / "docs/assets"
fig.savefig(target / "benchmark-open.png", dpi=180, facecolor=background)
fig.savefig(target / "benchmark-open.svg", facecolor=background)

svg = target / "benchmark-open.svg"
svg.write_text("\n".join(line.rstrip() for line in svg.read_text(encoding="utf-8").splitlines()) + "\n", encoding="utf-8")
