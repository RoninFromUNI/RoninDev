package com.ronin.therapeuticdev.detection;

import com.ronin.therapeuticdev.metrics.FlowMetrics;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * synthetic validation for FlowDetector — proves the algorithm classifies correctly
 * against known metric inputs before i collect real participant data.
 *
 * covers all seven FlowState classifications, boundary conditions, the error
 * recovery bonus, focus penalty accumulation, build streak penalties, the early
 * session guard (session < 60s), and stress level derivation.
 *
 * i'm writing about these in Chapter 6.1 of the dissertation as the first tier
 * of evaluation. the second tier is the controlled debugging study with TaskFlow.
 *
 * weights: typing 0.30, errors 0.25, focus 0.20, builds 0.15, context 0.10
 * thresholds: DEEP_FLOW >= 0.80, FLOW >= 0.65, EMERGING >= 0.52,
 *             NEUTRAL >= 0.40, DISRUPTED >= 0.28, PROCRASTINATING >= 0.15
 */
@DisplayName("FlowDetector — Synthetic Validation Suite")
class FlowDetectorSyntheticTest {

    private FlowDetector detector;

    @BeforeEach
    void setUp() {
        detector = new FlowDetector();
    }

    // ── the happy path: all metrics firing on all cylinders ──

    @Test
    @DisplayName("SC-01 | Optimal steady-state coding → DEEP_FLOW")
    void sc01_optimalSteadyState_shouldClassifyAsDeepFlow() {
        FlowMetrics metrics = FlowMetrics.builder()
                .keystrokesPerMin(80)
                .backspaceCount(3)
                .burstConsistency(0.95)
                .syntaxErrorCount(0)
                .compilationErrors(0)
                .errorsIntroduced(0)
                .errorsResolved(0)
                .timeSinceLastErrorMs(900_000L)
                .fileChangesLast5Min(1)
                .focusLossCount(0)
                .timeInCurrentFileMs(180_000L)
                .lastBuildSuccess(true)
                .consecutiveFailedBuilds(0)
                .timeSinceLastBuildMs(120_000L)
                .buildsInWindow(2)
                .buildSuccessRate(1.0)
                .ideFocusPct(0.95)
                .aiSuggestionsAccepted(0)
                .keyboardIdleMs(5_000L)
                .sessionDurationMs(1_200_000L)
                .build();

        FlowDetectionResult result = detector.detect(metrics);

        assertEquals(FlowState.DEEP_FLOW, result.getState(),
                "everything maxed out — if this isn't DEEP_FLOW the thresholds are wrong");
        assertTrue(result.getFlowTally() >= 0.80,
                "composite should clear 0.80 comfortably here");
        // sanity check the sub-scores too
        assertTrue(result.getTypingScore() >= 0.80, "typing should be high at 80 kpm");
        assertTrue(result.getErrorScore() >= 0.90, "zero errors + 15min clean streak");
        assertTrue(result.getFocusScore() >= 0.90, "one file change, no focus loss");
    }

    @Test
    @DisplayName("SC-02 | Good but imperfect session → FLOW")
    void sc02_goodSessionWithMinorNoise_shouldClassifyAsFlow() {
        // this simulates what i'd expect from a solid 15-minute stretch
        // with one syntax error and a single alt-tab
        FlowMetrics metrics = FlowMetrics.builder()
                .keystrokesPerMin(65)
                .backspaceCount(8)
                .burstConsistency(0.80)
                .syntaxErrorCount(1)
                .compilationErrors(0)
                .errorsIntroduced(1)
                .errorsResolved(0)
                .timeSinceLastErrorMs(120_000L)
                .fileChangesLast5Min(3)
                .focusLossCount(1)
                .timeInCurrentFileMs(150_000L)
                .lastBuildSuccess(true)
                .consecutiveFailedBuilds(0)
                .timeSinceLastBuildMs(200_000L)
                .buildsInWindow(1)
                .buildSuccessRate(1.0)
                .ideFocusPct(0.85)
                .aiSuggestionsAccepted(0)
                .keyboardIdleMs(15_000L)
                .sessionDurationMs(900_000L)
                .build();

        FlowDetectionResult result = detector.detect(metrics);

        assertEquals(FlowState.FLOW, result.getState());
        assertTrue(result.getFlowTally() >= 0.65 && result.getFlowTally() < 0.80,
                "should land in the FLOW band, got " + result.getFlowTally());
    }

