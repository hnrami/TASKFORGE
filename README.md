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

---

## Troubleshooting

### 1. Gradle wrapper issue

#### Problem

While running:

```bash
gradlew.bat clean bootRun
```

Error:

```text
gradle-wrapper.jar is not recognized as an internal or external command
```

#### Reason

Gradle wrapper files are corrupted or incomplete.

Expected wrapper files:

```text
gradlew
gradlew.bat
gradle/wrapper/
 ├── gradle-wrapper.jar
 └── gradle-wrapper.properties
```

#### Solution

Remove old wrapper:

Windows:

```cmd
del gradlew
del gradlew.bat
rmdir /s /q gradle
```

Generate wrapper again:

```cmd
gradle wrapper --gradle-version 8.7
```

Verify:

```cmd
gradlew.bat --version
```

---

## 2. Application startup issue - Port already in use

### Problem

Application startup fails:

```text
APPLICATION FAILED TO START

Web server failed to start.
Port 8080 was already in use.
```

### Reason

Another application is already using port 8080.

### Solution 1: Stop existing process

Find process:

Windows:

```cmd
netstat -ano | findstr :8080
```

Example:

```text
TCP 0.0.0.0:8080 LISTENING 12345
```

Kill process:

```cmd
taskkill /PID 12345 /F
```

Start again:

```cmd
gradlew.bat bootRun
```

---

### Solution 2: Run on different port

```cmd
gradlew.bat bootRun --args="--server.port=9090"
```

Application URL:

```text
http://localhost:9090
```

---

## 3. Run all test cases

Execute:

```cmd
gradlew.bat clean test
```

Expected:

```text
BUILD SUCCESSFUL
```

Test reports:

```text
build/reports/tests/test/index.html
```

---

## 4. Build application

```cmd
gradlew.bat clean build
```

Expected:

```text
BUILD SUCCESSFUL
```

Generated artifact:

```text
build/libs/
```

---

## 5. Start TaskForge

```cmd
gradlew.bat bootRun
```

Successful startup:

```text
Started TaskForgeApplication

Tomcat started on port 8080
```

---


## 7. Gradle version check

Verify:

```cmd
gradlew.bat --version
```

Recommended:

```text
Gradle 8.x
```

---

## API Testing With Postman

Import files from `postman` folder:

1. `TaskForge.postman_collection.json`
2. `TaskForge.postman_environment.json`

Select TaskForge environment.

## REST APIs

## API Testing Using Postman

A ready-to-use Postman collection is available:

```text
postman/TaskForge.postman_collection.json
```

Import the collection into Postman and execute requests in sequence.

Application default URL:

```text
http://localhost:9090
```

---

## API Validation Flow

### 1. Create Simple Workflow

```http
POST /api/workflows
```

Validates:

- Workflow creation
- Sequential DAG

Example:

```text
build
  |
test
  |
deploy
```

Expected response:

```json
{
  "id": "workflow-id",
  "message": "Workflow created successfully"
}
```

Save the returned id as:

```text
workflowId
```

---

### 2. Create Parallel DAG Workflow

```http
POST /api/workflows
```

Validates parallel task execution.

DAG:

```text
            build

        /          \

 unit-test      security-test

        \          /

            deploy
```

Expected:

```json
{
  "id": "workflow-id",
  "message": "Workflow created successfully"
}
```

---

### 3. Start Workflow Execution

```http
POST /api/workflows/{workflowId}/executions
```

Example:

```text
POST /api/workflows/<workflowId>/executions
```

Expected:

```json
{
  "id": "execution-id",
  "message": "Execution started"
}
```

Save returned id:

```text
executionId
```

---

### 4. Get Execution Status

```http
GET /api/executions/{executionId}
```

Validates:

- Execution lifecycle
- Workflow status

Expected states:

```text
RUNNING
SUCCESS
FAILED
CANCELLED
```

---

### 5. Get Execution Details

```http
GET /api/executions/{executionId}/details
```

Validates:

- Individual task execution
- Dependency order
- Task status tracking

Example:

```text
build        SUCCESS
unit-test    SUCCESS
security     SUCCESS
deploy       SUCCESS
```

---

## DAG Validation APIs

### Cycle Detection

```http
POST /api/workflows
```

Example invalid DAG:

```text
A -> B -> C -> A
```

Expected:

```text
400 Bad Request

Cycle detected
```

---

### Missing Dependency Validation

Example:

```text
deploy depends on build

but build task missing
```

Expected:

```text
400 Bad Request
```

---

## Approval Workflow

Create approval workflow:

```http
POST /api/workflows
```

Flow:

```text
build

  |

approval (WAITING_APPROVAL)

  |

production deploy
```

Approve:

```http
POST /api/executions/{executionId}/approval
```

---

## Cancellation

Cancel running workflow:

```http
POST /api/executions/{executionId}/cancel
```

Expected:

```text
Execution marked CANCELLED
```

---

## Covered Scenarios

| Feature | Status |
|---|---|
| Workflow creation | ✅ |
| DAG validation | ✅ |
| Sequential execution | ✅ |
| Parallel execution | ✅ |
| Task status tracking | ✅ |
| Approval flow | ✅ |
| Cancellation | ✅ |
| Error handling | ✅ |


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


