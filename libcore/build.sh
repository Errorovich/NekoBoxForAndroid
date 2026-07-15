#!/bin/bash

source ./env_java.sh || true
source ../buildScript/init/env_ndk.sh

BUILD=".build"

rm -rf $BUILD/android \
  $BUILD/java \
  $BUILD/javac-output \
  $BUILD/src

if [ -z "$GOPATH" ]; then
  GOPATH=$(go env GOPATH)
fi

# Version stamped into constant.Version (defaults to "unknown" otherwise).
# Derived from the sing-box sibling's nearest git tag so it always matches the
# fork's published release name (e.g. v1.13.14-mod2 -> 1.13.14-mod2) instead of
# diverging. get_source.sh clones sing-box as a real checkout, so git describe
# works in CI; the literal fallback covers a non-git copy of the sources and
# must track the pinned COMMIT_SING_BOX in get_source_env.sh.
SINGBOX_VERSION="$(git -C ../../sing-box describe --tags --abbrev=0 2>/dev/null | sed 's/^v//')"
[ -n "$SINGBOX_VERSION" ] || SINGBOX_VERSION="1.13.14-mod2"

export GOBIND=gobind-matsuri
# with_awg/with_tailscale only gate the core's include/*.go (which libcore does
# not import — it builds its own registry), so they add no code and cannot cause
# double registration; they are listed so VersionBox() reports awg/tailscale as
# built-in capabilities.
"$GOPATH"/bin/gomobile-matsuri bind -v -androidapi 21 -cache "$(realpath $BUILD)" -trimpath -ldflags="-s -w -X github.com/sagernet/sing-box/constant.Version=$SINGBOX_VERSION" -tags='with_conntrack,with_gvisor,with_quic,with_wireguard,with_utls,with_clash_api,with_naive_outbound,with_awg,with_tailscale' . || exit 1
rm -r libcore-sources.jar

proj=../app/libs
mkdir -p $proj
cp -f libcore.aar $proj
echo ">> install $(realpath $proj)/libcore.aar"
