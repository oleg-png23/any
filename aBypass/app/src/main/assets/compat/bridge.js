/* Privileged isolated world: no page message can grant adapter permission. */
(function () {
  "use strict";
  if (window.top !== window ||
      !["https://flow.google.com", "https://labs.google"].includes(location.origin)) return;
  const CONFIG_EVENT = "abypass:config-changed";
  const DIAGNOSTIC_EVENT = "abypass:diagnostic";
  let nativePort = null;
  let config = { known: false, enabled: false, revision: 0 };
  const allowedEvents = new Set(["ready", "patched", "schema-mismatch", "region-page", "config-enabled", "config-disabled"]);
  const allowedDetails = new Set(["country-field-type", "json-format", "batch-fields", "batch-framing", "size-limit", "response-read", "batch-header", "batch-length", "batch-frame-json", "batch-frame-limit", "batch-ambiguous"]);

  function notifyNative(event, count, detail) {
    if (!nativePort || !allowedEvents.has(event)) return;
    try {
      const message = { type: "diagnostic", event };
      if (Number.isSafeInteger(count) && count > 0) message.count = Math.min(count, 1000000);
      if (allowedDetails.has(detail)) message.detail = detail;
      nativePort.postMessage(message);
    } catch (_) { /* Diagnostics must never break browsing. */ }
  }

  function updateConfig(enabled, known) {
    // Native can repeat the same preference on SPA URL changes and settings
    // refreshes. Those notifications must not invalidate active requests.
    if (config.known === known && config.enabled === enabled) return;
    config = { known, enabled, revision: config.revision + 1 };
    if (known) notifyNative(enabled ? "config-enabled" : "config-disabled");
    window.dispatchEvent(new Event(CONFIG_EVENT));
  }

  try {
    const page = window.wrappedJSObject;
    if (!page || typeof exportFunction !== "function") return;
    // MAIN can read a primitive snapshot, but cannot mutate the closed-over
    // config. The function cannot be replaced and exposes no native API/port.
    Object.defineProperty(page, "__abypassReadConfig", {
      value: exportFunction(() => JSON.stringify(config), page),
      configurable: false,
      writable: false,
      enumerable: false
    });
    nativePort = browser.runtime.connectNative("abypass");
    nativePort.onMessage.addListener(message => {
      if (message && message.type === "config" && typeof message.enabled === "boolean") {
        updateConfig(message.enabled, true);
      }
    });
    nativePort.onDisconnect.addListener(() => {
      nativePort = null;
      updateConfig(false, true);
    });
    window.addEventListener(DIAGNOSTIC_EVENT, event => {
      if (event.target !== window || typeof event.detail !== "string" || event.detail.length > 160) return;
      try {
        const detail = JSON.parse(event.detail);
        notifyNative(detail.event, detail.count, detail.detail);
      } catch (_) { /* Page-originated diagnostics are untrusted. */ }
    });
    notifyNative("ready");
  } catch (_) {
    updateConfig(false, true);
    notifyNative("schema-mismatch", 1);
  }
})();

