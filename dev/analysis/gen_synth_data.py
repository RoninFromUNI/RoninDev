import pandas as pd
import numpy as np
from datetime import datetime, timedelta

np.random.seed(2215624)

# --- flow state classifier (mirrors FlowDetector.java thresholds) ---
def classify_flow_state(score):
    if score >= 80: return "DEEP_FLOW"
    if score >= 65: return "FLOW"
    if score >= 50: return "EMERGING"
    if score >= 40: return "NEUTRAL"
    if score >= 25: return "DISRUPTED"
    if score >= 15: return "PROCRASTINATING"
    return "NOT_IN_FLOW"


# --- participant profiles ---
profiles = {
    "P001": {
        "description": "steady mid-range performer",
        "flow_range": (55, 75),
        "esm_bias": 0.0
    },
    "P002": {
        "description": "struggles early, recovers late",
        "flow_range": (25, 50),
        "esm_bias": -0.2
    },
    "P003": {
        "description": "high performer, reaches deep flow",
        "flow_range": (70, 90),
        "esm_bias": 0.0
    },
    "P004": {
        "description": "disrupted, overestimates own focus",
        "flow_range": (20, 40),
        "esm_bias": 0.5
    },
    "P005": {
        "description": "inconsistent, bounces between states",
        "flow_range": (30, 75),
        "esm_bias": -0.1
    },
}


def generate_raw_metrics(flow_score):
    """
    work backwards from a target flow score to produce plausible raw metrics.
    the five category weights are: typing 30%, errors 25%, focus 20%, builds 15%, context 10%.
    each sub-score is normalised to [0, 1] before weighting.
    """
    target = flow_score / 100.0

    # generate five sub-scores that roughly average to the target when weighted
    # add noise so they're not perfectly uniform
    typing_sub = np.clip(target + np.random.uniform(-0.15, 0.15), 0, 1)
    error_sub = np.clip(target + np.random.uniform(-0.15, 0.15), 0, 1)
    focus_sub = np.clip(target + np.random.uniform(-0.10, 0.10), 0, 1)
    build_sub = np.clip(target + np.random.uniform(-0.20, 0.20), 0, 1)
    context_sub = np.clip(target + np.random.uniform(-0.15, 0.15), 0, 1)

    # derive raw metrics from sub-scores
    # kpm: 0-120 range, higher = more typing activity
    kpm = int(typing_sub * 120)
    typing_duration = int(typing_sub * 120000)  # ms in last window
    backspace_count = int((1 - error_sub) * 30)  # fewer backspaces = higher error sub
    error_markers = int((1 - error_sub) * 15)

    # focus: ms of sustained focus in window
    focus_duration = int(focus_sub * 120000)

    # builds
    build_count = np.random.randint(0, 4)
    build_success = int(build_sub * build_count) if build_count > 0 else 0
    build_failures = build_count - build_success

    # context switches
    context_switches = int((1 - context_sub) * 10)
    file_changes = context_switches + np.random.randint(0, 3)

    return {
        "kpm": kpm,
        "typing_duration": typing_duration,
        "backspace_count": backspace_count,
        "error_markers": error_markers,
        "focus_duration": focus_duration,
        "build_count": build_count,
        "build_success": build_success,
        "build_failures": build_failures,
        "context_switches": context_switches,
        "file_changes": file_changes,
        "typing_sub": round(typing_sub, 4),
        "error_sub": round(error_sub, 4),
        "focus_sub": round(focus_sub, 4),
        "build_sub": round(build_sub, 4),
        "context_sub": round(context_sub, 4),
    }


