# californium

Native Android app dedicated to Google Flow. The launcher label, in-app branding, Cf adaptive icon, diagnostic exports and APK use the californium name. The existing Android application ID is retained for compatibility.

The home screen identifies californium as an independent client, explains where Google sign-in happens, and has one primary action: open Flow. It labels the adapter as a setting and distinguishes a loaded page from verified access to generation. Settings control theme, wallpaper colors, desktop mode and Flow compatibility. The status page shows the last Flow page-load result and local diagnostics. The old multi-service catalog, search and favorites are removed. Existing preferences are migrated to retain Flow settings and discard removed-service records.

## Download

The latest published release is [californium 0.2.0 ARM64 APK](https://github.com/oleg-png23/any/releases/download/californium-v0.2.0/californium-0.2.0-arm64.apk). Version 0.2.2 builds are available from the matching CI run until a release is published. Android 8.0 or newer. Test builds from different CI runs use different debug certificates; uninstalling a prior build clears its site profile and app settings.

## Interface and motion

The fixed light and dark themes use a coordinated blue and neutral palette; wallpaper colors remain optional. The browser header displays the current host, and its toolbar keeps back, forward and reload/stop; the page menu contains desktop mode and external-browser actions. The native browser is not scaled during navigation, page progress keeps a fixed-height track, and the host survives progress updates. Controls use standard Material feedback without custom per-control scaling or animated theme tokens.

## Flow compatibility

The bundled opt-in adapter operates only on HTTPS Flow documents: `flow.google.com` and the legacy `/flow` and `/fx/tools/flow` paths on `labs.google`. Other Labs tools and removed services receive no adapter. Labs host permission is still needed for Flow's legacy endpoint. Response transformations preserve unknown formats, age, subscriptions, payments, credits, quotas and manual logout.

Loading a page does not prove access to generation. Google account and server requirements still apply. Authentication happens on Google's pages. The browser uses its own profile; it does not extract cookies from another app. Diagnostics contain fixed application events, without account tokens, cookies or prompts.

## Build and validation

The source directory retains its historical `aBypass` path. From it run:

```sh
./gradlew lintDebug testDebugUnitTest assembleDebug
node --test tests/compat.test.cjs
./gradlew -PabypassAbis=x86_64 connectedDebugAndroidTest
```

Gradle 9.6.0, AGP 9.4.0, Kotlin 2.4.10, Compose BOM 2026.09.00, Material 3 1.5.0-beta02, GeckoView 158.0.20261021001945 and SDK 37.2 are pinned. The GitHub workflow builds ARM64, runs regressions and emulator checks, and captures UI screenshots. Feature branches do not publish releases.

The emulator suite checks Flow-only navigation, browser host lifetime and layout during progress, opening without a disabled adapter, stop/retry/back, data clearing confirmation, page actions, themes and contrast, large text, launcher alignment and migration of old preferences. It needs no Google account or live Google access.

Bundled [Material Symbols Rounded](https://github.com/google/material-design-icons) are Apache 2.0; their license is included under `assets/notices`. GeckoView is MPL 2.0, AndroidX is Apache 2.0. This is an independent app, unaffiliated with Google.

Localized legacy Flow paths such as `/fx/ru/tools/flow` and `/fx/pt-BR/tools/flow` are included. Matching still ends at the Flow path boundary; other Labs tools are excluded.
