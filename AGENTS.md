# Repository guidance

## User interaction and delivery workflow

- Respond to the user in Russian by default. Keep source code, identifiers, and
  commit messages in English.
- Work directly on `main`; do not create feature branches for this repository.
- After a logically complete implementation has passed the relevant build and
  tests, stage only the task-related files, create a clear commit, and push it
  directly to `main` without waiting for a separate request. If completion or
  validation is uncertain, do not commit or push; ask the user first.
- Preserve unrelated user changes in a dirty worktree and never include them in
  a task commit.

## Project overview

This is NekoBox for Android (NB4A), an Android GUI for the `sing-box` proxy
core. This fork is hosted at `github.com/Errorovich/NekoBoxForAndroid`; its
direct upstream is `github.com/starifly/NekoBoxForAndroid`, which is itself a
fork of `github.com/MatsuriDayo/NekoBoxForAndroid`. The license is GPL-3.0.

The current Android application ID comes from `nb4a.properties` and is
`ru.errorovich.nb4a`. The Android namespace and most Kotlin package roots remain
`io.nekohasekai.sagernet.*`; additional code lives under
`moe.matsuri.nb4a.*`.

The repository has two main layers:

- `app/`: the Android application, mostly Kotlin, with min SDK 21 and target /
  compile SDK 35. It consumes the native core as a generated AAR from
  `app/libs/libcore.aar`.
- `libcore/`: a Go 1.25 gomobile wrapper around `sing-box` and `libneko`. Its
  exported API becomes the Java package `libcore` used by the Kotlin layer.

Build conventions and Android variants are centralized in
`buildSrc/src/main/kotlin/Helpers.kt`. Product flavors are `oss`, `fdroid`,
`play`, and `preview`; build types are `debug` and `release`.

## Core dependency model

There are no git submodules. Do not commit the proxy-core source trees into this
repository. The build fetches them as siblings of the repository root:

