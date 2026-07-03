package dev.kripta;

import java.util.Base64;

/**
 * Katmanlı geri-döndürülebilir kodlama.
 *
 * Katman sırası (encode):  bytes -> XOR(key) -> rotateLeft(r) -> Base64
 * Ters sıra    (decode):   Base64 -> rotateRight(r) -> XOR(key) -> bytes
 *
 * ÖNEMLI: Bu bir GIZLILIK/koruma degil, CAYDIRICILIK katmanidir.
 * Anahtar cozucu ile birlikte dagitildigi icin kararli biri geri cevirebilir.
 * Gercek koruma icin degerli mantigi sunucuda tutun (bkz. README).
 */
public final class Codec {

    private Codec() {}

    /** Anahtardan deterministik olarak 0..7 arası bir rotasyon miktarı türetir. */
    static int rotationFor(byte[] key) {
        int sum = 0;
        for (byte b : key) sum += (b & 0xFF);
        return sum % 8; // 0 ise bu katman no-op olur, sorun degil
    }

    private static byte rotateLeft(byte b, int r) {
        int v = b & 0xFF;
        return (byte) (((v << r) | (v >>> (8 - r))) & 0xFF);
    }

    private static byte rotateRight(byte b, int r) {
        int v = b & 0xFF;
        return (byte) (((v >>> r) | (v << (8 - r))) & 0xFF);
    }

    /** Ham baytları şifreleyip Base64 metin döndürür. */
    public static String encode(byte[] data, byte[] key) {
        if (key.length == 0) throw new IllegalArgumentException("Anahtar bos olamaz");
        int r = rotationFor(key);
        byte[] out = new byte[data.length];
        for (int i = 0; i < data.length; i++) {
            byte x = (byte) (data[i] ^ key[i % key.length]); // katman 1: XOR
            out[i] = (r == 0) ? x : rotateLeft(x, r);          // katman 2: rotasyon
        }
        return Base64.getEncoder().encodeToString(out);        // katman 3: Base64
    }

    /** Base64 metni çözüp ham baytlara döndürür. */
    public static byte[] decode(String encoded, byte[] key) {
        if (key.length == 0) throw new IllegalArgumentException("Anahtar bos olamaz");
        int r = rotationFor(key);
        byte[] in = Base64.getDecoder().decode(encoded);
        byte[] out = new byte[in.length];
        for (int i = 0; i < in.length; i++) {
            byte x = (r == 0) ? in[i] : rotateRight(in[i], r);
            out[i] = (byte) (x ^ key[i % key.length]);
        }
        return out;
    }
}
