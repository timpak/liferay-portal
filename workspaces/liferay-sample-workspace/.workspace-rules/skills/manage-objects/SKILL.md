---

description: Create, update, and publish Liferay Object definitions — fields, relationships, picklists, and validations. Use when the user asks to create an object, add a field, define a picklist, add a relationship, or set up an object validation.
name: manage-objects

---

# Manage Objects

Create, publish, and query Liferay Object definitions and entries via the Headless Admin Object API.

## When to Invoke

- "Create an object", "define a data model", "make a custom entity"
- "Add a field", "add a relationship", "add a picklist", "set up a validation"
- "Publish the object", "query object entries"
- Called by `build-site` during the data model phase

## Prerequisites

Probe these flags via `feature-flags` before the calls that need them; record the result for the session. Do not enable a flag without explicit user confirmation. Flag defaults are `inferred — verify`.

| Flag | Default | Required For |
| --- | --- | --- |
| `LPD-17564` | off | Object collaborators API (per entry permissions) |
| `LPD-52006` | off | Object entry folders (requires `LPD-17564`) |

Object definitions, fields, relationships, and validations need no flag. On a site built from a site initializer, object definitions and data apply **live** via these APIs (and batch import) with no reprovision, and — being company scoped — survive a page reprovision (see `rules/site-initializer-format.md`).

## Workflow

### Collect Object Definition Inputs

- `name` — singular CamelCase (e.g. `Book`)
- `label` — human readable singular (e.g. `Book`)
- `pluralLabel` — REST path safe plural (e.g. `books`)
- `scope` — `company` (default, global) or `site`
- `storageType` — Liferay's DB (default) or an external source such as `salesforce` or `ext-Service` (see `integrate-external-data`)
- Fields — each with `businessType`, `name`, `label`, `required`

### Create the Object Definition

```bash
curl \
	--data '{
		"label": {"en_US": "<Label>"},
		"name": "<Name>",
		"pluralLabel": {"en_US": "<PluralLabel>"},
		"scope": "company"
	}' \
	--header "Content-Type: application/json" \
	--request POST \
	--silent \
	--url "http://localhost:${PORT}/o/object-admin/v1.0/object-definitions" \
	--user "test@liferay.com:test"
```

**Do not send `"storageType": "default"` on create** — it returns `400 ObjectDefinitionStorageTypeException`. Omit `storageType` and Liferay assigns default DB storage (for external storage, see `integrate-external-data`). Save the returned `id` as `<definition-id>`.

### Add Fields

```bash
curl \
	--data '{
		"businessType": "<businessType>",
		"label": {"en_US": "<FieldLabel>"},
		"name": "<fieldName>",
		"required": false
	}' \
	--header "Content-Type: application/json" \
	--request POST \
	--silent \
	--url "http://localhost:${PORT}/o/object-admin/v1.0/object-definitions/<definition-id>/object-fields" \
	--user "test@liferay.com:test"
```

**`required` is mandatory on the standalone `POST .../object-fields` call** — omitting it returns `500` with a `getRequired()` NullPointerException. Always send `true` or `false` explicitly.

`businessType` values: `Text`, `LongText`, `Integer`, `Decimal`, `Boolean`, `Date`, `DateTime`, `Attachment`, `Relationship`, `Picklist`.

### Add Picklists (When Needed)

Create the picklist, then reference its `id` from a `Picklist` field (`"listTypeDefinitionId": <list-type-id>`):

```bash
# Create list type definition

curl \
	--data '{
		"name": "<PicklistName>",
		"listTypeEntries": [
			{"key": "value1", "name": "Value One", "type": ""},
			{"key": "value2", "name": "Value Two", "type": ""}
		]
	}' \
	--header "Content-Type: application/json" \
	--request POST \
	--silent \
	--url "http://localhost:${PORT}/o/headless-admin-list-type/v1.0/list-type-definitions" \
	--user "test@liferay.com:test"
```

### Add Relationships

Defined on the parent object; `objectDefinitionId2` is the child definition's ID. `type` values: `oneToMany`, `manyToMany`, `oneToOne`.

```bash
curl \
	--data '{
		"label": {"en_US": "<RelationshipLabel>"},
		"name": "<relationshipName>",
		"objectDefinitionId2": <child-definition-id>,
		"type": "oneToMany"
	}' \
	--header "Content-Type: application/json" \
	--request POST \
	--silent \
	--url "http://localhost:${PORT}/o/object-admin/v1.0/object-definitions/<parent-definition-id>/object-relationships" \
	--user "test@liferay.com:test"
```

### Add Validations

Expression and script validations are both available — no flag required. Consult learn.liferay.com for expression syntax (search `object validations expression builder`).

