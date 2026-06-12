# TaskForge Decision Log

This document captures major design decisions, assumptions, and trade-offs made while implementing TaskForge.

The assignment intentionally leaves some areas open-ended. This document explains how those decisions were handled.


---

# 1. Technology Choice

## Decision

Use:

- Kotlin
- Spring Boot
- Spring Data JPA
- H2 Database
- Gradle

## Reason

Kotlin provides strong JVM ecosystem support while reducing boilerplate.

Spring Boot provides:

- REST API support
- Dependency Injection
- Easy component registration
- Testing support

The assignment focus is workflow engine design, not infrastructure complexity.

H2 is used to keep setup simple while keeping persistence behavior close to a relational database.


## Trade-off

A production system would use PostgreSQL/MySQL with migration management.


---

# 2. Workflow Representation

## Decision

Represent workflow as Directed Acyclic Graph (DAG).

Mapping:

```
Task = Node

Dependency = Directed Edge
```

Example:

```
Build
  |
Test
 / \
Deploy Notify
```

## Reason

DAG naturally represents dependency-based execution.

It allows:

- ordering
- parallel execution
- dependency validation


---

# 3. Workflow Definition Format

## Decision

Use JSON workflow definitions.

Example:


```json
{
"name":"deployment",

"tasks":[
 {
  "id":"build",
  "type":"script"
 }
]
}
```

## Reason

JSON is:

- easy for REST clients
- human readable
- widely supported


## Trade-off

YAML may be more friendly for DevOps users, but JSON keeps API contracts simpler.


---

# 4. DAG Validation Strategy


## Decision

Validate workflow before storing.


Validation includes:


- cycle detection
- missing dependencies
- unsupported task types


Cycle detection uses DFS.


States:


```
NOT_VISITED

VISITING

VISITED
```


If a VISITING node is reached again, a cycle exists.


## Reason


Invalid workflows should fail early instead of during execution.


---

# 5. Task Extensibility Design


## Decision


Use TaskHandler abstraction.


```kotlin
interface TaskHandler {

 fun type():String

 fun execute(context:TaskContext):TaskResult

}
```


Execution engine depends only on this interface.


## Reason


Supports Open/Closed Principle.


Adding a new task should require:


- Create new TaskHandler implementation
- Register handler


No ExecutionEngine modification.


Example:


Adding KafkaTaskHandler should not require engine changes.


---

# 6. Handler Registry


## Decision


Maintain:


```
Map<TaskType, TaskHandler>
```


Example:


```
http -> HttpTaskHandler

script -> ScriptTaskHandler
```


## Reason


Avoids:


```
if task == http

if task == script

```


which makes engine tightly coupled.


---

# 7. Execution Scheduling


## Decision


Use DAG based scheduling.


A task can execute when:


```
all dependencies == SUCCESS
```


Independent tasks execute concurrently.


Example:


```
       A


    B     C


       D

```


Execution:


1. A

2. B and C parallel

3. D


## Reason


Improves execution time and matches real workflow systems.


---

# 8. Concurrency Model


## Decision


Use controlled thread execution.

Independent tasks run in parallel using execution framework.


## Reason


Avoid creating unlimited threads.

Allows:

- timeout handling
- cancellation
- resource control


---

# 9. State Model


## Decision


Workflow states:


```
CREATED

RUNNING

SUCCESS

FAILED

CANCELLED

```


Task states:


```
PENDING

RUNNING

SUCCESS

FAILED

SKIPPED

WAITING_APPROVAL

CANCELLED

```


## Reason


Each state represents a clear lifecycle phase.


Important distinction:


FAILED:

Task executed and failed.


SKIPPED:

Task never executed because condition/dependency prevented execution.


---

# 10. Shared State Between Tasks


## Decision


Each task contributes output into execution context.


Example:


Task output:


```json
{
"version":"1.2.0"
}
```


Stored as:


```
taskId.version
```


Downstream reference:


```
{{build.version}}
```


## Missing Reference Decision


If reference cannot resolve:


Fail the task.


## Reason


Silent empty replacement can create dangerous deployments.


---

# 11. Retry Strategy


## Decision


Retry policy configured per task.


Example:


```json
{
"attempts":3,
"delaySeconds":5
}
```


Retryable:


- temporary network issue
- HTTP 5xx
- transient failures


Non retryable:


- invalid input
- invalid configuration
- HTTP 4xx


## Reason


Not every failure should retry.


---

# 12. Failure Handling


## Decision


If task fails after retries:


- task marked FAILED
- dependent tasks marked SKIPPED
- workflow marked FAILED


## Reason


Avoid executing downstream tasks with incomplete data.


---

# 13. Timeout Handling


## Decision


Timeout is enforced by execution framework.


On timeout:


- cancel running operation
- update task state


## Reason


Checking elapsed time after completion is not reliable.


---

# 14. Cancellation


## Decision


Cancellation behavior:


Pending tasks:


```
PENDING -> CANCELLED
```


Running tasks:


1. Request graceful stop

2. Wait configured time

3. Force termination


## Reason


Prevents resource leaks while allowing cleanup.


---

# 15. Approval Tasks


## Decision


Approval is treated as a special task state.


State:


```
WAITING_APPROVAL
```


Approval request contains:


- execution id
- task id
- approver identity
- decision


## Timeout Decision


If approval timeout expires:


Workflow fails.


## Reason


A workflow should not remain waiting forever.


---

# 16. Task Types Implemented


## Required


### HTTP Task


Success:


HTTP status 200-299


Failure:


HTTP 4xx/5xx based on retry rules


Output:


- status code
- response


---


### Script Task


Success:


exitCode = 0


Failure:


non-zero exit


Output:


- stdout
- stderr
- exitCode


---


## Additional


### Notification Task


Reason:


Real DevOps workflows commonly notify users/systems.


---


### Database Task


Reason:


Deployment workflows frequently require database migrations.


---

# 17. Scope Decisions


## Not Implemented Initially


- distributed execution workers
- message queues
- multi-node scheduling
- authentication system


## Reason


Focus is the workflow engine design.


The architecture allows adding these later.
