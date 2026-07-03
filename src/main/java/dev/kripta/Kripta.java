package dev.kripta;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

/**
 * kripta CLI dispatcher.
 *
 *   encrypt <girdi> <cikti> [--no-bind]
 *   decrypt <girdi> <cikti>
 *   run     <sifreli> <AnaSinif> [arg...]
 *   help | version
 */
public final class Kripta {

    static final String VERSION = "2.0.0";
    // ANSI (terminal destekliyorsa)
    static final boolean COLOR = System.console() != null && System.getenv("NO_COLOR") == null;
    static String ok(String s)  { return COLOR ? "[32m" + s + "[0m" : s; }
    static String err(String s) { return COLOR ? "[31m" + s + "[0m" : s; }

    public static void main(String[] args) {
        if (args.length == 0) { usage(); System.exit(1); return; }
        try {
            switch (args[0]) {
                case "encrypt" -> encrypt(args);
                case "decrypt" -> decrypt(args);
                case "run"     -> run(args);
                case "version" -> System.out.println("kripta " + VERSION);
                case "help", "-h", "--help" -> usage();
                default -> {
                    System.err.println(err("Bilinmeyen komut: ") + args[0]);
                    usage();
                    System.exit(1);
                }
            }
        } catch (SecurityException e) {
            System.err.println(err("✗ ") + e.getMessage());
            System.exit(2);
        } catch (IllegalArgumentException | IllegalStateException e) {
            System.err.println(err("✗ ") + e.getMessage());
            System.exit(1);
        } catch (Exception e) {
            System.err.println(err("✗ Hata: ") + e.getMessage());
            System.exit(1);
        }
    }

    private static void encrypt(String[] args) throws Exception {
        if (args.length < 3) throw new IllegalArgumentException("Kullanım: encrypt <girdi> <çıktı> [--no-bind]");
        boolean bind = !hasFlag(args, "--no-bind");
        Path in = Path.of(args[1]);
        Path out = Path.of(args[2]);
        byte[] data = Files.readAllBytes(in);
        char[] pass = Passphrase.readConfirmed();
        byte[] pepper = null;
        if (bind) {
            pepper = MachineKey.fingerprint();
            if (MachineKey.isDegenerate())
                System.out.println("Uyarı: donanım kimliği bulunamadı, makine bağlama zayıf olabilir.");
        }
        byte[] enc = Crypto.encrypt(data, pass, pepper);
        Arrays.fill(pass, '\0');
        Files.write(out, enc);
        System.out.printf("%s %s -> %s (%d bayt, %s)%n", ok("✓ Şifrelendi:"),
                in, out, data.length, bind ? "makineye mühürlü" : "taşınabilir");
    }

    private static void decrypt(String[] args) throws Exception {
        if (args.length < 3) throw new IllegalArgumentException("Kullanım: decrypt <girdi> <çıktı>");
        Path in = Path.of(args[1]);
        Path out = Path.of(args[2]);
        byte[] container = Files.readAllBytes(in);
        char[] pass = Passphrase.read("Parola: ");
        byte[] pepper = Crypto.isBound(container) ? MachineKey.fingerprint() : null;
        byte[] data = Crypto.decrypt(container, pass, pepper);
        Arrays.fill(pass, '\0');
        Files.write(out, data);
        System.out.printf("%s %s -> %s (%d bayt)%n", ok("✓ Çözüldü:"), in, out, data.length);
    }

    private static void run(String[] args) throws Exception {
        if (args.length < 3) throw new IllegalArgumentException("Kullanım: run <şifreli> <AnaSınıf> [arg...]");
        char[] pass = Passphrase.read("Parola: ");
        String[] progArgs = Arrays.copyOfRange(args, 3, args.length);
        try {
            Loader.run(args[1], args[2], pass, progArgs);
        } finally {
            Arrays.fill(pass, '\0');
        }
    }

    private static boolean hasFlag(String[] args, String flag) {
        for (String a : args) if (a.equals(flag)) return true;
        return false;
    }

    private static void usage() {
        System.out.println("""
            kripta — katmanlı, makineye-mühürlü kod/dosya şifreleme

            Kullanım:
              kripta encrypt <girdi> <çıktı> [--no-bind]   dosyayı şifrele (parolayı sorar)
              kripta decrypt <girdi> <çıktı>               şifreli dosyayı çöz
              kripta run     <şifreli> <AnaSınıf> [arg...]  şifreli JAR'ı bellekte çalıştır
              kripta version                               sürümü göster
              kripta help                                  bu yardımı göster

            Notlar:
              • Varsayılan olarak dosya bu makineye mühürlenir; başka makinede açılmaz.
                Taşınabilir istersen: encrypt ... --no-bind
              • Parola gizli sorulur; script/CI'da KRIPTA_KEY ortam değişkeni kullanılır.

            Örnek:
              kripta encrypt app.jar app.enc
              kripta run     app.enc com.ornek.Main arg1 arg2
            """);
    }
}
