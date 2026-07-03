package dev.kripta;
import java.security.SecureRandom;
import java.util.Arrays;

/** Cesitli boyut ve anahtarlarda encode->decode == orijinal dogrulamasi. */
public final class SelfTest {
    public static void main(String[] args) {
        SecureRandom rnd = new SecureRandom();
        String[] keys = {"a", "M0untain!", "cok-uzun-bir-anahtar-1234567890"};
        int[] sizes = {0, 1, 15, 16, 1000, 65536};
        int pass = 0, total = 0;
        for (String k : keys) for (int s : sizes) {
            byte[] data = new byte[s]; rnd.nextBytes(data);
            byte[] key = k.getBytes();
            total++;
            byte[] back = Codec.decode(Codec.encode(data, key), key);
            if (Arrays.equals(data, back)) pass++;
            else System.out.println("FAIL key=" + k + " size=" + s);
        }
        System.out.printf("%d/%d test gecti%n", pass, total);
        if (pass != total) System.exit(1);
    }
}
