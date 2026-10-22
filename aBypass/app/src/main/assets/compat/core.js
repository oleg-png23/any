/* aBypass — original regional response transforms. No network or browser state. */
(function (root, factory) {
  "use strict";
  const api = Object.freeze(factory());
  if (typeof module === "object" && module.exports) module.exports = api;
  else Object.defineProperty(root, "__abypassCore", { value: api, configurable: true });
})(typeof globalThis !== "undefined" ? globalThis : this, function () {
  "use strict";
  const MAX_BYTES = 1024 * 1024;
  const origins = new Set(["https://flow.google.com", "https://labs.google"]);
  const encoder = new TextEncoder();
  const decoder = new TextDecoder("utf-8", { fatal: true });
  const parse = JSON.parse;
  const stringify = JSON.stringify;
  const countryKeys = new Set(["isCountrySupported", "is_country_supported"]);
  const mixedEligibilityRpcs = new Set(["KV2T2d", "rThb8d", "cO7JOb", "md9xJf"]);

  function allowedOrigin(origin) {
    return origins.has(origin);
  }

  function classifyRequest(input, base) {
    try {
      const source = typeof input === "string" ? input :
        input && typeof input.url === "string" ? input.url :
          input instanceof URL ? input.href : null;
      if (source === null) return null;
      const page = new URL(base);
      const target = new URL(source, page);
      if (!origins.has(page.origin) || target.origin !== page.origin ||
          target.username || target.password) return null;
      const path = target.pathname;
      // Full path matches: a query containing a familiar endpoint is never enough.
      if (/^\/_\/[A-Za-z][A-Za-z0-9]*\/data\/batchexecute$/.test(path)) {
        return { kind: "batch", url: target.href };
      }
      if (/^\/(?:fx\/)?api\/trpc\/general\.[A-Za-z][A-Za-z0-9]*(?:,general\.[A-Za-z][A-Za-z0-9]*)*$/.test(path) ||
          /^\/(?:fx\/)?(?:api\/)?(?:fetchToolAvailability|checkAppAvailability)$/.test(path)) {
        return { kind: "json", url: target.href };
      }
    } catch (_) { /* Unrecognized URL objects are transparent. */ }
    return null;
  }

  function unchanged(body, reason = "untouched") {
    return { changed: false, body, reason, count: 0 };
  }

  function patchCountryFlags(value) {
    let count = 0;
    let nodes = 0;
    let mismatch = false;
    function visit(node, depth) {
      if (!node || typeof node !== "object") return;
      if (++nodes > 20000 || depth > 64) throw new Error("bounded schema");
      for (const key of Object.keys(node)) {
        if (!Array.isArray(node) && countryKeys.has(key)) {
          if (node[key] === false) {
            node[key] = true;
            count++;
          } else if (node[key] !== true) mismatch = true;
        } else visit(node[key], depth + 1);
      }
    }
    visit(value, 0);
    return { count, mismatch };
  }

  function transformJson(body) {
    try {
      const value = parse(body);
      if (!value || typeof value !== "object") return unchanged(body);
      const result = patchCountryFlags(value);
      if (result.mismatch) return unchanged(body, "schema-mismatch");
      if (!result.count) return unchanged(body);
      return { changed: true, body: stringify(value), reason: "patched", count: result.count };
    } catch (_) { return unchanged(body, "schema-mismatch"); }
  }

  function patchBatch(batch) {
    if (!Array.isArray(batch)) throw new Error("batch is not an array");
    let count = 0;
    let unknownEligibility = false;
    for (const row of batch) {
      if (!Array.isArray(row) || row[0] !== "wrb.fr") continue;
      if (mixedEligibilityRpcs.has(row[1])) unknownEligibility = true;
      if (row[1] !== "cPZSdc") continue;
      if (typeof row[2] !== "string") throw new Error("unexpected RPC payload");
      const fields = parse(row[2]);
      // Field 31 is the observed country flag. Never create missing fields,
      // rewrite field 32 (age), or reinterpret a general eligibility enum.
      if (!Array.isArray(fields) || fields.length < 31 ||
          !Object.prototype.hasOwnProperty.call(fields, 30) ||
          ![0, 1, false, true].includes(fields[30])) {
        throw new Error("unknown Flow app configuration");
      }
      if (fields[30] === 0 || fields[30] === false) {
        fields[30] = typeof fields[30] === "boolean" ? true : 1;
        row[2] = stringify(fields);
        count++;
      }
    }
    return { count, unknownEligibility };
  }

  function prefixLength(body) {
    if (body.startsWith(")]}'\n\n")) return 6;
    if (body.startsWith(")]}'\n")) return 5;
    return 0;
  }

  // Parse one consistent length convention across the complete response. The
  // alternate format accepts UTF-16 code-unit counts including the leading
  // LF; try that only after complete UTF-8 parses fail, never frame by frame.
  function parseFrames(source, offset, inclusiveLf, utf16 = false) {
    const codeAt = index => utf16 ? source.charCodeAt(index) : source[index];
    const textAt = (start, end) => utf16 ? source.slice(start, end) : decoder.decode(source.slice(start, end));
    const frames = [];
    let pos = offset;
    while (pos < source.length) {
      const segmentStart = pos;
      while (pos < source.length && [9, 10, 13, 32].includes(codeAt(pos))) pos++;
      if (pos === source.length) {
        return { frames, tail: textAt(segmentStart) };
      }
      const headerStart = pos;
      while (pos < source.length && codeAt(pos) >= 48 && codeAt(pos) <= 57) pos++;
      const digits = textAt(headerStart, pos);
      if (!/^[1-9][0-9]{0,6}$/.test(digits) || codeAt(pos) !== 10) throw new Error("bad frame header");
      const declared = Number(digits);
      const length = declared - (inclusiveLf ? 1 : 0);
      const start = pos + 1;
      const end = start + length;
      if (length <= 0 || declared > MAX_BYTES || end > source.length) throw new Error("truncated frame");
      const text = textAt(start, end);
      // Do not let a counted separator LF make two different framing modes
      // look valid. Unknown padded payloads are passed through conservatively.
      if (!text.startsWith("[") || !text.endsWith("]")) throw new Error("ambiguous frame padding");
      const value = parse(text);
      if (!Array.isArray(value)) throw new Error("unknown frame schema");
      frames.push({
        value,
        leading: textAt(segmentStart, headerStart),
        original: textAt(headerStart, end)
      });
      pos = end;
    }
    return { frames, tail: "" };
  }

  function transformBatch(body) {
    const offset = prefixLength(body);
    const prefix = body.slice(0, offset);
    const content = body.slice(offset);
    if (content.trimStart().startsWith("[")) {
      try {
        const batch = parse(content);
        const result = patchBatch(batch);
        if (!result.count) return unchanged(body, result.unknownEligibility ? "schema-mismatch" : "untouched");
        return { changed: true, body: prefix + stringify(batch), reason: "patched", count: result.count };
      } catch (_) { return unchanged(body, "schema-mismatch"); }
    }
    const bytes = encoder.encode(body);
    let parsed;
    let inclusiveLf = false;
    let utf16 = false;
    try { parsed = parseFrames(bytes, offset, false); }
    catch (_) {
      inclusiveLf = true;
      try { parsed = parseFrames(bytes, offset, true); }
      catch (_) {
        utf16 = true;
        try { parsed = parseFrames(body, offset, true, true); }
        catch (_) { return unchanged(body, "schema-mismatch"); }
      }
    }
    try {
      let count = 0;
      let unknownEligibility = false;
      const output = [];
      for (const frame of parsed.frames) {
        const result = patchBatch(frame.value);
        count += result.count;
        unknownEligibility ||= result.unknownEligibility;
        if (result.count) {
          const text = stringify(frame.value);
          const length = utf16 ? text.length : encoder.encode(text).length;
          output.push(frame.leading + (length + (inclusiveLf ? 1 : 0)) + "\n" + text);
        } else output.push(frame.leading + frame.original);
      }
      if (!count) return unchanged(body, unknownEligibility ? "schema-mismatch" : "untouched");
      return { changed: true, body: prefix + output.join("") + parsed.tail, reason: "patched", count };
    } catch (_) { return unchanged(body, "schema-mismatch"); }
  }

  function transform(body, kind) {
    if (typeof body !== "string" || encoder.encode(body).length > MAX_BYTES) {
      return unchanged(body, "schema-mismatch");
    }
    if (kind === "json") return transformJson(body);
    if (kind === "batch") return transformBatch(body);
    return unchanged(body);
  }

  return { MAX_BYTES, allowedOrigin, classifyRequest, transform };
});
