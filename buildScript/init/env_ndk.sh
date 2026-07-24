#!/bin/bash

if [ -z "$ANDROID_HOME" ]; then
  if [ -d "$HOME/Android/Sdk" ]; then
    export ANDROID_HOME="$HOME/Android/Sdk"
  elif [ -d "$HOME/.local/lib/android/sdk" ]; then
    export ANDROID_HOME="$HOME/.local/lib/android/sdk"
  elif [ -d "$HOME/Library/Android/sdk" ]; then
    export ANDROID_HOME="$HOME/Library/Android/sdk"
  fi
fi

# Needs an NDK new enough to link the prebuilt cronet-go static lib (its recent
# builds use AArch64 relocation 315, which r25's ld.lld rejects). r28.2 links it;
# ANDROID_NDK_LATEST_HOME covers CI, where it points at the newest installed NDK.
_NDK="$ANDROID_HOME/ndk/28.2.13676358"
[ -f "$_NDK/source.properties" ] || _NDK="$ANDROID_NDK_LATEST_HOME"
[ -f "$_NDK/source.properties" ] || _NDK="$ANDROID_NDK_HOME"
[ -f "$_NDK/source.properties" ] || _NDK="$NDK"
[ -f "$_NDK/source.properties" ] || _NDK="$ANDROID_HOME/ndk-bundle"

if [ ! -f "$_NDK/source.properties" ]; then
  echo "Error: NDK not found."
  exit 1
fi

export ANDROID_NDK_HOME=$_NDK
export NDK=$_NDK
