"use strict";
const test = require("node:test");
const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const vm = require("node:vm");
const root = path.join(__dirname, "../app/src/main/assets/compat");
const core = require(path.join(root, "core.js"));
const install = require(path.join(root, "runtime.js"));
const ORIGIN = "https://labs.google";
const PAGE = ORIGIN + "/fx/tools/flow";
const API = ORIGIN + "/fx/api/trpc/general.fetchToolAvailability";
const BATCH = "https://flow.google.com/_/FlowUi/data/batchexecute";

function appFields(country = 0) {
  const fields = Array(34).fill(null);
  fields[3] = "Привет 🌍";
  fields[30] = country;
  fields[31] = 0;
  fields[32] = { credits: 0, subscription: "NONE" };
  return fields;
}
function batch(fields = appFields()) {
  return [["wrb.fr", "cPZSdc", JSON.stringify(fields), null, "generic"]];
}
function frame(value, inclusiveLf = false) {
  const text = JSON.stringify(value);
  return Buffer.byteLength(text) + (inclusiveLf ? 1 : 0) + "\n" + text;
}
function unframe(raw, inclusiveLf = false) {
  let bytes = Buffer.from(raw.slice(6));
  const frames = [];
  while (bytes.length) {
    while (bytes.length && /\s/.test(String.fromCharCode(bytes[0]))) bytes = bytes.subarray(1);
    if (!bytes.length) break;
    const nl = bytes.indexOf(10);
    const size = Number(bytes.subarray(0, nl).toString()) - (inclusiveLf ? 1 : 0);
    frames.push(JSON.parse(bytes.subarray(nl + 1, nl + 1 + size).toString()));
    bytes = bytes.subarray(nl + 1 + size);
  }
  return frames;
}

test("scope resolves relative URLs and Request inputs using exact origin/path matches", () => {
  assert.equal(core.classifyRequest("/fx/api/trpc/general.fetchToolAvailability", PAGE).kind, "json");
  assert.equal(core.classifyRequest(new Request(API), PAGE).kind, "json");
  assert.equal(core.classifyRequest(new URL(API), PAGE).kind, "json");
  assert.equal(core.classifyRequest(BATCH, "https://flow.google.com/").kind, "batch");
  for (const url of [
    "https://labs.google.evil.test/fx/api/trpc/general.fetchToolAvailability",
    "http://labs.google/fx/api/trpc/general.fetchToolAvailability",
    "https://aistudio.google.com/fx/api/trpc/general.fetchToolAvailability",
    "https://flow.google.com/fx/api/trpc/general.fetchToolAvailability",
    "/api/auth/signout?next=fetchToolAvailability",
    "/anything?url=/_/FlowUi/data/batchexecute",
    "/fx/api/trpc/general.fetchToolAvailability/extra",
    "/fx/api/trpc/general.fetchToolAvailability,auth.signout",
    "https://user:password@labs.google/fx/api/trpc/general.fetchToolAvailability"
  ]) assert.equal(core.classifyRequest(url, PAGE), null, url);
  assert.equal(core.classifyRequest(API, "https://attacker.test/"), null);
});

test("JSON only changes explicit geographic Boolean flags, preserving all other eligibility", () => {
  const source = {
    result: { data: {
      isCountrySupported: false,
      isAgeSupported: false,
      availabilityState: "UNAVAILABLE",
      reason: "AGE_RESTRICTED",
      subscription: "NONE",
      billing: false,
      quota: 0,
      nested: [{ is_country_supported: false, is_age_supported: false }]
    } }
  };
  const result = core.transform(JSON.stringify(source), "json");
  assert.equal(result.count, 2);
  const expected = structuredClone(source);
  expected.result.data.isCountrySupported = true;
  expected.result.data.nested[0].is_country_supported = true;
  assert.deepEqual(JSON.parse(result.body), expected);
  assert.equal(source.result.data.isCountrySupported, false);
});

