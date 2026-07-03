#!/usr/bin/env bash
# kripta'yi derler, testleri kosar ve tam bir demo calistirir.
set -euo pipefail
cd "$(dirname "$0")"

# Bagimlilik yoksa indir
if ! ls lib/bcprov-jdk18on-*.jar >/dev/null 2>&1; then
  ./setup.sh
fi

CP="out:lib/*"

echo ">> Derleniyor..."
rm -rf out && mkdir -p out
javac -cp "lib/*" -d out $(find src -name '*.java')

echo ">> Testler..."
java -cp "$CP" dev.kripta.SelfTest

echo ">> Demo: ornek uygulama -> JAR -> sifrele -> bellekte calistir"
export KRIPTA_KEY="${1:-M0untainSecret!42}"
mkdir -p demo/classes
javac -d demo/classes demo/app/SecretApp.java
jar cfe demo/secret.jar SecretApp -C demo/classes .
java -cp "$CP" dev.kripta.Kripta encrypt demo/secret.jar demo/secret.jar.enc --no-bind
echo ">> Sifreliyken calistiriliyor:"
java -cp "$CP" dev.kripta.Kripta run demo/secret.jar.enc SecretApp Micro
