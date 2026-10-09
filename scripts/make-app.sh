#!/usr/bin/env bash
# Builds a double-clickable Boot.app into dist/. macOS only.
set -euo pipefail

cd "$(dirname "$0")/.."

if [[ "$(uname)" != "Darwin" ]]; then
  echo "Boot.app can only be built on macOS. Elsewhere, play with: swift run boot-cli" >&2
  exit 1
fi

swift build -c release --product Boot
BIN="$(swift build -c release --show-bin-path)/Boot"

APP="dist/Boot.app"
rm -rf "$APP"
mkdir -p "$APP/Contents/MacOS" "$APP/Contents/Resources"
cp "$BIN" "$APP/Contents/MacOS/Boot"

cat > "$APP/Contents/Info.plist" <<'PLIST'
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
  <key>CFBundleName</key><string>Boot</string>
  <key>CFBundleDisplayName</key><string>Boot</string>
  <key>CFBundleIdentifier</key><string>com.boot.game</string>
  <key>CFBundleExecutable</key><string>Boot</string>
  <key>CFBundlePackageType</key><string>APPL</string>
  <key>CFBundleShortVersionString</key><string>0.1.0</string>
  <key>CFBundleVersion</key><string>1</string>
  <key>LSMinimumSystemVersion</key><string>14.0</string>
  <key>LSApplicationCategoryType</key><string>public.app-category.simulation-games</string>
  <key>NSHighResolutionCapable</key><true/>
</dict>
</plist>
PLIST

echo "Built $APP"