test("malformed or unknown JSON preserves the exact original string transactionally", () => {
  for (const body of [
    " { not JSON } ",
    '{ "availabilityState" : "PAYMENT_REQUIRED", "isAgeSupported": false }\n',
    '{ "isCountrySupported": false, "other": {"is_country_supported": "0"} }',
    '{ "isCountrySupported" : true }\n',
    " [null, 0, false] "
  ]) assert.equal(core.transform(body, "json").body, body);
  const tooDeep = '{"v":'.repeat(67) + '{}' + '}'.repeat(67);
  assert.equal(core.transform(tooDeep, "json").body, tooDeep);
  const oversized = '"' + 'x'.repeat(core.MAX_BYTES) + '"';
  assert.equal(core.transform(oversized, "json").body, oversized);
});

test("Flow config changes country field 30 and retains age, plan, credit and unknown fields", () => {
  const original = appFields();
  const result = core.transform(")]}'\n\n" + JSON.stringify(batch(original)), "batch");
  assert.equal(result.count, 1);
  const modified = JSON.parse(JSON.parse(result.body.slice(6))[0][2]);
  const expected = [...original];
  expected[30] = 1;
  assert.deepEqual(modified, expected);
  const booleanResult = core.transform(JSON.stringify(batch(appFields(false))), "batch");
  assert.equal(JSON.parse(JSON.parse(booleanResult.body)[0][2])[30], true);
});

test("unknown RPCs and unsupported field types pass through; later invalid fields roll back the batch", () => {
  for (const fields of [appFields("0"), appFields({ country: false }), appFields(2)]) {
    const source = " \n" + JSON.stringify(batch(fields));
    assert.equal(core.transform(source, "batch").body, source);
  }
  for (const rpc of ["unknown", "cPZSdc-extra", "kv2t2d"]) {
    const source = JSON.stringify([["wrb.fr", rpc, "[0,0,{\"billing\":false}]"]]);
    assert.equal(core.transform(source, "batch").body, source);
  }
  const source = JSON.stringify([...batch(), ...batch(appFields("invalid"))]);
  assert.equal(core.transform(source, "batch").body, source, "later bad RPC rolls back earlier change");
});

test("known availability RPCs change only their status field, including protobuf defaults", () => {
  for (const rpc of ["KV2T2d", "rThb8d", "cO7JOb", "md9xJf"]) {
    for (const status of [0, 2, 4, null, false]) {
      const fields = [status, false, { billing: false, credits: 0, age: false }, "日本語 🌏"];
      const source = JSON.stringify([["wrb.fr", rpc, JSON.stringify(fields)]]);
      const result = core.transform(source, "batch");
      assert.equal(result.changed, true, rpc + " status " + status);
      assert.deepEqual(JSON.parse(JSON.parse(result.body)[0][2]),
        [typeof status === "boolean" ? true : 1, ...fields.slice(1)]);
    }
    const omitted = core.transform(JSON.stringify([["wrb.fr", rpc, "[]"]]), "batch");
    assert.deepEqual(JSON.parse(JSON.parse(omitted.body)[0][2]), [1]);
    for (const fields of ["not JSON", "null", "{}", '["0"]', '[-1]', '[1.5]']) {
      const source = JSON.stringify([...batch(), ["wrb.fr", rpc, fields]]);
      const result = core.transform(source, "batch");
      assert.equal(result.body, source);
      assert.equal(result.detail, "batch-fields");
    }
  }
});

test("omitted and null country defaults are materialized without adding or changing the age field", () => {
  for (const fields of [[], ["existing"], Array(30).fill(null), appFields(null)]) {
    const source = JSON.stringify(batch(fields));
    const result = core.transform(source, "batch");
    const modified = JSON.parse(JSON.parse(result.body)[0][2]);
    assert.equal(modified[30], 1);
    assert.deepEqual(modified.slice(0, fields.length < 30 ? fields.length : 30), fields.slice(0, 30));
    assert.equal(modified.length, Math.max(31, fields.length));
    if (fields.length <= 31) assert.equal(Object.hasOwn(modified, 31), false);
    else assert.deepEqual(modified.slice(31), fields.slice(31));
  }
});

