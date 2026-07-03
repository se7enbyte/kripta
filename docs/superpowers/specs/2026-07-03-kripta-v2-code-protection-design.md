# kripta v2 — Tasarım Dokümanı

**Tarih:** 2026-07-03
**Durum:** Onaylandı (uygulamaya hazır)

## 1. Amaç

kripta'yı, oyuncak bir XOR/rotasyon kodlayıcısından, gerçek kimliği-doğrulanmış
şifreleme (authenticated encryption) kullanan, **makineye mühürlenmiş** ve
anti-tamper katmanlarıyla korunan bir kod-koruma aracına dönüştürmek.

Kullanıcının asıl derdi **kod korumaktır** (Sahne-2: şifreli kod kullanıcının
makinesinde çalışacak). Mimari tercihi **offline** (sunucusuz).

## 2. Dürüst Güvenlik Modeli (tasarımın temeli)

Bu, projenin en önemli bölümü ve README'nin merkezinde yer alacak.

**Temel gerçek:** Kod istemcide çalışıyorsa, çalışma anında bir noktada bellekte
çözülmek zorundadır. Makineyi kontrol eden kararlı bir tersine mühendis o anda
bellek dökümü alabilir. **Hiçbir offline şema bunu engelleyemez** — kripta dahil.

**kripta v2'nin gerçekten sağladığı:**

- **Taşınamazlık:** `.enc` artefaktı, mühürlendiği makine dışında bir yere
  kopyalanınca açılmaz. Sıradan "dosyayı kopyala-çalıştır" korsanlığını durdurur.
- **Otomatik analize direnç:** Anti-debug ve bütünlük kontrolleri, otomatik
  tarayıcıları ve gündelik kurcalamayı reddeder.
- **Dar dump penceresi:** Per-class şifreleme sayesinde tüm program hiçbir an
  bellekte tümüyle açık durmaz.

**kripta v2'nin sağlamadığı (README'de açıkça yazılacak):**

- Hedefli, kararlı bir tersine mühendise karşı mutlak koruma. Makine parmak izi
  taklit edilebilir, anti-debug atlatılabilir, çözülmüş baytlar dump edilebilir.
- Mutlak koruma isteyen kritik mantığını sunucuda tutmalıdır (indirilemeyen kod
  kırılamaz).

Bu dürüstlük bilinçli bir tasarım kararıdır: abartılı "unbreakable" iddiaları
projeyi ciddiyetsiz gösterir; net ve ölçülü bir güvenlik modeli olgun gösterir.

## 3. Bileşenler

Her dosya tek ve net bir sorumluluğa sahiptir; bağımsız test edilebilir.

### 3.1 `Crypto.java` — kripto çekirdeği (`Codec.java`'nın yerine)

Kimliği-doğrulanmış şifrelemenin tek sahibi.

- **Anahtar türetme:** Argon2id (BouncyCastle `Argon2BytesGenerator`).
  Parametreler: 64 MiB bellek (65536 KiB), 3 iterasyon, 4 paralellik,
  32 baytlık çıktı (AES-256 anahtarı). Parametreler dosya başlığında saklanır.
