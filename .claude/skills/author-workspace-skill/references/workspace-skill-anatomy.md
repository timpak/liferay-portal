# Workspace Skill Anatomy

The conventions and skeletons this skill emits. Everything here targets the **sample workspace**
(`.workspace-rules`), which is symlinked into five AI agents — so the output is the
lowest-common-denominator format every agent understands, not the richer Claude-only skill
format. Formatting follows `.claude/rules/markdown-style.md`.

## The Three Layers

The workspace guidance has three layers, and the generated files must fit them:

- **Entry point** — `liferay-rules.md` establishes context and routes to a skill through an intent
  → skill table. New skills and cards are registered here.

- **Reference cards** — `rules/<card>.md` hold static facts (API paths, scope strings, flag
  defaults) that skills cite. A fact lives in exactly one card. Cards hold facts, not procedure.

- **Skills** — `skills/<name>/SKILL.md`, one workflow each. Skills cite cards by relative path
  rather than inlining bulk lookup data, but keep every gotcha and load-bearing fact inline.

## Skill File Layout

A skill is a directory containing exactly one file, `SKILL.md`. There are no per-skill
`references/` or `scripts/` subdirectories — the symlink wiring is per-file, so subdirectory files
would never reach the four non-Claude agents. Shared lookup data goes into a card; a skill that is
really two jobs is split into two sibling skills, never nested.

```
.workspace-rules/skills/<name>/SKILL.md
```

`<name>` is a kebab-case verb phrase that names the job: `manage-objects`, `scaffold-fragment`,
`deploy-and-verify`. The directory name, the `name` frontmatter value, and the router-table entry
all use this same string.

## Frontmatter

`description` and `name` are required, in that order (alphabetical), wrapped in padded `---` fences
(a blank line after the opening fence and before the closing fence).

Claude-only **metadata** keys that the other four agents simply ignore may be added — `allowed-tools`
and `argument-hint`. Never add a key that another agent *acts on* or that drives Claude-specific
behavior: `globs` and `alwaysApply` are Cursor/Windsurf directives, and `disable-model-invocation`
changes Claude's routing — all three break agent-agnostic parity and are forbidden.

```
---

description: <what it does>. Use when the user asks to <trigger>, <trigger>, or <trigger>. Maps to <Liferay learning path or doc>.
name: <name>

---
```

The `description` follows a fixed formula: one clause stating what the skill does, a
`Use when the user asks to …` clause listing concrete trigger phrases (favor the words a user would
actually say), and an optional `Maps to …` tail tying the skill to a Liferay learning path. Enrich
the triggers with the concrete nouns a user would say (`hero`, `card`, `picklist`, `objectAction`)
— but only nouns grounded in Liferay documentation or already in the corpus; never invent
colloquial synonyms. The trigger list and the `Maps to` tail come from the checkpoint; any left
unanswered are written `TODO`.

## Prose Style

Write for scanning, not reading. Default to lists and tables; use prose only for a *why* or a
caveat a list cannot carry.

- Prefer a table for any enumerable set (types, fields, flags, errors).
- Prefer a bulleted list over a paragraph of sequential points.
- Short, imperative sentences. Cut hedging and throat-clearing.
- Keep the intro to a single sentence of intent — do not restate context that already lives in
  `liferay-rules.md` or a card.

## Body Skeleton

Sections appear in this fixed order. Omit one only when it does not apply (e.g. no
`## Prerequisites` when the component sits behind no flags).

```markdown
# <Title Case Of Name>

<One sentence of intent and scope.>

## When to Invoke

- "<trigger phrase>", "<trigger phrase>"
- Called by `<other-skill>` during <phase>   (when another skill orchestrates this one)

## Prerequisites

<What must be true before step 1: feature flags, IDs, values the workflow needs. Probe flags via
the feature-flags skill; never enable one without explicit user confirmation. Mark each flag
inferred — verify.>

| Flag | Default | Required For |
| --- | --- | --- |
| `LPD-XXXXX` | off | <capability> |

## Workflow

### <Imperative Title Case Step>

<Lists and short prose, then a runnable curl block per API call. State each anti-hallucination
guard inline and in bold at the step it protects.>

## Patterns and Gotchas

- <Guard mined from validation or exception code. Mark inferred — verify.>

## Success Signal

<The observable signal that proves the run worked — the thing to check, not "it looked fine."
Promote the verification the workflow already implies. If none exists, write
`TODO / inferred — verify` for the eval spike to fill against a running bundle; do not invent one.>
```

Conventions that hold across every skill:

- **Headings** are Title Case and never numbered.
- **Trailing gotcha section** is named `## Patterns and Gotchas` (use `## Common Errors and Fixes`
  only for a pure symptom → fix list).