test("JSON handles protobuf-like country values and explicit country rejection states", () => {
  for (const country of [false, 0, null]) {
    const source = { isCountrySupported: country, isAgeSupported: false, billing: false, credits: 0 };
    const modified = JSON.parse(core.transform(JSON.stringify(source), "json").body);
    assert.deepEqual(modified, { ...source, isCountrySupported: country === 0 ? 1 : true });
  }
  for (const state of ["COUNTRY_UNAVAILABLE", "UNSUPPORTED_COUNTRY", "UNAVAILABLE_IN_REGION"]) {
    const source = { availabilityState: state, isAgeSupported: false, plan: "NONE" };
    assert.deepEqual(JSON.parse(core.transform(JSON.stringify(source), "json").body),
      { ...source, availabilityState: "AVAILABLE" });
  }
  for (const state of ["UNAVAILABLE", "AGE_RESTRICTED", "PAYMENT_REQUIRED", "QUOTA_EXCEEDED", "NEW_UNKNOWN_STATE"]) {
    const source = JSON.stringify({ availabilityState: state });
    assert.equal(core.transform(source, "json").body, source);
  }
});

test("framed batches support every known availability RPC together and retain unrelated rows", () => {
  const rows = [...batch([]), ...["KV2T2d", "rThb8d", "cO7JOb", "md9xJf"].map(rpc =>
    ["wrb.fr", rpc, JSON.stringify([2, { age: false, plan: "NONE" }])]), ["wrb.fr", "unknown", "[0]"]];
  const source = ")]}'\n\n" + frame(rows);
  const result = core.transform(source, "batch");
  assert.equal(result.count, 5);
  const decoded = unframe(result.body)[0];
  assert.deepEqual(decoded.at(-1), rows.at(-1));
  assert.equal(JSON.parse(decoded[0][2])[30], 1);
  for (const row of decoded.slice(1, -1)) assert.deepEqual(JSON.parse(row[2]), [1, { age: false, plan: "NONE" }]);
});

test("Flow config can change existing country field 30 when trailing age field is omitted", () => {
  for (const country of [0, false]) {
    const fields = appFields(country).slice(0, 31);
    const result = core.transform(JSON.stringify(batch(fields)), "batch");
    assert.equal(result.changed, true);
    const modified = JSON.parse(JSON.parse(result.body)[0][2]);
    assert.equal(modified.length, 31);
    assert.equal(Object.hasOwn(modified, 31), false, "never create the omitted age field");
    assert.equal(modified[30], typeof country === "boolean" ? true : 1);
    assert.deepEqual(modified.slice(0, 30), fields.slice(0, 30));
  }
});

test("Google stream lengths use UTF-8 bytes across frames, retaining both framing conventions", () => {
  const unchangedFrame = [["wrb.fr", "unknown", JSON.stringify(["日本語 🌏", { age: false }])]];
  for (const inclusiveLf of [false, true]) {
    const source = ")]}'\n\n" + frame(batch(), inclusiveLf) + "\n" + frame(unchangedFrame, inclusiveLf) + "\n";
    const result = core.transform(source, "batch");
    assert.equal(result.changed, true);
    const decoded = unframe(result.body, inclusiveLf);
    assert.equal(JSON.parse(decoded[0][0][2])[30], 1);
    assert.equal(JSON.parse(decoded[0][0][2])[31], 0);
    assert.deepEqual(decoded[1], unchangedFrame);
    assert.equal(result.body.endsWith("\n"), true);
  }
});

test("corrupt frame lengths, incomplete UTF-8 and later malformed chunks pass through atomically", () => {
  const prefix = ")]}'\n\n";
  const good = frame(batch());
  for (const source of [
    prefix + good + "\n20\n[]",
    prefix + "-1\n[]",
    prefix + "0\n[]",
    prefix + good.slice(0, -1),
    prefix + good + "\n4\nnope",
    prefix + good + "trailing garbage"
  ]) assert.equal(core.transform(source, "batch").body, source);
});

