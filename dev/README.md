# Therapeutic Dev

Real-time flow state detection in IntelliJ IDEA through IDE behavioural metrics.

This repository contains three projects: the **plugin** itself, a **study task** used as a controlled debugging stimulus, and a **convergent validity analysis pipeline** for evaluating detection accuracy against self-report.

---

## Plugin

An IntelliJ IDEA plugin that passively monitors coding behaviour and classifies cognitive state into seven flow states: `DEEP_FLOW`, `FLOW`, `EMERGING`, `NEUTRAL`, `DISRUPTED`, `PROCRASTINATING`, `NOT_IN_FLOW`. Uses that classification to suggest intelligently timed breaks when flow naturally dips.

### Scoring Algorithm

Five weighted categories, each normalised to `[0.0, 1.0]` where higher = more favourable for flow:

| Category | Weight | What It Measures |
|---|---|---|
| Typing Behaviour | 30% | KPM (bell curve at 80), correction ratio, burst consistency |
| Error Patterns | 25% | Introduction rate, resolution rate, net trajectory |
| Focus Duration | 20% | Time since last context switch, logarithmic normalisation |
| Build Activity | 15% | Frequency, success rate, recency |
| Contextual Signals | 10% | IDE focus %, AI suggestion events, idle duration |

Weighted sum maps to `FlowState` via configurable thresholds: `≥0.80` DEEP_FLOW, `≥0.65` FLOW, `≥0.52` EMERGING, `≥0.40` NEUTRAL, `≥0.28` DISRUPTED, `≥0.15` PROCRASTINATING, `<0.15` NOT_IN_FLOW.

### Architecture

Four-layer unidirectional data flow. No layer communicates upward. Presentation is a pure consumer.

```
IDE Integration → Service → Detection → Persistence
                                      → Presentation
```

### Package Structure

```
com.ronin.therapeuticdev/
├── detection/        FlowDetector, FlowDetectionResult, FlowState
├── listeners/        9 IDE extension point implementations
├── metrics/          FlowMetrics (immutable Builder pattern, 20+ fields)
├── services/         MetricCollector, SnapshotScheduler, BreakManager,
│                     EsmProbeService, ParticipantSession
├── storage/          MetricRepository (SQLite + HikariCP, CSV export)
├── settings/         TherapeuticDevSettings, TherapeuticDevConfigurable
└── ui/
    ├── components/   HeroScoreCard, MetricBar, FlowTrendSparkline
    └── tabs/         MetricsTab, ActivityTab, GraphTab
```

### Tech Stack

- Java 21 (JetBrains Runtime 21.0.3b509)
- IntelliJ Platform SDK 2024.1+
- Gradle 8.x with IntelliJ Platform Gradle Plugin 2.x
- SQLite via sqlite-jdbc + HikariCP
- Swing (native IntelliJ toolkit)

### Build and Run

```bash
# dev sandbox
./gradlew runIde

# distributable zip
./gradlew clean buildPlugin
# output: build/distributions/therapeutic-dev-1.0-SNAPSHOT.zip

# install: Settings → Plugins → ⚙ → Install Plugin from Disk → select zip

# tests (12-scenario synthetic validation suite)
./gradlew test
```

### Data

All metrics persist to `metrics.db` (SQLite) at `[IDE config]/therapeutic-dev/`. Export via settings panel or `MetricRepository.exportToCsv()` / `MetricRepository.exportEsmToCsv()`.

---

## Study Task (TaskFlow)

A standalone Java project with five intentional cascading bugs embedded across a task management system. Participants debug sequentially while the plugin passively records behaviour. Bugs escalate in cognitive load to produce a behavioural arc that maps onto the detection algorithm's sensitivity profile.

### Project Structure

