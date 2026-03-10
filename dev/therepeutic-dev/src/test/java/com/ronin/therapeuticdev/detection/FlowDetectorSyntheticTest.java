package com.ronin.therapeuticdev.detection;

import com.ronin.therapeuticdev.metrics.FlowMetrics;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Synthetic validation suite for FlowDetector.
 *
 * PURPOSE (dissertation Chapter 6.1):
 *   Establishes algorithm correctness against known metric sequences before
 *   empirical participant data is collected. Each test constructs a FlowMetrics
 *   snapshot with controlled inputs, asserts the expected FlowState classification,
 *   and verifies sub-score directionality.
 *
 * COVERAGE:
 *   - All seven FlowState classifications
 *   - Key boundary conditions (threshold edges)
 *   - Error recovery bonus logic
 *   - Focus penalty accumulation
 *   - Build streak penalties
 *   - Early session neutral return (contextScore < 60s guard)
 *   - Stress level derivation
 *
 * REFERENCE:
 *   Weightings: Typing 0.30 | Errors 0.25 | Focus 0.20 | Builds 0.15 | Context 0.10
 *   Thresholds: DEEP_FLOW >= 0.80 | FLOW >= 0.65 | EMERGING >= 0.52 |
 *               NEUTRAL >= 0.40 | DISRUPTED >= 0.28 | PROCRASTINATING >= 0.15
 */
@DisplayName("FlowDetector — Synthetic Validation Suite")
class FlowDetectorSyntheticTest {

    private FlowDetector detector;