test("UTF-16 framing preserves Unicode and code-unit lengths across every frame", () => {
  const unchangedFrame = [["wrb.fr", "unknown", JSON.stringify(["日本語 🌏", { age: false }])]];
  const utf16Frame = value => {
    const text = JSON.stringify(value);
    return (text.length + 1) + "\n" + text;
  };
  const source = ")]}'\n\n" + utf16Frame(batch()) + "\n" + utf16Frame(unchangedFrame) + "\n";
  const result = core.transform(source, "batch");
  assert.equal(result.changed, true);
  let remaining = result.body.slice(6);
  const decoded = [];
  while (remaining.trim()) {
    remaining = remaining.trimStart();
    const newline = remaining.indexOf("\n");
    const length = Number(remaining.slice(0, newline)) - 1;
    const payload = remaining.slice(newline + 1, newline + 1 + length);
    assert.equal(payload.length, length);
    decoded.push(JSON.parse(payload));
    remaining = remaining.slice(newline + 1 + length);
  }
  assert.equal(JSON.parse(decoded[0][0][2])[30], 1);
  assert.equal(JSON.parse(decoded[0][0][2])[31], 0);
  assert.deepEqual(decoded[1], unchangedFrame);
  assert.equal(result.body.endsWith("\n"), true);

  for (const malformed of [
    source.slice(0, -2),
    source + "17\n[]",
    ")]}'\n\n" + utf16Frame(batch()) + "\n" + frame(unchangedFrame, true),
    ")]}'\n\n" + frame(batch(), true) + "\n" + utf16Frame(unchangedFrame)
  ]) {
    const rejected = core.transform(malformed, "batch");
    assert.equal(rejected.changed, false);
    assert.equal(rejected.body, malformed, "unknown/mixed/truncated framing never partially changes a response");
  }
});

function makeResponse(body, url = API, headers = { "content-type": "application/json" }) {
  const response = new Response(body, { status: 200, statusText: "Fine", headers });
  Object.defineProperty(response, "url", { value: url });
  return response;
}

function makeWindow(fetchFunction) {
  const win = new EventTarget();
  win.location = {
    href: PAGE,
    origin: ORIGIN,
    pathname: "/fx/tools/flow",
    replacements: [],
    replace(url) { this.replacements.push(url); }
  };
  win.Headers = Headers;
  win.Response = Response;
  win.CustomEvent = CustomEvent;
  win.fetch = fetchFunction;
  win.setTimeout = setTimeout;
  win.clearTimeout = clearTimeout;
  const storage = new Map();
  win.sessionStorage = { getItem: key => storage.get(key) ?? null, setItem: (key, value) => storage.set(key, value) };
  win.history = { pushState() {}, replaceState() {} };
  return win;
}

function connectNativeConfig(win, sent = []) {
  let nativeMessage;
  win.top = win;
  win.wrappedJSObject = win;
  const port = {
    postMessage(message) { sent.push(message); },
    onMessage: { addListener: fn => { nativeMessage = fn; } },
    onDisconnect: { addListener() {} }
  };
  vm.runInNewContext(fs.readFileSync(path.join(root, "bridge.js"), "utf8"), {
    window: win,
    location: win.location,
    exportFunction: fn => fn,
    browser: { runtime: { connectNative: () => port } },
    Event,
    JSON
  });
  return enabled => nativeMessage({ type: "config", enabled });
}

test("fetch rewrites a complete framed availability response and reports bounded parse failures", async () => {
  for (const malformed of [false, true]) {
    const source = ")]}'\n\n" + (malformed ? "40\n[]" : frame([["wrb.fr", "KV2T2d", "[2,false]"]]));
    const original = makeResponse(source, BATCH, { "content-type": "application/json" });
    const win = makeWindow(async () => original);
    win.location = { origin: "https://flow.google.com", href: "https://flow.google.com/", pathname: "/" };
    const diagnostics = [];
    install(win, core, { readConfig: () => ({ known: true, enabled: true, revision: 1 }), notify: (...args) => diagnostics.push(args) });
    const result = await win.fetch(BATCH);
    if (malformed) {
      assert.equal(result, original);
      assert.deepEqual(diagnostics, [["schema-mismatch", 1, "batch-framing"]]);
    } else {
      assert.deepEqual(JSON.parse(unframe(await result.text())[0][0][2]), [1, false]);
      assert.deepEqual(diagnostics, [["patched", 1]]);
    }
    assert.equal(original.bodyUsed, false);
    assert.equal(await original.text(), source);
  }
});