    @Test
    @DisplayName("SC-03 | Re-engagement after distraction → EMERGING")
    void sc03_reengagementState_shouldClassifyAsEmerging() {
        // the tricky zone — enough activity to be above NEUTRAL but
        // too many file switches and errors to hit FLOW
        FlowMetrics metrics = FlowMetrics.builder()
                .keystrokesPerMin(45)
                .backspaceCount(12)
                .burstConsistency(0.65)
                .syntaxErrorCount(3)
                .compilationErrors(2)
                .errorsIntroduced(0)
                .errorsResolved(0)
                .timeSinceLastErrorMs(200_000L)
                .fileChangesLast5Min(5)
                .focusLossCount(3)
                .timeInCurrentFileMs(90_000L)
                .lastBuildSuccess(true)
                .consecutiveFailedBuilds(0)
                .timeSinceLastBuildMs(400_000L)
                .buildsInWindow(0)
                .buildSuccessRate(1.0)
                .ideFocusPct(0.70)
                .aiSuggestionsAccepted(0)
                .keyboardIdleMs(30_000L)
                .sessionDurationMs(600_000L)
                .build();

        FlowDetectionResult result = detector.detect(metrics);

        assertEquals(FlowState.EMERGING, result.getState());
        assertTrue(result.getFlowTally() >= 0.52 && result.getFlowTally() < 0.65);
    }

    @Test
    @DisplayName("SC-04 | Present but disengaged → NEUTRAL")
    void sc04_presentButDisengaged_shouldClassifyAsNeutral() {
        FlowMetrics metrics = FlowMetrics.builder()
                .keystrokesPerMin(35)
                .backspaceCount(20)
                .burstConsistency(0.50)
                .syntaxErrorCount(5)
                .compilationErrors(3)
                .errorsIntroduced(0)
                .errorsResolved(0)
                .timeSinceLastErrorMs(90_000L)
                .fileChangesLast5Min(8)
                .focusLossCount(5)
                .timeInCurrentFileMs(40_000L)
                .lastBuildSuccess(true)
                .consecutiveFailedBuilds(0)
                .timeSinceLastBuildMs(600_000L)
                .buildsInWindow(0)
                .buildSuccessRate(1.0)
                .ideFocusPct(0.60)
                .aiSuggestionsAccepted(0)
                .keyboardIdleMs(50_000L)
                .sessionDurationMs(720_000L)
                .build();

        FlowDetectionResult result = detector.detect(metrics);

        // 8 file changes + 5 focus losses hammers the focus score here
        assertEquals(FlowState.NEUTRAL, result.getState(),
                "too many switches and errors to be EMERGING, not bad enough for DISRUPTED");
        assertTrue(result.getFlowTally() >= 0.40 && result.getFlowTally() < 0.52);
    }

    // ── the downward spiral scenarios ──

    @Test
    @DisplayName("SC-05 | Build failures + high errors → DISRUPTED")
    void sc05_buildFailuresHighErrors_shouldClassifyAsDisrupted() {
        FlowMetrics metrics = FlowMetrics.builder()
                .keystrokesPerMin(18)
                .backspaceCount(30)
                .burstConsistency(0.35)
                .syntaxErrorCount(8)
                .compilationErrors(5)
                .errorsIntroduced(3)
                .errorsResolved(0)
                .timeSinceLastErrorMs(30_000L)
                .fileChangesLast5Min(7)
                .focusLossCount(6)
                .timeInCurrentFileMs(25_000L)
                .lastBuildSuccess(false)
                .consecutiveFailedBuilds(3)
                .timeSinceLastBuildMs(900_000L)
                .buildsInWindow(0)
                .buildSuccessRate(0.0)
                .ideFocusPct(0.55)
                .aiSuggestionsAccepted(0)
                .keyboardIdleMs(80_000L)
                .sessionDurationMs(840_000L)
                .build();

        FlowDetectionResult result = detector.detect(metrics);

        // 3 unresolved errors + 3 consecutive failed builds = pain
        assertEquals(FlowState.DISRUPTED, result.getState());
        assertTrue(result.getFlowTally() >= 0.28 && result.getFlowTally() < 0.40);
    }

