# TaskFlow Bug Log — Researcher Copy

> **DO NOT distribute to participants.**
> This document details all planted bugs, their locations, fixes, and
> expected output changes.

---

## Bug 1 — Inverted validation condition (Easy, ~5–10 min)

**File:** `src/main/java/com/ronin/studytask/util/TaskValidator.java`
**Method:** `validate(Task task)` (line 39)

**Incorrect code:**
```java
if (titleLength >= 3 && titleLength <= 100) {
    return new ValidationResult(false,
            "Title must be between 3 and 100 characters, got " + titleLength);
}
```

**Correct fix:**
```java
if (titleLength < 3 || titleLength > 100) {
    return new ValidationResult(false,
            "Title must be between 3 and 100 characters, got " + titleLength);
}
```

**Symptom:** Valid tasks (titles 3–100 chars) are rejected by `addTask()`.
The condition is inverted — it rejects titles that ARE within range instead
of titles that are outside the range.

**Expected output change:** After fix, `T-100` should print `ADDED`
instead of `REJECTED`.

**Findable via:** Running `Main`, seeing `REJECTED` for a clearly valid task
in Section 1, reading the `if` condition on line 39.

---

## Bug 2 — Missing null guard causing NPE (Moderate, ~10–15 min)

**File:** `src/main/java/com/ronin/studytask/service/TaskService.java`
**Method:** `getTasksByStatus(TaskStatus status)` (line 88)

**Incorrect code:**
```java
if (task.getStatus().equals(status)) {
```

**Correct fix — use null-safe comparison:**
```java
if (status.equals(task.getStatus())) {
```
Or add a null guard:
```java
if (task.getStatus() != null && task.getStatus().equals(status)) {
```

**Symptom:** `NullPointerException` when iterating tasks because `T-006`
was added with `null` status via `addTaskDirectly()`. Calling
`task.getStatus().equals(status)` on T-006 triggers NPE.

**Expected output change:** After fix, Section 4 should print
`TODO tasks found: 3` (T-001, T-002, T-004) instead of throwing NPE.

**Findable via:** Stack trace points directly to line 88 in
`getTasksByStatus()`.

---

## Bug 3 — Incorrect filter producing wrong report totals (Harder, ~15–20 min)

**File:** `src/main/java/com/ronin/studytask/service/TaskFilter.java`
**Method:** `filterByPriorityRange(List<Task>, TaskPriority)` (line 27)

**Also involves:** `TaskReporter.generateSummary()` (line 44) which calls
`filterByPriorityRange(tasks, TaskPriority.HIGH)`.

**Incorrect code:**
```java
&& task.getPriority().ordinal() < minPriority.ordinal())
```

**Correct fix:**
```java
&& task.getPriority().ordinal() >= minPriority.ordinal())
```

**Symptom:** The summary report shows wrong "High Priority" count. The
filter returns tasks BELOW the threshold (LOW, MEDIUM) instead of tasks
AT OR ABOVE (HIGH, CRITICAL). No exception is thrown — just wrong numbers.

**Expected output change:** After fix, `High Priority` in the summary should
show the count of HIGH + CRITICAL tasks (3: T-001, T-003, T-005) instead
of LOW + MEDIUM tasks.

**Findable via:** Manually counting HIGH/CRITICAL tasks and comparing to
the report output. Tracing `TaskReporter.generateSummary()` →
`TaskFilter.filterByPriorityRange()` → seeing the `<` instead of `>=`.

---

## Bug 4 — Cascading: reverse sort + broken escalation (Complex, ~20–25 min)

### Bug 4a — Inverted date comparison

**File:** `src/main/java/com/ronin/studytask/util/DateUtils.java`
**Method:** `compare(LocalDate a, LocalDate b)` (line 32)

**Incorrect code:**
```java
return b.compareTo(a);
```

**Correct fix:**
```java
return a.compareTo(b);
```

**Symptom:** Tasks sorted by due date appear in reverse chronological order
(latest first instead of earliest first). The `sortByDueDate()` method in
TaskService delegates to `DateUtils.compare()`.

### Bug 4b — Priority escalation never reaches CRITICAL

**File:** `src/main/java/com/ronin/studytask/service/TaskService.java`
**Method:** `escalatePriority(String id)` (line 149)

**Incorrect code:**
```java
} else if (current.ordinal() > TaskPriority.HIGH.ordinal()) {
    task.setPriority(TaskPriority.CRITICAL);
```

**Correct fix:**
```java
} else if (current == TaskPriority.HIGH) {
    task.setPriority(TaskPriority.CRITICAL);
```

**Symptom:** Calling `escalatePriority("T-001")` on a HIGH-priority task
returns `false`. The condition `current.ordinal() > HIGH.ordinal()` is
never true when current IS HIGH (2 > 2 = false). Only CRITICAL (ordinal 3)
would pass, but CRITICAL has nowhere to escalate to.

**Expected output change:** After fixing both 4a and 4b:
- Sort order shows earliest due dates first.
- T-001 successfully escalates from HIGH to CRITICAL.

**Findable via:** Reading `DateUtils.compare()` carefully (the inversion
is subtle). Tracing `escalatePriority()` and noticing the `>`
should be `==` (or `>= HIGH.ordinal()`).

---

## Bug 5 — Illegal state transition (Optional stretch, ~10–15 min)

**File:** `src/main/java/com/ronin/studytask/service/TaskService.java`
**Method:** `updateTaskStatus(String id, TaskStatus newStatus)` (line 114–123)

**Incorrect code:**
```java
public boolean updateTaskStatus(String id, TaskStatus newStatus) {
    Optional<Task> taskOpt = getTaskById(id);
    if (taskOpt.isEmpty()) { ... return false; }
    Task task = taskOpt.get();
    // No transition guard — any status change is allowed
    task.setStatus(newStatus);
    return true;
}
```

**Correct fix — add guard before `setStatus`:**
```java
Task task = taskOpt.get();
TaskStatus current = task.getStatus();
if (current == TaskStatus.DONE &&
        (newStatus == TaskStatus.IN_PROGRESS || newStatus == TaskStatus.TODO)) {
    System.out.println("  [WARN] Cannot transition from DONE to " + newStatus);
    return false;
}
task.setStatus(newStatus);
return true;
```

**Symptom:** Section 8 shows `Transition DONE -> IN_PROGRESS allowed: true`
and T-007's status changes from DONE to IN_PROGRESS. A completed task
should not regress to an earlier lifecycle state.

**Expected output change:** After fix:
- `Transition DONE -> IN_PROGRESS allowed: false`
- `T-007 status after: DONE`

**Findable via:** Reading the output carefully — the DONE → IN_PROGRESS
transition should be rejected but succeeds silently.

---

## Estimated Time Per Bug Phase

| Bug | Difficulty | Est. Time  | Cumulative |
|-----|-----------|------------|------------|
| 1   | Easy      | 5–10 min   | 5–10 min   |
| 2   | Moderate  | 10–15 min  | 15–25 min  |
| 3   | Harder    | 15–20 min  | 30–45 min  |
| 4   | Complex   | 20–25 min  | 50–70 min  |
| 5   | Optional  | 10–15 min  | 60–85 min  |
