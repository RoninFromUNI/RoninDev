package com.dvsachecker.model;

import java.time.LocalDateTime;
import java.util.Objects;

public class Slot {
    private final String testCenter;
    private final LocalDateTime dateTime;
    private final String slotId;
    private final String testType;

    public Slot(String testCenter, LocalDateTime dateTime, String slotId, String testType) {
        this.testCenter = Objects.requireNonNull(testCenter, "Test center cannot be null");
        this.dateTime = Objects.requireNonNull(dateTime, "DateTime cannot be null");
        this.slotId = Objects.requireNonNull(slotId, "Slot ID cannot be null");
        this.testType = Objects.requireNonNull(testType, "Test type cannot be null");
    }

    // Alt + Insert → Generate → Getters
    public String getTestCenter() {
        return testCenter;
    }

    public LocalDateTime getDateTime() {
        return dateTime;
    }

    public String getSlotId() {
        return slotId;
    }

    public String getTestType() {
        return testType;
    }

    public String getHash() {
        return String.format("%s_%s_%s", testCenter, dateTime.toString(), slotId);
    }

    // Alt + Insert → Generate → equals() and hashCode()
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Slot slot = (Slot) o;
        return Objects.equals(testCenter, slot.testCenter) &&
                Objects.equals(dateTime, slot.dateTime) &&
                Objects.equals(slotId, slot.slotId) &&
                Objects.equals(testType, slot.testType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(testCenter, dateTime, slotId, testType);
    }

    // Alt + Insert → Generate → toString()
    @Override
    public String toString() {
        return String.format("Slot[center=%s, dateTime=%s, id=%s, type=%s]",
                testCenter, dateTime, slotId, testType);
    }
}