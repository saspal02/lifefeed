# Agentic Workflows

The other topic files describe the code an agent should write. This one describes how the agent
itself should work in the repo. It depends on your tools and CI, so adapt it.

## Allowed commands

- Agents may run `./mvnw test`, `./mvnw spotless:apply`, `./mvnw spotless:check`, `git status`
  and `git diff` unattended.
- Anything that pushes, deploys, edits CI config, changes a secret or runs a destructive
  database command needs a human to run it.
- Do not commit unless the user asked for a commit. Do not rewrite history.

## Order for a change that spans API and clients

1. Contract first (OpenAPI), so every later step builds on a fixed contract.
2. Backend implementation and tests.
3. Frontend or consumer changes.
4. Quality gate.

## Multi-step tasks

Split work into commits that each build and pass tests on their own, instead of one large
uncommitted change. A change that moves packages, changes behaviour and reformats unrelated
files is three changes.

## When to stop and ask

Pause before:

- deleting or rewriting a migration;
- changing a public API or an error contract;
- touching more than one feature package in a single change;
- changing the security filter chain or endpoint access;
- adding a dependency (Lombok, MapStruct, a tracing bridge, a new client);
- committing, pushing or opening a pull request.

## Quality gate before every commit

```shell
./mvnw spotless:check && ./mvnw test
```

The full gate is `./mvnw clean verify`, which also runs the JaCoCo coverage check. CI runs
`./mvnw -ntp verify`.

## Subagents

- Delegate read-only exploration (searching for usages, reading unfamiliar modules) to a
  subagent; keep the actual edit in the main agent so the diff stays reviewable.
- Subagents cannot ask the user questions and cannot spawn other agents. They report back to
  the orchestrator, which owns the product decisions, the spec status and the commits.
- Reviewers are read-only. They write findings to a file and never fix anything. The fix goes
  back to the agent that wrote the code, which still has the context.
- Reviewers run in parallel, but only the ones that apply: no security review for a change to
  a log message.

## A small roster

The article's roster, adapted to this project. Do not copy it blindly; each agent should do one
job, in its own context window, and report a short verdict.

| Stage   | Agent                         | What it does                                                        | Capability tier        |
| ------- | ----------------------------- | ------------------------------------------------------------------- | ---------------------- |
| Plan    | `context-scout`               | Explores the code once and writes a shared `context.md`             | Fast / cheap           |
| Plan    | `feature-planner`             | Writes the spec: scope, API shape, a `Why:` per choice, tests, open questions | Frontier / strongest   |
| Build   | `backend-feature-scaffolder`  | Implements the Spring Boot feature and its tests against the spec   | Standard coding model  |
| Review  | `code-reviewer`               | Checks the diff against the project rules and the spec              | Standard coding model  |
| Review  | `api-contract-auditor`        | Detects drift between the OpenAPI contract and the backend          | Standard coding model  |
| Review  | `security-reviewer`           | OWASP review. **Only** when the diff touches auth, uploads, SQL, headers or secrets | Frontier / strongest   |
| Verify  | `quality-gates-runner`        | Runs formatting and tests, returns pass/fail per gate               | Fast / cheap           |
| Upkeep  | `context-keeper`              | Keeps `AGENTS.md`, `docs/` and agent files accurate and within budget | Standard coding model  |
| Anywhere| `Explore`                     | Broad read-only searches when the answer matters more than the files | Fast / cheap           |

OpenCode discovers project agents in `.opencode/agents/<name>.md`. This repository does not
scaffold them yet; when it does, give each one `mode: subagent`, a description that says when
to use it, and permissions that match its job (reviewers deny `edit`). Leave `model` unset so
the agent inherits the parent session's model, then map the capability tiers above to the
provider's available models.

## How a feature flows

Every step is optional. A bug fix can be just 1, 6 and 8.

```
1  size the request ── small? do it inline → 8
2  context-scout (background)         → docs/specs/NNN/context.md
3  feature-planner                    → docs/specs/NNN/spec.md + acceptance examples + open questions
4  ask the user                       → answers go into the spec
5  openapi contract (inline skill)     → openapi.yaml, linted before any code
6  backend coder                      → code + tests
7  code-reviewer ∥ auditors           → docs/specs/NNN/review-<agent>.md
8  quality-gates-runner               → short verdict, then commit
```

## Sizing

- A change of up to two files, with no new endpoint or entity: do it inline.
- Anything bigger: delegate. The orchestrator is the main session and the only agent that
  talks to the user.
- If a subagent's report contradicts the spec, the orchestrator decides; the subagent does not
  silently expand scope.

## Specs

For larger changes, keep a short **spec** per change (what, why, open questions) and a few
Gherkin-style acceptance examples next to it, and link them from the root file. Suggested
layout:

```
docs/specs/NNN/
  context.md      shared exploration from context-scout
  spec.md         scope, API shape, Why: lines, test list, open questions
  review-*.md     read-only findings from reviewers
```

The spec is the handoff. Files carry the content; the chat carries paths and verdicts. A spawn
prompt should be short: task, paths, tier.

## Model choice

Match the model to the cost of a wrong answer: strongest models for design and security,
standard coding models for implementation against an accepted spec, fast/cheap models for
searching and running commands. This environment exposes `opencode/*-free` models and Ollama;
if a frontier model is not available, say so and use the strongest available model for the
planner and security reviewer rather than silently downgrading them.
