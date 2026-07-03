package dev.kripta;

import java.security.SecureRandom;
import java.util.Arrays;

/**
 * Ust duzey dogrulama: round-trip, tamper tespiti, yanlis parola reddi,
 * baslik butunlugu, makine baglama. CryptoTest ile ortusur; burada kullanici
 * senaryosu olarak ozetlenir. build.sh bunu kosar.
 */
public final class SelfTest {
    static int pass = 0, total = 0;
    static void check(String name, boolean ok) {
        total++;
        if (ok) { pass++; System.out.println("  ✓ " + name); }
        else System.out.println("  ✗ " + name);
    }

    public static void main(String[] args) {
        SecureRandom rnd = new SecureRandom();

        for (int size : new int[]{0, 1, 15, 16, 1000, 65536}) {
            byte[] data = new byte[size]; rnd.nextBytes(data);
            byte[] ct = Crypto.encrypt(data, "anahtar!42".toCharArray(), null);
            byte[] back = Crypto.decrypt(ct, "anahtar!42".toCharArray(), null);
            check("round-trip size=" + size, Arrays.equals(data, back));
        }

        byte[] ct = Crypto.encrypt("gizli".getBytes(), "dogru".toCharArray(), null);
        check("yanlış parola reddi", sec(() -> Crypto.decrypt(ct, "yanlis".toCharArray(), null)));

        byte[] t = ct.clone(); t[t.length - 1] ^= 1;
        check("tamper tespiti", sec(() -> Crypto.decrypt(t, "dogru".toCharArray(), null)));

        byte[] h = ct.clone(); h[6] ^= 1;
        check("başlık (AAD) bütünlüğü", sec(() -> Crypto.decrypt(h, "dogru".toCharArray(), null)));

        byte[] bound = Crypto.encrypt("x".getBytes(), "p".toCharArray(), new byte[]{1,2,3});
        check("makineye mühürlü işaretli", Crypto.isBound(bound));
        check("yanlış makine reddi", sec(() -> Crypto.decrypt(bound, "p".toCharArray(), new byte[]{9})));

        System.out.printf("%n%d/%d test geçti%n", pass, total);
        if (pass != total) System.exit(1);
    }

    interface Run { void go(); }
    static boolean sec(Run r) {
        try { r.go(); return false; }
        catch (SecurityException e) { return true; }
        catch (Exception e) { return false; }
    }
}
