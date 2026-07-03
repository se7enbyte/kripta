#!/usr/bin/env bash
# kripta'yi derler, testleri kosar ve tam bir demo calistirir.
set -e
cd "$(dirname "$0")"

echo ">> Derleniyor..."
javac -d out $(find src -name '*.java')

echo ">> Testler..."
java -cp out dev.kripta.SelfTest

echo ">> Demo: ornek uygulama -> JAR -> sifrele -> bellekte calistir"
KEY="${1:-M0untainSecret!42}"
mkdir -p demo/classes
javac -d demo/classes demo/app/SecretApp.java
jar cfe demo/secret.jar SecretApp -C demo/classes .

java -cp out dev.kripta.Kripta encrypt demo/secret.jar demo/secret.jar.enc --key "$KEY"
echo ">> Sifreliyken calistiriliyor:"
java -cp out dev.kripta.Loader demo/secret.jar.enc SecretApp --key "$KEY" Micro
