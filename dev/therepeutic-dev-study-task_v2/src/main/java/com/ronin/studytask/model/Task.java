package com.ronin.studytask.model;
import java.time.LocalDate;


public class Task {

    //ffirst gonna create a constructor that takes all six fields and then
    //proceed to add 6 getters for all of them.

    private final String title;
    private final String description;
    private TaskPriority priority;
    private TaskStatus status;
    private final LocalDate dueDate;
    private final LocalDate createdDate;

    public Task(String title, String description, TaskPriority priority,
                TaskStatus status, LocalDate dueDate, LocalDate createdDate) {
        this.title = title;
        this.description = description;
        this.priority = priority;
        this.status = status;
        this.dueDate = dueDate;
        this.createdDate = createdDate;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public TaskPriority getPriority() {
        return priority;
    }

    public void setPriority(TaskPriority priority) {
        this.priority = priority;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public LocalDate getCreatedDate() {
        return createdDate;
    }

    @Override
    public String toString()
    {
        return title + "[" + status + "," + priority + ", due: " + dueDate + "]";
    }
}