```text
<parent>/
|-- NekoBox/     # this repository
|-- sing-box/    # qr243vbi/sing-box at a pinned commit
`-- libneko/     # starifly/libneko at a pinned commit
```

- Clone URLs and checkout behavior are in
  `buildScript/lib/core/get_source.sh`.
- Pinned revisions are in `buildScript/lib/core/get_source_env.sh` as
  `COMMIT_SING_BOX` and `COMMIT_LIBNEKO`.
- `libcore/go.mod` replaces `github.com/sagernet/sing-box` and
  `github.com/matsuridayo/libneko` with those sibling directories.
- Because Go ignores replacement directives from dependency modules,
  `libcore/go.mod` must also retain the fork-compatible replacements for
  `sing-vmess`, `gvisor`, and `sing-tun`.

Never commit generated `*.aar` files, `libcore/.build`, downloaded geo assets,
or sibling source trees. The relevant ignore rules are already present in the
root and `libcore` `.gitignore` files.

## Build pipeline

The Unix/CI driver is `./run`; it resolves arguments beneath `buildScript`.
Both slash-separated paths and separate path components work with the current
driver (for example, `./run lib/core` and `./run lib core`).

1. `./run lib core` fetches the pinned sibling sources, initializes the
   MatsuriDayo `gomobile-matsuri` / `gobind-matsuri` toolchain, builds
   `libcore/libcore.aar`, and copies it to `app/libs/libcore.aar`.
2. `./run lib assets` downloads and xz-compresses `geoip.db` and `geosite.db`
   into `app/src/main/assets/sing-box/`.
3. Gradle builds the APK. The normal local/CI smoke target is
   `./gradlew app:assembleOssDebug` on Unix or
   `.\gradlew.bat :app:assembleOssDebug` on Windows.

The core bind tags currently include
`with_conntrack,with_gvisor,with_quic,with_wireguard,with_utls,with_clash_api,with_naive_outbound,with_awg,with_tailscale`.
`libcore/build.sh` derives the reported sing-box version from the pinned sibling
checkout and stamps it through the linker. Do not hard-code a different version
without changing the pinned core consistently.

Version and application metadata live in `nb4a.properties`. Release signing
values come from `local.properties` or environment variables; never expose
credentials in output or add local secrets to git.

### Native Windows build

The core and APK can be built natively on Windows; WSL is not required. The
host needs JDK 17, Android SDK/NDK, Go 1.25+, git, and the MatsuriDayo gomobile
fork from branch `master2`. Configure `ANDROID_HOME`, `ANDROID_NDK_HOME`,
`GOBIND=gobind-matsuri`, and `local.properties`, build the AAR from `libcore/`,
copy it to `app/libs/`, then run the Gradle target. `CLAUDE.md` contains the full
bootstrap command sequence when a clean Windows toolchain must be prepared.

The geo asset script requires `curl` and `xz`; on Windows, use Git Bash/WSL or
fetch and compress the files manually.

## libcore and app boundary

- `libcore/box_include.go` registers inbound, outbound, and DNS transports.
  Naive registration is isolated in `box_include_naive.go` behind its build
  tag. The custom Juicity implementation is under `libcore/protocol/juicity`.
- `libcore/box.go`, `platform_box.go`, `dns_box.go`, `nb4a.go`, `http.go`, and
  related files form the exported gomobile surface.
- Important Kotlin consumers include `bg/proto/BoxInstance.kt`,
  `bg/proto/TestInstance.kt`, `bg/BaseService.kt`,
  `moe/matsuri/nb4a/NativeInterface.kt`, and
  `moe/matsuri/nb4a/net/LocalResolverImpl.kt`.
- Any exported Go API change may require updates to these Kotlin callers and a
  regenerated AAR before the app can compile.

When changing or swapping the core, review these touch points in order:

1. clone URLs in `buildScript/lib/core/get_source.sh`;
2. pinned revisions in `buildScript/lib/core/get_source_env.sh`;
3. versions and replacements in `libcore/go.mod`;
4. protocol registration in `libcore/box_include*.go`;
5. changed sing-box APIs used throughout `libcore/*.go`;
6. Kotlin callers if the generated public API changes.

Pin core changes through `get_source_env.sh`; do not treat uncommitted edits in
the sibling repositories as part of a reproducible solution.

## Validation

- The repository currently contains no committed `test` or `androidTest`
  source trees. Gradle exposes test tasks, but most changes are validated by the
  appropriate APK build, lint, and focused runtime/protocol tests.
- For Android-only changes, normally run `:app:assembleOssDebug`; add the
  relevant lint task when the change can affect Android resources, manifests,
  or static analysis.
- For core changes, rebuild `libcore.aar`, then build the Android variant that
  consumes it. Run any available focused Go/runtime test appropriate to the
  touched package or protocol.
- CI builds the native core and `app:assembleOssDebug`. The release workflow
  builds `app:assembleOssRelease`.
- Documentation-only changes do not justify an expensive core/APK rebuild;
  inspect the diff and validate referenced paths and commands instead.

## Known naive/cronet certificate pitfall

Naive uses Cronet and rejects TLS certificates whose validity exceeds the
Chromium/CA-Browser-Forum limit, including locally trusted and self-signed test
certificates supplied through `certificate` or `certificate_path`.

- A 365-day test certificate can fail with net error `-213`,
  `ERR_CERT_VALIDITY_TOO_LONG`.
- Keep naive test certificates at 90 days. The known `_prototest` certificate
  recorded by the previous workflow expires on 2026-10-13 and must be reissued
  when expired.
- Diagnose naive TLS failures with `openssl x509 -noout -dates` before treating
  them as DNS failures.
- Failed Cronet probes to Google Secure DNS over IPv6 can be noisy but nonfatal
  on an IPv6-less network.
- Other listed protocols use Go TLS and do not share this specific Cronet
  validity behavior; naive is the relevant canary.