    @Test
    @DisplayName("SC-06 | Near-zero output + excessive switching → PROCRASTINATING")
    void sc06_nearZeroOutputExcessiveSwitching_shouldClassifyAsProcrastinating() {
        // i tuned these numbers to land just above NOT_IN_FLOW —
        // 5 kpm is basically opening files and staring at them
        FlowMetrics metrics = FlowMetrics.builder()
                .keystrokesPerMin(5)
                .backspaceCount(2)
                .burstConsistency(0.20)
                .syntaxErrorCount(2)
                .compilationErrors(1)
                .errorsIntroduced(0)
                .errorsResolved(0)
                .timeSinceLastErrorMs(60_000L)
                .fileChangesLast5Min(12)
                .focusLossCount(10)
                .timeInCurrentFileMs(10_000L)
                .lastBuildSuccess(false)
                .consecutiveFailedBuilds(4)
                .timeSinceLastBuildMs(2_400_000L)
                .buildsInWindow(0)
                .buildSuccessRate(0.0)
                .ideFocusPct(0.30)
                .aiSuggestionsAccepted(0)
                .keyboardIdleMs(250_000L)
                .sessionDurationMs(1_500_000L)
                .build();

        FlowDetectionResult result = detector.detect(metrics);

        assertEquals(FlowState.PROCRASTINATING, result.getState());
        assertTrue(result.getFlowTally() >= 0.15 && result.getFlowTally() < 0.28,
                "should be above NOT_IN_FLOW but below DISRUPTED, got " + result.getFlowTally());
    }

    @Test
    @DisplayName("SC-07 | Absolute minimum → NOT_IN_FLOW")
    void sc07_absoluteMinimumActivity_shouldClassifyAsNotInFlow() {
        // worst possible scenario: zero keystrokes, 20 syntax errors, 15 focus losses,
        // 10 consecutive failed builds. if this doesn't hit NOT_IN_FLOW something is broken
        FlowMetrics metrics = FlowMetrics.builder()
                .keystrokesPerMin(0)
                .backspaceCount(0)
                .burstConsistency(0.0)
                .syntaxErrorCount(20)
                .compilationErrors(15)
                .errorsIntroduced(5)
                .errorsResolved(0)
                .timeSinceLastErrorMs(1_000L)
                .fileChangesLast5Min(15)
                .focusLossCount(15)
                .timeInCurrentFileMs(0L)
                .lastBuildSuccess(false)
                .consecutiveFailedBuilds(10)
                .timeSinceLastBuildMs(3_600_000L)
                .buildsInWindow(0)
                .buildSuccessRate(0.0)
                .ideFocusPct(0.05)
                .aiSuggestionsAccepted(0)
                .keyboardIdleMs(600_000L)
                .sessionDurationMs(1_200_000L)
                .build();

        FlowDetectionResult result = detector.detect(metrics);

        assertEquals(FlowState.NOT_IN_FLOW, result.getState());
        assertTrue(result.getFlowTally() < 0.15);
    }

    // ── specific algorithm behaviours i need to validate ──

    @Test
    @DisplayName("SC-08 | Full error resolution → recovery bonus kicks in")
    void sc08_fullErrorResolution_shouldApplyRecoveryBonus() {
        // baseline: no errors introduced or resolved
        FlowMetrics baseline = FlowMetrics.builder()
                .keystrokesPerMin(60).backspaceCount(5).burstConsistency(0.80)
                .errorsIntroduced(0).errorsResolved(0)
                .syntaxErrorCount(0).compilationErrors(0)
                .timeSinceLastErrorMs(100_000L)
                .fileChangesLast5Min(2).focusLossCount(1)
                .timeInCurrentFileMs(200_000L)
                .lastBuildSuccess(true).buildsInWindow(1).buildSuccessRate(1.0)
                .timeSinceLastBuildMs(100_000L)
                .ideFocusPct(0.80).keyboardIdleMs(10_000L)
                .sessionDurationMs(900_000L)
                .build();

        // same metrics but 3 errors introduced AND all 3 resolved
        // this should trigger the +0.10 recovery bonus in normaliseErrorScore
        FlowMetrics withRecovery = FlowMetrics.builder()
                .keystrokesPerMin(60).backspaceCount(5).burstConsistency(0.80)
                .errorsIntroduced(3).errorsResolved(3)
                .syntaxErrorCount(0).compilationErrors(0)
                .timeSinceLastErrorMs(100_000L)
                .fileChangesLast5Min(2).focusLossCount(1)
                .timeInCurrentFileMs(200_000L)
                .lastBuildSuccess(true).buildsInWindow(1).buildSuccessRate(1.0)
                .timeSinceLastBuildMs(100_000L)
                .ideFocusPct(0.80).keyboardIdleMs(10_000L)
                .sessionDurationMs(900_000L)
                .build();

        FlowDetectionResult baseResult     = detector.detect(baseline);
        FlowDetectionResult recoveryResult = detector.detect(withRecovery);

        // recovery bonus should push the error score up (or at least equal)
        assertTrue(recoveryResult.getErrorScore() >= baseResult.getErrorScore(),
                "resolving all introduced errors should apply the +0.10 bonus");
    }

