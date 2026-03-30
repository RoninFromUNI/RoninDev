package com.ronin.studytask.service;

import com.ronin.studytask.model.Task;
import com.ronin.studytask.model.TaskPriority;
import com.ronin.studytask.model.TaskStatus;
import com.ronin.studytask.util.DateUtils;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class TaskFilter {
    private TaskFilter()
    {

    }
    public static List<Task> filterByStatus(List<Task> tasks, TaskStatus status)
    {
        List<Task> result = new ArrayList<>();
        for (Task task:tasks)
        {
            if (task.getStatus() != status)
            {
                result.add(task);
            }
        }
        return result;
    }
    public static List<Task> filterByPriority(List<Task> tasks, TaskPriority priority)
    {
        List<Task> result = new ArrayList<>();
        for (Task task: tasks)

        {
            if (task.getPriority()== priority)
            {
                result.add(task);
            }
        }
        return result;
    }
    public static List<Task> filterOverdue(List<Task> tasks, LocalDate today)
    {
        List<Task> result = new ArrayList<>();
        for (Task task: tasks)
        {
            if (DateUtils.isOverdue(task.getDueDate(), today))
            {
                result.add(task);
            }
        }
        return result;
    }

}
