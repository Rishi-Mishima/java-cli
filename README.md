# MyCliAgent

一个用 Java 17 从零实现的命令行 AI Agent 框架，核心目标是展示 Agent 工程中的关键能力：ReAct 工具调用、Plan-and-Execute、多 Agent 协作、代码库 RAG、记忆管理、人工审批、安全工具边界和终端交互体验。

这个项目不是简单封装大模型 API，而是把一个 CLI Agent 拆成可扩展的工程模块，适合作为面试作品讲解 Agent 系统设计、上下文管理、工具执行安全和并发编排。

A Java 17 command-line AI Agent framework built from scratch to demonstrate core agent-engineering capabilities: ReAct tool use, Plan-and-Execute workflows, multi-agent collaboration, codebase RAG, memory management, human approval, safe tool boundaries and terminal-first interaction.

This project is not just a thin wrapper around an LLM API. It decomposes a CLI Agent into extensible engineering modules, making it suitable for interview discussions around agent system design, context management, safe tool execution and concurrent orchestration.

## Highlights

- **Agent 核心循环**：实现 ReAct 风格的多轮推理、工具调用、工具结果回灌和最终回答生成。
  **Agent Core Loop**: Implements ReAct-style multi-turn reasoning, tool invocation, tool observation feedback and final answer generation.
- **Plan-and-Execute**：将复杂任务拆成依赖图，支持计划审核、依赖排序、失败重规划和并行执行。
  **Plan-and-Execute**: Breaks complex tasks into a dependency graph with plan review, dependency ordering, failure replanning and parallel execution.
- **Multi-Agent Orchestrator**：内置 planner、worker、reviewer 三类子 Agent，支持任务拆解、并发执行、结果评审和重试。
  **Multi-Agent Orchestrator**: Provides planner, worker and reviewer sub-agents for task decomposition, concurrent execution, result review and retry.
- **代码库 RAG**：支持 `/index` 建立代码索引，使用代码分块、JavaParser 关系分析、Embedding 和 SQLite 向量存储完成混合检索。
  **Codebase RAG**: Supports `/index` for code indexing, combining code chunking, JavaParser relation analysis, embeddings and SQLite-backed vector storage.
- **安全工具系统**：统一 ToolRegistry 注册文件、命令、搜索、浏览器、记忆、技能和快照相关工具，并通过路径保护、命令守卫、HITL 审批降低误操作风险。
  **Safe Tool System**: Uses a centralized ToolRegistry for file, command, search, browser, memory, skill and snapshot tools, with path guards, command guards and HITL approval.
- **记忆与上下文管理**：区分短期对话记忆和长期事实记忆，按模型上下文窗口动态设置预算，并支持历史压缩。
  **Memory and Context Management**: Separates short-term conversation memory from long-term facts, adjusts budgets by model context window and supports history compaction.
- **终端交互体验**：基于 JLine、ANSI 渲染、折叠块、状态栏和工具调用展示，提升 CLI Agent 的可观察性。
  **Terminal Interaction**: Uses JLine, ANSI rendering, foldable blocks, status display and tool-call visualization to make the CLI Agent observable.
- **可测试、可演示**：默认 mock 模式无需 API Key；Maven profile 区分日常打包、快速回归和阶段性 smoke test。
  **Testable and Demo-Friendly**: Runs in mock mode without an API key, with Maven profiles for packaging, quick regression and staged smoke tests.

## Tech Stack

- **Language**: Java 17
- **Build**: Maven
- **LLM**: GLM API + Mock provider
- **HTTP**: OkHttp
- **JSON**: Jackson
- **Logging**: Logback
- **Terminal UI**: JLine, Lanterna, ANSI renderer
- **Code Analysis**: JavaParser
- **RAG Storage**: SQLite JDBC
- **HTML Parsing**: Jsoup
- **Git Snapshot Foundation**: JGit
- **Testing**: JUnit 5, Mockito, MockWebServer

## Quick Start

### 1. Build

```bash
mvn -q package
```

By default, `package` skips tests so the CLI artifact can be built quickly for manual demo.

### 2. Run in mock mode