test("bridge exports only fixed diagnostic reason codes, never arbitrary page text", () => {
  const win = makeWindow();
  const sent = [];
  const nativeConfig = connectNativeConfig(win, sent);
  nativeConfig(true);
  nativeConfig(true);
  assert.equal(sent.filter(message => message.event === "config-enabled").length, 1);
  for (const detail of ["batch-fields", "https://private.example/?token=secret", { body: "private" }]) {
    win.dispatchEvent(new CustomEvent("abypass:diagnostic", { detail: JSON.stringify({ event: "schema-mismatch", count: 1, detail }) }));
  }
  const events = sent.filter(message => message.event === "schema-mismatch");
  assert.equal(events.length, 3);
  assert.equal(events[0].detail, "batch-fields");
  assert.equal(Object.hasOwn(events[1], "detail"), false);
  assert.equal(Object.hasOwn(events[2], "detail"), false);
  assert.equal(JSON.stringify(sent).includes("private"), false);
});

test("fetch preserves request arguments, original body, response metadata, headers and clone behavior", async () => {
  const original = makeResponse('{"isCountrySupported":false,"isAgeSupported":false}', API, {
    "content-type": "application/json", "content-length": "54", etag: "old", "x-test": "yes"
  });
  let observed;
  const win = makeWindow(async function (...args) { observed = { receiver: this, args }; return original; });
  const diagnostics = [];
  install(win, core, { readConfig: () => ({ known: true, enabled: true, revision: 1 }), notify: (...args) => diagnostics.push(args) });
  const init = { method: "POST", body: "unchanged request body", headers: { "X-Request": "keep" } };
  const result = await win.fetch("/fx/api/trpc/general.fetchToolAvailability", init);
  assert.equal(observed.args[1], init);
  assert.equal(observed.receiver, win);
  assert.equal(original.bodyUsed, false);
  assert.equal(result.url, original.url);
  assert.equal(result.clone().url, original.url);
  assert.equal(result.clone().clone().url, original.url);
  assert.equal(result.statusText, "Fine");
  assert.equal(result.headers.get("x-test"), "yes");
  assert.equal(result.headers.has("content-length"), false);
  assert.equal(result.headers.has("etag"), false);
  assert.deepEqual(await result.json(), { isCountrySupported: true, isAgeSupported: false });
  assert.deepEqual(await original.json(), { isCountrySupported: false, isAgeSupported: false });
  assert.deepEqual(diagnostics, [["patched", 1]]);
});

test("fetch disable and disable/re-enable invalidate a response already in flight", async () => {
  for (const reenable of [false, true]) {
    let resolve;
    const original = makeResponse('{"isCountrySupported":false}');
    let config = { known: true, enabled: true, revision: 1 };
    const win = makeWindow(() => new Promise(r => { resolve = r; }));
    install(win, core, { readConfig: () => config, notify() {} });
    const pending = win.fetch(API);
    await new Promise(setImmediate);
    config = { known: true, enabled: false, revision: 2 };
    if (reenable) config = { known: true, enabled: true, revision: 3 };
    resolve(original);
    assert.equal(await pending, original);
    assert.deepEqual(await original.json(), { isCountrySupported: false });
  }
});

test("fetch checks enable state again after asynchronously reading a cloned body", async () => {
  let controller;
  let config = { known: true, enabled: true, revision: 1 };
  const stream = new ReadableStream({ start(c) { controller = c; } });
  const original = makeResponse(stream);
  const win = makeWindow(async () => original);
  install(win, core, { readConfig: () => config, notify() {} });
  const pending = win.fetch(API);
  controller.enqueue(new TextEncoder().encode('{"isCountry'));
  await new Promise(setImmediate);
  config = { known: true, enabled: false, revision: 2 };
  controller.enqueue(new TextEncoder().encode('Supported":false}'));
  controller.close();
  assert.equal(await pending, original);
  assert.equal(original.bodyUsed, false);
  assert.deepEqual(await original.json(), { isCountrySupported: false });
});

