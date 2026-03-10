package com.ronin.therapeuticdev.services;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Persists the current participant ID across IDE restarts.
 *
 * Used to namespace session IDs in MetricCollector and gate the
 * tool window behind the participant setup screen.
 */
@State(
    name = "ParticipantSession",
    storages = @Storage("therapeutic-dev-participant.xml")
)
public final class ParticipantSession implements PersistentStateComponent<ParticipantSession> {

    public String participantId = "";

    public static ParticipantSession getInstance() {
        return ApplicationManager.getApplication().getService(ParticipantSession.class);
    }

    public boolean hasParticipant() {
        return !participantId.isBlank();
    }

    public void setParticipantId(String id) {
        this.participantId = id != null ? id.trim() : "";
    }

    public void clearParticipant() {
        this.participantId = "";
    }

    public String getParticipantId() {
        return participantId;
    }

    @Override
    public @Nullable ParticipantSession getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull ParticipantSession state) {
        XmlSerializerUtil.copyBean(state, this);
    }
}