```bash
java -jar target/MyCliAgent-1.0-SNAPSHOT.jar
```

Mock mode is the default. It does not require an external API key and is suitable for interviews or local demos.

### 3. Run with GLM

Create or edit `.env`:

```text
LLM_PROVIDER=glm
GLM_API_KEY=your_glm_api_key_here
HITL_ENABLED=true
```

Then run:

```bash
java -jar target/MyCliAgent-1.0-SNAPSHOT.jar
```

## CLI Usage

After startup, the CLI enters an interactive loop:

```text
你好，请介绍一下你自己
```

Use ReAct tool calling:

```text
请读取 README.md，并总结这个项目
```

Index the current project:

```text
/index
```

Search the indexed codebase:

```text
/search ToolRegistry 如何保护文件路径
```

Run a multi-agent task:

```text
/multi 分析这个项目的架构，并找出三个可以优化的点
```

Utility commands:

```text
clear
exit
```

## Architecture

```text
User
  |
  v
CLI Main
  |
  +--> Agent                         # ReAct loop
  |     +--> LlmClient                # provider abstraction
  |     +--> ToolRegistry             # tools and safety boundary
  |     +--> MemoryManager            # short-term / long-term memory
  |     +--> PromptAssembler          # prompt mode assembly
  |
  +--> PlanExecuteAgent               # task graph planning and execution
  |     +--> Planner
  |     +--> ExecutionPlan
  |     +--> Task
  |
  +--> AgentOrchestrator              # multi-agent collaboration
        +--> planner SubAgent
        +--> worker SubAgents
        +--> reviewer SubAgent
```

## Module Structure

```text
src/main/java/com/mycliagent
├── agent      # ReAct Agent, PlanExecuteAgent, multi-agent orchestration, streaming, token budget
├── browser    # browser mode and browser audit metadata
├── cli        # command-line entry point and runtime config loading
├── context    # model/context profile configuration
├── hitl       # human-in-the-loop approval flow
├── llm        # LlmClient abstraction, GLM client, mock client
├── memory     # conversation memory, long-term memory, retrieval, compression, token budget
├── plan       # execution plan, task model, planner
├── prompt     # prompt assembly, project memory loading, prompt modes
├── rag        # code chunking, indexing, embeddings, vector store, hybrid search
├── render     # plain and inline terminal renderers
├── runtime    # cancellation token/context
├── skill      # skill registry and skill context loading
├── tool       # tool registry, guards, web fetch/search, snapshots, audit log, LSP manager
└── util       # ANSI style and Chinese tokenizer helpers
```

## Core Design

### 1. Provider Abstraction

The agent depends on `LlmClient` instead of a concrete model SDK.

This makes the runtime independent from GLM and keeps the system open for future OpenAI-compatible providers. The mock implementation also allows reviewers to run the project without credentials.

```text
LlmClient
├── GLMClient
└── MockLlmClient
```

### 2. Tool Registry as a Safety Boundary

`ToolRegistry` is the single place where tools are registered, described, validated and executed.

Current tool categories include:

- file tools: `read_file`, `write_file`, `list_dir`, `glob_files`, `grep_code`
- command tools: `execute_command`, `create_project`
- code intelligence: `search_code`
- web tools: `web_search`, `web_fetch`
- browser tools: `browser_connect`, `browser_disconnect`, `browser_status`
- memory and skill tools: `save_memory`, `load_skill`
- recovery tools: `revert_turn`

Safety-related mechanisms include:

- path normalization and project-root restriction through `PathGuard`
- command validation through `CommandGuard`
- browser operation validation through `BrowserGuard`
- audit logging for sensitive tool calls
- optional HITL approval before risky operations
- file write size limit to avoid accidental large writes

### 3. ReAct Execution Loop

`Agent` implements the core ReAct loop:

```text
1. Store user input in memory.
2. Retrieve relevant long-term memory.
3. Rebuild the system prompt with memory, project context and skill index.
4. Send messages and tool definitions to the LLM.
5. Execute tool calls through ToolRegistry.
6. Append tool observations back into the conversation.
7. Continue until the model returns a final answer.
8. Stop early on cancellation, budget exhaustion or loop protection.
```

