# Workspace Skill Anatomy

The conventions and skeletons this skill emits. Everything here targets the **sample workspace**
(`.workspace-rules`), which is symlinked into five AI agents — so the output is the
lowest-common-denominator format every agent understands, not the richer Claude-only skill
format. Formatting follows `.claude/rules/markdown-style.md`.

## The Three Layers

The workspace guidance has three layers, and the generated files must fit them:

- **Entry point** — `liferay-rules.md` establishes context and routes to a skill. New skills and
  cards are registered here.

- **Reference cards** — `rules/<card>.md` hold static facts (API paths, scope strings, flag
  defaults) that skills cite. A fact lives in exactly one card.

- **Skills** — `skills/<name>/SKILL.md`, one workflow each. Skills cite cards by relative path
  rather than inlining facts.

## Skill File Layout

A skill is a directory containing exactly one file, `SKILL.md`. There are no per-skill
`references/` or `scripts/` subdirectories — shared data goes into a card instead.

```
.workspace-rules/skills/<name>/SKILL.md
```

`<name>` is a kebab-case verb phrase that names the job: `manage-objects`, `scaffold-fragment`,
`deploy-and-verify`. The directory name, the `name` frontmatter value, and the Skill Index entry
all use this same string.

## Frontmatter

Exactly two keys, `description` then `name`, wrapped in padded `---` fences (a blank line after
the opening fence and before the closing fence). No `allowed-tools`, no `argument-hint`, no
`globs` — those are agent-specific and would pollute the four non-Claude agents.

```
---

description: <what it does>. Use when the user asks to <trigger>, <trigger>, or <trigger>. Maps to <Liferay learning path or doc>.
name: <name>

---
```

The `description` follows a fixed formula: one clause stating what the skill does, a
`Use when the user asks to …` clause listing concrete trigger phrases (this is what routes the
skill, so favor the words a user would actually say), and an optional `Maps to …` tail tying the
skill to a Liferay learning path. The trigger list and the `Maps to` tail are not derivable from
source — they come from the checkpoint, and any left unanswered are written `TODO`.

## Body Skeleton

```markdown
# <Title Case Of Name>

<One or two sentences of intent and scope.>

## When to Invoke

- "<trigger phrase>", "<trigger phrase>"
- Called by `<other-skill>` during <phase>   (when another skill orchestrates this one)

## Prerequisites

<Feature-flag table, when the component sits behind flags. Probe via the feature-flags skill;
never enable a flag without explicit user confirmation. Mark each flag inferred — verify.>

| Flag | Default | Required For |
| --- | --- | --- |
| `LPD-XXXXX` | off | <capability> |

## Workflow

### <Imperative Title Case Step>

<Prose, then a runnable curl block per API call.>

## Patterns and Gotchas

- <Guard mined from validation or exception code. Mark inferred — verify.>
```

Headings are Title Case and never numbered. Omit `## Prerequisites` when the component has no
flags. Trailing sections may also be named `## Common Errors and Fixes` when that reads better.

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

State anti-hallucination guards inline and in bold where a request looks correct but fails — for
example, a field that must be omitted on create because sending it returns `400`.

## Reference Card Format

A card is a single `.md` file of lookup data — tables of endpoints, scope strings, or flags —
under `rules/`. Keep it factual and free of procedure; procedure belongs in a skill. Name it
kebab-case after the data it holds (`headless-apis.md`, `oauth-scopes.md`).

## Card Factoring Rule

Apply this identically on every run so teams factor consistently:

- Endpoints and base URIs → always extend `headless-apis`.

- OAuth scope strings → always extend `oauth-scopes`.

- Feature flags and defaults → always extend `feature-flags-catalog`.

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

## Index Registration

A skill or card is invisible until it is listed in `liferay-rules.md`. Add the entry to the
matching section, keeping the surrounding grouping and wording style:

- **Skill Index** — add a bullet `` - `<name>` — <one-line summary> `` under the confirmed group.

- **Reference Cards** — add a bullet `` - `rules/<card>.md` — <one-line summary> `` for each new
  card.

## Updating on a Re-Run

When a target already exists, update in place rather than clobbering:

- **Skill** — refresh the derived sections (endpoints, schemas, flag table); preserve
  human-edited sections (confirmed triggers, `Maps to`, hand-written gotchas).

- **Card** — dedup rows; add only rows not already present.

- **Symlinks and index entries** — skip any that already exist.

## What Not to Do

- Do not add `allowed-tools`, `argument-hint`, or `globs` to a skill's frontmatter.

- Do not inline facts that belong in a card, or duplicate a fact across cards.

- Do not number headings, and do not restate `.claude/rules/markdown-style.md` inside the skill.

- Do not commit or push; leave the changes for the author to review and commit.
