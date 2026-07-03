#!/usr/bin/env bash
# Calisilan platform icin cift tiklanabilir kripta masaustu paketi uretir.
set -euo pipefail
cd "$(dirname "$0")"

./setup.sh
rm -rf out package-input dist
mkdir -p out package-input dist
javac -cp "lib/*" -d out $(find src/main -name '*.java')
jar --create --file package-input/kripta.jar --main-class dev.kripta.Gui -C out .
cp lib/bcprov-jdk18on-*.jar package-input/

TYPE="app-image"
case "$(uname -s)" in
  Darwin) TYPE="dmg" ;;
  Linux)  TYPE="deb" ;;
esac

jpackage \
  --type "$TYPE" \
  --name kripta \
  --app-version 2.0.1 \
  --vendor kripta \
  --description "AES-256-GCM ve Argon2id dosya sifreleme" \
  --input package-input \
  --main-jar kripta.jar \
  --main-class dev.kripta.Gui \
  --java-options "-Dfile.encoding=UTF-8" \
  --dest dist

echo "✓ Masaüstü paketi hazır: dist/"