    @Test
    @DisplayName("SC-09 | Session < 60s → contextScore locks to 0.5")
    void sc09_earlySession_contextScoreShouldBeNeutral() {
        // the early session guard in normaliseContextScore returns 0.5 when
        // session < 60s, so ideFocusPct shouldn't affect the composite at all

        FlowMetrics earlyHighFocus = FlowMetrics.builder()
                .keystrokesPerMin(70).backspaceCount(3).burstConsistency(0.90)
                .syntaxErrorCount(0).compilationErrors(0)
                .errorsIntroduced(0).errorsResolved(0)
                .timeSinceLastErrorMs(Long.MAX_VALUE)
                .fileChangesLast5Min(0).focusLossCount(0)
                .timeInCurrentFileMs(200_000L)
                .lastBuildSuccess(true).buildsInWindow(1).buildSuccessRate(1.0)
                .timeSinceLastBuildMs(60_000L)
                .ideFocusPct(1.0)
                .aiSuggestionsAccepted(0).keyboardIdleMs(0L)
                .sessionDurationMs(30_000L)  // 30 seconds
                .build();

        FlowMetrics earlyLowFocus = FlowMetrics.builder()
                .keystrokesPerMin(70).backspaceCount(3).burstConsistency(0.90)
                .syntaxErrorCount(0).compilationErrors(0)
                .errorsIntroduced(0).errorsResolved(0)
                .timeSinceLastErrorMs(Long.MAX_VALUE)
                .fileChangesLast5Min(0).focusLossCount(0)
                .timeInCurrentFileMs(200_000L)
                .lastBuildSuccess(true).buildsInWindow(1).buildSuccessRate(1.0)
                .timeSinceLastBuildMs(60_000L)
                .ideFocusPct(0.0)            // completely unfocused
                .aiSuggestionsAccepted(0).keyboardIdleMs(0L)
                .sessionDurationMs(30_000L)  // same 30 seconds
                .build();

        FlowDetectionResult highResult = detector.detect(earlyHighFocus);
        FlowDetectionResult lowResult  = detector.detect(earlyLowFocus);

        // both should produce the same composite because context is fixed at 0.5
        assertEquals(highResult.getFlowTally(), lowResult.getFlowTally(), 0.001,
                "ideFocusPct shouldn't matter when session is under 60 seconds");
    }

