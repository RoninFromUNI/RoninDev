import pandas as pd
import numpy as np
import matplotlib.pyplot as plt
from scipy import stats
from sklearn.metrics import cohen_kappa_score
import os

SNAPSHOTS_FILE = "synthetic_snapshots.csv"
ESM_FILE = "synthetic_esm_responses.csv"
OUTPUT_DIR = "results"

os.makedirs(OUTPUT_DIR, exist_ok=True)

snapshots = pd.read_csv(SNAPSHOTS_FILE)
esm = pd.read_csv(ESM_FILE)

print(f"loaded {len(snapshots)} snapshots, {len(esm)} ESM responses")
print(f"participants: {list(snapshots['participant_id'].unique())}")
print()

# merge esm with matching snapshots
merged = esm.merge(
    snapshots[["snapshot_id", "flow_score", "flow_state", "composite_flow_score"]],
    on="snapshot_id",
    how="inner"
)
print(f"paired observations: {len(merged)}")
print()


# bin esm scores into flow state categories for kappa comparison
def esm_to_state(score):
    if score >= 4.2: return "DEEP_FLOW"
    if score >= 3.4: return "FLOW"
    if score >= 2.8: return "EMERGING"
    if score >= 2.2: return "NEUTRAL"
    if score >= 1.6: return "DISRUPTED"
    if score >= 1.0: return "PROCRASTINATING"
    return "NOT_IN_FLOW"

merged["esm_state"] = merged["composite_esm_score"].apply(esm_to_state)


# ~~~ spearman rank correlation ~~~
rho, p_value = stats.spearmanr(merged["flow_score"], merged["composite_esm_score"])
print(f"spearman rho: {rho:.4f}")
print(f"p-value: {p_value:.6f}")
print(f"significant at 0.05: {'yes' if p_value < 0.05 else 'no'}")
print()


# ~~~ cohen's weighted kappa ~~~
state_order = ["NOT_IN_FLOW", "PROCRASTINATING", "DISRUPTED", "NEUTRAL", "EMERGING", "FLOW", "DEEP_FLOW"]

algo_labels = pd.Categorical(merged["flow_state"], categories=state_order, ordered=True).codes
esm_labels = pd.Categorical(merged["esm_state"], categories=state_order, ordered=True).codes

kappa = cohen_kappa_score(algo_labels, esm_labels, weights="quadratic")
print(f"cohen's weighted kappa: {kappa:.4f}")
if kappa >= 0.81:
    agreement = "almost perfect"
elif kappa >= 0.61:
    agreement = "substantial"
elif kappa >= 0.41:
    agreement = "moderate"
elif kappa >= 0.21:
    agreement = "fair"
else:
    agreement = "slight"
print(f"interpretation: {agreement} agreement")
print()


# ~~~ divergence detection ~~~
# flag cases where algorithm and esm disagree by 2+ state bands
merged["algo_rank"] = pd.Categorical(merged["flow_state"], categories=state_order, ordered=True).codes
merged["esm_rank"] = pd.Categorical(merged["esm_state"], categories=state_order, ordered=True).codes
merged["divergence"] = abs(merged["algo_rank"] - merged["esm_rank"])
divergent = merged[merged["divergence"] >= 2]

print(f"divergent observations (2+ bands apart): {len(divergent)} / {len(merged)}")
if len(divergent) > 0:
    print("flagged:")
    for _, row in divergent.iterrows():
        print(f"  {row['participant_id']}: algo={row['flow_state']} vs esm={row['esm_state']} (gap={row['divergence']})")
print()


# ~~~ per-participant summary ~~~
print("per-participant breakdown:")
for pid in sorted(merged["participant_id"].unique()):
    sub = merged[merged["participant_id"] == pid]
    sub_rho, sub_p = stats.spearmanr(sub["flow_score"], sub["composite_esm_score"])
    mean_flow = sub["flow_score"].mean()
    mean_esm = sub["composite_esm_score"].mean()
    print(f"  {pid}: mean_flow={mean_flow:.1f}, mean_esm={mean_esm:.2f}, rho={sub_rho:.3f} (p={sub_p:.4f})")
