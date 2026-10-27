# californium Flow adapter

The adapter is opt-in and limited to Flow documents on `https://flow.google.com/*` and the exact legacy Flow paths on `https://labs.google/flow` and `https://labs.google/fx/tools/flow`, including their child paths. It does not activate on the Labs home page or other tools.

The extension ID `abypass-compat@app.abypass`, native application name `abypass` and private bridge event names are internal compatibility identifiers. They are deliberately retained so existing browser profiles can upgrade the built-in extension. The visible name is californium Flow compatibility and the extension version is 1.0.4.

The isolated world owns the native channel. Page events cannot enable the adapter. Native configuration is deduplicated and revisioned; in-flight work is invalidated when settings change. HTTPS URL matching rejects lookalike hosts, credentials, insecure schemes and unknown paths. The MAIN-world wrappers preserve fetch/XHR behavior and only transform known response formats transactionally.

Flow country field 30 and the known availability status positions are the supported response fields. RPC framing preserves Unicode length conventions, separators and complete-frame boundaries. Unrecognized schemas, excessive sizes or ambiguous framing are passed through. Age, payments, credits, subscription, quotas and manual logout are preserved. Diagnostic values come from fixed allowlists and never export arbitrary page content.

Run `node --test tests/compat.test.cjs` from the project root. Android emulator tests separately check native extension installation and browser lifetime. Live account access and generation require a device check in Flow.
