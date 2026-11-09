The previous topic files describe the code an agent should write. This one describes how the agent itself should work in the repo. It depends on your tools and CI, so adapt it. A starting point:

```
# Agentic Workflows

- **Allowed commands**: agents may run `./mvnw test`, `./mvnw spotless:apply`, and `git status` /
  `git diff` unattended; anything that pushes, deploys, or edits CI config needs a human to run it.
- **Order for a change that spans API and clients**: contract first (OpenAPI), then backend, then
  frontend, then the quality gate. Each step builds on a fixed contract instead of guessing it.
- **Multi-step tasks**: split work into commits that each build and pass tests on their own,
  instead of one large uncommitted change.
- **When to stop and ask**: pause before deleting a migration, changing a public API, or touching
  more than one feature package in a single change.
- **Subagents / tool use**: delegate read-only exploration (searching for usages, reading
  unfamiliar modules) to a subagent; keep the actual edit in the main agent so the diff stays
  reviewable.
- **Quality gate before every commit**: `./mvnw spotless:check && ./mvnw test`.
```

Once the project is bigger, this file is also the place to describe a small roster of specialised agents. The next section shows a real one, and the routing back to them lives in the task table of the root file.

For larger changes, keep a short **spec** per change (what, why, open questions) and a few Gherkin-style acceptance examples next to it, and link them from the root file. The agent then has a written decision to follow instead of reconstructing intent from the code.

## Agents in Practice: a Real Roster

"A small roster of specialised agents" sounds nice, but it doesn't help much until you see one. This is the setup I use on a real Spring Boot backend: **one orchestrator and a handful of subagents**, each with one job.

Don't copy the roster. **Copy the idea**: each agent does one job, in its own context window, and reports back a short verdict. If you only adopt three, start with a planner, a coder and a reviewer.

### The Orchestrator

The orchestrator is the main session, and the only agent that talks to me. It sizes every request first: a change of up to two files, with no new endpoint or entity, it does inline. Anything bigger it delegates.

Subagents can't ask me questions and can't spawn other agents. They report back to the orchestrator, which owns the product decisions, the spec status and the commits.

### The Team

| Stage | Agent | What it does | Model |
| --- | --- | --- | --- |
| Plan | `context-scout` | Explores the code once and writes a shared `context.md` so nobody else re-reads the repo | Haiku |
| Plan | `feature-planner` | Writes the spec: scope, API shape, a `Why:` line for each technical choice, the test list, open questions | Opus |
| Build | `backend-feature-scaffolder` | Implements the Spring Boot feature and its tests against the accepted spec | Sonnet |
| Review | `code-reviewer` | Checks the diff against the project rules and the spec; hunts for correctness bugs | Sonnet |
| Review | `api-contract-auditor` | Detects drift between the OpenAPI contract and the backend | Sonnet |
| Review | `security-reviewer` | OWASP review. **Only** when the diff touches auth, uploads, SQL, headers or secrets | Opus |
| Verify | `quality-gates-runner` | Runs formatting and tests, and returns a pass or fail per gate instead of the full logs | Haiku |
| Upkeep | `context-keeper` | Keeps `AGENTS.md`, `docs/` and the agent files accurate and within a size budget | Sonnet |
| Anywhere | `Explore` | Broad read-only searches when the answer matters more than the files | Haiku |

### How a Feature Flows

Every step is optional. A bug fix can be just 1, 6 and 8\.

```
1  size the request ── small? do it inline → 8
2  context-scout (background)         → specs/NNN/context.md
3  feature-planner                    → specs/NNN/spec.md + acceptance examples + open questions
4  ask the user                       → answers go into the spec
5  openapi-contract (inline skill)    → openapi.yaml, linted before any code
6  backend coder                      → code + tests
7  code-reviewer ∥ auditors           → specs/NNN/review-<agent>.md
8  quality-gates-runner               → short verdict, then commit
```

Reviewers run in parallel, but only the ones that apply: no `security-reviewer` for a change to a log message.

### The Rules That Make It Work

- **Files carry the content, the chat carries paths and verdicts.** The spec is the handoff, and the `context.md` beside it is the shared exploration. Every spawn prompt is five lines: task, paths, tier.

- **Reviewers are read-only.** They write findings to a file and never fix anything. The fix goes back to the agent that wrote the code, which still has the context.

- **Nobody commits on a coder's "verified".** The gates run fresh in `quality-gates-runner`, because a coder's own baseline has been wrong before.

- **The model matches the cost of a wrong answer.** Opus for design and security. Sonnet for code written against an accepted spec. Haiku for searching and running commands.

- **Conditional agents say so in their description.** "Use ONLY when…" keeps the expensive reviewers from running on every diff.

## Recommendations

Copy the root AGENTS.md above, plus whichever topic files under `docs/` actually apply to your project, and you've got good practices in place from day one.

Splitting the rules this way also makes it easy to adopt only what you need — drop a topic file you don't agree with, or add a new one for something not covered here, like `docs/security.md`.

If you work in a team, it's worth getting everyone to actually agree with these practices before they go in the file — a rule nobody follows is worse than no rule at all. And it's worth revisiting the file periodically, since Java and Spring Boot keep moving and a rule that made sense two years ago sometimes doesn't anymore.

Whatever an agent or autocompletion tool generates from this file still needs a human to review it — the file raises the odds the code looks right, it doesn't guarantee it.

## Maintenance

I will be maintaining and updating this AGENTS.md file, so there may be new versions in the future with improvements or changes in the recommended practices.
