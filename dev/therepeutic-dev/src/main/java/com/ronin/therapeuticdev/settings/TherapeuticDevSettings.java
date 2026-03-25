package com.ronin.therapeuticdev.settings;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * all user-configurable settings for the plugin, persisted to therapeutic-dev.xml.
 *
 * i made every field public because PersistentStateComponent serialises via
 * XmlSerializerUtil which needs direct field access. using private fields with
 * getters/setters would need explicit xml annotations for no real benefit
 * in a single-developer plugin.
 *
 * the weight fields (weightTyping, weightErrors, etc.) help to kinda mirror the defaults in
 * FlowDetector. they're exposed here so the settings panel could eventually let
 * users adjust them, but right now FlowDetector uses its own internal defaults AS INTENDED.
 * connecting these to FlowDetector's setWeights() is a post-study task, i really really don't
 * want participants accidentally changing algorithm weights during the study, and it should stay fixed.
 */
@Service(Service.Level.APP)
@State(
    name = "TherapeuticDevSettings",
    storages = @Storage("therapeutic-dev.xml")
)
public final class TherapeuticDevSettings implements PersistentStateComponent<TherapeuticDevSettings> {

    // THEREPEUTIC DEV : DETECTION
    // these thresholds control sensitivity — higher flowThreshold means the
    // developer needs a higher composite score to be classified as FLOW
    public double flowThreshold = 0.65;
    public double procrastinatingThreshold = 0.35;
    public double optimalKpm = 80.0;

    // THEREPEUTIC DEV : BREAKS

    // how long before suggesting a break, and whether to do it automatically
    public int breakIntervalMinutes = 60;
    public boolean autoBreakSuggestions = true;
    public int minFlowDurationForBreak = 30;

    // THEREPEUTIC DEV : NOTIFICATIONS

    // modal = BreakNotificationDialog, balloon = ide notification popup (not yet implemented)
    public boolean useModalNotifications = true;
    public boolean useBalloonNotifications = false;
    public boolean showStatusBarWidget = true;
    public boolean playSoundOnBreak = false;

    // THEREPEUTIC DEV : ACTIVITY VIEW

    public boolean enableActivityHeatmap = true;
    public boolean trackContextSwitches = true;

    // how many file switches per interval before the activity tab highlights it as a warning
    public int contextSwitchWarningThreshold = 7;

    // THEREPEUTIC DEV : GRAPH VIEW

    public boolean enableGraphView = true;
    public boolean autoRefreshGraph = false;
    public boolean showDependencies = true;

    // THEREPEUTIC DEV : DATA

    // master toggles for metric collection and sqlite persistence
    // disabling collectMetrics stops all listener recording
    // disabling persistSnapshots keeps live detection running but doesn't write to db
    public boolean collectMetrics = true;
    public boolean persistSnapshots = true;

    // THEREPEUTIC DEV : WEIGHTS...but advanced

    // these mirror FlowDetector's defaults — exposed for future settings panel integration
    public double weightTyping = 0.30;
    public double weightErrors = 0.25;
    public double weightFocus = 0.20;
    public double weightBuilds = 0.15;
    public double weightActivity = 0.10;

    public static TherapeuticDevSettings getInstance() {
        return ApplicationManager.getApplication().getService(TherapeuticDevSettings.class);
    }

    @Override
    public @Nullable TherapeuticDevSettings getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull TherapeuticDevSettings state) {
        XmlSerializerUtil.copyBean(state, this);
    }

    /**
     * resets everything to factory defaults.
     * i deem it useful for testing or if a participant's settings get into a weird state.
     */
    public void resetToDefaults() {
        flowThreshold = 0.65;
        procrastinatingThreshold = 0.35;
        optimalKpm = 80.0;

        breakIntervalMinutes = 60;
        autoBreakSuggestions = true;
        minFlowDurationForBreak = 30;

        useModalNotifications = true;
        useBalloonNotifications = false;
        showStatusBarWidget = true;
        playSoundOnBreak = false;

        enableActivityHeatmap = true;
        trackContextSwitches = true;
        contextSwitchWarningThreshold = 7;

        enableGraphView = true;
        autoRefreshGraph = false;
        showDependencies = true;

        collectMetrics = true;
        persistSnapshots = true;

        weightTyping = 0.30;
        weightErrors = 0.25;
        weightFocus = 0.20;
        weightBuilds = 0.15;
        weightActivity = 0.10;
    }

    /**
     * good ol sanity check - if weights don't sum to 1.0 the composite score maths breaks.
     * tolerance of 0.001 handles floating point imprecision.
     */
    public boolean areWeightsValid() {
        double sum = weightTyping + weightErrors + weightFocus + weightBuilds + weightActivity;
        return Math.abs(sum - 1.0) < 0.001;
    }
}
