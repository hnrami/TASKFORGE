# TaskForge Postman Collection

Import:

1. `TaskForge.postman_collection.json`
2. `TaskForge.postman_environment.json`

Select the **TaskForge Local** environment and run the application on
`http://localhost:8080`.

Run **Create Valid Workflow** before execution requests. Its test script stores
`workflowId`; **Start Execution** stores `executionId`.

The current Task 1 project intentionally has no REST controllers, services,
engine, handlers, or database logic. Requests therefore return `404` until
those later implementation stages are completed. The collection defines the
documented target contracts and validation scenarios.
