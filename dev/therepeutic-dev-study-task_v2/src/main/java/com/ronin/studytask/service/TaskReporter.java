package com.ronin.studytask.service;

import com.ronin.studytask.model.Task;

import com.ronin.studytask.model.TaskPriority;
import com.ronin.studytask.model.TaskStatus;
import java.time.LocalDate;
import java.util.List;


public class TaskReporter {
    private TaskReporter() {

    }

    public static String genReport(List<Task> tasks) {
        int total = tasks.size();

        int todoCount = TaskFilter.filterByStatus(tasks, TaskStatus.TODO).size();
        int inProgressCount = TaskFilter.filterByStatus(tasks, TaskStatus.IN_PROGRESS).size();
        int blockedCount = TaskFilter.filterByStatus(tasks, TaskStatus.BLOCKED).size();
        int doneCount = TaskFilter.filterByStatus(tasks, TaskStatus.DONE).size();
        //now to do the priority ones
        int lowCount = TaskFilter.filterByPriority(tasks, TaskPriority.LOW).size();
        int medCount = TaskFilter.filterByPriority(tasks, TaskPriority.MEDIUM).size();
        int highCount = TaskFilter.filterByPriority(tasks, TaskPriority.HIGH).size();
        int criticalCount = TaskFilter.filterByPriority(tasks, TaskPriority.CRITICAL).size();
        int overdueCount = TaskFilter.filterOverdue(tasks, LocalDate.now()).size();
        //returning now the report for this crap

        return "___TASK REPORT___\n" + "Total; " + total + "\n"
                + "TODO: " + todoCount + "\n"
                + "IN_PROGRESS: " + inProgressCount + "\n"
                + "BLOCKED: " + blockedCount + "\n"
                + "DONE: " + doneCount + "\n"
                + "Low: " + lowCount + "\n"
                + "Medium: " + medCount + "\n"
                + "High: " + highCount + "\n"
                + "Critical: " + criticalCount + "\n"
                + "Overdue: " + overdueCount;
    }
}
