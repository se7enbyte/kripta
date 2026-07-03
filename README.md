# kripta

Java uygulamalarını (JAR/class) veya herhangi bir kaynak dosyayı katmanlı,
geri-döndürülebilir bir şema ile şifreleyen küçük bir araç. Şifrelenmiş bir
JAR'ı **diske hiç çözmeden** bellekte çözüp çalıştıran bir loader içerir.

## Ne yapar

Katmanlı kodlama (encode sırası):

```
bytes → XOR(anahtar) → bit rotation → Base64
```

decode tam tersini uygular. Round-trip her zaman orijinali geri verir
(`SelfTest` bunu doğrular).

## Dürüst güvenlik notu (oku)

Bu araç **caydırıcılıktır, gizlilik değildir.** "Kimsenin çözemeyeceği"
şifreleme değildir — ve hiçbir obfuscator/packer öyle değildir. Sebep basit:

- Uygulamanın çalışması için kod, çalışma anında çözülmek zorunda.
- Çözme anahtarı, dağıttığın paketin (loader'ın) **içinde** bulunur.
- Yani anahtar da, çözülmüş kod da bir noktada kullanıcının makinesinde olur.

Kararlı biri debugger, bellek dökümü veya loader'ı tersine çevirerek anahtarı
bulup çözebilir. Bu araç sıradan gözü ve otomatik tarayıcıları yavaşlatır;
motive bir tersine mühendisi **durduramaz.**

### Gerçekten korumak istediğin şey için

Değerli/gizli mantığı (lisans doğrulama, ücret/ödül hesabı, kritik algoritma)
istemciye hiç indirme — **sunucunda** tut. İndirilemeyen kod kırılamaz. En
sağlam kurulum ikisini birlikte kullanır (defense in depth):

1. Kritik mantık sunucuda (asıl koruma)
2. İstemcide kalması zorunlu kod obfuscate/şifreli (maliyeti artıran katman)

## Kullanım

```bash
./build.sh                 # derle + test + demo

# manuel:
javac -d out $(find src -name '*.java')

# herhangi bir dosyayı şifrele / çöz
java -cp out dev.kripta.Kripta encrypt app.jar app.jar.enc --key "ANAHTAR"
java -cp out dev.kripta.Kripta decrypt app.jar.enc app.jar --key "ANAHTAR"

# şifreli JAR'ı diske çözmeden bellekte çalıştır
java -cp out dev.kripta.Loader app.jar.enc AnaSinif --key "ANAHTAR" [arg...]
```

## Yapı

| Dosya | Görev |
|-------|-------|
| `Codec.java` | Katmanlı encode/decode çekirdeği |
| `Kripta.java` | encrypt/decrypt CLI |
| `Loader.java` | Şifreli JAR'ı bellekte çözüp çalıştıran custom ClassLoader |
| `SelfTest.java` | Round-trip doğrulama |

## Lisans

MIT
