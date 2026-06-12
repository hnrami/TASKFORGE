# TaskForge - Workflow Orchestration Engine

TaskForge is a backend task orchestration engine for DevOps/platform workflows. A workflow is represented as a Directed Acyclic Graph (DAG) where tasks execute based on dependencies.

## Features

- Create workflow definitions
- DAG validation (cycle and dependency validation)
- Dependency based execution
- Parallel execution support
- TaskHandler plugin architecture
- Retry and timeout support
- Approval workflow
- Cancellation
- Output sharing between tasks
- REST APIs

## Tech Stack

- Kotlin
- Spring Boot
- Gradle
- JUnit 5

## Project Structure

```
src/main/kotlin/com/taskforge

api          REST Controllers
engine       DAG validation, scheduler, execution engine
handler      TaskHandler implementations
model        Domain models
repository   Persistence abstraction
service      Business services
```

## Run Application

### Prerequisite

Java 17+

Check:

```bash
java -version
```

### Windows

```bash
gradlew.bat clean bootRun
```

### Linux/Mac

```bash
./gradlew clean bootRun
```

Application starts:

```
http://localhost:8080
```

## Run Test Cases

Windows:

```bash
gradlew.bat test
```

Linux/Mac:

```bash
./gradlew test
```

Run full verification:

```bash
./gradlew clean build
```

## API Testing With Postman

Import files from `postman` folder:

1. `TaskForge.postman_collection.json`
2. `TaskForge.postman_environment.json`

Select TaskForge environment.

## REST APIs

### Create Workflow

POST

```
/api/workflows
```

### Get Workflow

GET

```
/api/workflows/{id}
```

### Start Execution

POST

```
/api/workflows/{workflowId}/executions
```

### Get Execution

GET

```
/api/executions/{id}
```

### Cancel Execution

POST

```
/api/executions/{id}/cancel
```

### Approval

POST

```
/api/executions/{id}/approval
```

Body:

```json
{
  "taskId":"approval",
  "approved":true
}
```

## Design

ExecutionEngine never depends on concrete task implementations.

New task support:

1. Implement TaskHandler
2. Register handler
3. No engine changes required

Example:

```
EmailTaskHandler -> TaskHandler -> Registry -> Engine
```

See:

- ARCHITECTURE.md
- DECISIONS.md
- TESTDESIGN.md
