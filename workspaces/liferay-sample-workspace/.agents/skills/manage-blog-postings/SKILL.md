---

description: Create, update, and delete Liferay blog entries and their cover images via the Headless Delivery API, and seed reproducible blog content from a site initializer. Use when the user asks to create a blog entry, write a blog post, add a cover image to a post, or publish blog content to a site.
name: manage-blog-postings

---

# Manage Blog Postings

Author blog entries through `headless-delivery`. Blog content is ongoing editorial material rather than site structure, so the **live API is the primary path** here — unlike `manage-pages`, where the initializer tree is the source of truth. Seed the initializer only for posts that must come back with a fresh site.

Liferay names this entity three ways: the model is `BlogsEntry`, the UI says *Blogs*, and the entire REST surface says **`blog-postings` / `BlogPosting`**. This skill uses the REST vocabulary because every command below is a REST call.

## When to Invoke

- "Create a blog entry", "write a blog post", "publish a post to the site"
- "Add a cover image to that post", "upload a blog image"
- "Seed the site with blog content"
- Called by `build-site` during the content phase

## Prerequisites

- A running bundle, and a site to post into.
- **No feature flag.** The blog endpoints are on by default — verified on `dxp-2026.q3.4`. If a call 404s, suspect the site ID rather than a flag.
- OAuth scope for a CET caller: `Liferay.Headless.Delivery.everything` (`rules/oauth-scopes.md`).

### This Module Keys Sites by Numeric ID

`headless-delivery` addresses sites by **numeric `siteId`**, not by external reference code. Passing an ERC where `{siteId}` is expected returns 404. This is the opposite of `headless-admin-site` — see the path identifier note in `rules/headless-apis.md`.

```bash
curl \
	--silent \
	--url "http://localhost:${PORT}/o/headless-admin-site/v1.0/sites" \
	--user "test@liferay.com:test" \
	| jq --raw-output '.items[] | "\(.id)\t\(.name)\t\(.friendlyUrlPath)"'
```

Save the numeric `id` as `<site-id>` for every call below.

## Workflow

### Create a Blog Posting

Only `articleBody` and `headline` are required. Everything else is optional and several fields autogenerate when omitted:

```bash
curl \
	--data '{
		"alternativeHeadline": "<subtitle>",
		"articleBody": "<p><html body></p>",
		"description": "<abstract shown in listings>",
		"externalReferenceCode": "<STABLE_ERC>",
		"friendlyUrlPath": "<url-slug>",
		"headline": "<Title>",
		"keywords": ["<tag>"],
		"viewableBy": "Anyone"
	}' \
	--header "Content-Type: application/json" \
	--request POST \
	--silent \
	--url "http://localhost:${PORT}/o/headless-delivery/v1.0/sites/<site-id>/blog-postings" \
	--user "test@liferay.com:test"
```

What the server fills in when a field is omitted — all verified on 2026.Q3.4:

| Field | Omitted behavior |
| --- | --- |
| `friendlyUrlPath` | Slugified from `headline` (`Plain Probe` → `plain-probe`) |
| `description` | Derived from `articleBody` with tags stripped, but not whitespace — a body of `<p>ok</p><script>…</script><img …>` yielded `"ok\n\n\n"`. Set it explicitly on anything that appears in a listing |
| `encodingFormat` | `text/html`, whether or not the body contains tags |
| `datePublished` | Now, **truncated to the minute** — several posts created in the same minute share a timestamp and sort arbitrarily. Set it explicitly when order matters |
| `externalReferenceCode` | Generated UUID. Set it explicitly for anything you intend to update or seed later |

`viewableBy` is write only (`Anyone`, `Members`, `Owner`) — it never appears in a response, so the value you set cannot be read back from the entry. It also does **not** make the entry readable over REST by an anonymous visitor; see "Guest Cannot Read Blog Postings Over REST" below.

### Attach a Cover Image

Two calls. Upload the file first, then reference the returned `imageId` on the posting.

```bash
curl \
	--form 'blogPostingImage={"title":"<Image Title>"};type=application/json' \
	--form 'file=@<path/to/image.png>;type=image/png' \
	--request POST \
	--silent \
	--url "http://localhost:${PORT}/o/headless-delivery/v1.0/sites/<site-id>/blog-posting-images" \
	--user "test@liferay.com:test"
```

Both parts are required, and the `blogPostingImage` part must carry `type=application/json` — it is a JSON document inside a multipart body, not a form field. Take the numeric `id` from the response.

