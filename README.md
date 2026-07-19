# NekoBox for Android

[![API](https://img.shields.io/badge/API-21%2B-brightgreen.svg?style=flat)](https://android-arsenal.com/api?level=21)
[![Releases](https://img.shields.io/github/v/release/Errorovich/NekoBoxForAndroid)](https://github.com/Errorovich/NekoBoxForAndroid/releases)
[![License: GPL-3.0](https://img.shields.io/badge/license-GPL--3.0-orange.svg)](https://www.gnu.org/licenses/gpl-3.0)
[![Contributors](https://img.shields.io/github/contributors/Errorovich/NekoBoxForAndroid)](https://github.com/Errorovich/NekoBoxForAndroid/graphs/contributors)

## Disclaimer

> This project is intended solely for technical research and code learning purposes and does not provide any form of network proxy service. Please do not use this project for any activities that violate local laws and regulations. Do not use this project in production environments. Users are fully responsible for any risks that may arise from using this project. If you download or reference this project, please delete all related content within 24 hours and avoid long-term storage, distribution, or dissemination of any part of this project. **The author reserves the right to modify, update, or remove any part of this project or its contents at any time without prior notice.**

## Downloads

[![GitHub All Releases](https://img.shields.io/github/downloads/Errorovich/NekoBoxForAndroid/total?label=downloads-total&logo=github&style=flat-square)](https://github.com/Errorovich/NekoBoxForAndroid/releases)

[GitHub Releases](https://github.com/Errorovich/NekoBoxForAndroid/releases)

**The Google Play version has been controlled by a third party since May 2024 and is a non-open
source version. Please do not download it.**

## Supported Proxy Protocols

* SOCKS (4/4a/5)
* HTTP(S)
* SSH
* Shadowsocks
* VMess
* Trojan
* VLESS
* AnyTLS/AnyReality
* Snell 1/2/3/4/5/6
* ShadowTLS
* TUIC
* Juicity
* Hysteria 1/2
* WireGuard
* Trojan-Go (trojan-go-plugin)
* NaïveProxy (naive-plugin)
* Mieru (mieru-plugin)

<details>
<summary>XHTTP Extra TLS configuration example</summary>

<pre><code class="language-json">
{
    "no_grpc_header": false,  // stream-up/one
	"x_padding_bytes": "100-10000",
	"sc_max_each_post_bytes": 1000000, // packet-up only
	"sc_min_posts_interval_ms": 30, // packet-up only
	"xmux": {
		"max_concurrency": "16-32",
		"max_connections": "0-0",
		"c_max_reuse_times": "0-0",
		"h_max_request_times": "600-900",
		"h_max_reusable_secs": "1800-3000",
		"h_keep_alive_period": 0
	},
    "x_padding_obfs_mode": false,
    "x_padding_key": "",
    "x_padding_header": "",
    "x_padding_placement": "",
    "x_padding_method": "",
    "uplink_http_method": "",
    "session_placement": "",
    "session_key": "",
    "seq_placement": "",
    "seq_key": "",
    "uplink_data_placement": "",
    "uplink_data_key": "",
    "uplink_chunk_size": 0,
	"download": {
		"mode": "auto",
		"host": "b.yourdomain.com",
		"path": "/xhttp",
        "no_grpc_header": false,  // stream-up/one
	    "x_padding_bytes": "100-10000",
	    "sc_max_each_post_bytes": 1000000, // packet-up only
	    "sc_min_posts_interval_ms": 30, // packet-up only
		"xmux": {
			"max_concurrency": "16-32",
			"max_connections": "0-0",
			"c_max_reuse_times": "0-0",
			"h_max_request_times": "600-900",
			"h_max_reusable_secs": "1800-3000",
			"h_keep_alive_period": 0
		},
        "x_padding_obfs_mode": false,
        "x_padding_key": "",
        "x_padding_header": "",
        "x_padding_placement": "",
        "x_padding_method": "",
        "uplink_http_method": "",
        "session_placement": "",
        "session_key": "",
        "seq_placement": "",
        "seq_key": "",
        "uplink_data_placement": "",
        "uplink_data_key": "",
        "uplink_chunk_size": 0,
		"server": "$(ip_or_domain_of_your_cdn)",
		"server_port": 443,
		"tls": {
			"enabled": true,
			"server_name": "b.yourdomain.com",
			"alpn": "h2",
			"utls": {
				"enabled": true,
				"fingerprint": "chrome"
			}
		}
	}
}
</code></pre>
</details>

<details>
<summary>XHTTP Extra Reality configuration example</summary>

<pre><code class="language-json">
{
    "no_grpc_header": false,  // stream-up/one
	"x_padding_bytes": "100-10000",
	"sc_max_each_post_bytes": 1000000, // packet-up only
	"sc_min_posts_interval_ms": 30, // packet-up only
	"xmux": {
		"max_concurrency": "16-32",
		"max_connections": "0-0",
		"c_max_reuse_times": "0-0",
		"h_max_request_times": "600-900",
		"h_max_reusable_secs": "1800-3000",
		"h_keep_alive_period": 0
	},
    "x_padding_obfs_mode": false,
    "x_padding_key": "",
    "x_padding_header": "",
    "x_padding_placement": "",
    "x_padding_method": "",
    "uplink_http_method": "",
    "session_placement": "",
    "session_key": "",
    "seq_placement": "",
    "seq_key": "",
    "uplink_data_placement": "",
    "uplink_data_key": "",
    "uplink_chunk_size": 0,
	"download": {
		"mode": "auto",
		"host": "example.com",
		"path": "/xhttp",
        "no_grpc_header": false,  // stream-up/one
	    "x_padding_bytes": "100-10000",
	    "sc_max_each_post_bytes": 1000000, // packet-up only
	    "sc_min_posts_interval_ms": 30, // packet-up only
		"xmux": {
			"max_concurrency": "16-32",
			"max_connections": "0-0",
			"c_max_reuse_times": "0-0",
			"h_max_request_times": "600-900",
			"h_max_reusable_secs": "1800-3000",
			"h_keep_alive_period": 0
		},
        "x_padding_obfs_mode": false,
        "x_padding_key": "",
        "x_padding_header": "",
        "x_padding_placement": "",
        "x_padding_method": "",
        "uplink_http_method": "",
        "session_placement": "",
        "session_key": "",
        "seq_placement": "",
        "seq_key": "",
        "uplink_data_placement": "",
        "uplink_data_key": "",
        "uplink_chunk_size": 0,
		"server": "$(ip_or_domain_of_your_cdn)",
		"server_port": 443,
		"tls": {
			"enabled": true,
			"server_name": "example.com",
			"reality": {
				"enabled": true,
				"public_key": "$(your_publicKey)",
				"short_id": "$(your_shortId)"
			},
			"utls": {
				"enabled": true,
				"fingerprint": "chrome"
			}
		}
	}
}
</code></pre>
</details>

Please visit [here](https://matsuridayo.github.io/nb4a-plugin/) to download plugins for full proxy
support.

## Supported Subscription Format

* Some widely used formats (like Shadowsocks, ClashMeta and v2rayN)
* sing-box outbound

Only resolving outbound, i.e. nodes, is supported. Information such as diversion rules are ignored.

## Building from Source

The app is Kotlin/Gradle; the proxy engine (`sing-box`) is a Go module compiled
to a native Android library (`libcore.aar`) via [gomobile](https://pkg.go.dev/golang.org/x/mobile/cmd/gomobile).
Its sources are **not** committed in this repo (no git submodules) — they're
fetched as sibling folders and pinned by commit. Building has two parts:
compile the core once into `libcore.aar`, then build the APK with Gradle.

### What you need installed

| Tool | Version | Used for |
|---|---|---|
| [Git](https://git-scm.com/) | any recent | cloning this repo and the core sources |
| [JDK](https://adoptium.net/) | 17 | Gradle / Android build |
| [Android SDK](https://developer.android.com/studio) | API 35 (compile), min API 21 | building the APK |
| Android NDK | r19c or newer (project pins `25.0.8775105`, a newer one such as r28 works too) | compiling the Go core to native `.so` libraries |
| [Go](https://go.dev/dl/) | 1.25 or newer | building `libcore.aar` |
| `curl` + `xz` | any | downloading/compressing the `geoip.db`/`geosite.db` assets |

The Android SDK/NDK and JDK can come from a full Android Studio install, or
from the SDK command-line tools alone. Building works both on Linux/macOS and
natively on Windows (no WSL required) — WSL is only convenient if you'd rather
run the bundled bash scripts as-is.

### 1. Get the sources

```sh
git clone https://github.com/Errorovich/NekoBoxForAndroid.git
cd NekoBoxForAndroid
```

The `sing-box` core and `libneko` are separate git repos that this project
pins by commit (see `buildScript/lib/core/get_source_env.sh`). On Linux/macOS/
WSL, fetching them is one command:

```sh
./run lib/core
```

This clones `qr243vbi/sing-box` and `starifly/libneko` as siblings of this
repo (`../sing-box`, `../libneko`), checks out the pinned commits, installs the
`gomobile-matsuri` toolchain, and compiles `libcore.aar` into `app/libs/`.

On native Windows, do the same steps manually (clone the two repos next to
this one at the pinned commits, install `gomobile-matsuri`/`gobind-matsuri`
from `MatsuriDayo/gomobile` branch `master2`, then run `gomobile-matsuri bind`
from `libcore/`) — see `CLAUDE.md`'s "Native Windows build" section for the
exact commands.

### 2. Fetch the routing assets

```sh
./run lib/assets
```

Downloads `geoip.db`/`geosite.db` (from `SagerNet/sing-geoip`/`sing-geosite`),
compresses them with `xz`, and places them in `app/src/main/assets/sing-box/`.
On Windows without WSL/Git Bash, fetch the two `.db` files from the latest
releases of those repos and `xz -9` them into that folder yourself.

### 3. Build the APK

```sh
./gradlew :app:assembleOssDebug     # or assembleOssRelease, on Linux/macOS
```

```powershell
.\gradlew.bat :app:assembleOssDebug   # on Windows
```

The resulting APK lands in `app/build/outputs/apk/oss/debug/` (or
`.../release/`). `local.properties` (git-ignored) must point `sdk.dir` at your
Android SDK.

## Credits

Core:

- [qr243vbi/sing-box](https://github.com/qr243vbi/sing-box) (based on [SagerNet/sing-box](https://github.com/SagerNet/sing-box))

Android GUI:

- [shadowsocks/shadowsocks-android](https://github.com/shadowsocks/shadowsocks-android)
- [SagerNet/SagerNet](https://github.com/SagerNet/SagerNet)

Web Dashboard:

- [Yacd-meta](https://github.com/MetaCubeX/Yacd-meta)

## Contributors

![Contributors](https://contrib.rocks/image?repo=Errorovich/NekoBoxForAndroid)
