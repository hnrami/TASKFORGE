# TaskForge Engineering Assignment - Implementation Plan

## Goal

Build a backend workflow orchestration engine.

Users define workflows consisting of multiple tasks. Each workflow is represented as a Directed Acyclic Graph (DAG).

The engine is responsible for:

- Workflow validation
- Dependency based execution
- Parallel task execution
- State tracking
- Retry handling
- Timeout handling
- Cancellation
- Human approvals
- Passing data between tasks
- Extensible task handlers


---

# Technology Stack

Use:

- Kotlin
- Spring Boot
- Spring Web
- Spring Data JPA
- H2 Database
- Gradle
- JUnit 5


---

# Package Structure

Create:

```
com.taskforge


api
 └── WorkflowController.kt
 └── ExecutionController.kt


model

 └── WorkflowDefinition.kt
 └── TaskDefinition.kt
 └── WorkflowExecution.kt
 └── TaskExecution.kt
 └── ExecutionStatus.kt
 └── TaskStatus.kt


service

 └── WorkflowService.kt
 └── ExecutionService.kt


engine

 └── ExecutionEngine.kt
 └── DagValidator.kt
 └── DagScheduler.kt
 └── StateManager.kt


handler

 └── TaskHandler.kt
 └── TaskHandlerRegistry.kt
 └── HttpTaskHandler.kt
 └── ScriptTaskHandler.kt
 └── NotificationTaskHandler.kt
 └── DatabaseTaskHandler.kt


repository

 └── WorkflowRepository.kt
 └── ExecutionRepository.kt


exception

 └── GlobalExceptionHandler.kt

```

---

# Core Design Rule

The engine must not know individual task types.

Never write:

```kotlin
if(task.type=="http") {

}

if(task.type=="script") {

}
```

Use plugin architecture.


---

# Task Handler Contract

Create:

```kotlin
interface TaskHandler {


    fun type(): String


    fun execute(
        context: TaskContext
    ): TaskResult


    fun cancel()

}
```

Every new task implements this interface.


Example:

```kotlin
@Component
class HttpTaskHandler : TaskHandler {


 override fun type()="http"


 override fun execute(
   context:TaskContext
 ):TaskResult {


   // execute HTTP request

 }

}
```

---

# Task Handler Registry


Maintain:

```
Map<String, TaskHandler>
```


Example:


```
http -> HttpTaskHandler

script -> ScriptTaskHandler

notification -> NotificationTaskHandler

database -> DatabaseTaskHandler
```


Engine should call:

```
handlerRegistry
    .get(task.type)
    .execute(context)
```


---

# Workflow API


## Create Workflow


POST /api/workflows


Request:


```json
{
"name":"deployment",

"tasks":[

{
"id":"build",
"type":"script",
"config":{
"command":"mvn clean package"
}
},


{
"id":"deploy",
"type":"http",
"dependsOn":["build"]
}

]
}
```


Responsibilities:

- Validate DAG
- Validate task types
- Store workflow


---

## Get Workflow


GET /api/workflows/{id}


---

# Execution API


## Start


POST /api/workflows/{id}/executions


Creates new execution.


---

## Check Execution


GET /api/executions/{id}


Return:


```json
{
"status":"RUNNING",

"tasks":{

"build":"SUCCESS",

"deploy":"RUNNING"

}

}
```


---

## Cancel


POST /api/executions/{id}/cancel


---

## Approval


POST /api/executions/{id}/approval


---

# DAG Validation


Implement:


DagValidator.kt


Check:


## Missing dependency


Invalid:


```
A depends on B

but B does not exist
```


Reject.


---


## Cycle detection


Invalid:


```
A -> B -> C -> A
```


Use DFS.


States:


```
NOT_VISITED

VISITING

VISITED
```


If DFS reaches VISITING node:

Cycle exists.


---

# Execution Engine Logic


Algorithm:


```
start execution


while workflow not completed:


    find tasks where dependencies completed


    run ready tasks concurrently


    collect output


    update shared state


finish execution

```


---

# Parallel Execution


Use:


Kotlin Coroutine

or

ExecutorService


Independent tasks should run together.


Example:


```
        A


     B      C


        D

```


Execution:


```
Run A


Run B and C parallel


Run D

```


---

# State Management


Execution states:


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

```


---

# Shared State


Each task returns output.


Example:


Task A:


```json
{
"version":"1.0"
}
```


Stored:


```
context.outputs["taskA.version"]
```


Task B can reference:


```
{{taskA.version}}
```


---

# Retry Design


Support:


```
attempts

delay

```


Example:


attempt=3


Flow:


```
try 1 fail

try 2 fail

try 3 success

```


---

# Timeout


Every task can define timeout.


If exceeded:


- stop execution
- mark task TIMEOUT


---

# Cancellation


When execution cancelled:


Pending tasks:

```
PENDING -> CANCELLED
```


Running tasks:


call:


```
handler.cancel()
```


Give graceful shutdown time.

Then force stop.


---

# Approval Task


Support manual gate.


State:


```
WAITING_APPROVAL
```


API continues execution.


If timeout expires:

Fail workflow.


---

# Required Task Types


Implement:


## 1. HTTP Task


Input:

- url
- method
- headers
- body


Success:

HTTP status 200-299


Output:

- statusCode
- responseBody


---


## 2. Script Task


Input:


command


Output:


stdout

stderr

exitCode


exitCode 0 = success


---

## 3. Notification Task


Simulate:

Email/Slack message


---

## 4. Database Task


Simulate:

migration/query execution


---

# Tests Required


Implement tests for:


- Workflow creation
- Cycle detection
- Missing dependency
- Unknown task type
- Parallel execution
- Retry success
- Retry exhausted
- Timeout
- Cancellation
- Approval flow
- Adding new custom handler without modifying engine


---

# Documentation


Create:


ARCHITECTURE.md

Explain:

- components
- APIs
- extensibility


DECISIONS.md

Explain:

- why DAG
- why handler registry
- retry decisions
- timeout decisions


TESTDESIGN.md

Explain testing approach

