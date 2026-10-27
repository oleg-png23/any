# Flow device verification

californium supports one service: Google Flow. Canonical address: https://flow.google.com/. The adapter also recognizes Flow's legacy `/flow` and `/fx/tools/flow` routes on `labs.google`.

The UI distinguishes an unopened page, successful page loading, Google sign-in and network failure. Page loading is separate from the availability of generation. A user-requested load stop has its own state and is neither a successful access check nor a network failure.

To check a build, open Flow, sign in on Google's page if prompted, perform one generation operation and save the result. Record app version, Android version, date, network and the visible outcome. When troubleshooting, compare Flow compatibility enabled and disabled, refreshing the page after a setting change. Export diagnostics only when needed; the export excludes cookies, account tokens, page query parameters and prompts.

CI verifies native UI and engine behavior without a Google account. It does not prove current Google account eligibility or generation access.

Localized legacy Flow paths such as `/fx/ru/tools/flow` and `/fx/pt-BR/tools/flow` are included. Matching still ends at the Flow path boundary; other Labs tools are excluded.
