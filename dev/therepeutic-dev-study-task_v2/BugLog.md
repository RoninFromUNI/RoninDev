# BugLog — researcher's eyes only

> this is the answer key. do not distribute to participants.
> if you're a participant reading this... well, that's a bug in the study design.

---

## bug 1 — the validator that validates nothing

| | |
|---|---|
| **file** | `TaskValidator.java` |
| **method** | `isValid(Task task)` |
| **what's wrong** | the entire boolean expression is wrapped in `!()`, so valid tasks get rejected and invalid ones pass |
| **the fix** | remove the `!` negation from the return statement |
| **how they'll find it** | run Main, see every task get DENIED, read the "Added" vs "DENIED" logic in addTask, trace back to isValid |
| **expected time** | ~5-10 min |

---

## bug 2 — null goes brrr

| | |
|---|---|
| **file** | `TaskService.java` |
| **method** | `getTasksByStatus(TaskStatus status)` |
| **what's wrong** | no null guard on the status parameter. passing null doesn't crash here but produces unexpected results because the filter comparison behaves weirdly with null |
| **the fix** | add `if (status == null) return new ArrayList<>()` at the top of the method |
| **how they'll find it** | the Main output for `getTasksByStatus(null)` returns a weird result set |
| **expected time** | ~10-15 min |

---

## bug 3 — the filter that filters backwards

| | |
|---|---|
| **file** | `TaskFilter.java` |
| **method** | `filterByStatus(List<Task> tasks, TaskStatus status)` |
| **what's wrong** | uses `!=` instead of `==` in the comparison, so it returns every task that DOESN'T match the status |
| **the fix** | change `task.getStatus() != status` to `task.getStatus() == status` |
| **how they'll find it** | the report numbers don't add up. TODO shows 3 but there are only 2 TODO tasks. tracing through TaskReporter → TaskFilter reveals the inverted comparison |
| **expected time** | ~15-20 min |

---

## bug 4 — time goes backwards + priority ceiling

this one is a two-parter that only surfaces after bug 3 is fixed.

### part 1: reversed sort

| | |
|---|---|
| **file** | `DateUtils.java` |
| **method** | `compareDates(LocalDate a, LocalDate b)` |
| **what's wrong** | returns `b.compareTo(a)` instead of `a.compareTo(b)`, which flips sort order to reverse chronological |
| **the fix** | swap to `a.compareTo(b)` |
| **how they'll find it** | after fixing bug 3, the sorted output shows latest dates first instead of earliest |

### part 2: missing escalation

| | |
|---|---|
| **file** | `TaskService.java` |
| **method** | `bumpPriority(LocalDate today)` |
| **what's wrong** | the priority chain goes LOW → MEDIUM and MEDIUM → HIGH but never HIGH → CRITICAL |
| **the fix** | add `else if (task.getPriority() == TaskPriority.HIGH) { task.setPriority(TaskPriority.CRITICAL); }` |
| **how they'll find it** | overdue HIGH priority tasks stay HIGH after the bump. no CRITICAL tasks ever appear |

| **expected time (both parts)** | ~20-25 min |

---

## bug 5 — transitions without boundaries

| | |
|---|---|
| **file** | `TaskService.java` |
| **method** | `transitionStatus(Task task, TaskStatus newStatus)` |
| **what's wrong** | no validation at all. any status can transition to any other status, including illegal moves like DONE → IN_PROGRESS |
| **the fix** | add a guard that checks valid transitions before calling setStatus. e.g. DONE can only go to TODO (reopened), not directly to IN_PROGRESS |
| **how they'll find it** | the output shows t4 going from DONE → IN_PROGRESS with no complaint. reading the method reveals zero guard logic |
| **expected time** | ~10-15 min |

---

## the cascade explained

the bugs are ordered so each fix changes runtime behaviour enough to reveal the next one:

1. fix the validator → tasks actually get added → list is no longer empty
2. fix the null check → status filtering works without weird null behaviour
3. fix the filter → report numbers are correct, but sort order and priority bump are still wrong
4. fix date comparison + add missing escalation → output is correct
5. fix transitions → state management is valid

total estimated session time: **60-90 minutes**

this escalation arc is designed to produce the exact behavioural signatures the flow detection algorithm is most sensitive to. bugs 1-2 are warmup (low cognitive load). bug 3 introduces cross-file tracing (rising load). bug 4 is peak challenge (multi-method, multi-file). bug 5 is a cooldown requiring careful logic reading.