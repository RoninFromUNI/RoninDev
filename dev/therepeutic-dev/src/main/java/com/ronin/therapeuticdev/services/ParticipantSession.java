package com.ronin.therapeuticdev.services;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;

/**
 * holds the current participant ID and persists it across ide restarts.
 *
 * i use PersistentStateComponent rather than a database because all i need is a
 * single string that survives restarts. the @State annotation tells intellij to
 * serialise this to therapeutic-dev-participant.xml in the config directory.
 *
 * THE REASON why i use the word persist alot because i need a buzzword to just explain the ability
 * to write data to a drable storage that even survives termination. i dont want to say save which means generic
 * or store or write. thats my justification.
 *
 * the participant ID serves two purposes:
 *   1. it gates the tool window — FlowStateToolWindowFactory checks hasParticipant()
 *      and shows ParticipantSetupPanel if no ID is set
 *   2. it namespaces metric storage — MetricCollector prepends the participant ID
 *      to the session UUID (e.g. "P001_uuid-abc123") so each participant's sqlite
 *      rows are trivially separable in the analysis phase
 *
 * this was added based on supervisor feedback recommending personalisation/login,
 * which is grounded in muller and fritz's finding that biometric baselines vary
 * between individuals. even though my metrics are behavioural not biometric,
 * the per-developer namespacing principle applies.
 */
@State(name = "ParticipantSession", storages = @Storage("therapeutic-dev-participant.xml"))
public class ParticipantSession implements PersistentStateComponent<ParticipantSession> {

    // public field because PersistentStateComponent serialises via XmlSerializerUtil
    // which needs direct field access. making this private would require explicit
    // getter/setter annotations that add complexity for no benefit.
    public String participantId = "";

    public static ParticipantSession getInstance() {
        return ApplicationManager.getApplication().getService(ParticipantSession.class);
    }

    public boolean hasParticipant() {
        return !participantId.isBlank();
    }

    public void setParticipantId(String id) {
        this.participantId = id == null ? "" : id.trim();
    }

    public void clearParticipant() {
        this.participantId = "";
    }

    public String getParticipantId() {
        return participantId;
    }

    @Override
    public ParticipantSession getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull ParticipantSession state) {
        XmlSerializerUtil.copyBean(state, this);
    }
}
