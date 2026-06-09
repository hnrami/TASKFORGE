# TaskForge Architecture

## Overview

TaskForge is a workflow orchestration backend.

It allows users to define workflows as Directed Acyclic Graphs (DAG) of tasks.

The engine is responsible for:

- Validating workflow definitions
- Executing tasks based on dependency order
- Running independent tasks concurrently
- Managing execution state
- Handling retries and failures
- Supporting cancellation
- Supporting manual approvals
- Passing task outputs to downstream tasks

The design goal is that new task types can be added without changing the execution engine.


---

# High Level Architecture


```
                 Client

                   |

             REST API Layer

                   |

           Workflow Service


                   |

            DAG Validator


                   |

             Persistence


================================================


            Execution Request


                   |

            Execution Engine


                   |

             DAG Scheduler


                   |

        Task Handler Registry


        /          |          \


 HTTP Handler  Script Handler  Custom Handler

```


---

# Components


## API Layer


Responsible for exposing HTTP contracts.


Controllers:


- WorkflowController
- ExecutionController


Supported operations:


- Register workflow
- Retrieve workflow
- Start execution
- Get execution status
- Cancel execution
- Resolve approval


---

# Workflow Service


Responsibilities:


- Receive workflow definition
- Validate workflow
- Store workflow metadata


Before storing:

- Validate DAG
- Validate task references
- Validate task handlers exist


---

# DAG Validator


A workflow is internally represented as:


```
Task = Node

Dependency = Edge
```


Example:


```
        build

          |

         test

       /      \

 deploy    notify

```


Validation:


## Cycle Detection


Invalid:


```
A -> B -> C -> A
```


Algorithm:


Depth First Search (DFS)


Node states:


```
NOT_VISITED

VISITING

VISITED
```


Finding VISITING again means cycle exists.


---


## Dependency Validation


Reject:


```
Task A depends on Task B

but B does not exist
```


---


# Execution Engine


Execution engine manages lifecycle of a workflow run.


Algorithm:


```
Create execution


while tasks remain:


    find executable tasks


    execute tasks


    collect results


    update state


finish execution

```


The engine does not know about HTTP, Script, or any specific task.


It only knows:


```
TaskHandler interface
```


---

# DAG Scheduler


Responsible for deciding which tasks are ready.


A task is ready when:


```
All dependencies are SUCCESS

and

current state is PENDING
```


Independent tasks are executed concurrently.


Example:


```
        A


     B      C


        D

```


Execution:


1. A

2. B and C parallel

3. D


---

# Task Handler Design


The system follows Open/Closed Principle.


Existing engine should be closed for modification but open for extension.


Contract:


```kotlin

interface TaskHandler {


    fun type():String


    fun execute(

       context:TaskContext

    ):TaskResult


    fun cancel()

}

```


Adding new task:


Example Kafka task:


```kotlin

@Component

class KafkaTaskHandler:TaskHandler {


 override fun type()="kafka"


 override fun execute(

 context:TaskContext

 ):TaskResult {


 }


}

```


No engine code changes required.


---

# Handler Registry


During application startup:


```
HTTP Handler

Script Handler

Notification Handler

Database Handler

```


registered as:


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


---

# Execution State Model


## Workflow Execution State


```
CREATED

RUNNING

SUCCESS

FAILED

CANCELLED

```


---


## Task Execution State


```
PENDING

RUNNING

SUCCESS

FAILED

SKIPPED

WAITING_APPROVAL

CANCELLED

```


---

# Shared State Design


Every task can produce output.


Example:


Task:


```
build
```


Output:


```json
{
"image":"payment:v1"
}
```


Stored:


```
outputs["build.image"]

```


Downstream task:


```
deploy {{build.image}}

```


Resolution failure:


If a variable reference cannot be resolved:

- task fails
- clear validation error is returned


Reason:

Avoid silent deployments with wrong values.


---

# Retry Handling


Each task supports retry configuration.


Example:


```json

{
"attempts":3,

"delaySeconds":5

}

```


Retryable:


- network failures
- HTTP 5xx
- temporary execution errors


Non retryable:


- invalid configuration
- HTTP 4xx


After retries exhausted:


Task marked FAILED.


Downstream tasks become SKIPPED.


---

# Timeout Handling


Each task supports timeout.


Execution runs with timeout control.


If timeout expires:


- running operation is cancelled
- task marked FAILED_TIMEOUT


Timeout is enforced by execution framework instead of only checking elapsed time.


---

# Cancellation


When execution cancellation requested:


Pending tasks:


```
PENDING -> CANCELLED

```


Running tasks:


1. request graceful cancellation

2. wait for shutdown window

3. force terminate


---

# Approval Tasks


Approval task pauses workflow.


State:


```
WAITING_APPROVAL
```


Approval API provides:


- execution id
- task id
- approver identity
- approve/reject decision


Timeout without approval:


Task fails and workflow stops.


---

# Task Types


## HTTP Task


Executes HTTP request.


Success:


HTTP 2xx


Output:


- status code
- response body


---


## Script Task


Runs shell command.


Success:


exitCode = 0


Output:


- stdout
- stderr
- exit code


---

## Notification Task


Sends deployment or incident notification.


Output:


delivery status


---

## Database Task


Executes database operation or migration.


Output:


execution result


---

# Storage Model


Main entities:


```
WorkflowDefinition


WorkflowExecution


TaskExecution

```


WorkflowDefinition describes what should happen.


WorkflowExecution represents one run.


TaskExecution stores individual task state.


---

# Design Priority


The priority of TaskForge is:

1. Correct execution ordering

2. Extensibility

3. Failure handling

4. Observability

5. Maintainability

