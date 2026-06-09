# TaskForge Test Design

## Overview

This document describes the testing strategy for TaskForge workflow orchestration engine.

The goal is to validate:

- Workflow correctness
- DAG validation
- Execution ordering
- Parallel execution
- Failure handling
- Retry behavior
- Timeout handling
- Cancellation
- Approval flow
- Task extensibility


Testing is divided into:

1. Unit Tests
2. Integration Tests
3. Engine Behavior Tests


---

# 1. Workflow Validation Tests


## Test: Valid workflow creation


### Scenario


Workflow:


```
A -> B -> C
```


Expected:


- Workflow accepted
- Stored successfully


---


## Test: Cycle detection


### Scenario


Invalid DAG:


```
A -> B -> C -> A
```


Expected:


Workflow rejected.


Error:


```
Cycle detected:
A -> B -> C -> A
```


Purpose:


Ensure workflow cannot enter deadlock state.


---

## Test: Missing dependency


Scenario:


```
Task B depends on Task A

but Task A does not exist
```


Expected:


Validation error:


```
Unknown dependency A for task B
```


---

## Test: Unknown task type


Scenario:


Task:


```json
{
"type":"unknown"
}
```


Expected:


Reject workflow.


Reason:


No registered TaskHandler exists.


---

# 2. DAG Scheduler Tests


## Test: Sequential execution


Workflow:


```
A

|

B

|

C
```


Expected order:


```
A -> B -> C
```


---

## Test: Parallel execution


Workflow:


```

        A


     B      C


        D

```


Expected:


Execution order:


Step 1:


```
A
```


Step 2:


```
B and C together
```


Step 3:


```
D
```


Validation:


B should not wait for C.

C should not wait for B.


---

# 3. Execution State Tests


## Test: Successful workflow


Scenario:


All tasks succeed.


Expected:


Workflow:


```
SUCCESS
```


Tasks:


```
SUCCESS
SUCCESS
SUCCESS
```


---

## Test: Task failure


Scenario:


Task B fails:


```
A -> B -> C
```


Expected:


```
A = SUCCESS

B = FAILED

C = SKIPPED

Workflow = FAILED
```


---

# 4. Retry Tests


## Test: Retry success


Configuration:


```json
{
"attempts":3
}
```


Scenario:


```
Attempt 1 -> fail

Attempt 2 -> success
```


Expected:


Task status:


```
SUCCESS
```


Attempt count:


```
2
```


---

## Test: Retry exhausted


Scenario:


```
Attempt 1 fail

Attempt 2 fail

Attempt 3 fail
```


Expected:


Task:


```
FAILED
```


Workflow:


```
FAILED
```


---

# 5. Timeout Tests


## Test: Task timeout


Scenario:


Script task:


```
sleep 100 seconds
```


Configured timeout:


```
5 seconds
```


Expected:


- Execution interrupted
- Task marked failed
- Timeout reason stored


---

# 6. Cancellation Tests


## Test: Cancel running workflow


Workflow:


```
A -> B -> C
```


While B is running:

Cancel request received.


Expected:


```
A = SUCCESS

B = CANCELLED

C = CANCELLED

Workflow = CANCELLED
```


---

# 7. Approval Tests


## Test: Approval waiting


Workflow:


```
Build

 |

Approval

 |

Deploy
```


Expected:


After build:


```
Approval = WAITING_APPROVAL
```


Deploy should not start.


---

## Test: Approval accepted


API request:


```json
{
"approved":true
}
```


Expected:


Workflow continues.


---

## Test: Approval timeout


Scenario:


No approval received within configured time.


Expected:


Approval task fails.

Workflow stops.


---

# 8. Shared State Tests


## Test: Task output passing


Task A output:


```json
{
"version":"1.0"
}
```


Task B config:


```
deploy {{A.version}}
```


Expected resolved:


```
deploy 1.0
```


---

## Test: Missing variable


Input:


```
{{unknown.value}}
```


Expected:


Task fails with clear error.


---

# 9. Task Handler Tests


## HTTP Task


Success cases:


- HTTP 200
- HTTP 201


Expected:


SUCCESS


Failures:


- HTTP 500 retry
- HTTP 400 permanent failure


---

# Script Task


Success:


```
exitCode=0
```


Expected:


SUCCESS


Failure:


```
exitCode !=0
```


Expected:


FAILED


---

# 10. Extensibility Test


Most important test.


Create new handler only in test:


Example:


```kotlin
class TestTaskHandler: TaskHandler {


override fun type()="test"


override fun execute(
context:TaskContext
):TaskResult {


return TaskResult.success()


}


}
```


Expected:


- Register handler
- Create workflow using type "test"
- Execute successfully


ExecutionEngine code should not change.


---

# 11. API Integration Tests


## Workflow APIs


Test:


```
POST /workflows
GET /workflows/{id}
```


Validate:


- response status
- stored data


---

# Execution APIs


Test:


```
POST /workflows/{id}/executions

GET /executions/{id}

POST /executions/{id}/cancel

POST /executions/{id}/approval
```


Validate:


Correct state transitions.


---

# Testing Tools


Use:


- JUnit 5
- Spring Boot Test
- MockMvc
- Mockito


---

# Testing Priority


Priority order:


1. DAG correctness

2. Handler extensibility

3. State transitions

4. Failure handling

5. API contracts

