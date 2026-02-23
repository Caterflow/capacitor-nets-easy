#!/bin/bash
set -e

SDK_VERSION="1.6.1"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PLUGIN_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
DEST="$PLUGIN_ROOT/ios/Frameworks"

if [ -d "$DEST/Mia.xcframework" ]; then
  echo "Mia.xcframework already exists at $DEST/Mia.xcframework"
  exit 0
fi

echo "Downloading Nets Easy iOS SDK (Mia.xcframework) v${SDK_VERSION}..."

TMPDIR=$(mktemp -d)
trap 'rm -rf "$TMPDIR"' EXIT

git clone --depth 1 --branch "$SDK_VERSION" --filter=blob:none --sparse \
  https://github.com/Nets-eCom/Nets-Easy-iOS-SDK.git "$TMPDIR/sdk" 2>&1

cd "$TMPDIR/sdk"
git sparse-checkout set Mia.xcframework 2>&1
cd - > /dev/null

mkdir -p "$DEST"
cp -R "$TMPDIR/sdk/Mia.xcframework" "$DEST/"

# Strip bitcode if possible — Mia.xcframework v1.6.1 ships with bitcode,
# which Apple rejects since Xcode 14. On macOS with Xcode this strips it here;
# otherwise the podspec sets STRIP_BITCODE_FROM_COPIED_FILES=YES as a fallback.
if command -v xcrun &>/dev/null; then
  echo "Stripping bitcode from Mia.xcframework..."
  find "$DEST/Mia.xcframework" -name "Mia" -type f | while read -r binary; do
    xcrun bitcode_strip "$binary" -r -o "$binary"
  done
fi

echo "Done. Mia.xcframework installed at $DEST/Mia.xcframework"
