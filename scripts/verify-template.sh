#!/usr/bin/env bash
# Verifies the consumer setup shown in the docs: publishes Bloom to Maven Local and builds the standalone template
# (bloom-examples/template) as a separate Gradle build, then checks the shaded jar's relocation.
set -euo pipefail
cd "$(dirname "$0")/.."
./gradlew publishToMavenLocal --console=plain -q
# Build the template against the Bloom version just published to Maven Local (the template defaults to the release).
BLOOM_VERSION="$(grep '^version=' gradle.properties | cut -d= -f2)"
(cd bloom-examples/template && ./gradlew shadowJar --console=plain -q -PbloomVersion="$BLOOM_VERSION")
JAR="bloom-examples/template/build/libs/my-bloom-plugin-1.0.0.jar"
# List once: `unzip | grep -q` under pipefail fails at random when grep exits early (SIGPIPE).
LISTING="$(unzip -l "$JAR")"
has() { grep -q "$1" <<<"$LISTING"; }
has "com/example/myplugin/MyPlugin.class" || { echo "plugin class missing/relocated" >&2; exit 1; }
has "com/example/myplugin/libs/bloom/platform/modern/ModernPlatformAdapter.class" || { echo "modern adapter missing" >&2; exit 1; }
has "com/example/myplugin/libs/bloom/platform/legacy/LegacyPlatformAdapter.class" || { echo "legacy adapter missing" >&2; exit 1; }
if has " io/github/crownalonedev/bloom/"; then echo "unrelocated Bloom classes found" >&2; exit 1; fi
./gradlew :bloom-doctor:shadowJar --console=plain -q
java -jar bloom-doctor/build/libs/bloom-doctor-*-all.jar "$JAR"
echo "Template verified: $JAR"
