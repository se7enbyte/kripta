package dev.kripta;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Basit CLI:
 *   encrypt <girdi> <cikti> --key <ANAHTAR>
 *   decrypt <girdi> <cikti> --key <ANAHTAR>
 *
 * Herhangi bir dosyayla calisir: kaynak dosyasi (.rb/.py/.java) ya da
 * derlenmis artefakt (.jar/.class). Cikti Base64 metindir.
 */
public final class Kripta {

    public static void main(String[] args) throws Exception {
        if (args.length < 4) {
            usage();
            System.exit(1);
        }
        String cmd = args[0];
        Path in = Path.of(args[1]);
        Path out = Path.of(args[2]);
        byte[] key = parseKey(args);

        switch (cmd) {
            case "encrypt" -> {
                byte[] data = Files.readAllBytes(in);
                String enc = Codec.encode(data, key);
                Files.writeString(out, enc, StandardCharsets.UTF_8);
                System.out.printf("Sifrelendi: %s -> %s (%d bayt, rot=%d)%n",
                        in, out, data.length, Codec.rotationFor(key));
            }
            case "decrypt" -> {
                String enc = Files.readString(in, StandardCharsets.UTF_8);
                byte[] data = Codec.decode(enc, key);
                Files.write(out, data);
                System.out.printf("Cozuldu: %s -> %s (%d bayt)%n", in, out, data.length);
            }
            default -> {
                System.err.println("Bilinmeyen komut: " + cmd);
                usage();
                System.exit(1);
            }
        }
    }

    private static byte[] parseKey(String[] args) {
        for (int i = 3; i < args.length - 1; i++) {
            if (args[i].equals("--key")) {
                return args[i + 1].getBytes(StandardCharsets.UTF_8);
            }
        }
        throw new IllegalArgumentException("--key <ANAHTAR> gerekli");
    }

    private static void usage() {
        System.out.println("""
            Kullanim:
              encrypt <girdi> <cikti> --key <ANAHTAR>
              decrypt <girdi> <cikti> --key <ANAHTAR>
            """);
    }
}