```bash
curl \
	--data '{
		"active": true,
		"engine": "function",
		"errorLabel": {"en_US": "<Error message>"},
		"name": "<validationName>",
		"script": "<expression>"
	}' \
	--header "Content-Type: application/json" \
	--request POST \
	--silent \
	--url "http://localhost:${PORT}/o/object-admin/v1.0/object-definitions/<definition-id>/object-validation-rules" \
	--user "test@liferay.com:test"
```

### Publish the Object Definition

An unpublished object has no REST endpoint and no UI entry. Publish after adding fields and relationships; entries are then available at `/o/c/<pluralLabel>`.

```bash
curl \
	--request POST \
	--silent \
	--url "http://localhost:${PORT}/o/object-admin/v1.0/object-definitions/<definition-id>/publish" \
	--user "test@liferay.com:test"
```

### Create and Query Object Entries

```bash
# Create entry

curl \
	--data '{"<fieldName>": "<value>"}' \
	--header "Content-Type: application/json" \
	--request POST \
	--silent \
	--url "http://localhost:${PORT}/o/c/<pluralLabel>" \
	--user "test@liferay.com:test"

# List entries

curl \
	--silent \
	--url "http://localhost:${PORT}/o/c/<pluralLabel>" \
	--user "test@liferay.com:test"
```

