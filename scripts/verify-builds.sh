#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."

if [[ "$(uname -s)" != Darwin ]]; then
  echo "Android + iOS verification requires macOS and Xcode." >&2
  exit 1
fi
if [[ -z "${DEVELOPER_DIR:-}" ]] && [[ -d /Applications/Xcode.app/Contents/Developer ]]; then
  export DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer
fi
xcodebuild -version
./gradlew --no-daemon \
  :app:assembleDebug :app:assembleRelease :app:testDebugUnitTest \
  :netlogger:testAndroidHostTest \
  :netlogger:iosSimulatorArm64Test

for configuration in Debug Release; do
  for sdk in iphonesimulator iphoneos; do
    xcodebuild -project iosApp/NetloggerSample.xcodeproj -scheme NetloggerSample \
      -configuration "$configuration" -sdk "$sdk" \
      -destination "generic/platform=$([[ "$sdk" == iphonesimulator ]] && echo 'iOS Simulator' || echo 'iOS')" \
      -derivedDataPath iosApp/DerivedData CODE_SIGNING_ALLOWED=NO build
    plist="iosApp/DerivedData/Build/Products/$configuration-$sdk/NetloggerSample.app/Info.plist"
    [[ "$(plutil -extract CADisableMinimumFrameDurationOnPhone raw -o - "$plist")" == true ]]
    if [[ "${NETLOGGER_CLEAN_FRAMEWORK_OUTPUTS:-0}" == 1 ]]; then
      # The linked sample apps and test reports remain available. Remove only
      # generated framework copies to limit peak disk usage on local machines.
      if [[ "$sdk" == iphonesimulator ]]; then
        native_target=iosSimulatorArm64
      else
        native_target=iosArm64
      fi
      framework_kind=$(printf '%s' "$configuration" | tr '[:upper:]' '[:lower:]')
      rm -rf "netlogger/build/bin/$native_target/${framework_kind}Framework" \
        "netlogger/build/xcode-frameworks/$configuration/$sdk"*
    fi
  done
done
