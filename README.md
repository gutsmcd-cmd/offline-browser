# Offline Browser / オフラインブラウザ

Save web pages and read them later with no signal. 電波がなくても読めるように、ページを丸ごと保存。
Free forever · no ads · no login · no tracking.

## Features
- Simple browser: address bar that also searches (DuckDuckGo), back / forward, reload
- **⬇ Save** keeps the whole page (Android's WebView web archive, `.mht`) plus a reading-text copy
- **Save + links**: also saves up to 15 pages on the same site that the page links to (one level deep)
- **Saved** list: titles, dates, site, search (titles, addresses and page text), delete; open the full page or **Text only**
- Share → **Save to Offline Browser** from Chrome or any app saves a link straight away
- 日本語 / English switch (Japanese by default) and あ large-text switch

## Permissions
- `INTERNET` — only to load the pages you open. Saved pages stay in the app's private storage. No backups, no analytics.

## Limits
- Videos, and pages built entirely by scripts after loading, may not save fully. Links inside a saved page only work if that page was also saved.

## Install (sideload)
Download `offline-browser.apk` from the [Free Apps site](https://gutsmcd-cmd.github.io/free-apps/) and open it.

## Build
Kotlin, no AndroidX, minSdk 26, target 37.
```
./gradlew lint test assembleRelease
```
Release signing reads a git-ignored `keystore.properties`.

MIT License.
