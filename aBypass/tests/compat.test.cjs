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
    '{ "availabilityState" : "COUNTRY_UNAVAILABLE", "isAgeSupported": false }\n',
    '{ "isCountrySupported": false, "other": {"is_country_supported": 0} }',
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

test("general eligibility RPCs are not rewritten and short or unknown schemas are not padded", () => {
  for (const fields of [[], Array(31).fill(0), appFields(null), appFields("0")]) {
    const source = " \n" + JSON.stringify(batch(fields));
    assert.equal(core.transform(source, "batch").body, source);
  }
  for (const rpc of ["KV2T2d", "rThb8d", "cO7JOb", "md9xJf", "unknown"]) {
    const source = JSON.stringify([["wrb.fr", rpc, "[0,0,{\"billing\":false}]"]]);
    assert.equal(core.transform(source, "batch").body, source);
  }
  const source = JSON.stringify([...batch(), ...batch([])]);
  assert.equal(core.transform(source, "batch").body, source, "later bad RPC rolls back earlier change");
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
