---

description: Create, update, and delete blog posts on a site through the Headless Delivery REST API, including attaching a cover image. Use when the user asks to create a blog post, publish a blog entry, add a blog posting, update or delete a blog post, or upload a blog cover image.
name: manage-blog-postings

---

# Manage Blog Postings

Create and maintain a site's blog posts through the Headless Delivery API (`/o/headless-delivery/v1.0`). See `rules/headless-apis.md` for the module's paths and `rules/oauth-scopes.md` for the scope a CET needs.

## When to Invoke

- "Create a blog post", "publish a blog entry", "add a blog posting"
- "Update the blog post", "delete that blog post"
- "Upload a cover image for the blog post"
- Called by `build-site` when a site needs seeded blog content

## Blog Posting Fields

The request body is a `BlogPosting`. `headline` and `articleBody` are required; the rest are optional.

| Field | Required | Notes |
| --- | --- | --- |
| `headline` | Yes | The post's main title |
| `articleBody` | Yes | The post's body; accepts HTML |
| `alternativeHeadline` | No | Subtitle |
| `description` | No | Short summary |
| `friendlyUrlPath` | No | Relative URL slug; derived from the headline when omitted |
| `datePublished` | No | ISO-8601 date-time; defaults to now when omitted |
| `keywords` | No | Array of tag strings |
| `taxonomyCategoryIds` | No | Write-only; category IDs to assign |
| `viewableBy` | No | Write-only enum: `Anyone`, `Members`, `Owner` |
| `image.imageId` | No | Cover image; the ID of an already-uploaded document |
| `externalReferenceCode` | No | Stable external key for upsert by ERC |

Fields such as `id`, `siteId`, `creator`, `dateCreated`, `numberOfComments`, and `taxonomyCategoryBriefs` are read-only — they appear in responses and are ignored on write.

## Workflow

### Resolve the Site ID

Headless Delivery keys blog paths by the numeric site (group) ID, not the external reference code. List sites and read the numeric `id`:

```bash
curl \
	--silent \
	--url "http://localhost:${PORT}/o/headless-admin-site/v1.0/sites" \
	--user "test@liferay.com:test" \
	| jq '[.items[] | {id, name, friendlyUrlPath}]'
```

Save the numeric `id` as `<site-id>`. **Passing a site external reference code where a numeric `siteId` is expected returns 404.**

### Attach a Cover Image (Optional)

When the post needs a cover image, upload it first and capture the returned image ID. The request is `multipart/form-data` with the file bytes in `file` and optional JSON metadata in `blogPostingImage`:

```bash
curl \
	--form 'blogPostingImage={"title": "<Caption>"};type=application/json' \
	--form 'file=@<path-to-image>' \
	--request POST \
	--silent \
	--url "http://localhost:${PORT}/o/headless-delivery/v1.0/sites/<site-id>/blog-posting-images" \
	--user "test@liferay.com:test" \
	| jq '{id, contentUrl}'
```

Save the returned `id` as `<image-id>`. **`image.imageId` must reference a document that already exists — an unknown ID fails the create because the resource resolves the file entry by that ID.**

### Create a Blog Post

`POST` the `BlogPosting` to the site's collection. Include `image.imageId` only when a cover image was uploaded:

```bash
curl \
	--data '{
		"alternativeHeadline": "<Subtitle>",
		"articleBody": "<p>The post body, HTML allowed.</p>",
		"friendlyUrlPath": "<url-slug>",
		"headline": "<Post Title>",
		"image": {"imageId": <image-id>},
		"keywords": ["<tag>"],
		"viewableBy": "Anyone"
	}' \
	--header "Content-Type: application/json" \
	--request POST \
	--silent \
	--url "http://localhost:${PORT}/o/headless-delivery/v1.0/sites/<site-id>/blog-postings" \
	--user "test@liferay.com:test" \
	| jq '{id, headline, friendlyUrlPath, datePublished}'
```

Save the returned `id` as `<blog-posting-id>`. **The created post is published immediately (approved and visible); this API exposes no draft state.** *(inferred — verify)*

To schedule or backdate a post, set `datePublished` to the intended ISO-8601 date-time — when omitted it defaults to the current time.

### Update a Blog Post

`PATCH` a partial change by numeric ID (only the supplied fields change):

```bash
curl \
	--data '{"headline": "<Revised Title>"}' \
	--header "Content-Type: application/json" \
	--request PATCH \
	--silent \
	--url "http://localhost:${PORT}/o/headless-delivery/v1.0/blog-postings/<blog-posting-id>" \
	--user "test@liferay.com:test"
```

For idempotent create-or-replace keyed by your own identifier, `PUT` by external reference code instead — it creates the post when the ERC does not yet exist:

```bash
curl \
	--data '{"articleBody": "<p>Body.</p>", "headline": "<Title>"}' \
	--header "Content-Type: application/json" \
	--request PUT \
	--silent \
	--url "http://localhost:${PORT}/o/headless-delivery/v1.0/sites/<site-id>/blog-postings/by-external-reference-code/<erc>" \
	--user "test@liferay.com:test"
```

### Delete a Blog Post

```bash
curl \
	--request DELETE \
	--silent \
	--url "http://localhost:${PORT}/o/headless-delivery/v1.0/blog-postings/<blog-posting-id>" \
	--user "test@liferay.com:test"
```

### Related Operations

Comments, ratings, permissions, and subscriptions hang off the same resource — `/blog-postings/{blogPostingId}/comments`, `/my-rating`, `/permissions`, and `/sites/{siteId}/blog-postings/subscribe`. See `rules/headless-apis.md` for the full path list.

## Patterns and Gotchas

- **`headline` and `articleBody` are required.** Omitting either returns `400` with the missing field named in the problem detail.

- **Site paths use the numeric `siteId`, not the ERC.** Blog collection and image paths are under `/sites/{siteId}/…`; an external reference code there returns 404. The per-post paths (`/blog-postings/{id}`) use the numeric post ID.

- **A cover image must be uploaded before it is referenced.** The create path resolves `image.imageId` to a document and errors when the ID does not exist — upload via the blog-posting-images endpoint (or the documents API) first. *(inferred — verify)*

- **New posts publish immediately.** The create path adds the entry as approved and visible; there is no draft or pending-workflow state through this API. *(inferred — verify)*

- **`viewableBy` and `taxonomyCategoryIds` are write-only.** They shape the create or update but never appear in responses; read assigned categories back from `taxonomyCategoryBriefs`.

## Success Signal

The post is retrievable by ID and appears in the site's blog listing:

```bash
# Fetch the created post

curl \
	--silent \
	--url "http://localhost:${PORT}/o/headless-delivery/v1.0/blog-postings/<blog-posting-id>" \
	--user "test@liferay.com:test" \
	| jq '{id, headline, friendlyUrlPath, datePublished}'

# Confirm it is in the site listing

curl \
	--silent \
	--url "http://localhost:${PORT}/o/headless-delivery/v1.0/sites/<site-id>/blog-postings" \
	--user "test@liferay.com:test" \
	| jq '[.items[] | {id, headline}]'
```

Expect the GET to return `200` with the headline you sent, and the post to appear in the listing.