print()


# ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
# figures
# ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

plt.style.use("dark_background")
fig_bg = "#1a1a2e"
accent_colors = ["#00d2ff", "#ff6b6b", "#ffd93d", "#6bcb77", "#c084fc"]
participant_colors = {
    f"P00{i+1}": accent_colors[i] for i in range(5)
}


# fig 1: scatter - algorithm vs self-report
fig, ax = plt.subplots(figsize=(8, 6))
fig.patch.set_facecolor(fig_bg)
ax.set_facecolor(fig_bg)

for pid in sorted(merged["participant_id"].unique()):
    subset = merged[merged["participant_id"] == pid]
    ax.scatter(
        subset["flow_score"], subset["composite_esm_score"],
        c=participant_colors[pid], label=pid, s=70, alpha=0.85,
        edgecolors="white", linewidths=0.5
    )

# trend line
z = np.polyfit(merged["flow_score"], merged["composite_esm_score"], 1)
p = np.poly1d(z)
x_line = np.linspace(merged["flow_score"].min(), merged["flow_score"].max(), 100)
ax.plot(x_line, p(x_line), "--", color="#ffffff", alpha=0.4, linewidth=1)

ax.set_xlabel("Algorithmic Flow Score", fontsize=11, color="white")
ax.set_ylabel("ESM Composite Score", fontsize=11, color="white")
ax.set_title(f"Algorithm vs Self-Report (Spearman \u03C1 = {rho:.3f}, p = {p_value:.4f})",
             fontsize=12, color="white", pad=12)
ax.legend(framealpha=0.3, fontsize=9)
ax.tick_params(colors="white")
for spine in ax.spines.values():
    spine.set_color("#333")

plt.tight_layout()
plt.savefig(os.path.join(OUTPUT_DIR, "spearman_correlation.png"), dpi=200, facecolor=fig_bg)
plt.close()
print("saved spearman_correlation.png")


# fig 2: confusion matrix style - algo state vs esm state
fig, ax = plt.subplots(figsize=(8, 7))
fig.patch.set_facecolor(fig_bg)
ax.set_facecolor(fig_bg)

confusion = pd.crosstab(merged["flow_state"], merged["esm_state"],
                        rownames=["Algorithm"], colnames=["Self-Report"])
# reindex to state order
confusion = confusion.reindex(index=state_order, columns=state_order, fill_value=0)

im = ax.imshow(confusion.values, cmap="YlOrRd", aspect="auto")

ax.set_xticks(range(len(state_order)))
ax.set_yticks(range(len(state_order)))
ax.set_xticklabels([s.replace("_", "\n") for s in state_order], fontsize=8, color="white", rotation=45, ha="right")
ax.set_yticklabels([s.replace("_", "\n") for s in state_order], fontsize=8, color="white")
ax.set_xlabel("Self-Report Classification", fontsize=11, color="white")
ax.set_ylabel("Algorithm Classification", fontsize=11, color="white")
ax.set_title(f"Classification Agreement (Cohen's \u03BA = {kappa:.3f}, {agreement})",
             fontsize=12, color="white", pad=12)

# annotate cells
for i in range(len(state_order)):
    for j in range(len(state_order)):
        val = confusion.values[i, j]
        if val > 0:
            ax.text(j, i, str(val), ha="center", va="center",
                    color="black" if val > 1 else "white", fontsize=10, fontweight="bold")

plt.colorbar(im, ax=ax, shrink=0.8)
plt.tight_layout()
plt.savefig(os.path.join(OUTPUT_DIR, "cohens_kappa.png"), dpi=200, facecolor=fig_bg)
plt.close()
print("saved cohens_kappa.png")