To create a child entry already linked to its parent over a `oneToMany` relationship, **POST the child to its own endpoint** and set the foreign-key field `r_<relationshipName>_c_<childObject>Id` (the parent's numeric entry ID):

```bash
curl \
	--data '{
		"<childField>": "<value>",
		"r_<relationshipName>_c_<childObject>Id": <parent-entry-id>
	}' \
	--header "Content-Type: application/json" \
	--request POST \
	--silent \
	--url "http://localhost:${PORT}/o/c/<childPlural>" \
	--user "test@liferay.com:test"
```

### Initialize in Bulk via a Batch Client Extension

Use a Batch CET to initialize definitions, folders, and seed data at deploy time. **Do not mix a Batch CET with a Custom Element CET in the same `client-extension.yaml`.** Files under `batch/` are processed alphabetically — use numeric prefixes for dependency order (`01-00` folders → `01-01` definitions → `02-xx` relationships → `03-xx` entries).

```
client-extensions/my-batch-init/
├── client-extension.yaml
├── bnd.bnd
└── batch/
    ├── 01-00-folder-definition.batch-engine-data.json
    ├── 01-01-object-definition.batch-engine-data.json
    ├── 02-00-relationship.batch-engine-data.json
    └── 03-00-entries.batch-engine-data.json
```

```yaml
assemble:
    - from: batch
      into: batch

my-batch-init:
    name: My Batch Initialization
    oAuthApplicationHeadlessServer: my-batch-oauth-server
    type: batch

my-batch-oauth-server:
    .serviceAddress: <host>:<port>
    .serviceScheme: http
    name: My Batch OAuth Server
    scopes:
        - Liferay.Headless.Batch.Engine.everything
        - Liferay.Object.Admin.REST.everything
    type: oAuthApplicationHeadlessServer
```

**Use `oAuthApplicationHeadlessServer` (not `oAuthApplicationUserAgent`)** — the Batch Engine requires server to server OAuth, not a user delegated token.

Folder definition (`01-00-...json`):

```json
{
	"configuration": {
		"className": "com.liferay.object.admin.rest.dto.v1_0.ObjectFolder",
		"parameters": {
			"createStrategy": "UPSERT",
			"updateStrategy": "UPDATE"
		}
	},
	"items": [
		{
			"externalReferenceCode": "MY_FOLDER_ERC",
			"label": {
				"en_US": "My Custom Folder"
			},
			"name": "MyFolder"
		}
	]
}
```

Object definition (`01-01-...json`):

```json
{
	"configuration": {
		"className": "com.liferay.object.admin.rest.dto.v1_0.ObjectDefinition",
		"parameters": {
			"createStrategy": "UPSERT",
			"updateStrategy": "UPDATE"
		}
	},
	"items": [
		{
			"enableCategorization": true,
			"externalReferenceCode": "MY_OBJECT_ERC",
			"label": {
				"en_US": "My Object"
			},
			"name": "MyObject",
			"objectFields": [
				{
					"businessType": "Text",
					"indexed": true,
					"indexedAsKeyword": true,
					"label": {
						"en_US": "My Field"
					},
					"name": "myField",
					"required": false
				}
			],
			"objectFolderExternalReferenceCode": "MY_FOLDER_ERC",
			"scope": "company",
			"status": {
				"code": 0,
				"label": "approved"
			}
		}
	]
}
```

`"status": {"code": 0}` is required for the object to be immediately active. Without it the definition deploys in draft state and returns no entries.

Data entries (`03-00-...json`):

```json
{
	"configuration": {
		"className": "com.liferay.object.rest.dto.v1_0.ObjectEntry",
		"parameters": {
			"createStrategy": "UPSERT",
			"taskItemDelegateName": "C_MyObject"
		}
	},
	"items": [
		{
			"assetCategoryIds": [
				12345
			],
			"externalReferenceCode": "ENTRY-001",
			"values": {
				"myField": "value",
				"timestamp": "2024-03-27T10:00:00Z"
			}
		}
	]
}
```

- `taskItemDelegateName` must match the Object's **name** with a `C_` prefix (e.g., `C_MyObject` for an object named `MyObject`).
- `assetCategoryIds` belongs **outside** the `values` block.
- Dates must use ISO 8601 with UTC `Z` suffix.

Relationship mapping — **preferred (portable)** uses the relationship's camelCase name as the key; **direct field mapping** (`r_...` syntax) accepts integer IDs only, not ERC:

```json
"relationshipName": {"externalReferenceCode": "TARGET-ERC-001"}
```

```json
"r_accountToMyObject_accountEntryId": 38660
```

## Patterns and Gotchas

- **Field namespace safety** — never use `userId` as a custom field name; it collides with a system column in `ObjectEntryTable`. Use `liferayUserId`.
- **Date/DateTime storage** — every `Date`/`DateTime` field must set `timeStorage` in `objectFieldSettings` (e.g. `"convertToUTC"`).
- **Indexed language** — `indexedLanguageId` is valid only on `String`/`Clob`; never on `Date`/`DateTime` or other non-text fields.
- **Schema discovery before writes** — the OpenAPI spec for `object-admin` and `/o/c/<pluralLabel>` is the source of truth (Liferay's hosted docs lag). GET response shape ≠ POST/PATCH request shape. Fetch via the `get-openapi` MCP tool or `/o/object-admin/v1.0/openapi.yaml`.
- **Field settings not in the schema** — `objectFieldSettings` values like `fileSource`, `acceptedFileExtensions`, `maximumFileSize` resolve as a generic string and are not enumerated in OpenAPI or GraphQL. A wrong value returns `400` with no hint; search the [liferay-portal repo](https://github.com/liferay/liferay-portal) for the constants rather than guessing.
- **Nested create is rejected** — `POST /o/c/<parentPlural>/{parentId}/<relationshipName>` returns `400 UnsupportedOperationException`; use the direct child POST with the FK field (above). `PUT .../{relatedId}` only *attaches* an existing child, it does not create one.
- **OData Date/DateTime filters are broken** — `eq`/`ge`/`le` return `BAD_REQUEST` regardless of format. Fetch all records and filter in client code.
- **Permission grants — verify via a follow-up GET** — object permission APIs may return `200 OK` without persisting. If the GET does not reflect the grant, use Control Panel → Objects → [Object] → Permissions.
- **Batch permissions are not importable** — the Batch Engine ignores a `permissions` block in `*.batch-engine-data.json`; grant permissions after deploy.
- **Batch troubleshooting** — NPE on deploy → missing `.serviceAddress`/`.serviceScheme`; object not created → `className` mismatch with your DXP version's DTO; folder not found → folder ERC/prefix must sort before the objects that reference it.

### OData Relationship Filters Use ERC Strings, Not Numeric IDs

Filtering relationship fields by numeric ID throws `HTTP 400 InvalidFilterException: Incompatible types`. Always filter by the string ERC of the related entry instead:

```text
# Single value

r_<relationshipName>_c_<objectName>ERC eq 'ERC_VALUE'

# Multivalue

r_<relationshipName>_c_<objectName>ERC in ('ERC1','ERC2')
```

Applies to both `eq` and `in`. Numeric ID filters are broken for all relationship fields regardless of syntax.

### Picklist Fields Return `{key, name}` Objects, Not Strings

Picklist values in API responses are objects:

```json
{
	"key": "DRAFT",
	"name": "Draft"
}
```

Always destructure before use: `const key = entry.status?.key || ''`. Rendering `entry.status` directly outputs `[object Object]`.

## Success Signal

Listing definitions filtered to approved returns the definition with `status: "approved"`, and its entries respond at `/o/c/<pluralLabel>`:

```bash
# List all published definitions

curl \
	--silent \
	--url "http://localhost:${PORT}/o/object-admin/v1.0/object-definitions?filter=status%20eq%20%27approved%27" \
	--user "test@liferay.com:test" \
	| jq '[.items[] | {id, name, status}]'
```