    @Test
    @DisplayName("SC-10 | Stress level: high-error config vs low-error config")
    void sc10_stressLevelDerivation_shouldReflectErrorAndFocusInverse() {
        FlowMetrics highStress = FlowMetrics.builder()
                .keystrokesPerMin(25).backspaceCount(15).burstConsistency(0.40)
                .syntaxErrorCount(10).compilationErrors(8)
                .errorsIntroduced(4).errorsResolved(0)
                .timeSinceLastErrorMs(5_000L)
                .fileChangesLast5Min(10).focusLossCount(8)
                .timeInCurrentFileMs(10_000L)
                .lastBuildSuccess(false).consecutiveFailedBuilds(3)
                .timeSinceLastBuildMs(1_000_000L)
                .buildsInWindow(0).buildSuccessRate(0.0)
                .ideFocusPct(0.40).keyboardIdleMs(200_000L)
                .sessionDurationMs(900_000L).build();

        FlowMetrics lowStress = FlowMetrics.builder()
                .keystrokesPerMin(80).backspaceCount(2).burstConsistency(0.95)
                .syntaxErrorCount(0).compilationErrors(0)
                .errorsIntroduced(0).errorsResolved(0)
                .timeSinceLastErrorMs(1_200_000L)
                .fileChangesLast5Min(1).focusLossCount(0)
                .timeInCurrentFileMs(300_000L)
                .lastBuildSuccess(true).consecutiveFailedBuilds(0)
                .timeSinceLastBuildMs(100_000L)
                .buildsInWindow(2).buildSuccessRate(1.0)
                .ideFocusPct(0.95).keyboardIdleMs(3_000L)
                .sessionDurationMs(1_200_000L).build();

        FlowDetectionResult highResult = detector.detect(highStress);
        FlowDetectionResult lowResult  = detector.detect(lowStress);

        assertTrue(highResult.getStressLevel() > lowResult.getStressLevel(),
                "stress is derived from inverse of error + focus scores, so bad metrics = high stress");
        // also make sure it's actually bounded
        assertTrue(highResult.getStressLevel() <= 1.0 && lowResult.getStressLevel() >= 0.0);
    }

    @Test
    @DisplayName("SC-11 | Right at the FLOW/EMERGING boundary")
    void sc11_flowEmergingBoundary_shouldClassifyCorrectly() {
        // i spent ages tuning these values to land right on the 0.65 boundary.
        // it should classify as one or the other — the point is it doesn't
        // fall through to NEUTRAL or jump to DEEP_FLOW
        FlowMetrics nearFlowThreshold = FlowMetrics.builder()
                .keystrokesPerMin(55).backspaceCount(6).burstConsistency(0.70)
                .syntaxErrorCount(1).compilationErrors(0)
                .errorsIntroduced(1).errorsResolved(0)
                .timeSinceLastErrorMs(200_000L)
                .fileChangesLast5Min(4).focusLossCount(2)
                .timeInCurrentFileMs(130_000L)
                .lastBuildSuccess(true).buildsInWindow(1).buildSuccessRate(1.0)
                .timeSinceLastBuildMs(180_000L)
                .ideFocusPct(0.65).aiSuggestionsAccepted(0)
                .keyboardIdleMs(20_000L)
                .sessionDurationMs(660_000L)
                .build();

        FlowDetectionResult result = detector.detect(nearFlowThreshold);

        assertTrue(
                result.getState() == FlowState.FLOW || result.getState() == FlowState.EMERGING,
                "expected FLOW or EMERGING at the boundary, got " + result.getState()
                        + " (composite: " + String.format("%.3f", result.getFlowTally()) + ")"
        );
    }

    @Test
    @DisplayName("SC-12 | setWeights() actually changes the output")
    void sc12_configurableWeights_shouldMutateComposite() {
        // high typing score but terrible error score — then i flip the weights
        // so errors matter more than typing. composite should drop.
        FlowMetrics goodTypingBadErrors = FlowMetrics.builder()
                .keystrokesPerMin(80).backspaceCount(2).burstConsistency(0.90)
                .syntaxErrorCount(8).compilationErrors(6)
                .errorsIntroduced(3).errorsResolved(0)
                .timeSinceLastErrorMs(5_000L)
                .fileChangesLast5Min(2).focusLossCount(1)
                .timeInCurrentFileMs(200_000L)
                .lastBuildSuccess(true).buildsInWindow(1).buildSuccessRate(1.0)
                .timeSinceLastBuildMs(120_000L)
                .ideFocusPct(0.80).keyboardIdleMs(10_000L)
                .sessionDurationMs(900_000L)
                .build();

        double defaultComposite = detector.detect(goodTypingBadErrors).getFlowTally();

        // now make errors worth 0.40 instead of 0.25, and typing only 0.10 instead of 0.30
        detector.setWeights(0.10, 0.40, 0.20, 0.15, 0.15);
        double invertedComposite = detector.detect(goodTypingBadErrors).getFlowTally();

        assertTrue(invertedComposite < defaultComposite,
                "bumping error weight when errors are bad should tank the composite. "
                        + "default: " + defaultComposite + ", inverted: " + invertedComposite);
    }
}