- **Şifre:** AES-256-GCM, 12 baytlık rastgele nonce, 128-bit tag.
- **Rastgelelik:** `SecureRandom`; her şifrelemede yeni salt (16 bayt) ve nonce.
- **Bütünlük/kimlik:** GCM tag; başlık baytları GCM'e **AAD** olarak verilir
  (parametreler kurcalanamaz, ciphertext'e bağlanır).
- **Hata davranışı:** yanlış anahtar/parola veya bozulmuş veri → `AEADBadTagException`
  yakalanır, kullanıcıya "parola yanlış ya da dosya bozuk/kurcalanmış" gibi net
  bir mesajla çevrilir. Asla çöp bayt döndürmez.

**Dosya formatı (ikili konteyner):**

```
offset  alan
0       magic           "KRPT" (4 bayt)
4       version         1 bayt (= 2)
5       kdf-id          1 bayt (= 1, Argon2id)
6       argon2-mem      4 bayt big-endian int (KiB)
10      argon2-iter     4 bayt big-endian int
14      argon2-par      1 bayt
15      bind-flag       1 bayt (0 = yalnız parola, 1 = makineye mühürlü)
16      salt            16 bayt
32      nonce           12 bayt
44..    ciphertext + GCM tag (kalan)
```

Başlık (0..43 arası, ciphertext hariç) AAD olarak kullanılır.

### 3.2 `MachineKey.java` — makine parmak izi + mühürleme

Anahtarı iki girdinin karışımından türetir; asıl "az yapılan" katman budur.

- **Parmak izi girdileri:** ağ arayüzü MAC adres(ler)i (sıralı, deterministik),
  hostname, OS adı + mimari. Elde edilebilenler birleştirilip SHA-256'dan geçer.
- **Karışım:** parmak izi Argon2id'e ek "pepper"/associated girdi olarak katılır;
  kullanıcı parolası ana giriş olur. Böylece anahtar = f(parola, makine).
- **Sonuç:** `bind-flag = 1` ile şifrelenen dosya, başka makinede farklı parmak
  izi ürettiği için açılamaz. `bind-flag = 0` seçilirse yalnız parolayla çalışır
  (taşınabilir mod — kullanıcı tercihi için `--no-bind` bayrağı).
- **Dayanıklılık:** MAC adresleri değişebildiği için parmak izi tek bir kaynağa
  bağlanmaz; birden fazla stabil sinyalin hash'i alınır. Hiçbir arayüz yoksa
  makul bir fallback (hostname+OS) kullanılır ve kullanıcı uyarılır.

### 3.3 `Guard.java` — anti-tamper

- **Anti-debug:** `ManagementFactory.getRuntimeMXBean().getInputArguments()`
  içinde `-agentlib:jdwp`, `-Xdebug`, `-Xrunjdwp` gibi imzaları arar; bulursa
  `run` çalışmayı reddeder. (Best-effort; atlatılabilir olduğu README'de yazılı.)
- **Bütünlük self-check:** loader, çalışırken kendi kritik sınıflarının bilinen
  SHA-256 hash'lerini karşılaştırır; uyuşmazsa durur. (Hash'ler derleme sonrası
  gömülür; ayrıntı uygulama planında.)
- **Per-class çözme:** JAR bir bütün olarak değil, her `.class` girişi ayrı
  şifrelenir; özel ClassLoader `findClass` çağrıldığında ilgili sınıfı o an çözer.
  Böylece program tümüyle aynı anda bellekte açık kalmaz.

### 3.4 `Kripta.java` — CLI dispatcher (yeniden yazılır)

Alt komutlar:

```
kripta encrypt <girdi> <çıktı> [--no-bind]   # gizli parola sorar, 2 kez doğrular
kripta decrypt <girdi> <çıktı>               # makine+parola doğrular
kripta run <enc> <AnaSınıf> [arg...]         # bellekte per-class çöz + çalıştır
kripta selftest                              # tüm doğrulama testleri
kripta help
kripta version
```

- Bilinmeyen komut / eksik argüman → kullanım metni + exit 1.
- `encrypt` varsayılan olarak makineye mühürler; `--no-bind` ile taşınabilir.

**CLI kalite hedefleri (kullanıcı isteği):** kompakt, kolay çalıştırılabilir,
kullanışlı. Bunun için:

- Kök dizinde tek bir `kripta` çalıştırıcı script'i (wrapper). Kullanıcı
  `./kripta encrypt app.jar app.enc` yazar; script `java -cp "out:lib/*"
  dev.kripta.Kripta ...` detayını gizler. İlk çalıştırmada `out/` yoksa otomatik
  derler.
- Kısa, net, hizalı yardım metni; her komut için tek satır açıklama + örnek.
- Renkli/işaretli çıktı (✓ başarı, ✗ hata) ANSI ile, terminal destekliyorsa.
- Anlaşılır hata mesajları (yanlış parola, yanlış makine, eksik dosya ayrı ayrı).

### 3.5 `Passphrase.java` — parola edinme

- **Interaktif:** `System.console().readPassword("Parola: ")`. `encrypt` sırasında
  iki kez sorup karşılaştırır; uyuşmazsa tekrar ister.
- **Non-interaktif** (console yok, ör. `build.sh`/CI): `KRIPTA_KEY` env değişkeni.
- İkisi de yoksa: net hata ("interaktif terminal yok ve KRIPTA_KEY tanımlı değil").
- Parola `char[]` olarak tutulur, kullanımdan sonra `Arrays.fill(..., '\0')` ile
  sıfırlanır (JVM'de best-effort; README'de belirtilir).

### 3.6 `Loader.java` — güncellenir

- `Crypto` + `MachineKey` + `Guard` kullanır; `run` alt komutuna bağlanır.
- Şifreli konteyneri okur, per-class çözer, bellekten yükleyen ClassLoader ile
  ana sınıfı çalıştırır.

### 3.7 `SelfTest.java` — genişletilir

Testler:

1. **Round-trip:** çeşitli boyut (0,1,15,16,1000,65536) ve parolalarda
   encrypt→decrypt == orijinal.
2. **Tamper tespiti:** ciphertext'te tek bayt çevrilince decrypt **hata** vermeli.
3. **Yanlış parola:** farklı parola ile decrypt **hata** vermeli (çöp değil).
4. **Yanlış makine (simüle):** farklı parmak izi ile mühürlü dosya açılamamalı.
5. **Başlık bütünlüğü:** başlıkta bir parametre değiştirilince (AAD) decrypt
   **hata** vermeli.

Tüm testler geçmezse exit 1.

### 3.8 `Codec.java` — silinir

Eski XOR/rotasyon tamamen kaldırılır. Geriye dönük uyumluluk yok (kullanıcı
"tamamen değiştir" seçti).

## 4. Build & Kurulum

### 4.1 `setup.sh` (yeni)

- Maven Central'dan `bcprov-jdk18on` (BouncyCastle, saf-Java tek JAR) `curl` ile
  `lib/`'e indirir.
- İndirilen JAR'ın **SHA-256'sını sabitlenmiş bir değerle doğrular** (supply-chain
  bütünlüğü); uyuşmazsa siler ve hata verir.
- `lib/` zaten doluysa atlar (idempotent).

### 4.2 `build.sh` (güncellenir)

- `lib/` yoksa `setup.sh` çağırır.
- `javac -cp "lib/*" -d out $(find src -name '*.java')`.
- `selftest` çalıştırır.
- Demoyu `-cp "out:lib/*"` ile çalıştırır (encrypt → run).

## 5. Test Stratejisi

- `SelfTest` yukarıdaki 5 senaryoyu kapsar (round-trip, tamper, yanlış parola,
  yanlış makine, başlık bütünlüğü).
- `build.sh` uçtan uca demo: örnek uygulama → JAR → encrypt (makineye mühürlü)
  → `run` ile bellekte çalıştır. Aynı makinede başarılı olmalı.
- Anti-debug: JDWP argümanıyla başlatılınca `run` reddetmeli (manuel doğrulama
  adımı planda).

## 5.1 README (iki dilli — kullanıcı isteği)

`README.md` hem **Türkçe hem İngilizce** yazılır (üstte dil seçim bağlantıları,
önce TR sonra EN blok). İçerik:

- Ne yapar (AES-256-GCM + Argon2id + makineye-mühürleme + anti-tamper).
- Bölüm 2'deki **dürüst güvenlik modeli** (neyi korur / neyi korumaz).
- Hızlı başlangıç: `./setup.sh` → `./kripta encrypt ...` → `./kripta run ...`.
- Komut referansı ve örnekler.

## 6. Kapsam Dışı (YAGNI)

- Sunucu/uzaktan anahtar (kullanıcı offline seçti).
- Argon2 dışı KDF'ler, çoklu şifre paketi seçimi.
- Eski format okuma / geriye dönük uyumluluk.
- GUI.

## 7. Onaylanan Kararlar

| Karar | Seçim |
|-------|-------|
| Ana hedef | Gerçek şifreleme + CLI yeniden tasarım |
| KDF | Argon2id (BouncyCastle), setup.sh ile indirilir |
| Parola girişi | Gizli prompt (varsayılan) + KRIPTA_KEY fallback |
| Eski format | Tamamen değiştirilir |
| Mimari | Offline, makineye-bağlı + anti-tamper |
