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
 * Persistent settings for Therapeutic Dev plugin.
 * 
 * <p>Stores user preferences in IDE configuration:
 * - Detection sensitivity thresholds
 * - Break interval preferences
 * - Notification style options
 * - Feature toggles
 *
 * @see <a href="https://plugins.jetbrains.com/docs/intellij/persisting-state-of-components.html">
 *      IntelliJ Platform SDK - Persisting State</a>
 */
@Service(Service.Level.APP)
@State(
    name = "TherapeuticDevSettings",
    storages = @Storage("therapeutic-dev.xml")
)
public final class TherapeuticDevSettings implements PersistentStateComponent<TherapeuticDevSettings> {

    // ==================== DETECTION SETTINGS ====================
    
    /** Flow state threshold (0.0 - 1.0). Score >= this = FLOW state */
    public double flowThreshold = 0.65;
    
    /** Procrastinating threshold. Score <= this = PROCRASTINATING state */
    public double procrastinatingThreshold = 0.35;
    
    /** Optimal keystrokes per minute for flow detection */
    public double optimalKpm = 80.0;
    
    // ==================== BREAK SETTINGS ====================
    
    /** Break suggestion interval in minutes */
    public int breakIntervalMinutes = 60;
    
    /** Enable automatic break suggestions */
    public boolean autoBreakSuggestions = true;
    
    /** Minimum flow duration (minutes) before suggesting break */
    public int minFlowDurationForBreak = 30;
    
    // ==================== NOTIFICATION SETTINGS ====================
    
    /** Show break suggestions as modal dialog */
    public boolean useModalNotifications = true;
    
    /** Show break suggestions as balloon notification */
    public boolean useBalloonNotifications = false;
    
    /** Show flow state in status bar */
    public boolean showStatusBarWidget = true;
    
    /** Play sound on break suggestion */
    public boolean playSoundOnBreak = false;
    
    // ==================== ACTIVITY VIEW SETTINGS ====================
    
    /** Enable activity heatmap view */
    public boolean enableActivityHeatmap = true;
    
    /** Track context switches */
    public boolean trackContextSwitches = true;
    
    /** Warning threshold for context switches per interval */
    public int contextSwitchWarningThreshold = 7;
    
    // ==================== GRAPH VIEW SETTINGS ====================
    
    /** Enable architecture graph view */
    public boolean enableGraphView = true;
    
    /** Auto-refresh graph on file changes */
    public boolean autoRefreshGraph = false;
    
    /** Show dependencies in graph */
    public boolean showDependencies = true;
    
    // ==================== DATA SETTINGS ====================
    
    /** Enable metric collection */
    public boolean collectMetrics = true;
    
    /** Store snapshots to database */
    public boolean persistSnapshots = true;
    
    // ==================== WEIGHT SETTINGS (Advanced) ====================
    
    /** Typing metric weight */
    public double weightTyping = 0.30;
    
    /** Errors metric weight */
    public double weightErrors = 0.25;
    
    /** Focus metric weight */
    public double weightFocus = 0.20;
    
    /** Builds metric weight */
    public double weightBuilds = 0.15;
    
    /** Activity metric weight */
    public double weightActivity = 0.10;

    /**
     * Gets the singleton instance.
     */
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
     * Resets all settings to defaults.
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
     * Validates that weights sum to 1.0.
     */
    public boolean areWeightsValid() {
        double sum = weightTyping + weightErrors + weightFocus + weightBuilds + weightActivity;
        return Math.abs(sum - 1.0) < 0.001;
    }
}
