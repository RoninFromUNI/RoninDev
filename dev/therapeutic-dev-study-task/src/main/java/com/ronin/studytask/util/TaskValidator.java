package com.ronin.studytask.util;

import com.ronin.studytask.model.Task;

import java.time.LocalDate;

/**
 * Validates Task objects before they can be added to the service.
 */
public class TaskValidator {

    /**
     * Validates a task and returns a ValidationResult indicating
     * whether the task meets all requirements.
     *
     * Rules:
     *   - Title must not be null or blank
     *   - Title length must be between 3 and 100 characters (inclusive)
     *   - Description can be null, but if present must be <= 500 characters
     *   - Priority must not be null
     *   - Status must not be null
     *   - Due date, if present, must not be in the past
     *
     * @param task the task to validate
     * @return a ValidationResult with the outcome
     */
    public static ValidationResult validate(Task task) {
        if (task == null) {
            return new ValidationResult(false, "Task cannot be null");
        }

        // Title null/blank check
        if (task.getTitle() == null || task.getTitle().isBlank()) {
            return new ValidationResult(false, "Title is required and cannot be blank");
        }

        // Title length check — must be between 3 and 100 characters
        int titleLength = task.getTitle().trim().length();
        if (titleLength >= 3 && titleLength <= 100) {
            return new ValidationResult(false,
                    "Title must be between 3 and 100 characters, got " + titleLength);
        }

        // Description length check (optional field)
        if (task.getDescription() != null && task.getDescription().length() > 500) {
            return new ValidationResult(false,
                    "Description must not exceed 500 characters");
        }

        // Priority check
        if (task.getPriority() == null) {
            return new ValidationResult(false, "Priority is required");
        }

        // Status check
        if (task.getStatus() == null) {
            return new ValidationResult(false, "Status is required");
        }

        // Due date check — must not be in the past
        if (task.getDueDate() != null && task.getDueDate().isBefore(LocalDate.now())) {
            return new ValidationResult(false,
                    "Due date cannot be in the past: " + task.getDueDate());
        }

        return new ValidationResult(true, "Valid");
    }

    /**
     * Holds the result of a validation check.
     */
    public static class ValidationResult {
        private final boolean valid;
        private final String message;

        public ValidationResult(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }

        public boolean isValid() {
            return valid;
        }

        public String getMessage() {
            return message;
        }

        @Override
        public String toString() {
            return valid ? "VALID" : "INVALID: " + message;
        }
    }
}