test("fetch preserves manual logout, unrelated redirects, unknown bodies and native network errors", async () => {
  for (const { request, response } of [
    { request: ORIGIN + "/api/auth/signout", response: makeResponse("logout", ORIGIN + "/api/auth/signout") },
    { request: API, response: makeResponse('{"isCountrySupported":false}', "https://accounts.google.com/") },
    { request: API, response: makeResponse(" {bad json} ") },
    { request: API, response: makeResponse('{"availabilityState":"UNAVAILABLE"}') },
    { request: API, response: makeResponse("<html>isCountrySupported</html>", API, { "content-type": "text/html" }) }
  ]) {
    const win = makeWindow(async () => response);
    install(win, core, { readConfig: () => ({ known: true, enabled: true, revision: 1 }), notify() {} });
    assert.equal(await win.fetch(request), response);
    assert.equal(response.bodyUsed, false);
  }
  const failure = new TypeError("network failed");
  const win = makeWindow(async () => { throw failure; });
  install(win, core, { readConfig: () => ({ known: true, enabled: true, revision: 1 }), notify() {} });
  await assert.rejects(win.fetch(API), error => error === failure);
});

function fakeXhrClass() {
  return class {
    constructor() { this.readyState = 0; this.status = 0; this.responseType = ""; this.responseURL = ""; }
    open(method, url) { this.method = method; this.url = url; this.readyState = 1; }
    send(body) { this.sentBody = body; }
    complete(body, finalUrl = API) {
      this._text = body;
      this._json = JSON.parse(body);
      this.responseURL = finalUrl;
      this.status = 200;
      this.readyState = 4;
    }
    get responseText() {
      if (this.responseType !== "" && this.responseType !== "text") throw new DOMException("wrong responseType", "InvalidStateError");
      return this._text || "";
    }
    get response() { return this.responseType === "json" ? this._json : this._text || ""; }
  };
}

test("XHR preserves native semantics, checks final responseURL and does not mutate the original JSON object", () => {
  const win = makeWindow();
  win.XMLHttpRequest = fakeXhrClass();
  let config = { known: true, enabled: true, revision: 1 };
  install(win, core, { readConfig: () => config, notify() {} });
  const xhr = new win.XMLHttpRequest();
  xhr.open("POST", API);
  const body = { arbitrary: "request" };
  xhr.send(body);
  assert.equal(xhr.sentBody, body);
  xhr.complete('{"isCountrySupported":false,"isAgeSupported":false}');
  assert.deepEqual(JSON.parse(xhr.responseText), { isCountrySupported: true, isAgeSupported: false });
  assert.equal(xhr.response, xhr.responseText);
  config = { known: true, enabled: false, revision: 2 };
  assert.equal(JSON.parse(xhr.responseText).isCountrySupported, false);
  config = { known: true, enabled: true, revision: 3 };
  assert.equal(JSON.parse(xhr.responseText).isCountrySupported, false, "in-flight revision is invalid after re-enable");

  const redirected = new win.XMLHttpRequest();
  redirected.open("GET", API);
  redirected.send();
  redirected.complete('{"isCountrySupported":false}', "https://evil.test/fetchToolAvailability");
  assert.equal(JSON.parse(redirected.responseText).isCountrySupported, false);

  const json = new win.XMLHttpRequest();
  json.open("GET", API);
  json.responseType = "json";
  json.send();
  json.complete('{"isCountrySupported":false,"isAgeSupported":false}');
  assert.deepEqual(json.response, { isCountrySupported: true, isAgeSupported: false });
  assert.equal(json._json.isCountrySupported, false);
  assert.throws(() => json.responseText, { name: "InvalidStateError" });
});

test("region route recovery is opt-in and bounded across attempts in the same tab", () => {
  const win = makeWindow();
  win.location.origin = "https://flow.google.com";
  win.location.href = "https://flow.google.com/unsupported-country";
  win.location.pathname = "/unsupported-country";
  let enabled = false;
  const api = install(win, core, { readConfig: () => ({ known: true, enabled, revision: 1 }), notify() {} });
  assert.deepEqual(win.location.replacements, []);
  enabled = true;
  api.recoverRegionPage();
  api.recoverRegionPage();
  assert.deepEqual(win.location.replacements, ["https://flow.google.com/"]);
  win.location.pathname = "/signout";
  api.recoverRegionPage();
  assert.equal(win.location.replacements.length, 1);
});

