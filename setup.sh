#!/usr/bin/env bash
# BouncyCastle'i lib/'e indirir ve SHA-256 dogrular. Idempotent.
set -euo pipefail
cd "$(dirname "$0")"

BC_VER="1.84"
BC_JAR="bcprov-jdk18on-${BC_VER}.jar"
BC_URL="https://repo1.maven.org/maven2/org/bouncycastle/bcprov-jdk18on/${BC_VER}/${BC_JAR}"
BC_SHA="64d6c5a6121fcd927152dd182cbed39afe0fda641a970d9bcc0c9cb1858b2731"

mkdir -p lib
if [ -f "lib/${BC_JAR}" ]; then
  echo "✓ Bağımlılık zaten var: lib/${BC_JAR}"
  exit 0
fi

echo ">> İndiriliyor: ${BC_JAR}"
curl -fsSL -o "lib/${BC_JAR}.tmp" "${BC_URL}"

if command -v shasum >/dev/null 2>&1; then
  ACTUAL=$(shasum -a 256 "lib/${BC_JAR}.tmp" | cut -d' ' -f1)
else
  ACTUAL=$(sha256sum "lib/${BC_JAR}.tmp" | cut -d' ' -f1)
fi

if [ "${ACTUAL}" != "${BC_SHA}" ]; then
  rm -f "lib/${BC_JAR}.tmp"
  echo "✗ SHA-256 uyuşmadı! beklenen=${BC_SHA} gelen=${ACTUAL}" >&2
  exit 1
fi

mv "lib/${BC_JAR}.tmp" "lib/${BC_JAR}"
echo "✓ Doğrulandı ve kuruldu: lib/${BC_JAR}"
