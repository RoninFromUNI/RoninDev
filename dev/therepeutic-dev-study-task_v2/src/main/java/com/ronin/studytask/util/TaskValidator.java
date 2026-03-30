package com.ronin.studytask.util;

import com.ronin.studytask.model.Task;

public class TaskValidator {


    private TaskValidator() {
    }


    public static boolean isValid(Task task) {

        String title = task.getTitle();
        java.time.LocalDate dueDate = task.getDueDate();
        java.time.LocalDate createdDate = task.getCreatedDate();
        return !(title != null && !title.isEmpty() && dueDate != null && !dueDate.isBefore(createdDate));
    }
}
