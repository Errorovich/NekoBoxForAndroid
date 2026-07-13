# CLAUDE.md

Guidance for working in this repository.

## What this is

**NekoBox for Android (NB4A)** — Android GUI client for the `sing-box` universal
proxy core. This fork lives at `github.com/Errorovich/NekoBoxForAndroid`
(direct upstream: `github.com/starifly/NekoBoxForAndroid`, itself a fork of
`github.com/MatsuriDayo/NekoBoxForAndroid`). Package `com.nb4a`, license GPL-3.0.

Two layers:
- **`app/`** — Android app, Kotlin, package roots `io.nekohasekai.sagernet.*`
  and `moe.matsuri.nb4a.*`. Min SDK 21. Consumes the core as a prebuilt AAR.
- **`libcore/`** — Go module (`module libcore`, Go 1.24). A gomobile wrapper
  around `sing-box` + `libneko`. Compiled to `libcore.aar` and dropped into
  `app/libs/`.

## Core dependency model (read before touching the core)

There are **no git submodules** (no `.gitmodules`). The proxy core is **not**
committed here and must not be — it is fetched as sibling directories and wired
in via Go `replace` directives.

Layout after fetching sources (all siblings of the repo root):
```
VisualStudio/
├── NekoBox/     ← this repo (SRC_ROOT)
├── sing-box/    ← cloned by build script, pinned by commit
└── libneko/     ← cloned by build script, pinned by commit
```

- `buildScript/lib/core/get_source.sh` clones `starifly/sing-box` and
  `starifly/libneko` into `../` (the parent dir) and checks out pinned commits.
- Pinned commits live in `buildScript/lib/core/get_source_env.sh`
  (`COMMIT_SING_BOX`, `COMMIT_LIBNEKO`).
- `libcore/go.mod` `replace`s point at those siblings:
  `github.com/sagernet/sing-box => ../../sing-box`,
  `github.com/matsuridayo/libneko => ../../libneko`,
  plus `sing-vmess => github.com/starifly/sing-vmess`.

`.gitignore` rules keep build output out of git: `libcore/.gitignore` ignores
`*.aar`, `binary*.go`, `.build`, `env_*.sh`; `app/libs/libcore.aar` is a build
artifact.

## Build pipeline

Driver: `./run <path-under-buildScript>` executes the matching `*.sh`.
Everything sources `buildScript/init/env.sh` (needs `ANDROID_NDK_HOME`, Go,
gomobile). Build is Unix/bash + NDK — Windows host builds the core via WSL/CI,
not natively.

Order:
1. `./run lib/core` → `init.sh` then `build.sh`:
   - `get_source.sh` — clone/checkout `sing-box` + `libneko` siblings.
   - `libcore/init.sh` — install MatsuriDayo's `gomobile-matsuri`/`gobind-matsuri`
     fork (branch `master2`) and `gomobile init`.
   - `libcore/build.sh` — `gomobile-matsuri bind` with tags
     `with_conntrack,with_gvisor,with_quic,with_wireguard,with_utls,with_clash_api`,
     produces `libcore.aar`, copies it to `app/libs/`.
2. `./run lib/assets` — download `geoip.db` / `geosite.db` from
   `SagerNet/sing-geoip` + `sing-geosite`, xz-compress into
   `app/src/main/assets/sing-box/`.
3. Gradle builds the APK (`./gradlew :app:assembleRelease`), consuming
   `app/libs/*.aar` via `implementation(fileTree("libs"))`.

Version metadata: `nb4a.properties` (`VERSION_NAME`, `VERSION_CODE`, etc.).

## libcore ↔ app boundary

- `libcore/box_include.go` — registers all inbound/outbound/DNS protocol
  handlers on the sing-box registries. Custom protocol: `libcore/protocol/juicity`.
- `libcore/box.go`, `platform_box.go`, `dns_box.go`, `nb4a.go`, `http.go`, etc.
  — the exported gomobile surface, built on `sing-box/adapter`, `option`,
  `boxapi`, `experimental/libbox/platform`.
- Kotlin side imports the generated `libcore` package in ~14 files; key ones:
  `bg/proto/BoxInstance.kt`, `bg/proto/TestInstance.kt`, `bg/BaseService.kt`,
  `moe/matsuri/nb4a/NativeInterface.kt`, `moe/matsuri/nb4a/net/LocalResolverImpl.kt`.
  Changing libcore's exported API means updating these.

## Changing the core

The core is swappable by editing where its sources come from — the app repo
stays thin. Touch points, in order of how often they matter:

1. `buildScript/lib/core/get_source.sh` — the `sing-box` / `libneko` clone URLs.
2. `buildScript/lib/core/get_source_env.sh` — the pinned `COMMIT_*` values.
3. `libcore/go.mod` — the `replace => ../../sing-box` stays; run `go mod tidy`
   and reconcile versions of shared deps (`sagernet/sing`, `sing-tun`,
   `quic-go`, etc.) with what the core requires.
4. `libcore/box_include.go` — protocol/DNS registration, if the core's registry
   API or protocol set differs.
5. `libcore/*.go` — reconcile against any changed `adapter`/`option`/`boxapi`
   API. This is where most adaptation effort lands.
6. Kotlin bridge — only if the exported `libcore` API changes.

A core that tracks a different upstream lineage may expose an incompatible API;
diff its `adapter`/`option`/`boxapi` packages against what `libcore` imports
before committing to a swap.

## Conventions

- Core build scripts are bash + NDK; do not assume they run on a raw Windows
  shell. The Kotlin/Gradle layer builds cross-platform.
- Never commit `libcore.aar`, downloaded `*.db` assets, or the `sing-box` /
  `libneko` source trees — they are fetched/generated.
- Pin core changes by commit in `get_source_env.sh`, not by mutating siblings
  in place.