Then set it on the posting, at create time or by PATCH. `imageId` is an integer, not a string — substitute the uploaded ID for the `36181` below:

```json
{
	"image": {
		"caption": "<caption>",
		"imageId": 36181
	}
}
```

**The `imageId` that comes back is not the one you sent.** Liferay copies the uploaded file into a per entry location and assigns a fresh ID and `contentUrl` — posting `imageId: 36181` returned `imageId: 36196` under a different folder, verified on 2026.Q3.4. Do not assert that the response echoes the ID you posted, and read `image.contentUrl` from the response rather than constructing it.

An `imageId` that does not resolve fails the **whole posting** with a bare `404 {"status": "NOT_FOUND"}` and no detail field. That reads exactly like a wrong endpoint or a missing site, so check the image ID before re-examining the URL.

### Update With PATCH, Not PUT

`PUT` replaces the entry. Because only `articleBody` and `headline` are required, a `PUT` carrying just those two **succeeds with 200 and silently discards everything else** — verified on 2026.Q3.4, where it left `image: null`, `keywords: []`, and overwrote the hand written `description` with an excerpt of the body. The ERC and `friendlyUrlPath` survive, which makes the entry look intact in a listing.

Use `PATCH` for every edit unless you are deliberately resetting the entry:

```bash
curl \
	--data '{"headline": "<New Title>"}' \
	--header "Content-Type: application/json" \
	--request PATCH \
	--silent \
	--url "http://localhost:${PORT}/o/headless-delivery/v1.0/blog-postings/<posting-id>" \
	--user "test@liferay.com:test"
```

`PATCH` merges — `articleBody`, `description`, `image`, and `keywords` all survive an edit that does not mention them.

Renaming the headline does **not** move `friendlyUrlPath`. That keeps published URLs stable, so change the slug explicitly when a rename is meant to be visible.

### Delete

```bash
curl \
	--request DELETE \
	--silent \
	--url "http://localhost:${PORT}/o/headless-delivery/v1.0/blog-postings/<posting-id>" \
	--user "test@liferay.com:test" \
	--write-out '%{http_code}\n'
```

Expect `204`.

Deleting a posting removes the **copy** it holds but leaves the **original upload** behind. Verified on 2026.Q3.4: after deleting a posting built from upload `36213` (copied onto the entry as `36227`), a `GET` on `36227` returned `404` while `36213` still returned `200`. So every cover image upload outlives the post it was made for and stays in the site's blog image library. Clean it up in the same breath:

```bash
curl \
	--request DELETE \
	--silent \
	--url "http://localhost:${PORT}/o/headless-delivery/v1.0/blog-posting-images/<image-id>" \
	--user "test@liferay.com:test" \
	--write-out '%{http_code}\n'
```

`<image-id>` is the ID returned by the **upload**, not the `image.imageId` on the entry — that one is already gone with the posting.

### Seed Blog Postings in the Site Initializer

For blog content that must come back when the site is rebuilt, add **`site-initializer/blog-postings.json`** — a single file holding a flat JSON array of raw `BlogPosting` DTOs. See `rules/site-initializer-format.md` for where it sits in the tree and how it is applied.

```json
[
	{
		"articleBody": "<p><html body></p>",
		"description": "<abstract>",
		"externalReferenceCode": "<STABLE_ERC>",
		"friendlyUrlPath": "<url-slug>",
		"headline": "<Title>"
	}
]
```

Three properties of this handler, all read from `BundleSiteInitializer._addOrUpdateBlogPostings`:

- **It is one file, not a directory.** Unlike `object-definitions/` or `layouts/`, there is no per entry file — a `blog-postings/` directory is read by nothing and ignored in silence.
- **`externalReferenceCode` is the upsert key.** The handler calls `putSiteBlogPostingByExternalReferenceCode`, so entries are matched by ERC and rerunning is idempotent. An entry without an ERC has nothing to match on — always set one here.
- **It runs after documents and keywords**, and before display page templates and asset lists. So a seeded posting may reference an image from `documents/` and tags from `keywords`, and a display page template may bind to it.

The file goes through token substitution like every other initializer file, and each posting registers `[$BLOG_POSTING_ID:<ERC>$]` for later handlers to reference.

Because the initializer only runs at site creation, changing this file means reprovisioning — delete the site, redeploy the CET. `rules/site-initializer-format.md` holds the command and the `reprovision.sh` wrapper.

## Patterns and Gotchas

