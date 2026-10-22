#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."

platform="${1:-all}"
if [[ "$platform" != all && "$platform" != ios && "$platform" != android ]]; then
  echo "Usage: $0 [all|ios|android]" >&2
  exit 1
fi

if [[ "$platform" == all || "$platform" == android ]]; then
  # Start an emulator or connect a device before running this command.
  ./gradlew --no-daemon :netlogger:connectedAndroidDeviceTest
fi

if [[ "$platform" == all || "$platform" == ios ]]; then
  if [[ "$(uname -s)" != Darwin ]]; then
    echo "iOS UI tests require macOS and Xcode." >&2
    exit 1
  fi
  if [[ -z "${DEVELOPER_DIR:-}" && -d /Applications/Xcode.app/Contents/Developer ]]; then
    export DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer
  fi
  simulator_id="${NETLOGGER_IOS_SIMULATOR_ID:-}"
  if [[ -z "$simulator_id" ]]; then
    simulator_id=$(xcrun simctl list devices available --json | python3 -c '
import json, sys
devices = [d for group in json.load(sys.stdin)["devices"].values() for d in group if d["name"].startswith("iPhone")]
devices.sort(key=lambda d: d["state"] != "Booted")
if not devices:
    sys.exit("No available iPhone simulator. Install an iOS runtime in Xcode.")
print(devices[0]["udid"])
')
  fi
  ./gradlew --no-daemon :netlogger:iosSimulatorArm64Test
  xcodebuild -project iosApp/NetloggerSample.xcodeproj -scheme NetloggerSample \
    -configuration Debug -destination "platform=iOS Simulator,id=$simulator_id" \
    -derivedDataPath iosApp/DerivedData CODE_SIGNING_ALLOWED=NO test
fi