def generate_snapshots(participant_id, profile, num_snapshots=25):
    """generate snapshot rows for one participant (2 min intervals = 50 min session)"""
    snapshots = []
    session_id = f"{participant_id}_session"
    start_time = datetime(2026, 3, 23, 14, 0, 0)

    low, high = profile["flow_range"]

    for i in range(num_snapshots):
        timestamp = start_time + timedelta(minutes=i * 2)

        # flow score with some temporal shaping
        # early snapshots trend lower (warmup), middle trends higher, end drifts
        progress = i / num_snapshots
        if progress < 0.2:
            # warmup phase: bias toward lower end
            flow_score = np.random.randint(low, min(low + 15, high + 1))
        elif progress > 0.8:
            # fatigue phase: drift down slightly
            flow_score = np.random.randint(max(low, high - 20), high + 1)
        else:
            # main phase: full range
            flow_score = np.random.randint(low, high + 1)

        flow_state = classify_flow_state(flow_score)
        metrics = generate_raw_metrics(flow_score)

        # recalculate composite from weighted sub-scores to match plugin logic
        composite = (
                metrics["typing_sub"] * 0.30 +
                metrics["error_sub"] * 0.25 +
                metrics["focus_sub"] * 0.20 +
                metrics["build_sub"] * 0.15 +
                metrics["context_sub"] * 0.10
        )

        stress_level = round(1.0 - composite, 4)

        snapshots.append({
            "snapshot_id": f"{session_id}_{i}",
            "session_id": session_id,
            "participant_id": participant_id,
            "timestamp": timestamp.isoformat(),
            "kpm": metrics["kpm"],
            "typing_duration": metrics["typing_duration"],
            "backspace_count": metrics["backspace_count"],
            "error_markers": metrics["error_markers"],
            "focus_duration": metrics["focus_duration"],
            "build_count": metrics["build_count"],
            "build_success": metrics["build_success"],
            "build_failures": metrics["build_failures"],
            "context_switches": metrics["context_switches"],
            "file_changes": metrics["file_changes"],
            "composite_flow_score": round(composite, 4),
            "typing_sub": metrics["typing_sub"],
            "error_sub": metrics["error_sub"],
            "focus_sub": metrics["focus_sub"],
            "build_sub": metrics["build_sub"],
            "context_sub": metrics["context_sub"],
            "flow_score": flow_score,
            "stress_level": stress_level,
            "flow_state": flow_state,
        })

    return snapshots


def generate_esm_responses(participant_id, profile, snapshots, num_responses=5):
    """
    generate ESM self-report responses linked to specific snapshots.
    one response every ~10 minutes (every 5th snapshot).
    """
    responses = []
    session_id = f"{participant_id}_session"
    bias = profile["esm_bias"]

    for r in range(num_responses):
        # link to every 5th snapshot
        snap_index = (r + 1) * 4
        if snap_index >= len(snapshots):
            snap_index = len(snapshots) - 1

        snap = snapshots[snap_index]
        snap_flow = snap["flow_score"]

        # convert flow score (0-100) to likert base (1-5)
        likert_base = 1 + (snap_flow / 100.0) * 4

        # generate 7 likert items with noise and bias
        items = []
        for _ in range(7):
            val = likert_base + bias + np.random.uniform(-0.8, 0.8)
            val = int(np.clip(round(val), 1, 5))
            items.append(val)

        triggered_at = snap["timestamp"]
        responded_at = (
                datetime.fromisoformat(triggered_at) + timedelta(seconds=np.random.randint(15, 45))
        ).isoformat()

        responses.append({
            "snapshot_id": snap["snapshot_id"],
            "session_id": session_id,
            "participant_id": participant_id,
            "triggered_at": triggered_at,
            "responded_at": responded_at,
            "challenge_skill_balance": items[0],
            "action_awareness_merging": items[1],
            "clear_goals": items[2],
            "unambiguous_feedback": items[3],
            "concentration": items[4],
            "sense_of_control": items[5],
            "autotelic_experience": items[6],
            "composite_esm_score": round(np.mean(items), 2),
            "qualitative_note": "",
            "using_ai_tools": int(np.random.choice([0, 1])),
            "ai_tool_name": "Copilot" if np.random.random() > 0.6 else "",
        })

    return responses


# --- generate everything ---
all_snapshots = []
all_esm = []

for pid, profile in profiles.items():
    snaps = generate_snapshots(pid, profile)
    esm = generate_esm_responses(pid, profile, snaps)
    all_snapshots.extend(snaps)
    all_esm.extend(esm)

snapshots_df = pd.DataFrame(all_snapshots)
esm_df = pd.DataFrame(all_esm)

snapshots_df.to_csv("synthetic_snapshots.csv", index=False)
esm_df.to_csv("synthetic_esm_responses.csv", index=False)

print(f"generated {len(snapshots_df)} snapshots across {len(profiles)} participants")
print(f"generated {len(esm_df)} ESM responses")
print()
for pid, profile in profiles.items():
    sid = f"{pid}_session"
    s = len(snapshots_df[snapshots_df.session_id == sid])
    e = len(esm_df[esm_df.session_id == sid])
    print(f"  {pid} ({profile['description']}): {s} snapshots, {e} ESM responses")