### Guest Cannot Read Blog Postings Over REST

An unauthenticated `GET` on `/o/headless-delivery/v1.0/sites/<site-id>/blog-postings` returns **403** with an XML body — `<Forbidden xmlns=""></Forbidden>`, not a JSON problem detail. Setting `viewableBy: "Anyone"` on the entry does not lift it, because the refusal happens at the **Service Access Policy** gate, before entry permissions are consulted at all.

The XML body is the tell: a JSON `403` is a permissions answer, an XML `<Forbidden>` is the SAP answer, and only the second one means the grant you are staring at was never the problem. `rules/guest-access.md` holds the policy setup and the signature format.

The practical consequence is the same one that card states for objects: **never build a public listing from a browser `fetch` of this endpoint.** It returns nothing for the visitor while looking correct to you. Render blog content server side — a Blogs widget, or a Collection bound to `com.liferay.blogs.model.BlogsEntry`.

### A Duplicate ERC Returns 400, Not 409

Reposting an ERC that already exists returns `400 BAD_REQUEST` with `"This external reference code is already in use."` — verified on 2026.Q3.4. The general error table in `rules/headless-apis.md` reserves `409` for duplicates, so do not branch on `409` to detect an existing post. Check for the entry first instead:

```bash
curl \
	--silent \
	--url "http://localhost:${PORT}/o/headless-delivery/v1.0/sites/<site-id>/blog-postings/by-external-reference-code/<STABLE_ERC>" \
	--user "test@liferay.com:test" \
	--write-out '\n%{http_code}\n'
```

`200` means it exists — `PATCH` it. `404` means it does not — `POST` it.

### The `articleBody` Is Sanitized on Write

The stored body is **not** the string you posted. Liferay strips scripting before persisting, and the response already reflects it — posting

```html
<p>ok</p><script>alert(1)</script><img src=x onerror=alert(2)>
```

came back as `<p>ok</p>\n\n\n<img src="x">` on 2026.Q3.4: the `<script>` element is gone, the `onerror` handler is gone, and the bare attribute is normalized to quoted form.

Two consequences:

- **Do not hand escape HTML you intend to render.** Escaping first means the entities are what gets stored, and the post renders as visible angle brackets.
- **Do not treat this as a sanitizer you control.** It is the platform's filter, its exact allowlist is not documented here, and the round trip above is one sample rather than a security boundary. Anything genuinely untrusted should still be validated before it reaches this endpoint.

There is no Markdown conversion. Plain text is stored verbatim and renders as one unformatted run, with `encodingFormat` reporting `text/html` either way — send real HTML.

## Success Signal

Read the entry back by its ERC and assert on its content, not on the create call's status:

```bash
curl \
	--silent \
	--url "http://localhost:${PORT}/o/headless-delivery/v1.0/sites/<site-id>/blog-postings/by-external-reference-code/<STABLE_ERC>" \
	--user "test@liferay.com:test" \
	| jq '{headline, friendlyUrlPath, datePublished, description, image: .image.contentUrl, keywords}'
```

Check that `description` is the abstract you wrote rather than a slice of the body, and that `image.contentUrl` is present whenever a cover image was attached — both are fields that go wrong while the call reports success.

**A `200` on the post's public URL proves nothing.** The entry renders at `/web/<site>/<page>/-/blogs/<friendly-url-path>`, and that path only resolves when the named page actually hosts a Blogs widget. Without one, Liferay serves the **page itself** with a `200` and no error — verified on 2026.Q3.4, where a probe URL returned the untouched Home page, 88 KB of it, with none of the posting's text. Assert on the content:

```bash
curl --silent --url "http://localhost:${PORT}/web/<site>/<page>/-/blogs/<friendly-url-path>" \
	| grep --count "<a distinctive phrase from the headline>"
```

Do this **without** `--user`, which is exactly a Guest request — a post that reads correctly while signed in and returns the bare page signed out is the SAP and permission gap above, not a routing mistake.

## See Also

- `rules/headless-apis.md` — `headless-delivery` base URI and the numeric `siteId` convention.
- `rules/guest-access.md` — the Service Access Policy gate behind the `403`, and what an anonymous visitor may read.
- `rules/site-initializer-format.md` — where `blog-postings.json` sits in the tree, and the reprovision cycle.
- `skills/manage-pages/SKILL.md` — the page that hosts the Blogs widget, and display page templates for blog entries.
- `skills/manage-roles-permissions/SKILL.md` — granting roles on blog content.