### 4. Plan-and-Execute

`PlanExecuteAgent` is built for larger tasks that benefit from planning before acting.

It supports:

- planner-generated JSON execution plans
- task dependency graph
- executable task detection
- parallel execution for independent tasks
- plan review hook
- replanning on early failure
- history compression during long runs

### 5. Multi-Agent Collaboration

`AgentOrchestrator` implements a simple but interview-friendly multi-agent workflow:

```text
planner -> workers -> reviewer -> final summary
```

Independent steps in the same dependency batch can run concurrently. Each worker writes to an isolated output buffer, and the orchestrator flushes results in deterministic order to avoid messy terminal output.

### 6. Code RAG

The RAG subsystem is designed for project-aware code questions:

```text
CodeIndex
  -> collect source files
  -> chunk code
  -> generate embeddings
  -> analyze Java symbol relations
  -> persist chunks and relations to SQLite

CodeRetriever
  -> combine vector search and keyword/symbol search
  -> format ranked results for CLI
```

This allows the CLI to answer questions about the current repository without reading the entire codebase into context.

### 7. Memory and Context Budget

`MemoryManager` separates:

- short-term conversation memory
- long-term fact memory
- context retrieval
- token usage tracking
- context profile configuration

`ConversationHistoryCompactor` provides an additional safeguard when the active conversation approaches the model context limit.

## Human-in-the-Loop

Risky operations can go through a human approval layer:

```text
Agent / PlanExecuteAgent / SubAgent
  -> HitlToolRegistry
  -> HitlHandler
  -> TerminalHitlHandler
```

This is useful in interviews because it shows that the agent is not only capable of acting, but also designed with operational control in mind.

## Testing

Run quick regression tests:

```bash
mvn test -Pquick
```

Run the phase 16 smoke test profile:

```bash
mvn test -Pphase16-smoke
```

Run package with tests explicitly enabled:

```bash
mvn -q package -DskipTests=false
```

Existing tests cover concurrency-sensitive areas such as:

- `ToolRegistryConcurrencyTest`
- `AgentOrchestratorConcurrencyTest`
- `PlanExecuteAgentConcurrencyTest`
- `RagSmokeTest`

## Interview Talking Points

- **Why Java?** Java makes concurrency, type boundaries and long-running CLI processes explicit, which helps demonstrate system design rather than only prompt scripting.
- **Why a ToolRegistry?** It centralizes capability exposure, schema generation, validation, execution and audit behavior.
- **Why mock mode?** A reviewer can run the project without API credentials, while the production path still uses the same `LlmClient` contract.
- **Why Plan-and-Execute and ReAct both exist?** ReAct works well for exploratory tool use; Plan-and-Execute is better for larger goals with dependencies and review points.
- **Where is the safety model?** Path restrictions, command guards, browser guards, HITL approval, audit logs and write limits form a layered safety boundary.
- **Where is the scalability story?** The project separates model provider, prompt assembly, memory, tools, RAG, rendering and orchestration so each layer can evolve independently.

## Roadmap

- Add more provider implementations such as OpenAI-compatible APIs.
- Persist long-term memory with project-level scope.
- Replace the placeholder snapshot service with full pre-turn restore support.
- Expand MCP tool integration and external resource indexing.
- Add richer LSP diagnostics and automatic post-edit validation.
- Add integration tests for browser and web tool flows.
- Package the CLI with a more polished install command.

## Portfolio Summary

MyCliAgent is a Java CLI Agent framework that demonstrates practical AI agent engineering beyond a basic chatbot. It includes a provider-agnostic LLM layer, ReAct tool use, Plan-and-Execute task graphs, multi-agent orchestration, codebase RAG, memory retrieval, terminal rendering, HITL approval and tool safety guards.

For interviews, the strongest story is that the project treats the LLM as one component inside a controlled runtime: tools are explicit, context is budgeted, actions are auditable, and larger tasks can be planned, reviewed and executed with concurrency.

## License

Add an open-source license such as MIT or Apache 2.0 before publishing this project publicly.