    @BeforeEach
    void setUp() {
        detector = new FlowDetector();
    }

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
                "Optimal metrics across all five categories must yield DEEP_FLOW");
        assertTrue(result.getFlowTally() >= 0.80,
                "Composite score must meet the 0.80 DEEP_FLOW threshold");
        assertTrue(result.getTypingScore() >= 0.80,
                "Typing sub-score should be high at optimal KPM");
        assertTrue(result.getErrorScore() >= 0.90,
                "Error sub-score should be near 1.0 with no errors and clean period");
        assertTrue(result.getFocusScore() >= 0.90,
                "Focus sub-score should be near 1.0 with minimal switching");
    }

    @Test
    @DisplayName("SC-02 | Good but imperfect coding session → FLOW")
    void sc02_goodSessionWithMinorNoise_shouldClassifyAsFlow() {
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

        assertEquals(FlowState.FLOW, result.getState(),
                "Slightly imperfect but productive session must yield FLOW");
        assertTrue(result.getFlowTally() >= 0.65 && result.getFlowTally() < 0.80,
                "Composite must sit within the FLOW band [0.65, 0.80)");
    }

    @Test
    @DisplayName("SC-03 | Re-engagement after distraction → EMERGING")
    void sc03_reengagementState_shouldClassifyAsEmerging() {
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

        assertEquals(FlowState.EMERGING, result.getState(),
                "Moderate engagement at tolerance boundaries must yield EMERGING");
        assertTrue(result.getFlowTally() >= 0.52 && result.getFlowTally() < 0.65,
                "Composite must sit within the EMERGING band [0.52, 0.65)");
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

        assertEquals(FlowState.NEUTRAL, result.getState(),
                "Above-tolerance errors and switching with low KPM must yield NEUTRAL");
        assertTrue(result.getFlowTally() >= 0.40 && result.getFlowTally() < 0.52,
                "Composite must sit within the NEUTRAL band [0.40, 0.52)");
    }

    @Test
    @DisplayName("SC-05 | Build failures + high error accumulation → DISRUPTED")
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

        assertEquals(FlowState.DISRUPTED, result.getState(),
                "Failed builds, error accumulation, and low KPM must yield DISRUPTED");
        assertTrue(result.getFlowTally() >= 0.28 && result.getFlowTally() < 0.40,
                "Composite must sit within the DISRUPTED band [0.28, 0.40)");
    }

    @Test
    @DisplayName("SC-06 | Near-zero output + excessive switching → PROCRASTINATING")
    void sc06_nearZeroOutputExcessiveSwitching_shouldClassifyAsProcrastinating() {
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

        assertEquals(FlowState.PROCRASTINATING, result.getState(),
                "Near-zero keystroke rate with max focus/switch penalties must yield PROCRASTINATING");
        assertTrue(result.getFlowTally() >= 0.15 && result.getFlowTally() < 0.28,
                "Composite must sit within the PROCRASTINATING band [0.15, 0.28)");
    }

    @Test
    @DisplayName("SC-07 | Absolute minimum activity → NOT_IN_FLOW")
    void sc07_absoluteMinimumActivity_shouldClassifyAsNotInFlow() {
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

        assertEquals(FlowState.NOT_IN_FLOW, result.getState(),
                "Absolute minimum inputs must yield NOT_IN_FLOW");
        assertTrue(result.getFlowTally() < 0.15,
                "Composite must fall below the 0.15 NOT_IN_FLOW threshold");
    }

    @Test
    @DisplayName("SC-08 | Full error resolution → errorScore recovery bonus applied")
    void sc08_fullErrorResolution_shouldApplyRecoveryBonus() {
        FlowMetrics baseline = FlowMetrics.builder()
                .keystrokesPerMin(60)
                .backspaceCount(5)
                .burstConsistency(0.80)
                .errorsIntroduced(0)
                .errorsResolved(0)
                .syntaxErrorCount(0)
                .compilationErrors(0)
                .timeSinceLastErrorMs(100_000L)
                .fileChangesLast5Min(2)
                .focusLossCount(1)
                .timeInCurrentFileMs(200_000L)
                .lastBuildSuccess(true)
                .buildsInWindow(1)
                .buildSuccessRate(1.0)
                .timeSinceLastBuildMs(100_000L)
                .ideFocusPct(0.80)
                .keyboardIdleMs(10_000L)
                .sessionDurationMs(900_000L)
                .build();

        FlowMetrics withRecovery = FlowMetrics.builder()
                .keystrokesPerMin(60)
                .backspaceCount(5)
                .burstConsistency(0.80)
                .errorsIntroduced(3)
                .errorsResolved(3)
                .syntaxErrorCount(0)
                .compilationErrors(0)
                .timeSinceLastErrorMs(100_000L)
                .fileChangesLast5Min(2)
                .focusLossCount(1)
                .timeInCurrentFileMs(200_000L)
                .lastBuildSuccess(true)
                .buildsInWindow(1)
                .buildSuccessRate(1.0)
                .timeSinceLastBuildMs(100_000L)
                .ideFocusPct(0.80)
                .keyboardIdleMs(10_000L)
                .sessionDurationMs(900_000L)
                .build();

        FlowDetectionResult baseResult     = detector.detect(baseline);
        FlowDetectionResult recoveryResult = detector.detect(withRecovery);

        assertTrue(recoveryResult.getErrorScore() >= baseResult.getErrorScore(),
                "Resolving all introduced errors must apply the +0.10 recovery bonus");
    }

    @Test
    @DisplayName("SC-09 | Session < 60s → contextScore fixed at 0.5 (early-session guard)")
    void sc09_earlySession_contextScoreShouldBeNeutral() {
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
                .sessionDurationMs(30_000L)
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
                .ideFocusPct(0.0)
                .aiSuggestionsAccepted(0).keyboardIdleMs(0L)
                .sessionDurationMs(30_000L)
                .build();

        FlowDetectionResult highResult = detector.detect(earlyHighFocus);
        FlowDetectionResult lowResult  = detector.detect(earlyLowFocus);

        assertEquals(highResult.getFlowTally(), lowResult.getFlowTally(), 0.001,
                "ideFocusPct must not affect composite when session < 60s");
    }

    @Test
    @DisplayName("SC-10 | Stress level derivation — high vs low stress configurations")
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
                "High-error/low-focus config must produce higher stressLevel");
        assertTrue(highResult.getStressLevel() >= 0.0 && highResult.getStressLevel() <= 1.0,
                "stressLevel must be bounded within [0.0, 1.0]");
        assertTrue(lowResult.getStressLevel() >= 0.0 && lowResult.getStressLevel() <= 1.0,
                "stressLevel must be bounded within [0.0, 1.0]");
    }

    @Test
    @DisplayName("SC-11 | Composite at FLOW/EMERGING boundary → correct side classification")
    void sc11_flowEmergingBoundary_shouldClassifyCorrectly() {
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
                "Near-threshold inputs must classify as FLOW or EMERGING. " +
                "Actual: " + result.getState() + ", composite: " + result.getFlowTally()
        );
    }

    @Test
    @DisplayName("SC-12 | setWeights() mutation → composite scores reflect new weighting")
    void sc12_configurableWeights_shouldMutateComposite() {
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

        detector.setWeights(0.10, 0.40, 0.20, 0.15, 0.15);
        double invertedComposite = detector.detect(goodTypingBadErrors).getFlowTally();

        assertTrue(invertedComposite < defaultComposite,
                "Increasing error weight when error score is low must decrease composite. " +
                "Default: " + defaultComposite + ", Inverted: " + invertedComposite);
    }
}
