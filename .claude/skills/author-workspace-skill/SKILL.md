---

allowed-tools: [Bash, Edit, Glob, Grep, Read, Write]
argument-hint: "<path to component source>"
description: Generate a Liferay sample-workspace skill and reference cards for a component by reading its source, then wire the result into every AI agent. Use when a Liferay product team wants to add AI-agent coverage for their component to the sample workspace, or asks to author, scaffold, or generate a workspace skill from code.
name: author-workspace-skill

---

# Author Workspace Skill

Generate a sample-workspace **skill** (and any supporting reference **cards**) for a Liferay
component by reading that component's source, then wire the generated files into every AI agent.
The output targets external developers building on Liferay, so it teaches the component's
consumer-facing surface — Headless REST, MCP, client extensions — not its internal
implementation.

This is a **scaffolder with a substance-capture checkpoint**, not a one-shot finished-skill
generator. The code reliably supplies structure, endpoints, and schemas; the high-value gotchas
and the product framing come from a short checkpoint and from marked inference. For a component
with a REST surface it drafts a substantial skill; for a UI-only or service-only component it
often produces a mostly-inferred skeleton — that is expected, not a failure.

This skill is **not opinionated about which component** it documents. It enforces the sample
workspace's conventions — naming, file layout, section structure, `curl` house style, card
factoring, symlink wiring, index registration — and derives the substance from source. The target
conventions and the exact skeletons it emits live in
[references/workspace-skill-anatomy.md](references/workspace-skill-anatomy.md); read that file
before drafting. Formatting follows `.claude/rules/markdown-style.md` — apply it, do not restate
it.

## Input

- **A path to component source** (`${ARGUMENTS}`) — a module, a directory, or a single file
  anywhere under the repo. Any path is accepted; this skill locates the consumer surface itself.

- Optional: a short hint naming the audience or the intended entry point, when the path alone is
  ambiguous.

## Preconditions

- The sample workspace exists at `workspaces/liferay-sample-workspace`. Resolve its
  `.workspace-rules` directory once and reuse it; that is where every base file is written.

- The five agent directories (`.claude`, `.cursor`, `.gemini`, `.github`, `.windsurf`) are
  present under the workspace. Each receives one symlink per generated base file.

## Workflow

### Locate the Consumer Surface

Follow [references/surface-discovery.md](references/surface-discovery.md) to resolve the given
path to the component's **external** contract — prefer a sibling `*-rest-api` / `*-rest-impl`
module and its hand-authored `rest-openapi.yaml`, then generated `Base*ResourceImpl` classes,
then an MCP surface or a client-extension `client-extension.yaml`. Generate from that surface,
never from implementation internals.

When no externally consumable surface can be found — only internal services or UI APIs — do not
fabricate one. Record a prominent warning for the final report, draft best-effort from whatever
surface exists, and mark the whole skill `inferred — verify`.

### Extract Facts

From the OpenAPI spec and resource classes, collect the facts the skill will state:

- Endpoints, methods, and request/response schemas — the raw material for `curl` blocks.

- `components.schemas` enums and DTOs — the valid-value tables.

- OAuth scopes.

- Feature flags and their defaults. Mark every flag `inferred — verify`; a flag's default is
  easy to misread from source and a wrong default in the Prerequisites table misleads callers.

### Mine Gotchas

Scan validation and exception code — `throw new *Exception`, required-field checks, allowed-value
checks — for constraints a caller would otherwise trip. Turn each into a guard in the skill's
**Patterns and Gotchas** section (for example, a value that must be omitted on create because it
returns `400`). Mark every mined guard `inferred — verify`; a control-flow read is not proof of
runtime behavior.

### Confirm the Non-Derivable Fields

Some fields cannot come from code. Present drafts and let the developer confirm or edit each in a
single short pass:

- **Skill name** — a proposed kebab-case verb phrase, checked for collisions.

- **Router-table intent** — the user intent the skill answers, for its `liferay-rules.md` row.

- **Trigger phrases** — the `Use when the user asks to …` list and the `## When to Invoke`
  bullets.

- **Known gotchas** — any the developer wants to add beyond what mining found.

Write any field left unanswered as `TODO`.

### Draft the Skill

Write `<workspace-rules>/skills/<name>/SKILL.md` following the skeleton and conventions in
[references/workspace-skill-anatomy.md](references/workspace-skill-anatomy.md) — the fixed section
order, list/table-first prose, and a `## Success Signal` promoted from the workflow's own
verification (or `TODO / inferred — verify` when none exists; never invented). Frontmatter is
`description` + `name`; Claude-only metadata keys the other agents ignore (`allowed-tools`,
`argument-hint`) are allowed, but never `globs`/`alwaysApply`/`disable-model-invocation`. On a
re-run where the file already exists, **update in place**: refresh the derived sections (endpoints,
schemas, flag table) and preserve human-edited sections (confirmed triggers, hand-written gotchas,
success signal).

### Draft or Update Cards

Apply the Card Factoring Rule in the anatomy reference: endpoints, scopes, and flags always
extend the existing shared cards (`headless-apis`, `oauth-scopes`, `feature-flags-catalog`);
other reusable data becomes one component-named card only when it crosses the reuse threshold;
never create a second card that overlaps an existing one. Cite each card from the skill by
relative path, and dedup rows when extending a card on a re-run.

### Wire Into Every Agent

For each base file written under `.workspace-rules`, create the matching symlink in all five
agent directories and register the skill in the entry point — a row in the intent → skill router
table, and a Reference Cards entry for any new card. The exact symlink targets and router
edits are specified in
[references/workspace-skill-anatomy.md](references/workspace-skill-anatomy.md). Skip any symlink
or index entry that already exists.

### Lint and Coverage Check

Verify the generated artifacts before reporting.

Structural gate:

- Frontmatter has `description` + `name`; `allowed-tools`/`argument-hint` are allowed. Strip any
  `globs`, `alwaysApply`, or `disable-model-invocation` — those are parsed by other agents or drive
  Claude-specific behavior and break agent-agnostic parity.

- Headings are Title Case and unnumbered; the file otherwise passes `.claude/rules/markdown-style.md`.

- The skill ends with a `## Success Signal` (promoted from the workflow, or `TODO`), and any new
  card holds facts only — no curl blocks or step sequences leaked into a card.

- Every cited card exists, every created symlink resolves (`readlink -e`), and the router-table
  and Reference Cards entries are present in `liferay-rules.md`.

Coverage checklist:

- Every OpenAPI operation on the surface is represented by a workflow step.

- Every workflow step that hits an API has a `curl` block.

- Every extracted flag has a row in the Prerequisites table.

### Report

Summarize what was created: the base files, the symlinks, and the index edits — all left
**uncommitted**. List every `inferred — verify` guard and flag and every `TODO` field so the
developer knows exactly what to confirm before committing. Do not commit, and do not push.

## Output

A conventions-correct workspace skill (and any cards) written under `.workspace-rules`, symlinked
into every agent and registered in the entry point, left uncommitted — plus a report enumerating
the files touched and the human follow-ups (the `inferred — verify` items and the `TODO` fields).
