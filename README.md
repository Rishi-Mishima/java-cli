# MyCliAgent

A Java command-line AI agent that demonstrates ReAct-style tool calling, memory-aware prompting, and a Plan-and-Execute architecture prototype.

This project is designed as a portfolio-friendly AI agent implementation: it can run in `mock` mode without a paid API key, while still keeping a real GLM provider behind the same `LlmClient` abstraction.

## Features

- Java 17 CLI entry point with an interactive chat loop.
- Provider-agnostic LLM interface through `LlmClient`.
- Real GLM integration through `GLMClient`.
- API-free demo mode through `MockLlmClient`.
- ReAct loop: model response, tool call, tool observation, final answer.
- Tool registry for file reading, file writing, directory listing, shell commands, and project directory creation.
- Short-term conversation memory and long-term memory retrieval.
- Plan-and-Execute prototype with task modeling, dependency tracking, topological sorting, and cycle detection.

## Quick Start

Requirements:

- Java 17
- Maven

Clone the project and create a local `.env` file:

```bash
cp .env.example .env
```

Run in mock mode:

```bash
mvn -q package
mvn -q exec:java -Dexec.mainClass=com.mycliagent.cli.Main
```

Mock mode is the default and does not call any external LLM API.

## Demo Script

After the CLI starts, try:

```text
你好，请用一句话介绍你自己
```

Then test tool calling:

```text
请读取 README.md，并总结这个项目是做什么的
```

```text
请列出当前项目的 src/main/java 目录结构
```

```text
请执行 mvn -q package，告诉我是否编译成功
```

```text
请创建一个文件 temp-agent-test.txt，内容是 hello from agent
```

Use:

```text
clear
```

to clear conversation history, and:

```text
exit
```

to quit.

## Real GLM Mode

To call the real BigModel GLM API, edit `.env`:

```text
LLM_PROVIDER=glm
GLM_API_KEY=your_glm_api_key_here
```

Then run:

```bash
mvn -q exec:java -Dexec.mainClass=com.mycliagent.cli.Main
```

The current GLM client uses:

```text
https://open.bigmodel.cn/api/paas/v4/chat/completions
glm-5.1
```

If the GLM account has no balance or resource package, the API may return an HTTP 429 billing error.

## Architecture

```text
User
  |
  v
CLI Main
  |
  v
Agent
  |
  +--> LlmClient
  |      +--> MockLlmClient
  |      +--> GLMClient
  |
  +--> ToolRegistry
  |      +--> read_file
  |      +--> write_file
  |      +--> list_dir
  |      +--> execute_command
  |      +--> create_project
  |
  +--> MemoryManager
         +--> ConversationMemory
         +--> LongTermMemory
         +--> MemoryRetriever
         +--> ContextCompressor
```

The agent depends on the `LlmClient` interface rather than a concrete provider. This keeps the core loop independent from GLM and makes it possible to add OpenAI, Claude, DeepSeek, or other OpenAI-compatible providers later.

## ReAct Flow

The ReAct implementation is centered in:

```text
src/main/java/com/mycliagent/agent/Agent.java
```

The loop:

```text
1. Store user input in memory.
2. Retrieve relevant memory and inject it into the system prompt.
3. Send conversation history and tool definitions to the LLM.
4. If the LLM returns tool calls, execute them through ToolRegistry.
5. Add tool observations back to conversation history.
6. Repeat until the LLM returns a final answer.
7. Stop after a maximum iteration limit.
```

## Plan-and-Execute Prototype

The Plan-and-Execute implementation is centered in:

```text
src/main/java/com/mycliagent/agent/PlanExecuteAgent.java
src/main/java/com/mycliagent/plan/Planner.java
src/main/java/com/mycliagent/plan/ExecutionPlan.java
src/main/java/com/mycliagent/plan/Task.java
```

It models larger tasks as a dependency graph:

```text
1. Ask the planner to produce a JSON execution plan.
2. Parse tasks into domain objects.
3. Normalize task IDs.
4. Build dependency links.
5. Topologically sort tasks.
6. Detect cycles before execution.
```

The execution step is intentionally still a prototype, which makes it a good area for future extension.

## Design Notes

- `LlmClient` isolates provider-specific request and response formats from agent behavior.
- `ToolRegistry` centralizes tool metadata and execution.
- `MockLlmClient` makes the project reviewable without external API credentials.
- The memory layer separates conversation memory, long-term storage, retrieval, compression, and token budgeting.
- Plan-and-Execute is kept separate from ReAct because the two patterns serve different task shapes.

## Project Structure

```text
src/main/java/com/mycliagent
├── agent
│   ├── Agent.java
│   └── PlanExecuteAgent.java
├── cli
│   └── Main.java
├── llm
│   ├── LlmClient.java
│   ├── GLMClient.java
│   └── MockLlmClient.java
├── memory
├── plan
└── tool
```

## Portfolio Summary

MyCliAgent is a Java CLI AI agent framework built to explore production-style agent architecture. It implements a ReAct tool-calling loop, provider abstraction, configurable mock and GLM backends, memory retrieval, and an early Plan-and-Execute task graph. The project demonstrates practical agent engineering concepts without relying on a heavyweight framework.

## Roadmap

- Add `OpenAIClient` and provider selection for OpenAI-compatible APIs.
- Add unit tests for `ExecutionPlan`, `MemoryRetriever`, and `ToolRegistry`.
- Add safety confirmation before shell command execution.
- Connect `PlanExecuteAgent` to the CLI with a mode switch.
- Add structured logs or trace output for each ReAct iteration.
- Improve JSON validation and error handling for planner responses.

## License

Add an open-source license such as MIT or Apache 2.0 before using this as a public portfolio project.
