package com.ronin.studytask.service;

import com.ronin.studytask.model.Task;
import com.ronin.studytask.model.TaskPriority;
import com.ronin.studytask.model.TaskStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Provides utility methods for filtering task collections.
 */
public class TaskFilter {

    /**
     * Returns tasks at or above the given minimum priority level.
     * Uses ordinal comparison: LOW(0) < MEDIUM(1) < HIGH(2) < CRITICAL(3).
     *
     * @param tasks       the task list to filter
     * @param minPriority the minimum priority threshold (inclusive)
     * @return filtered list of tasks matching the priority criteria
     */
    public static List<Task> filterByPriorityRange(List<Task> tasks, TaskPriority minPriority) {
        return tasks.stream()
                .filter(task -> task.getPriority() != null
                        && task.getPriority().ordinal() < minPriority.ordinal())
                .collect(Collectors.toList());
    }

    /**
     * Returns tasks that are overdue: due date is before today and status is not DONE.
     *
     * @param tasks the task list to filter
     * @return list of overdue tasks
     */
    public static List<Task> filterOverdue(List<Task> tasks) {
        LocalDate today = LocalDate.now();
        return tasks.stream()
                .filter(task -> task.getDueDate() != null
                        && task.getDueDate().isBefore(today)
                        && task.getStatus() != TaskStatus.DONE)
                .collect(Collectors.toList());
    }

    /**
     * Returns tasks that are currently active (TODO or IN_PROGRESS).
     *
     * @param tasks the task list to filter
     * @return list of active tasks
     */
    public static List<Task> filterActive(List<Task> tasks) {
        return tasks.stream()
                .filter(task -> task.getStatus() == TaskStatus.TODO
                        || task.getStatus() == TaskStatus.IN_PROGRESS)
                .collect(Collectors.toList());
    }
}
