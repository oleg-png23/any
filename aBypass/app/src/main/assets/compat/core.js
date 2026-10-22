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
  const availabilityRpcs = new Set(["KV2T2d", "rThb8d", "cO7JOb", "md9xJf"]);
  const countryStates = new Set(["COUNTRY_UNAVAILABLE", "UNSUPPORTED_COUNTRY", "COUNTRY_NOT_SUPPORTED",
    "REGION_UNAVAILABLE", "UNSUPPORTED_REGION", "REGION_NOT_SUPPORTED",
    "UNAVAILABLE_IN_COUNTRY", "UNAVAILABLE_IN_REGION"]);

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

  function unchanged(body, reason = "untouched", detail) {
    return { changed: false, body, reason, count: 0, ...(detail ? { detail } : {}) };
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
          if (node[key] === false || node[key] === 0 || node[key] === null) {
            node[key] = typeof node[key] === "number" ? 1 : true;
            count++;
          } else if (node[key] !== true && node[key] !== 1) mismatch = true;
        } else if (!Array.isArray(node) && key === "availabilityState" && countryStates.has(node[key])) {
          node[key] = "AVAILABLE";
          count++;
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
      if (result.mismatch) return unchanged(body, "schema-mismatch", "country-field-type");
      if (!result.count) return unchanged(body);
      return { changed: true, body: stringify(value), reason: "patched", count: result.count };
    } catch (_) { return unchanged(body, "schema-mismatch", "json-format"); }
  }

  function patchBatch(batch) {
    if (!Array.isArray(batch)) throw new Error("batch is not an array");
    let count = 0;
    for (const row of batch) {
      if (!Array.isArray(row) || row[0] !== "wrb.fr") continue;
      if (row[1] !== "cPZSdc" && !availabilityRpcs.has(row[1])) continue;
      if (typeof row[2] !== "string") throw new Error("unexpected RPC payload");
      const fields = parse(row[2]);
      if (!Array.isArray(fields)) throw new Error("unknown RPC array");
      // Protobuf JSON omits trailing default fields and uses null for defaults.
      // Only the known RPC's country/status position may be materialized.
      const index = row[1] === "cPZSdc" ? 30 : 0;
      const value = fields[index];
      const knownType = index === 30 ? [null, 0, 1, false, true].includes(value) :
        value === null || typeof value === "boolean" ||
        (Number.isSafeInteger(value) && value >= 0 && value <= 2147483647);
      if (value !== undefined && !knownType) {
        throw new Error("unknown availability field");
      }
      if (value !== 1 && value !== true) {
        while (fields.length <= index) fields.push(null);
        fields[index] = typeof value === "boolean" ? true : 1;
        row[2] = stringify(fields);
        count++;
      }
    }
    return { count };
  }

  function prefixLength(body) {
    const match = /^[\uFEFF \t\r\n]*\)\]}'(?:\r?\n){1,2}/.exec(body);
    return match ? match[0].length : 0;
  }

  function framingError(code) {
    const error = new Error(code);
    error.framingCode = code;
    return error;
  }

  function serializeFrame(value, original) {
    // Keep the original number of literal Unicode code points. Newly decoded
    // escape sequences stay escaped, so byte and UTF-16 length deltas agree
    // even when whitespace makes both wire conventions parse successfully.
    const remaining = new Map();
    for (const character of original.match(/[^\x00-\x7f]/gu) || []) {
      remaining.set(character, (remaining.get(character) || 0) + 1);
    }
    const json = stringify(value).replace(/[^\x00-\x7f]/gu, character => {
      const count = remaining.get(character) || 0;
      if (count) { remaining.set(character, count - 1); return character; }
      let escaped = "";
      for (let index = 0; index < character.length; index++) {
        escaped += "\\u" + character.charCodeAt(index).toString(16).padStart(4, "0");
      }
      return escaped;
    });
    if ([...remaining.values()].some(count => count !== 0)) throw new Error("Unicode preservation failed");
    return json;
  }

  // A length convention applies to the complete stream, never frame by frame.
  // Preserve JSON padding and update the original declared length by its delta.
  // This makes counted/un-counted separator whitespace equivalent on rewrite.
  function parseFrames(source, offset, includedSeparator, utf16 = false) {
    const codeAt = index => utf16 ? source.charCodeAt(index) : source[index];
    const textAt = (start, end) => utf16 ? source.slice(start, end) : decoder.decode(source.slice(start, end));
    const frames = [];
    let pos = offset;
    while (pos < source.length) {
      const segmentStart = pos;
      while (pos < source.length && [9, 10, 13, 32].includes(codeAt(pos))) pos++;
      if (pos === source.length) return { frames, tail: textAt(segmentStart) };
      if (frames.length >= 2048) throw framingError("batch-frame-limit");
      const headerStart = pos;
      while (pos < source.length && codeAt(pos) >= 48 && codeAt(pos) <= 57) pos++;
      const digitsEnd = pos;
      const digits = textAt(headerStart, pos);
      if (codeAt(pos) === 13) pos++;
      if (!/^[1-9][0-9]{0,6}$/.test(digits) || codeAt(pos) !== 10) throw framingError("batch-header");
      pos++;
      const separator = textAt(digitsEnd, pos);
      const declared = Number(digits);
      const included = includedSeparator === "header" ? separator.length : includedSeparator;
      const length = declared - included;
      const end = pos + length;
      if (length <= 0 || declared > MAX_BYTES || end > source.length) throw framingError("batch-length");
      let text;
      let value;
      try { text = textAt(pos, end); value = parse(text); }
      catch (_) { throw framingError("batch-frame-json"); }
      if (!Array.isArray(value)) throw framingError("batch-frame-json");
      const before = /^[ \t\r\n]*/.exec(text)[0];
      const after = /[ \t\r\n]*$/.exec(text)[0];
      frames.push({ value, declared, separator, before, after,
        json: text.slice(before.length, text.length - after.length),
        leading: textAt(segmentStart, headerStart), original: textAt(headerStart, end) });
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
        if (!result.count) return unchanged(body);
        return { changed: true, body: prefix + stringify(batch), reason: "patched", count: result.count };
      } catch (_) { return unchanged(body, "schema-mismatch", "batch-fields"); }
    }
    const bytes = encoder.encode(body);
    const byteOffset = encoder.encode(prefix).length;
    const failures = new Set();
    let accepted = null;
    let badFields = false;
    for (const utf16 of [false, true]) {
      for (const includedSeparator of [0, 1, "header"]) {
        let parsed;
        try { parsed = parseFrames(utf16 ? body : bytes, utf16 ? offset : byteOffset, includedSeparator, utf16); }
        catch (error) { failures.add(error.framingCode || "batch-framing"); continue; }
        try {
          let count = 0;
          const output = [];
          const measure = text => utf16 ? text.length : encoder.encode(text).length;
          for (const frame of parsed.frames) {
            const result = patchBatch(frame.value);
            count += result.count;
            if (result.count) {
              const json = serializeFrame(frame.value, frame.json);
              const declared = frame.declared + measure(json) - measure(frame.json);
              if (declared <= 0) throw new Error("frame length limit");
              if (declared > MAX_BYTES) return unchanged(body, "schema-mismatch", "size-limit");
              output.push(frame.leading + declared + frame.separator + frame.before + json + frame.after);
            } else output.push(frame.leading + frame.original);
          }
          const candidate = count ? { changed: true, body: prefix + output.join("") + parsed.tail, reason: "patched", count } : unchanged(body);
          if (encoder.encode(candidate.body).length > MAX_BYTES) return unchanged(body, "schema-mismatch", "size-limit");
          // If two complete interpretations would rewrite differently, pass
          // through atomically rather than choosing an arbitrary convention.
          if (accepted && (candidate.body !== accepted.body || candidate.count !== accepted.count)) {
            return unchanged(body, "schema-mismatch", "batch-ambiguous");
          }
          accepted = candidate;
        } catch (_) { badFields = true; }
      }
    }
    if (badFields) return unchanged(body, "schema-mismatch", "batch-fields");
    if (accepted) return accepted;
    const detail = failures.size === 1 ? failures.values().next().value : "batch-framing";
    return unchanged(body, "schema-mismatch", detail);
  }

  function transform(body, kind) {
    if (typeof body !== "string" || encoder.encode(body).length > MAX_BYTES) {
      return unchanged(body, "schema-mismatch", "size-limit");
    }
    if (kind === "json") return transformJson(body);
    if (kind === "batch") return transformBatch(body);
    return unchanged(body);
  }

  return { MAX_BYTES, allowedOrigin, classifyRequest, transform };
});

