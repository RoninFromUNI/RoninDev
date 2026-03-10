package com.ronin.studytask.service;

import com.ronin.studytask.model.Task;
import com.ronin.studytask.model.TaskPriority;
import com.ronin.studytask.model.TaskStatus;
import com.ronin.studytask.util.DateUtils;
import com.ronin.studytask.util.TaskValidator;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Central service for managing a collection of Task objects.
 * Provides CRUD operations, filtering, sorting, and priority escalation.
 */
public class TaskService {

    private final List<Task> tasks = new ArrayList<>();

    /**
     * Validates and adds a task to the collection.
     *
     * @param task the task to add
     * @return true if added successfully, false if validation failed
     */
    public boolean addTask(Task task) {
        TaskValidator.ValidationResult result = TaskValidator.validate(task);
        if (!result.isValid()) {
            System.out.println("  [WARN] Validation failed: " + result.getMessage());
            return false;
        }
        tasks.add(task);
        return true;
    }

    /**
     * Adds a task directly without validation. Intended for bulk imports,
     * data migrations, or test fixtures where the source is trusted.
     *
     * @param task the task to add
     */
    public void addTaskDirectly(Task task) {
        tasks.add(task);
    }

    /**
     * Removes a task by its ID.
     *
     * @param id the task ID to remove
     * @return true if a task was removed, false if not found
     */
    public boolean removeTask(String id) {
        return tasks.removeIf(task -> task.getId().equals(id));
    }

    /**
     * Retrieves a task by its ID.
     *
     * @param id the task ID
     * @return an Optional containing the task, or empty if not found
     */
    public Optional<Task> getTaskById(String id) {
        return tasks.stream()
                .filter(task -> task.getId().equals(id))
                .findFirst();
    }

    /**
     * Returns an unmodifiable view of all tasks.
     */
    public List<Task> getAllTasks() {
        return Collections.unmodifiableList(tasks);
    }

    /**
     * Returns all tasks matching the given status.
     *
     * @param status the status to filter by
     * @return list of matching tasks
     */
    public List<Task> getTasksByStatus(TaskStatus status) {
        List<Task> result = new ArrayList<>();
        for (Task task : tasks) {
            if (task.getStatus().equals(status)) {
                result.add(task);
            }
        }
        return result;
    }

    /**
     * Returns all tasks matching the given priority.
     *
     * @param priority the priority to filter by
     * @return list of matching tasks
     */
    public List<Task> getTasksByPriority(TaskPriority priority) {
        return tasks.stream()
                .filter(task -> priority.equals(task.getPriority()))
                .collect(Collectors.toList());
    }

    /**
     * Updates the status of a task.
     *
     * @param id        the task ID
     * @param newStatus the new status to set
     * @return true if the task was found and updated, false otherwise
     */
    public boolean updateTaskStatus(String id, TaskStatus newStatus) {
        Optional<Task> taskOpt = getTaskById(id);
        if (taskOpt.isEmpty()) {
            System.out.println("  [WARN] Task not found: " + id);
            return false;
        }

        Task task = taskOpt.get();
        task.setStatus(newStatus);
        return true;
    }

    /**
     * Escalates the priority of a task by one level.
     * LOW -> MEDIUM -> HIGH -> CRITICAL
     *
     * @param id the task ID to escalate
     * @return true if priority was escalated, false otherwise
     */
    public boolean escalatePriority(String id) {
        Optional<Task> taskOpt = getTaskById(id);
        if (taskOpt.isEmpty()) {
            System.out.println("  [WARN] Task not found: " + id);
            return false;
        }

        Task task = taskOpt.get();
        TaskPriority current = task.getPriority();

        if (current == TaskPriority.LOW) {
            task.setPriority(TaskPriority.MEDIUM);
            return true;
        } else if (current == TaskPriority.MEDIUM) {
            task.setPriority(TaskPriority.HIGH);
            return true;
        } else if (current.ordinal() > TaskPriority.HIGH.ordinal()) {
            task.setPriority(TaskPriority.CRITICAL);
            return true;
        }

        return false;
    }

    /**
     * Sorts the internal task list by due date using DateUtils.compare.
     * Tasks with earlier due dates should appear first.
     */
    public void sortByDueDate() {
        tasks.sort((a, b) -> DateUtils.compare(a.getDueDate(), b.getDueDate()));
    }

    /**
     * Returns all tasks with a due date before the given date.
     *
     * @param date the cutoff date
     * @return list of tasks due before the given date
     */
    public List<Task> getTasksDueBefore(LocalDate date) {
        return tasks.stream()
                .filter(task -> task.getDueDate() != null && task.getDueDate().isBefore(date))
                .collect(Collectors.toList());
    }
}
