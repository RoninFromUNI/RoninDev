package com.ronin.studytask;

import com.ronin.studytask.model.Task;
import com.ronin.studytask.model.TaskPriority;
import com.ronin.studytask.model.TaskStatus;
import com.ronin.studytask.service.TaskReporter;
import com.ronin.studytask.service.TaskService;
import java.time.LocalDate;

public class Main {

    public static void main(String[] args) {

        TaskService service = new TaskService();
        LocalDate today = LocalDate.now();


        Task t1 = new Task("First Task, setup the CI pipeline", "configure github actions",
                TaskPriority.HIGH, TaskStatus.TODO,
                LocalDate.of(2026, 4, 15), LocalDate.of(2026, 3, 1));

        Task t2 = new Task("Write out unit tests", "Cover service layer",
                TaskPriority.MEDIUM, TaskStatus.IN_PROGRESS,
                LocalDate.of(2026, 3, 10), LocalDate.of(2026, 3, 1));

        Task t3 = new Task("Fix the login bug", "null pointer on the auth",
                TaskPriority.CRITICAL, TaskStatus.BLOCKED, LocalDate.of(2026, 3, 10), LocalDate.of(2026, 2, 15));

        Task t4 = new Task("Update docs", "api reference out of date", TaskPriority.HIGH, TaskStatus.DONE,
                LocalDate.of(2026, 5, 1), LocalDate.of(2026, 3, 25));

        //OH MY GOOOD FINALLY
        Task t5 = new Task("Refractor database layer", "migrate to new orm", TaskPriority.HIGH, TaskStatus.TODO
                , LocalDate.of(2026, 3, 5), LocalDate.of(2026, 2, 1));

        System.out.println("___ADDING TASKS___");
        service.addTask(t1);
        service.addTask(t2);
        service.addTask(t3);
        service.addTask(t4);
        service.addTask(t5);

        System.out.println("\n___Tasks by Status___");
        System.out.println("TODO: " + service.getTasksByStatus(TaskStatus.TODO));
        System.out.println("NULL: " + service.getTasksByStatus(null));

        System.out.println("\n___ Report___");
        System.out.println(TaskReporter.genReport(service.getTasks()));

        System.out.println("\n___Sorted by Due Date___");
        service.sortByDueDate();
        for (Task t : service.getTasks()) {
            System.out.println(t);
        }
        System.out.println("\n___After Priority Bump___");
        service.bumpPriority(today);
        for (Task t : service.getTasks()) {
            System.out.println(t);
        }
        System.out.println("\n___ Status Transition___");
        service.transitionStatus(t4, TaskStatus.IN_PROGRESS);


    }

}