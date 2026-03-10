package com.ronin.studytask;

import com.ronin.studytask.model.Task;
import com.ronin.studytask.model.TaskPriority;
import com.ronin.studytask.model.TaskStatus;
import com.ronin.studytask.service.TaskReporter;
import com.ronin.studytask.service.TaskService;
import com.ronin.studytask.util.DateUtils;

import java.time.LocalDate;

/**
 * Main entry point for the therapeutic-dev-study-task project.
 *
 * This program exercises a simple task management system. There are 5 intentional
 * bugs hidden across the codebase. Run this program, observe the unexpected
 * behaviour, and try to find and fix each bug.
 *
 * Each section is wrapped in try-catch so that one bug does not prevent the
 * rest of the program from running.
 */
public class Main {

    public static void main(String[] args) {
        System.out.println("================================================");
        System.out.println("  Therapeutic Dev — Study Task Debugging Exercise");
        System.out.println("================================================\n");

        TaskService service = new TaskService();

        // ======================================================================
        // SECTION 1: Adding Tasks via addTask (validates through TaskValidator)
        // Exercises Bug 1 — inverted title validation rejects valid titles
        // ======================================================================
        System.out.println("=== ADDING TASKS (via addTask — with validation) ===");
        try {
            Task validTask = new Task("T-100", "Set up CI pipeline",
                    "Configure GitHub Actions for automated builds",
                    TaskPriority.HIGH, TaskStatus.TODO,
                    LocalDate.now(), LocalDate.now().plusDays(5));

            boolean added = service.addTask(validTask);
            System.out.printf("  T-100 %-30s : %s%n", validTask.getTitle(), added ? "ADDED" : "REJECTED");
            System.out.println("  (Hint: this task has a perfectly valid title. Why was it rejected?)\n");

        } catch (Exception e) {
            System.out.println("  ERROR in ADDING TASKS: " + e.getClass().getSimpleName()
                    + " — " + e.getMessage());
        }

        // ======================================================================
        // SECTION 2: Loading sample data directly (bypasses validation)
        // These tasks populate the board so we can exercise the remaining bugs.
        // In a real system this simulates a database load or data migration.
        // ======================================================================
        System.out.println("=== LOADING SAMPLE DATA (direct import) ===");
        try {
            Task t1 = new Task("T-001", "Set up CI pipeline",
                    "Configure GitHub Actions for automated builds",
                    TaskPriority.HIGH, TaskStatus.TODO,
                    LocalDate.now(), LocalDate.now().plusDays(5));

            Task t2 = new Task("T-002", "Write unit tests",
                    "Cover TaskService and TaskFilter with JUnit tests",
                    TaskPriority.MEDIUM, TaskStatus.TODO,
                    LocalDate.now(), LocalDate.now().plusDays(10));

            Task t3 = new Task("T-003", "Fix login bug",
                    "Users cannot log in when password contains special chars",
                    TaskPriority.CRITICAL, TaskStatus.IN_PROGRESS,
                    LocalDate.now().minusDays(3), LocalDate.now().plusDays(1));

            Task t4 = new Task("T-004", "Update README",
                    "Add setup instructions and architecture diagram",
                    TaskPriority.LOW, TaskStatus.TODO,
                    LocalDate.now(), LocalDate.now().plusDays(14));

            Task t5 = new Task("T-005", "Refactor database layer",
                    "Migrate from raw JDBC to JPA for cleaner data access",
                    TaskPriority.HIGH, TaskStatus.TODO,
                    LocalDate.now().minusDays(5), LocalDate.now().minusDays(2));

            // This task has a null status — will trigger Bug 2 when filtering
            Task t6 = new Task("T-006", "Research caching strategies",
                    "Evaluate Redis vs Memcached for session caching",
                    TaskPriority.MEDIUM, null,
                    LocalDate.now(), LocalDate.now().plusDays(7));

            // This task is already DONE — used to test Bug 5 (state transition)
            Task t7 = new Task("T-007", "Deploy v1.0",
                    "Deploy the initial release to production",
                    TaskPriority.CRITICAL, TaskStatus.DONE,
                    LocalDate.now().minusDays(10), LocalDate.now().minusDays(5));

            service.addTaskDirectly(t1);
            service.addTaskDirectly(t2);
            service.addTaskDirectly(t3);
            service.addTaskDirectly(t4);
            service.addTaskDirectly(t5);
            service.addTaskDirectly(t6);
            service.addTaskDirectly(t7);

            System.out.printf("  Loaded %d tasks into the system.%n%n", service.getAllTasks().size());

        } catch (Exception e) {
            System.out.println("  ERROR in LOADING DATA: " + e.getClass().getSimpleName()
                    + " — " + e.getMessage());
        }

        // ======================================================================
        // SECTION 3: Print All Tasks
        // ======================================================================
        System.out.println("=== ALL TASKS ===");
        try {
            for (Task task : service.getAllTasks()) {
                System.out.println("  " + task);
            }
            System.out.println();
        } catch (Exception e) {
            System.out.println("  ERROR in ALL TASKS: " + e.getClass().getSimpleName()
                    + " — " + e.getMessage());
        }

        // ======================================================================
        // SECTION 4: Filter by Status
        // Exercises Bug 2 — NPE when a task has null status
        // ======================================================================
        System.out.println("=== FILTER BY STATUS (TODO) ===");
        try {
            var todoTasks = service.getTasksByStatus(TaskStatus.TODO);
            System.out.printf("  TODO tasks found: %d%n", todoTasks.size());
            todoTasks.forEach(t -> System.out.println("    " + t));
            System.out.println();
        } catch (Exception e) {
            System.out.println("  ERROR: " + e.getClass().getSimpleName() + " — " + e.getMessage());
            System.out.println("  (Hint: a task with null status may cause NPE in getTasksByStatus)\n");
        }

        // ======================================================================
        // SECTION 5: Generate Summary Report
        // Exercises Bug 3 — wrong high-priority count due to inverted filter
        // ======================================================================
        System.out.println("=== SUMMARY REPORT ===");
        try {
            String summary = TaskReporter.generateSummary(service.getAllTasks());
            System.out.println(summary);
            System.out.println("  (Hint: count the HIGH and CRITICAL tasks manually.");
            System.out.println("   Does the 'High Priority' number in the report match?)\n");
        } catch (Exception e) {
            System.out.println("  ERROR in SUMMARY REPORT: " + e.getClass().getSimpleName()
                    + " — " + e.getMessage());
        }

        // ======================================================================
        // SECTION 6: Sort by Due Date
        // Exercises Bug 4 (partial) — reversed date comparison in DateUtils
        // ======================================================================
        System.out.println("=== SORT BY DUE DATE (should be earliest first) ===");
        try {
            service.sortByDueDate();
            var allTasks = service.getAllTasks();
            for (int i = 0; i < allTasks.size(); i++) {
                Task t = allTasks.get(i);
                System.out.printf("  %d. %-30s due: %s%n",
                        i + 1, t.getTitle(), DateUtils.formatDate(t.getDueDate()));
            }
            System.out.println("  (Hint: are the dates in ascending order? Check DateUtils.compare)\n");
        } catch (Exception e) {
            System.out.println("  ERROR in SORT BY DUE DATE: " + e.getClass().getSimpleName()
                    + " — " + e.getMessage());
        }

        // ======================================================================
        // SECTION 7: Escalate Priority
        // Exercises Bug 4 (partial) — HIGH never reaches CRITICAL
        // ======================================================================
        System.out.println("=== ESCALATE PRIORITY ===");
        try {
            System.out.println("  Before escalation:");
            service.getAllTasks().stream()
                    .filter(t -> t.getPriority() == TaskPriority.HIGH)
                    .forEach(t -> System.out.printf("    %s: %s (priority=%s)%n",
                            t.getId(), t.getTitle(), t.getPriority()));

            // Attempt to escalate T-001 (HIGH -> should become CRITICAL)
            boolean escalated = service.escalatePriority("T-001");
            System.out.printf("%n  Escalation of T-001 result: %s%n", escalated ? "SUCCESS" : "FAILED");

            System.out.println("  After escalation:");
            service.getTaskById("T-001").ifPresent(t ->
                    System.out.printf("    %s: %s (priority=%s)%n",
                            t.getId(), t.getTitle(), t.getPriority()));

            System.out.println("  (Hint: did T-001 reach CRITICAL? Check the escalatePriority logic)\n");
        } catch (Exception e) {
            System.out.println("  ERROR in ESCALATE PRIORITY: " + e.getClass().getSimpleName()
                    + " — " + e.getMessage());
        }

        // ======================================================================
        // SECTION 8: State Transition — DONE back to IN_PROGRESS
        // Exercises Bug 5 — no guard on invalid state transitions
        // ======================================================================
        System.out.println("=== STATE TRANSITION (DONE -> IN_PROGRESS) ===");
        try {
            System.out.println("  T-007 status before: "
                    + service.getTaskById("T-007")
                            .map(t -> t.getStatus().toString())
                            .orElse("NOT FOUND"));

            boolean transitioned = service.updateTaskStatus("T-007", TaskStatus.IN_PROGRESS);
            System.out.printf("  Transition DONE -> IN_PROGRESS allowed: %s%n", transitioned);

            System.out.println("  T-007 status after:  "
                    + service.getTaskById("T-007")
                            .map(t -> t.getStatus().toString())
                            .orElse("NOT FOUND"));

            System.out.println("  (Hint: should a DONE task be allowed to go back to IN_PROGRESS?)\n");
        } catch (Exception e) {
            System.out.println("  ERROR in STATE TRANSITION: " + e.getClass().getSimpleName()
                    + " — " + e.getMessage());
        }

        // ======================================================================
        // SECTION 9: Priority Breakdown
        // ======================================================================
        System.out.println("=== PRIORITY BREAKDOWN ===");
        try {
            String breakdown = TaskReporter.generatePriorityBreakdown(service.getAllTasks());
            System.out.println(breakdown);
        } catch (Exception e) {
            System.out.println("  ERROR in PRIORITY BREAKDOWN: " + e.getClass().getSimpleName()
                    + " — " + e.getMessage());
        }

        System.out.println("================================================");
        System.out.println("  Session complete. Find and fix the 5 bugs!");
        System.out.println("================================================");
    }
}
