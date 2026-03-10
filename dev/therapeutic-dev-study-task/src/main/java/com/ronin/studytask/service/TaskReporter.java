package com.ronin.studytask.service;

import com.ronin.studytask.model.Task;
import com.ronin.studytask.model.TaskPriority;
import com.ronin.studytask.model.TaskStatus;

import java.util.List;

/**
 * Generates summary reports from a collection of tasks.
 */
public class TaskReporter {

    /**
     * Generates a formatted summary report of the task collection.
     *
     * Includes:
     *   - Total task count
     *   - Count by status (TODO, IN_PROGRESS, BLOCKED, DONE)
     *   - Count of high-priority tasks (HIGH + CRITICAL) via filterByPriorityRange
     *   - Count of overdue tasks
     *
     * @param tasks the task list to report on
     * @return formatted summary string
     */
    public static String generateSummary(List<Task> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            return "No tasks to report.";
        }

        int total = tasks.size();

        // Count by status
        long todoCount = tasks.stream()
                .filter(t -> t.getStatus() == TaskStatus.TODO).count();
        long inProgressCount = tasks.stream()
                .filter(t -> t.getStatus() == TaskStatus.IN_PROGRESS).count();
        long blockedCount = tasks.stream()
                .filter(t -> t.getStatus() == TaskStatus.BLOCKED).count();
        long doneCount = tasks.stream()
                .filter(t -> t.getStatus() == TaskStatus.DONE).count();

        // High priority tasks (HIGH and CRITICAL)
        List<Task> highPriorityTasks = TaskFilter.filterByPriorityRange(tasks, TaskPriority.HIGH);
        int highPriorityCount = highPriorityTasks.size();

        // Overdue tasks
        List<Task> overdueTasks = TaskFilter.filterOverdue(tasks);
        int overdueCount = overdueTasks.size();

        StringBuilder sb = new StringBuilder();
        sb.append("=== Task Summary Report ===\n");
        sb.append(String.format("  Total tasks:        %d%n", total));
        sb.append(String.format("  TODO:               %d%n", todoCount));
        sb.append(String.format("  IN_PROGRESS:        %d%n", inProgressCount));
        sb.append(String.format("  BLOCKED:            %d%n", blockedCount));
        sb.append(String.format("  DONE:               %d%n", doneCount));
        sb.append(String.format("  High Priority:      %d%n", highPriorityCount));
        sb.append(String.format("  Overdue:            %d%n", overdueCount));
        sb.append("===========================\n");

        return sb.toString();
    }

    /**
     * Generates a breakdown of tasks by priority level.
     *
     * @param tasks the task list
     * @return formatted priority breakdown string
     */
    public static String generatePriorityBreakdown(List<Task> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            return "No tasks to report.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("=== Priority Breakdown ===\n");

        for (TaskPriority priority : TaskPriority.values()) {
            long count = tasks.stream()
                    .filter(t -> t.getPriority() == priority)
                    .count();
            sb.append(String.format("  %-10s %d%n", priority + ":", count));
        }

        sb.append("==========================\n");
        return sb.toString();
    }
}
