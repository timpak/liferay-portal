# Surface Discovery

How to resolve any component path to the **consumer-facing surface** a workspace skill should
teach. The input can be any module, directory, or file; the job is to find the external contract
an outside developer actually calls, and to generate from that — not from implementation
internals. Extraction is strongest for REST/OpenAPI surfaces; other surfaces are accepted but
typically yield mostly-inferred skeletons.

## Priority Order

Search in this order and generate from the highest surface that exists.

### REST Builder OpenAPI Spec

The strongest signal. A REST Builder feature lives in sibling modules under `modules/apps/<area>`:
`<name>-rest-api`, `<name>-rest-impl`, `<name>-rest-client`, `<name>-rest-test` (see
`.claude/rules/rest-builder.md`). The hand-authored `<name>-rest-impl/rest-openapi.yaml` is the
authoritative surface — it carries paths, operations, request/response schemas, and descriptions.
Read `<name>-rest-impl/rest-config.yaml` for `application.baseURI`; endpoints resolve to
`/o/<baseURI>/v1.0/...`.

### Generated Resource Base Classes

When the OpenAPI response shape is ambiguous, read the generated
`<name>-rest-impl/src/main/java/.../internal/resource/v1_0/Base<Tag>ResourceImpl.java` for exact
method signatures and return types (`Page<DTO>` versus a bare DTO).

### JAX-RS Resources

For headless modules not built with REST Builder, find `@Path`-annotated classes and their
`@GET`/`@POST` methods to reconstruct the endpoints.

### MCP Surface

When the component exposes MCP tools, treat those tool definitions as the surface — the workspace
prefers MCP over raw `curl` where available.

### Client Extension Contract

When the component is consumed as a client extension, read its `client-extension.yaml` for the
CET type and fields (see the `client-extension-types` card).

## Finding Siblings From an Arbitrary Path

The given path may point anywhere — an impl class, an API package, a test. Walk up to the module
root (the directory containing `bnd.bnd` and `build.gradle`), then up to the area directory
(`modules/apps/<area>`), and glob for `*-rest-api` / `*-rest-impl` siblings. If the path is
already inside a `*-rest-impl`, use it directly.

## Supporting Facts

Once the surface is found, gather the facts the skill and cards need:

- **OAuth scopes** — read scope strings declared in the REST impl; reconcile against the
  `oauth-scopes` card.

- **Valid values** — take enums and DTO field types from the OpenAPI `components.schemas`.

- **Feature flags** — search the component's modules for `FeatureFlag` definitions and
  `feature.flag.<KEY>` defaults; record each flag ID and its default, and mark it
  `inferred — verify` — flags are declared in several ways and the default is easy to misread.

## Gotcha Mining Sources

Anti-hallucination guards come from constraint code, not the spec:

- Exception classes thrown by the resource impl and its validators (`throw new *Exception`).

- Required-field and allowed-value checks that reject otherwise well-formed requests.

- Fields that must be omitted on create or that are server-assigned.

Every guard read this way is a hypothesis about runtime behavior — mark it `inferred — verify`.

## When No External Surface Exists

If the path yields only internal OSGi services (exported packages consumed via `@Reference`) or
UI-only APIs, there is no surface an external developer can call. Do not invent endpoints. Record
a prominent warning for the report, draft the skill best-effort from whatever surface exists, and
mark the whole skill `inferred — verify`.
