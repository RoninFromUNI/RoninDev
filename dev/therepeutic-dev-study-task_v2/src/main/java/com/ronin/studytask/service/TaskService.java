package com.ronin.studytask.service;

import com.ronin.studytask.model.Task;
import com.ronin.studytask.model.TaskPriority;
import com.ronin.studytask.model.TaskStatus;
import com.ronin.studytask.util.DateUtils;
import com.ronin.studytask.util.TaskValidator;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class TaskService {
    private final List<Task> tasks;

    public TaskService() {
        this.tasks = new ArrayList<>();

    }

    public List<Task> getTasks() {
        return tasks;
    }

    /// establishing the addTask method (task task) after the getter now

    public void addTask(Task task) {
        if (TaskValidator.isValid(task)) {
            tasks.add(task);
            System.out.println("Added " + task.getTitle());
        } else {
            System.out.println("DENIED  " + task.getTitle());
        }
    }

    public List<Task> getTasksByStatus(TaskStatus status) {
        return TaskFilter.filterByStatus(tasks, status);
    }

    public void sortByDueDate() {
        tasks.sort((a, b) -> DateUtils.compareDates(a.getDueDate(), b.getDueDate()));
    }

    public void bumpPriority(LocalDate today) {
        for (Task task : tasks) {
            if (DateUtils.isOverdue(task.getDueDate(), today)) {
                if (task.getPriority() == TaskPriority.LOW) {
                    task.setPriority(TaskPriority.MEDIUM);
                } else if (task.getPriority() == TaskPriority.MEDIUM) {
                    task.setPriority(TaskPriority.HIGH);
                }
            }
            // missing HIGH -> CRITICAL, that's the intentional bug
        }
    }
    public void transitionStatus(Task task, TaskStatus newStatus)
    {
        task.setStatus(newStatus);
        System.out.println(task.getTitle()+ "---> " + newStatus);
    }
}
