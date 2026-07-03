package dev.kripta;

import java.security.SecureRandom;
import java.util.Arrays;

/** Crypto: round-trip, tamper, yanlis parola, yanlis makine, baslik butunlugu. */
public final class CryptoTest {
    static int pass = 0, fail = 0;
    static void check(String name, boolean ok) {
        if (ok) { pass++; System.out.println("  ✓ " + name); }
        else { fail++; System.out.println("  ✗ " + name); }
    }

    public static void main(String[] args) {
        SecureRandom rnd = new SecureRandom();

        // 1) round-trip cesitli boyutlarda (bind yok)
        for (int size : new int[]{0, 1, 15, 16, 1000, 65536}) {
            byte[] data = new byte[size]; rnd.nextBytes(data);
            byte[] ct = Crypto.encrypt(data, "parola123".toCharArray(), null);
            byte[] back = Crypto.decrypt(ct, "parola123".toCharArray(), null);
            check("round-trip size=" + size, Arrays.equals(data, back));
        }

        // 2) yanlis parola -> SecurityException
        byte[] ct = Crypto.encrypt("gizli".getBytes(), "dogru".toCharArray(), null);
        check("yanlis parola reddedilir", throwsSecurity(ct, "yanlis".toCharArray(), null));

        // 3) tamper: ciphertext'te bir bayt cevir -> hata
        byte[] tampered = ct.clone();
        tampered[tampered.length - 1] ^= 0x01;
        check("tamper (ct) reddedilir", throwsSecurity(tampered, "dogru".toCharArray(), null));

        // 4) baslik butunlugu: mem parametresini boz (AAD) -> hata
        byte[] hdrTampered = ct.clone();
        hdrTampered[6] ^= 0x01; // mem baytini degistir
        check("baslik (AAD) tamper reddedilir", throwsSecurity(hdrTampered, "dogru".toCharArray(), null));

        // 5) makine baglama: A ile sifrele, B pepper ile coz -> hata
        byte[] pepA = new byte[]{1,2,3,4};
        byte[] pepB = new byte[]{9,9,9,9};
        byte[] boundCt = Crypto.encrypt("gizli".getBytes(), "p".toCharArray(), pepA);
        check("isBound=true", Crypto.isBound(boundCt));
        check("dogru makine cozer", Arrays.equals("gizli".getBytes(),
                Crypto.decrypt(boundCt, "p".toCharArray(), pepA)));
        check("yanlis makine reddedilir", throwsSecurity(boundCt, "p".toCharArray(), pepB));

        System.out.printf("%nCrypto: %d geçti, %d kaldı%n", pass, fail);
        if (fail != 0) System.exit(1);
    }

    static boolean throwsSecurity(byte[] ct, char[] pass, byte[] pepper) {
        try { Crypto.decrypt(ct, pass, pepper); return false; }
        catch (SecurityException e) { return true; }
        catch (Exception e) { return false; }
    }
}