- **Feature-flag prerequisites** live in the `## Prerequisites` table, not scattered in the
  description or mid-step.
- **Troubleshooting**, when a skill has known failure modes, is a symptom → cause → fix table
  (model: the error-code table in `rules/headless-apis.md`).
- **Cross-skill references** use a consistent form: `Called by <skill>` in `## When to Invoke`, and
  `Call <skill>` at the step that hands off.
- **Version-gating** — note DXP-version applicability inline wherever behavior differs by release.
- **Voice** is imperative and second person throughout (`Create the definition`, not "the assistant
  should create").
- **Length discipline** — if a skill outgrows this skeleton (extra ad-hoc `##` sections, large
  inlined lookup tables), that is the signal to tighten prose or move bulk lookup data to a card,
  not to add sections.

## Curl House Style

Every API call is a runnable `curl` block: one flag per line, flags in alphabetical order,
`--silent`, the base `http://localhost:${PORT}`, and Basic auth `test@liferay.com:test`. Parse
responses with `jq`. Brace every shell variable as `${VAR}`.

```bash
curl \
	--data '{"<field>": "<value>"}' \
	--header "Content-Type: application/json" \
	--request POST \
	--silent \
	--url "http://localhost:${PORT}/o/<base-uri>/v1.0/<resource>" \
	--user "test@liferay.com:test"
```

## Reference Card Format

A card is a single `.md` file of lookup data — tables of endpoints, scope strings, flags, or enums
— under `rules/`. Keep it factual: **procedure belongs in a skill, never a card.** Name it
kebab-case after the data it holds (`headless-apis.md`, `oauth-scopes.md`). End each card with a
consistent `## References` footer linking the related cards and skills.

## Card Factoring Rule

Apply this identically on every run so teams factor consistently:

- Endpoints and base URIs → always extend `headless-apis`.

- OAuth scope strings → always extend `oauth-scopes`.

- Feature flags and defaults → always extend `feature-flags-catalog`; other cards cite it rather
  than repeating flag tables.

- Everything else (valid-value enums, DTO field tables, component-specific lookup data) → stays
  inline in the skill, unless it crosses a size or reuse threshold, in which case it becomes one
  new card named after the component.

- Never create a second card that overlaps an existing one.

## Symlink Wiring

Each base file under `.workspace-rules` is symlinked into all five agent directories: `.claude`,
`.cursor`, `.gemini`, `.github`, `.windsurf`. The skill *directory* is a real directory in each
agent tree; only `SKILL.md` inside it is the symlink.

For a skill named `<name>`, from the workspace root:

```bash
for agent in .claude .cursor .gemini .github .windsurf
do
	mkdir -p "${agent}/skills/<name>"
	ln -s "../../../.workspace-rules/skills/<name>/SKILL.md" "${agent}/skills/<name>/SKILL.md"
done
```

For a card named `<card>.md`:

```bash
for agent in .claude .cursor .gemini .github .windsurf
do
	ln -s "../../.workspace-rules/rules/<card>.md" "${agent}/rules/<card>.md"
done
```

Confirm each link resolves with `readlink -e`. Do not create or touch the per-agent entry-point
symlinks (`CLAUDE.md`, `GEMINI.md`, `copilot-instructions.md`, `rules/liferay.mdc`,
`rules/liferay.md`) — those already exist and are not per-skill.

## Router Registration

A skill or card is invisible until it is registered in `liferay-rules.md`:

- **Router table** — the entry point routes intent to skill through a table. Add one row mapping the
  user intent to the skill; keep every skill represented exactly once, and keep the table consistent
  with the `build-site` orchestrator's phase list.

- **Reference Cards** — add a bullet `` - `rules/<card>.md` — <one-line summary> `` for each new
  card.

## Updating on a Re-Run

When a target already exists, update in place rather than clobbering:

- **Skill** — refresh the derived sections (endpoints, schemas, flag table); preserve
  human-edited sections (confirmed triggers, `Maps to`, hand-written gotchas, success signal).

- **Card** — dedup rows; add only rows not already present.

- **Symlinks and router entries** — skip any that already exist.

## What Not to Do

- Do not add `globs`, `alwaysApply`, or `disable-model-invocation` to a skill's frontmatter.

- Do not inline bulk lookup data that belongs in a card, or duplicate a fact across cards; but do
  keep every gotcha and load-bearing fact inline where the step needs it.

- Do not put procedure (curl blocks, step sequences) in a card.

- Do not number headings, and do not restate `.claude/rules/markdown-style.md` inside the skill.

- Do not invent a success signal, a feature-flag default, or a trigger noun you cannot ground in
  source or Liferay docs — mark it `inferred — verify` or `TODO` instead.

- Do not commit or push; leave the changes for the author to review and commit.