```
com.ronin.studytask/
├── Main.java              Entry point, exercises each bug via labelled sections
├── model/
│   ├── Task.java           Domain object (title, description, priority, status, dates)
│   ├── TaskPriority.java   Enum: LOW, MEDIUM, HIGH, CRITICAL
│   └── TaskStatus.java     Enum: TODO, IN_PROGRESS, BLOCKED, DONE
├── service/
│   ├── TaskService.java    CRUD, status filtering, priority escalation, sorting
│   ├── TaskFilter.java     Static filter utilities (priority range, overdue, active)
│   └── TaskReporter.java   Summary report generation
└── util/
    ├── DateUtils.java      Date comparison and formatting
    └── TaskValidator.java  Task validation with ValidationResult record
```

### Bug Design

| Bug | Location | Severity | Type |
|---|---|---|---|
| 1 | `TaskValidator.java` | Easy | Inverted boolean, valid tasks rejected |
| 2 | `TaskService.java` | Easy-Medium | Missing null check, NPE on status filter |
| 3 | `TaskFilter.java` | Medium | Wrong comparator in priority range filter |
| 4 | `DateUtils.java` + `TaskService.java` | Medium-Hard | Reversed sort + off-by-one escalation |
| 5 | `TaskService.java` | Hard (optional) | Missing state transition guard |

Bugs cascade: fixing one changes runtime behaviour that the next depends on. Participants cannot skip ahead.

### Predicted Behavioural Arc

```
Bug 1-2  →  Orientation, quick fixes        →  NEUTRAL → EMERGING
Bug 3    →  Cross-file reasoning begins      →  NEUTRAL (focus drops)
Bug 4    →  Peak cognitive load              →  DISRUPTED (error accumulation)
Bug 5    →  Recovery, careful analysis       →  Recovery bonus → EMERGING/FLOW
```

### Running

```bash
mvn compile exec:java -Dexec.mainClass="com.ronin.studytask.Main"
```

Java 21, Maven, zero external dependencies.

---

## Analysis Pipeline

Python pipeline for convergent validity analysis between algorithmic flow scores and ESM self-report data.

### What It Produces

Four dissertation-ready PNG figures:

| File | Content |
|---|---|
| `spearman_correlation.png` | Scatter plot, per-participant colouring, trend line |
| `classification_agreement.png` | Heatmap of algorithm vs ESM state classifications |
| `divergence_by_participant.png` | Bar chart of divergent response percentages |
| `flow_trajectories.png` | Per-participant score timelines with state band shading |

### Statistical Methods

- **Spearman rank correlation** between `flow_score` and `composite_esm_score`
- **Cohen's weighted kappa** between binned `FlowState` and ESM-derived bands
- **Divergence detection** flagging 2+ band disagreements
- **Per-participant trajectory visualisation**

### Requirements

```
pip install pandas numpy scipy matplotlib seaborn scikit-learn
```

Python 3.10+

### Usage

```bash
# with real participant data
python therapeutic_dev_analysis.py \
  --snapshots flow_snapshots.csv \
  --esm esm_responses.csv \
  --output ./results

# with synthetic data (pipeline demonstration)
python generate_synthetic_data.py
python therapeutic_dev_analysis.py \
  --snapshots synthetic_snapshots.csv \
  --esm synthetic_esm_responses.csv \
  --output ./results
```

### Synthetic Participant Profiles

Generator seeded with student ID `2215624` for reproducibility. Five profiles, 25 snapshots each (2-min intervals), 5 ESM responses each (10-min intervals):

| ID | Profile |
|---|---|
| P001 | Steady mid-range, EMERGING to FLOW |
| P002 | Struggles early, recovers late |
| P003 | High performer, reaches DEEP_FLOW |
| P004 | Disrupted, overestimates self-report (ESM bias +0.5) |
| P005 | Inconsistent, bounces across multiple bands |

### Key Results (Synthetic)

Reflects constructed data properties, not observed behaviour:

- Spearman ρ = 0.851 (p < 0.0001)
- Cohen's κ = 0.781 (substantial agreement)
- P004 accounts for 60% of divergent responses

---

## Research Context

Final year dissertation project (CS3072/CS3605, Brunel University London, Student ID 2215624). Investigating whether IDE-level behavioural metrics can reliably detect developer flow states in AI-augmented development environments.

## License

Academic project. IntelliJ Platform SDK is Apache 2.0.
