# Regional compatibility adapter

This is an original implementation based on static observations of the
user-supplied FlowBridge 0.3.0 APK. It is not copied from its adapter. Runtime
compatibility with Google's changing private schemas remains experimental.

## Native integration

- Built-in extension URI: `resource://android/assets/compat/`
- Extension ID: `abypass-compat@app.abypass`
- Native application name: `abypass`
- Install with `ensureBuiltIn`, then register a **session-specific** message
  delegate before `loadUri` (content-script messages are not background messages).
- On connection, send `{ "type": "config", "enabled": true/false }` for the
  session's current service. Send the same shape whenever its preference changes.
  Config is false until a native reply, and false on port disconnect. Reload the
  service when changing its preference so initial XHR checks run consistently.
- Native receives `{ "type": "diagnostic", "event": "ready" | "patched" |
  "schema-mismatch" | "region-page", "count"?: positive integer }`.
  Diagnostics contain no URLs, cookies or request/response bodies. They describe
  local activity, not successful authentication or server access, and page-side
  diagnostic events must be treated as untrusted observations.

`bridge.js` runs in ISOLATED world and owns the native port. Firefox's
`exportFunction` publishes a non-writable, non-configurable primitive config
reader for MAIN-world wrappers. No DOM message or event can change native config.
The native API and port are never exported. `runtime.js` and `core.js` run at
document_start in MAIN, which is necessary to wrap the page's fetch/XHR. MAIN
world is shared with the website; the site can observe/interfere with browsing
wrappers. The bridge is not a mechanism for granting native privileges to a page.

## Exact scope

Only top-level HTTPS documents on `flow.google.com` and `labs.google`, and only
same-origin calls to these path families, are considered:

- `/_/<service>/data/batchexecute`
- `/(fx/)?api/trpc/general.<method>` (including explicit general-method batches)
- `/(fx/)?(api/)?fetchToolAvailability` or `checkAppAvailability`

These paths are conservative schema candidates inferred from the APK's endpoint
names. A name in a query, other host/subdomain, auth/logout endpoint, redirect to
another endpoint, iframe, or AI Studio is excluded.

JSON transforms only existing Boolean `isCountrySupported` /
`is_country_supported` flags. `availabilityState`, age, plan, billing, credits,
quotas and all other fields are untouched. An unknown type for a geographic flag
rejects the whole response. Batchexecute supports only existing 31+-field
`cPZSdc` arrays with country field index 30 holding Boolean or 0/1. It preserves
age index 31 and never pads arrays. Other observed RPCs return mixed eligibility;
they are intentionally passed through because a general availability override
could also override age, subscription or payment checks.

Parsing is bounded to 1 MiB, with recursion/node limits. Length-delimited batches
first try consistent UTF-8 byte lengths, then UTF-16
code-unit counts including the leading LF. A mode is accepted only after the
whole response parses consistently; modes cannot be mixed between frames. The
original length convention is preserved on rewrite, and any malformed frame or
target payload rejects the entire response. Unmodified and rejected
responses preserve the original bytes. Fetch reads only a clone and returns the
original response on failure, disable, or redirect; request bodies and errors
are untouched. XHR checks its final `responseURL`. Requests sent before the first
native config can adopt only an initial enabled answer at revision 1. A first
disabled answer or any disable/re-enable cycle invalidates older requests.
Fetch waits at most 800 ms for initial config, then sends normally; a later
initial enabled answer may still authorize its response before transformation.
Manual logout is never intercepted.

Flow's `/unsupported-country` can return to `/` at most once in five minutes per
tab, only while enabled. If sessionStorage is unavailable, no redirect is tried.

## Validation

Run `node --test tests/compat.test.cjs` from the project root. Tests exercise
scope, malformed/unknown pass-through, UTF-8 framing, preservation of unrelated
eligibility, browser response behavior and native disable races. Device tests
are still required for GeckoView's world bridge, Google sign-in, upload/download,
and an actual permitted server operation.

Official integration references:

- https://mozilla.github.io/geckoview/consumer/docs/web-extensions
- https://developer.mozilla.org/en-US/docs/Mozilla/Add-ons/WebExtensions/manifest.json/content_scripts
