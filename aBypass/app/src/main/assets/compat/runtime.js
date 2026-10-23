/* MAIN world: transparent browser wrappers with native-owned enable state. */
(function (root, factory) {
  "use strict";
  if (typeof module === "object" && module.exports) module.exports = factory;
  else {
    const core = root.__abypassCore;
    delete root.__abypassCore;
    if (core) factory(root, core);
  }
})(typeof globalThis !== "undefined" ? globalThis : this, function install(win, core, testHooks) {
  "use strict";
  if (!win.location || !core.allowedOrigin(win.location.origin)) return null;
  const CONFIG_EVENT = "abypass:config-changed";
  const DIAGNOSTIC_EVENT = "abypass:diagnostic";
  const readConfig = testHooks && testHooks.readConfig ? testHooks.readConfig : () => {
    try {
      const reader = win.__abypassReadConfig;
      const value = typeof reader === "function" ? JSON.parse(reader()) : null;
      if (value && value.known === true && typeof value.enabled === "boolean" &&
          Number.isSafeInteger(value.revision)) return value;
    } catch (_) { /* Missing/invalid privileged bridge means disabled. */ }
    return { known: false, enabled: false, revision: -1 };
  };
  const nativeJson = JSON;
  const notify = testHooks && testHooks.notify ? testHooks.notify : (event, count, detail) => {
    try {
      win.dispatchEvent(new win.CustomEvent(DIAGNOSTIC_EVENT, {
        detail: nativeJson.stringify({ event, count, ...(detail ? { detail } : {}) })
      }));
    } catch (_) { /* Browsing does not depend on diagnostics. */ }
  };
  function unchangedRevision(revision) {
    const now = readConfig();
    return now.enabled === true && now.revision === revision;
  }

  function requestRevision(initial) {
    const now = readConfig();
    if (initial.known === true) {
      return initial.enabled === true && now.enabled === true &&
        now.revision === initial.revision ? initial.revision : null;
    }
    // A request sent during native IPC startup may adopt only the first
    // authoritative answer. A first false, disconnect, or disable/re-enable
    // cycle cannot retroactively authorize an older request.
    return now.known === true && now.enabled === true && now.revision === 1 ? 1 : null;
  }

  function firstConfigStillPending(initial) {
    return initial.known !== true && readConfig().known !== true;
  }

  function waitForInitialConfig() {
    if (readConfig().known || typeof win.addEventListener !== "function") return Promise.resolve();
    return new Promise(resolve => {
      let timer;
      const finish = () => {
        win.removeEventListener(CONFIG_EVENT, check);
        win.clearTimeout(timer);
        resolve();
      };
      const check = () => { if (readConfig().known) finish(); };
      win.addEventListener(CONFIG_EVENT, check);
      timer = win.setTimeout(finish, 800);
      check();
    });
  }

  function finalTarget(url, expected) {
    const target = core.classifyRequest(url, win.location.href);
    return target && target.kind === expected.kind;
  }

  async function readLimitedText(response) {
    const clone = response.clone();
    if (!clone.body) return "";
    const reader = clone.body.getReader();
    const chunks = [];
    let total = 0;
    try {
      while (true) {
        const part = await reader.read();
        if (part.done) break;
        total += part.value.byteLength;
        if (total > core.MAX_BYTES) {
          // Do not await a tee cancellation: its promise may wait for the
          // untouched original response to be consumed by the caller.
          Promise.resolve(reader.cancel()).catch(() => {});
          return null;
        }
        chunks.push(part.value);
      }
      const bytes = new Uint8Array(total);
      let offset = 0;
      for (const part of chunks) { bytes.set(part, offset); offset += part.byteLength; }
      return new TextDecoder("utf-8", { fatal: true }).decode(bytes);
    } finally { reader.releaseLock(); }
  }

  function replacementResponse(text, original) {
    const headers = new win.Headers(original.headers);
    for (const name of ["content-length", "content-encoding", "etag", "content-md5", "content-digest", "digest"]) {
      headers.delete(name);
    }
    const response = new win.Response(text, {
      status: original.status,
      statusText: original.statusText,
      headers
    });
    // Preserve fetch metadata that the public Response constructor cannot set,
    // including clones of clones. Native clone still enforces bodyUsed rules.
    function attachMetadata(value) {
      for (const key of ["url", "redirected", "type"]) {
        Object.defineProperty(value, key, { value: original[key], configurable: true });
      }
      const nativeClone = value.clone.bind(value);
      Object.defineProperty(value, "clone", { value: () => attachMetadata(nativeClone()) });
      return value;
    }
    return attachMetadata(response);
  }

  if (typeof win.fetch === "function") {
    const originalFetch = win.fetch;
    win.fetch = async function (...args) {
      const target = core.classifyRequest(args[0], win.location.href);
      if (!target) return originalFetch.apply(this, args);
      await waitForInitialConfig();
      const config = readConfig();
      // Native errors, aborts, request bodies and the caller's init are untouched.
      const response = await originalFetch.apply(this, args);
      let revision = requestRevision(config);
      if ((revision === null && !firstConfigStillPending(config)) ||
          !finalTarget(response.url, target) || !response.ok ||
          [204, 205].includes(response.status) || response.type === "opaque" ||
          response.type === "opaqueredirect") return response;
      const contentType = response.headers.get("content-type") || "";
      const contentLength = Number(response.headers.get("content-length"));
      if (!/^(?:application\/(?:json|x-javascript)|text\/plain)(?:;|$)/i.test(contentType) ||
          (Number.isFinite(contentLength) && contentLength > core.MAX_BYTES)) return response;
      try {
        const body = await readLimitedText(response);
        revision = requestRevision(config);
        if (body === null || revision === null) return response;
        const result = core.transform(body, target.kind);
        if (!unchangedRevision(revision)) return response;
        if (!result.changed) {
          if (result.reason === "schema-mismatch") notify("schema-mismatch", 1, result.detail);
          return response;
        }
        const replacement = replacementResponse(result.body, response);
        if (!unchangedRevision(revision)) return response;
        notify("patched", result.count);
        return replacement;
      } catch (_) {
        // Only a clone was read, so any parse/stream/wrapper failure can safely
        // return the original Response with its body still available.
        notify("schema-mismatch", 1, "response-read");
        return response;
      }
    };
  }

  if (typeof win.XMLHttpRequest === "function") {
    const proto = win.XMLHttpRequest.prototype;
    const originalOpen = proto.open;
    const originalSend = proto.send;
    const textDescriptor = Object.getOwnPropertyDescriptor(proto, "responseText");
    const responseDescriptor = Object.getOwnPropertyDescriptor(proto, "response");
    const requests = new WeakMap();
    if (textDescriptor && responseDescriptor && textDescriptor.get && responseDescriptor.get &&
        textDescriptor.configurable && responseDescriptor.configurable) {
      proto.open = function (...args) {
        const result = originalOpen.apply(this, args);
        requests.set(this, { target: core.classifyRequest(args[1], win.location.href), config: null, cache: null });
        return result;
      };
      proto.send = function (...args) {
        const state = requests.get(this);
        if (state) state.config = readConfig();
        return originalSend.apply(this, args);
      };
      function transformed(xhr, raw, isJson) {
        const state = requests.get(xhr);
        if (!state || !state.target || !state.config ||
            xhr.readyState !== 4 || xhr.status < 200 || xhr.status >= 300 ||
            !finalTarget(xhr.responseURL, state.target)) return raw;
        const revision = requestRevision(state.config);
        if (revision === null) return raw;
        if (state.cache && state.cache.raw === raw) return state.cache.value;
        try {
          const body = isJson ? nativeJson.stringify(raw) : raw;
          const result = core.transform(body, state.target.kind);
          if (!unchangedRevision(revision)) return raw;
          const value = result.changed && isJson ? nativeJson.parse(result.body) : result.changed ? result.body : raw;
          state.cache = { raw, value };
          if (result.changed) notify("patched", result.count);
          else if (result.reason === "schema-mismatch") notify("schema-mismatch", 1, result.detail);
          return value;
        } catch (_) { return raw; }
      }
      Object.defineProperty(proto, "responseText", {
        ...textDescriptor,
        get() {
          const raw = textDescriptor.get.call(this);
          return transformed(this, raw, false);
        }
      });
      Object.defineProperty(proto, "response", {
        ...responseDescriptor,
        get() {
          const raw = responseDescriptor.get.call(this);
          if (this.responseType === "json") return transformed(this, raw, true);
          if (this.responseType === "" || this.responseType === "text") return transformed(this, raw, false);
          return raw;
        }
      });
    }
  }

  const recoveryKey = "abypass.region-recovery.v1";
  function recoverRegionPage() {
    if (win.location.origin !== "https://flow.google.com" ||
        !/^\/unsupported-country\/?$/.test(win.location.pathname) || readConfig().enabled !== true) return;
    notify("region-page", 1);
    try {
      const now = Date.now();
      const previous = Number(win.sessionStorage.getItem(recoveryKey));
      if (Number.isFinite(previous) && now - previous < 300000) return;
      win.sessionStorage.setItem(recoveryKey, String(now));
      win.location.replace("https://flow.google.com/");
    } catch (_) { /* Storage unavailable: keep the page; never create a loop. */ }
  }
  if (typeof win.addEventListener === "function") {
    win.addEventListener(CONFIG_EVENT, recoverRegionPage);
    win.addEventListener("popstate", recoverRegionPage);
    if (win.history) {
      for (const method of ["pushState", "replaceState"]) {
        const original = win.history[method];
        if (typeof original === "function") {
          win.history[method] = function (...args) {
            const result = original.apply(this, args);
            recoverRegionPage();
            return result;
          };
        }
      }
    }
  }
  recoverRegionPage();
  return { recoverRegionPage };
});

