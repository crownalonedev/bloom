#!/usr/bin/env bash
# Verifies the consumer setup shown in the docs: publishes Bloom to Maven Local and builds the standalone template
# (bloom-examples/template) as a separate Gradle build, then checks the shaded jar's relocation.
set -euo pipefail
cd "$(dirname "$0")/.."
./gradlew publishToMavenLocal --console=plain -q
(cd bloom-examples/template && ./gradlew shadowJar --console=plain -q)
JAR="bloom-examples/template/build/libs/my-bloom-plugin-1.0.0.jar"
unzip -l "$JAR" | grep -q "com/example/myplugin/MyPlugin.class" || { echo "plugin class missing/relocated" >&2; exit 1; }
unzip -l "$JAR" | grep -q "com/example/myplugin/libs/bloom/platform/modern/ModernPlatformAdapter.class" || { echo "modern adapter missing" >&2; exit 1; }
unzip -l "$JAR" | grep -q "com/example/myplugin/libs/bloom/platform/legacy/LegacyPlatformAdapter.class" || { echo "legacy adapter missing" >&2; exit 1; }
if unzip -l "$JAR" | grep -q " io/github/crownalonedev/bloom/"; then echo "unrelocated Bloom classes found" >&2; exit 1; fi
echo "Template verified: $JAR"