test("isolated native bridge defaults disabled and DOM events cannot grant enable permission", async () => {
  const sent = [];
  let nativeMessage;
  let disconnected;
  const port = {
    postMessage: message => sent.push(message),
    onMessage: { addListener: fn => { nativeMessage = fn; } },
    onDisconnect: { addListener: fn => { disconnected = fn; } }
  };
  const original = makeResponse('{"isCountrySupported":false}');
  const win = makeWindow(async () => original);
  win.top = win;
  win.wrappedJSObject = win;
  const context = {
    window: win,
    location: win.location,
    exportFunction: fn => fn,
    browser: { runtime: { connectNative(name) { assert.equal(name, "abypass"); return port; } } },
    Event,
    JSON
  };
  vm.runInNewContext(fs.readFileSync(path.join(root, "bridge.js"), "utf8"), context);
  install(win, core);
  const descriptor = Object.getOwnPropertyDescriptor(win, "__abypassReadConfig");
  assert.equal(descriptor.writable, false);
  assert.equal(descriptor.configurable, false);
  assert.equal(JSON.parse(win.__abypassReadConfig()).enabled, false);
  win.dispatchEvent(new CustomEvent("abypass:config-changed", { detail: JSON.stringify({ enabled: true }) }));
  win.dispatchEvent(new CustomEvent("message", { detail: { type: "config", enabled: true } }));
  assert.equal(JSON.parse(win.__abypassReadConfig()).enabled, false);
  nativeMessage({ type: "config", enabled: "true" });
  assert.equal(JSON.parse(win.__abypassReadConfig()).enabled, false);
  nativeMessage({ type: "config", enabled: true });
  const patched = await win.fetch(API);
  assert.deepEqual(await patched.json(), { isCountrySupported: true });
  assert.equal(sent.some(value => value.event === "ready"), true);
  assert.equal(sent.some(value => value.event === "patched" && value.count === 1), true);
  for (const message of sent) assert.deepEqual(Object.keys(message).sort(),
    message.count ? ["count", "event", "type"] : ["event", "type"]);
  disconnected();
  assert.equal(JSON.parse(win.__abypassReadConfig()).enabled, false);
});

test("real native config deduplicates identical updates without invalidating in-flight fetch/XHR", async () => {
  for (const mode of ["identical", "disable", "disable-reenable"]) {
    let nativeMessage;
    let resolveResponse;
    const original = makeResponse('{"isCountrySupported":false,"isAgeSupported":false}');
    const win = makeWindow(() => new Promise(resolve => { resolveResponse = resolve; }));
    win.XMLHttpRequest = fakeXhrClass();
    win.top = win;
    win.wrappedJSObject = win;
    const port = {
      postMessage() {},
      onMessage: { addListener: fn => { nativeMessage = fn; } },
      onDisconnect: { addListener() {} }
    };
    vm.runInNewContext(fs.readFileSync(path.join(root, "bridge.js"), "utf8"), {
      window: win,
      location: win.location,
      exportFunction: fn => fn,
      browser: { runtime: { connectNative: () => port } },
      Event,
      JSON
    });
    let configEvents = 0;
    win.addEventListener("abypass:config-changed", () => { configEvents++; });
    nativeMessage({ type: "config", enabled: true });
    const revision = JSON.parse(win.__abypassReadConfig()).revision;
    assert.equal(configEvents, 1, "first native answer publishes known state");
    install(win, core);
    const xhr = new win.XMLHttpRequest();
    xhr.open("GET", API);
    xhr.send();
    const pending = win.fetch(API);
    await new Promise(setImmediate);

    nativeMessage({ type: "config", enabled: true });
    nativeMessage({ type: "config", enabled: true });
    assert.equal(JSON.parse(win.__abypassReadConfig()).revision, revision);
    assert.equal(configEvents, 1, "identical native answers emit no new event");
    if (mode !== "identical") nativeMessage({ type: "config", enabled: false });
    if (mode === "disable-reenable") nativeMessage({ type: "config", enabled: true });

    xhr.complete('{"isCountrySupported":false,"isAgeSupported":false}');
    resolveResponse(original);
    const response = await pending;
    const shouldPatch = mode === "identical";
    assert.equal(JSON.parse(xhr.responseText).isCountrySupported, shouldPatch, mode + " XHR");
    assert.equal((await response.json()).isCountrySupported, shouldPatch, mode + " fetch");
    assert.equal(JSON.parse(xhr.responseText).isAgeSupported, false);
    if (shouldPatch) {
      assert.notEqual(response, original);
      assert.equal(original.bodyUsed, false);
    } else {
      assert.equal(response, original);
      assert.ok(JSON.parse(win.__abypassReadConfig()).revision > revision);
    }
  }
});

