# MyCliAgent

MyCliAgent is a Java learning project for building a command-line AI agent. The project is focused on understanding two common agent architectures:

- ReAct: the agent reasons, calls tools, observes tool results, and repeats.
- Plan-and-Execute: the agent first creates a structured plan, then executes tasks according to dependencies.

This project is still in progress. The ReAct path has the core tool-calling loop and local tool registry. The Plan-and-Execute path has planning, task modeling, dependency handling, and execution-order calculation, but task execution is not fully wired yet.

## Current Status

Implemented:

- Java CLI entry point.
- GLM chat client wrapper.
- ReAct-style agent loop.
- Tool definitions for file reading, file writing, directory listing, shell command execution, and project directory creation.
- Tool call data model.
- Conversation history management.
- Planner prompt for generating JSON execution plans.
- `ExecutionPlan` and `Task` domain models.
- Dependency tracking between tasks.
- Topological sorting and cycle detection for execution plans.
- Basic `.env` loading for `GLM_API_KEY`.

Not finished yet:

- `PlanExecuteAgent.executeTask()` currently only prints the task description.
- Plan-and-Execute is not connected to the CLI by default.
- Replanning exists as a method, but failure-driven automatic replanning is not fully integrated.
- Tool execution does not yet enforce sandboxing or user confirmation.
- There are no unit tests yet.
- Error handling and JSON validation can be improved.

## Architecture Overview

```text
User
  |
  v
CLI Main
  |
  v
Agent / PlanExecuteAgent
  |
  +--> LlmClient / GLMClient
  |
  +--> ToolRegistry
  |
  +--> Planner / ExecutionPlan / Task
```

## ReAct Agent

The ReAct implementation is centered in:

```text
src/main/java/com/mycliagent/agent/Agent.java
```

The loop works like this:

```text
1. Add user input to conversation history.
2. Send conversation history and tool definitions to the LLM.
3. If the LLM returns tool calls:
   - Save the assistant tool-call message.
   - Execute each requested tool.
   - Add tool results back into conversation history.
   - Continue the loop.
4. If the LLM returns normal text:
   - Save the assistant response.
   - Return the final answer.
5. Stop if MAX_ITERATIONS is reached.
```

Key classes:

- `Agent`: controls the ReAct loop.
- `LlmClient`: defines the chat, message, tool, and tool-call abstractions.
- `GLMClient`: sends requests to the GLM chat completion API.
- `ToolRegistry`: stores available tools and executes tool calls.

Why this matters:

ReAct turns an LLM from a single-response system into an iterative decision-making agent. The model can decide when to inspect files, run commands, write files, or continue reasoning based on observations.

## Plan-and-Execute Agent

The Plan-and-Execute implementation is centered in:

```text
src/main/java/com/mycliagent/agent/PlanExecuteAgent.java
src/main/java/com/mycliagent/plan/Planner.java
src/main/java/com/mycliagent/plan/ExecutionPlan.java
src/main/java/com/mycliagent/plan/Task.java
```

The intended flow is:

```text
1. Decide whether the user request needs planning.
2. Ask the LLM planner to produce a JSON task plan.
3. Parse the JSON into an ExecutionPlan.
4. Normalize task IDs.
5. Build task dependencies.
6. Run topological sorting to compute execution order.
7. Execute each task in dependency-safe order.
8. Summarize results.
```

The planner asks the LLM to return JSON like:

```json
{
  "summary": "任务摘要",
  "tasks": [
    {
      "id": "task_1",
      "description": "任务描述",
      "type": "FILE_READ",
      "dependencies": []
    }
  ]
}
```

Supported task types:

- `FILE_READ`
- `FILE_WRITE`
- `COMMAND`
- `ANALYSIS`
- `VERIFICATION`

Why this matters:

Plan-and-Execute is better for complex, multi-step tasks. It separates planning from execution, making the agent easier to debug, visualize, and extend. It also makes dependency handling explicit through a task graph.

## ReAct vs Plan-and-Execute

| Aspect | ReAct | Plan-and-Execute |
| --- | --- | --- |
| Strategy | Think and act step by step | Plan first, execute later |
| Best for | Exploratory tasks | Complex multi-step tasks |
| Main state | Conversation history | Execution plan and task graph |
| Strength | Flexible and adaptive | Structured and easier to inspect |
| Risk | Can loop or drift | Initial plan may be wrong |
| Project class | `Agent` | `PlanExecuteAgent` |

## How to Build

Requirements:

- Java 17
- Maven

Compile and run tests:

```bash
mvn test
```

Compile only:

```bash
mvn compile
```

## How to Run

Create a `.env` file in the project root:

```text
GLM_API_KEY=your_api_key_here
```

Run the CLI:

```bash
mvn exec:java -Dexec.mainClass=com.mycliagent.cli.Main
```

If Maven cannot find the exec plugin in some environments, add `exec-maven-plugin` to `pom.xml` or run the compiled class through your IDE.

## Interview Explanation

A concise way to explain this project:

> This is a Java command-line AI agent project. I used it to learn two agent architectures: ReAct and Plan-and-Execute. The ReAct agent keeps conversation history, sends tool definitions to the model, executes returned tool calls, feeds observations back into the model, and repeats until the model returns a final answer. The Plan-and-Execute part asks the model to produce a structured JSON plan, parses it into task objects, builds dependencies, and uses topological sorting to compute a valid execution order.

More detailed interview points:

- I separated the LLM provider behind `LlmClient`, so the agent logic is not tightly coupled to `GLMClient`.
- I modeled tools with name, description, JSON parameters, and executor function.
- I used conversation history to preserve context across ReAct iterations.
- I added a maximum iteration limit to prevent infinite loops.
- I represented plans as tasks with dependencies, which forms a directed graph.
- I used DFS-based topological sorting to detect cycles and generate a valid execution order.
- I intentionally keep Plan-and-Execute separate from ReAct because they solve different workflow problems.

## Next Steps

Useful improvements:

- Connect `PlanExecuteAgent` to the CLI with a mode switch.
- Implement real execution in `executeTask()` based on `TaskType`.
- Add unit tests for `ExecutionPlan.computeExecutionOrder()`.
- Add tests for `ToolRegistry.executeTool()`.
- Improve JSON validation in `Planner`.
- Add safety checks before shell command execution.
- Add automatic replanning after failed tasks.
- Add logging instead of `System.out.println`.