# fig 3: divergence bar chart per participant
fig, ax = plt.subplots(figsize=(8, 5))
fig.patch.set_facecolor(fig_bg)
ax.set_facecolor(fig_bg)

div_counts = merged.groupby("participant_id")["divergence"].apply(lambda x: (x >= 2).sum())
total_counts = merged.groupby("participant_id").size()
div_pct = (div_counts / total_counts * 100).fillna(0)

bars = ax.bar(div_pct.index, div_pct.values,
              color=[participant_colors[p] for p in div_pct.index],
              edgecolor="white", linewidth=0.5, alpha=0.85)

ax.set_xlabel("Participant", fontsize=11, color="white")
ax.set_ylabel("Divergent Responses (%)", fontsize=11, color="white")
ax.set_title("Self-Report Divergence by Participant", fontsize=12, color="white", pad=12)
ax.tick_params(colors="white")
for spine in ax.spines.values():
    spine.set_color("#333")

for bar, val in zip(bars, div_pct.values):
    if val > 0:
        ax.text(bar.get_x() + bar.get_width() / 2, bar.get_height() + 1,
                f"{val:.0f}%", ha="center", va="bottom", color="white", fontsize=10)

plt.tight_layout()
plt.savefig(os.path.join(OUTPUT_DIR, "divergence_flags.png"), dpi=200, facecolor=fig_bg)
plt.close()
print("saved divergence_flags.png")


# fig 4: flow trajectory per participant over session
fig, axes = plt.subplots(2, 3, figsize=(14, 8))
fig.patch.set_facecolor(fig_bg)

# state band colors for background shading
band_colors = {
    "DEEP_FLOW": "#1b5e20", "FLOW": "#2e7d32", "EMERGING": "#558b2f",
    "NEUTRAL": "#f9a825", "DISRUPTED": "#ef6c00", "PROCRASTINATING": "#d32f2f",
    "NOT_IN_FLOW": "#424242"
}
band_thresholds = [80, 65, 50, 40, 25, 15, 0]
band_names = ["DEEP_FLOW", "FLOW", "EMERGING", "NEUTRAL", "DISRUPTED", "PROCRASTINATING", "NOT_IN_FLOW"]

for idx, pid in enumerate(sorted(snapshots["participant_id"].unique())):
    row, col = divmod(idx, 3)
    ax = axes[row][col]
    ax.set_facecolor(fig_bg)

    # draw state bands
    prev = 100
    for threshold, name in zip(band_thresholds, band_names):
        ax.axhspan(threshold, prev, alpha=0.15, color=band_colors[name])
        prev = threshold

    sub = snapshots[snapshots["participant_id"] == pid].reset_index(drop=True)
    ax.plot(range(len(sub)), sub["flow_score"],
            color=participant_colors[pid], linewidth=2, alpha=0.9)
    ax.scatter(range(len(sub)), sub["flow_score"],
               color=participant_colors[pid], s=15, zorder=5)

    ax.set_ylim(0, 100)
    ax.set_title(pid, fontsize=11, color=participant_colors[pid], fontweight="bold")
    ax.set_xlabel("Snapshot", fontsize=9, color="white")
    ax.set_ylabel("Flow Score", fontsize=9, color="white")
    ax.tick_params(colors="white", labelsize=8)
    for spine in ax.spines.values():
        spine.set_color("#333")

# hide the 6th subplot (2x3 grid for 5 participants)
axes[1][2].set_visible(False)

fig.suptitle("Flow Score Trajectory per Participant", fontsize=14, color="white", y=0.98)
plt.tight_layout(rect=[0, 0, 1, 0.95])
plt.savefig(os.path.join(OUTPUT_DIR, "flow_trajectory.png"), dpi=200, facecolor=fig_bg)
plt.close()
print("saved flow_trajectory.png")

print()
print("all done. check the results/ folder.")