test("early XHR and fetch after initial timeout may adopt only the first native enabled answer", async () => {
  for (const mode of ["first-enabled", "first-disabled", "disable-reenable", "never-known"]) {
    let resolveResponse;
    let markFetchStarted;
    const fetchStarted = new Promise(resolve => { markFetchStarted = resolve; });
    const original = makeResponse('{"isCountrySupported":false,"isAgeSupported":false}');
    const win = makeWindow(() => {
      markFetchStarted();
      return new Promise(resolve => { resolveResponse = resolve; });
    });
    // Exercise the actual 800 ms timeout branch without adding wall time.
    win.setTimeout = (callback, milliseconds) => {
      assert.equal(milliseconds, 800);
      return setTimeout(callback, 0);
    };
    win.XMLHttpRequest = fakeXhrClass();
    const nativeConfig = connectNativeConfig(win);
    install(win, core);
    const xhr = new win.XMLHttpRequest();
    xhr.open("GET", API);
    xhr.send();
    const pending = win.fetch(API);
    await fetchStarted;
    assert.equal(JSON.parse(win.__abypassReadConfig()).known, false, "network proceeds after timeout");
    win.dispatchEvent(new CustomEvent("abypass:config-changed", { detail: JSON.stringify({ enabled: true }) }));
    assert.equal(JSON.parse(win.__abypassReadConfig()).enabled, false, "DOM events never authorize a response");
    if (mode === "first-enabled") nativeConfig(true);
    else if (mode === "first-disabled") { nativeConfig(false); nativeConfig(true); }
    else if (mode === "disable-reenable") { nativeConfig(true); nativeConfig(false); nativeConfig(true); }
    xhr.complete('{"isCountrySupported":false,"isAgeSupported":false}');
    resolveResponse(original);
    const response = await pending;
    const shouldPatch = mode === "first-enabled";
    assert.equal(JSON.parse(xhr.responseText).isCountrySupported, shouldPatch, mode + " XHR");
    assert.equal((await response.json()).isCountrySupported, shouldPatch, mode + " fetch");
    assert.equal(JSON.parse(xhr.responseText).isAgeSupported, false);
    assert.equal(response === original, !shouldPatch);
  }
});

test("first native enabled answer may arrive while a post-timeout response clone is being read", async () => {
  let controller;
  let markFetchStarted;
  const fetchStarted = new Promise(resolve => { markFetchStarted = resolve; });
  const stream = new ReadableStream({ start(value) { controller = value; } });
  const original = makeResponse(stream);
  const win = makeWindow(async () => { markFetchStarted(); return original; });
  win.setTimeout = callback => setTimeout(callback, 0);
  const nativeConfig = connectNativeConfig(win);
  install(win, core);
  const pending = win.fetch(API);
  await fetchStarted;
  controller.enqueue(new TextEncoder().encode('{"isCountry'));
  await new Promise(setImmediate);
  nativeConfig(true);
  controller.enqueue(new TextEncoder().encode('Supported":false,"isAgeSupported":false}'));
  controller.close();
  const response = await pending;
  assert.deepEqual(await response.json(), { isCountrySupported: true, isAgeSupported: false });
  assert.equal(original.bodyUsed, false);
});

test("manifest isolates privileged messaging and only declares the two supported HTTPS hosts", () => {
  const manifest = JSON.parse(fs.readFileSync(path.join(root, "manifest.json")));
  assert.equal(manifest.browser_specific_settings.gecko.id, "abypass-compat@app.abypass");
  assert.ok(manifest.permissions.includes("nativeMessagingFromContent"));
  for (const script of manifest.content_scripts) {
    assert.deepEqual(script.matches.sort(), ["https://flow.google.com/*", "https://labs.google/*"]);
    assert.equal(script.run_at, "document_start");
    assert.equal(script.all_frames, false);
  }
  assert.equal(manifest.content_scripts.find(script => script.world === "ISOLATED").js[0], "bridge.js");
  assert.deepEqual(manifest.content_scripts.find(script => script.world === "MAIN").js, ["core.js", "runtime.js"]);
});

