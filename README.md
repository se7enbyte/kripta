# kripta

> [Türkçe](#türkçe) · [English](#english)

---

## Türkçe

Java uygulamalarını (JAR/class) veya herhangi bir dosyayı **AES-256-GCM +
Argon2id** ile şifreleyen, dilerse **bu makineye mühürleyen** ve şifreli bir
JAR'ı **diske hiç yazmadan bellekte** çözüp çalıştıran kompakt bir araç.

### Ne yapar

- **AES-256-GCM**: kimliği-doğrulanmış şifreleme. Kurcalamayı ve yanlış parolayı
  temiz bir hatayla yakalar; asla çöp veri döndürmez.
- **Argon2id**: bellek-sert anahtar türetme; kaba kuvvet saldırısını pahalılaştırır.
- **Makineye mühürleme**: anahtar, makine parmak izinden de türetilir → şifreli
  dosya başka makineye kopyalanınca açılmaz (`--no-bind` ile kapatılır).
- **Bellekte çalıştırma**: `run` şifreli JAR'ı bellekte, sınıf-sınıf çözüp
  çalıştırır; çözülmüş kod diske yazılmaz.

### Dürüst güvenlik notu (oku)

Bu araç **çıtayı yükseltir; sihir yapmaz.** Bir kodu çalıştırmak, onu bir an için
bellekte çözmek demektir. Makineyi kontrol eden kararlı bir tersine mühendis o
anı yakalayıp çözülmüş baytları alabilir; makine parmak izini taklit edebilir;
anti-debug'ı atlatabilir. kripta'nın gerçekten sağladığı:

- **Kopyalamayı durdurur**: mühürlü dosya yetkisiz makinede açılmaz.
- **Otomatik analizi zorlaştırır**: anti-debug + per-class çözme.
- **Dosya kasası olarak güçlüdür**: parola sende kaldığı sürece, sadece `.enc`
  dosyasını ele geçiren biri (güçlü parolayla) pratikte açamaz.

**Gerçekten kritik mantığı** (lisans, ödeme, çekirdek algoritma) istemciye hiç
indirme — **sunucunda** tut. İndirilemeyen kod kırılamaz. En sağlam kurulum
ikisini birleştirir (defense in depth).

### Kurulum

```bash
./setup.sh    # BouncyCastle'ı indirir + SHA-256 doğrular (tek sefer)
```

### Kullanım

```bash
# dosya şifrele (bu makineye mühürlü, parolayı gizli sorar)
./kripta encrypt app.jar app.enc

# taşınabilir (herhangi bir makinede parolayla açılır)
./kripta encrypt app.jar app.enc --no-bind

# çöz
./kripta decrypt app.enc app.jar

# şifreli JAR'ı diske yazmadan bellekte çalıştır
./kripta run app.enc com.ornek.Main arg1 arg2
```

### Masaüstü uygulaması

Koyu temalı masaüstü arayüzünü başlatmak için:

```bash
./kripta-gui
```

Arayüz; dosya sürükle-bırak, şifreleme, çözme, makineye mühürleme ve şifreli
JAR çalıştırma işlemlerini destekler. Platforma özel kurulum paketi üretmek için
JDK 21+ ile `./package.sh` çalıştırılır; çıktı `dist/` klasörüne yazılır.

GitHub Actions, `v*` etiketi gönderildiğinde macOS, Windows ve Linux paketlerini
oluşturup GitHub Releases'a ekler.

Script/CI için parolayı `KRIPTA_KEY` ortam değişkeninden verebilirsin (argv'de
parola geçirme — `ps` çıktısında görünür).

### Yapı

| Dosya | Görev |
|-------|-------|
| `Crypto.java` | AES-256-GCM + Argon2id + konteyner formatı |
| `MachineKey.java` | Makine parmak izi (Argon2 pepper) |
| `Passphrase.java` | Gizli parola girişi / KRIPTA_KEY |
| `Guard.java` | Anti-debug |
| `Loader.java` | Şifreli JAR'ı bellekte per-class çalıştıran ClassLoader |
| `Kripta.java` | CLI dispatcher |
| `SelfTest.java` | Doğrulama testleri |

### Lisans

MIT

---

## English

A compact tool that encrypts Java apps (JAR/class) or any file with
**AES-256-GCM + Argon2id**, optionally **seals it to this machine**, and can run
an encrypted JAR **entirely in memory** without ever writing the decrypted code
to disk.

### What it does

- **AES-256-GCM**: authenticated encryption. Detects tampering and wrong
  passwords with a clean error; never returns garbage.
- **Argon2id**: memory-hard key derivation; makes brute force expensive.
- **Machine sealing**: the key is also derived from a machine fingerprint, so an
  encrypted file won't open on another machine (disable with `--no-bind`).
- **In-memory execution**: `run` decrypts the JAR class-by-class in memory; the
  decrypted code never touches disk.

### Honest security note (read this)

This tool **raises the bar; it is not magic.** Running code means decrypting it
in memory for an instant. A determined reverse engineer who controls the machine
can capture that moment, spoof the machine fingerprint, or bypass the anti-debug
check. What kripta genuinely provides:

- **Stops copying**: a sealed file won't open on an unauthorized machine.
- **Hinders automated analysis**: anti-debug + per-class decryption.
- **Strong as a file vault**: as long as you keep the passphrase, someone who
  only obtains the `.enc` file cannot realistically open it (with a strong
  passphrase).

For **truly critical logic** (licensing, payments, core algorithms), never ship
it to the client — keep it on **your server**. Code that can't be downloaded
can't be cracked. The strongest setup combines both (defense in depth).

### Setup

```bash
./setup.sh    # downloads BouncyCastle + verifies SHA-256 (one time)
```

### Usage

```bash
# encrypt a file (sealed to this machine, prompts for passphrase)
./kripta encrypt app.jar app.enc

# portable (opens on any machine with the passphrase)
./kripta encrypt app.jar app.enc --no-bind

# decrypt
./kripta decrypt app.enc app.jar

# run an encrypted JAR in memory, never touching disk
./kripta run app.enc com.example.Main arg1 arg2
```

### Desktop app

Launch the dark desktop interface with `./kripta-gui`. It supports drag and
drop, encryption, decryption, machine sealing, and encrypted JAR execution.
Run `./package.sh` with JDK 21+ to create a native package under `dist/`.
Tags matching `v*` trigger macOS, Windows, and Linux GitHub Release builds.

For scripts/CI, provide the passphrase via the `KRIPTA_KEY` environment variable
(never pass it in argv — it shows up in `ps`).

### License

MIT
