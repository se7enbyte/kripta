package dev.kripta;

import org.bouncycastle.crypto.generators.Argon2BytesGenerator;
import org.bouncycastle.crypto.params.Argon2Parameters;

import javax.crypto.AEADBadTagException;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Arrays;

/**
 * Kimligi-dogrulanmis sifreleme cekirdegi: AES-256-GCM + Argon2id.
 *
 * Anahtar = Argon2id(parola, salt, secret=pepper). pepper genelde makine
 * parmak izidir (bkz. MachineKey); null/bos ise dosya makineye baglanmaz.
 *
 * ONEMLI: Kod istemcide calisirsa bir an bellekte cozulur; mutlak koruma
 * degildir (bkz. README). Bu katman kopyalamayi ve otomatik analizi durdurur.
 */
public final class Crypto {
    private Crypto() {}

    static final byte[] MAGIC = {'K', 'R', 'P', 'T'};
    static final byte VERSION = 2;
    static final byte KDF_ARGON2ID = 1;
    static final int MEM_KIB = 65536;
    static final int ITERATIONS = 3;
    static final int PARALLELISM = 4;
    static final int KEY_LEN = 32;
    static final int SALT_LEN = 16;
    static final int NONCE_LEN = 12;
    static final int TAG_BITS = 128;
    static final int HEADER_LEN = 44;

    private static final SecureRandom RNG = new SecureRandom();

    public static byte[] encrypt(byte[] plaintext, char[] passphrase, byte[] pepper) {
        boolean bound = pepper != null && pepper.length > 0;
        byte[] salt = new byte[SALT_LEN];
        byte[] nonce = new byte[NONCE_LEN];
        RNG.nextBytes(salt);
        RNG.nextBytes(nonce);
        byte[] header = buildHeader(bound, salt, nonce);
        byte[] key = deriveKey(passphrase, salt, bound ? pepper : null,
                MEM_KIB, ITERATIONS, PARALLELISM);
        try {
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"),
                    new GCMParameterSpec(TAG_BITS, nonce));
            c.updateAAD(header);
            byte[] ct = c.doFinal(plaintext);
            byte[] out = new byte[header.length + ct.length];
            System.arraycopy(header, 0, out, 0, header.length);
            System.arraycopy(ct, 0, out, header.length, ct.length);
            return out;
        } catch (Exception e) {
            throw new RuntimeException("Şifreleme başarısız: " + e.getMessage(), e);
        } finally {
            Arrays.fill(key, (byte) 0);
        }
    }

    public static byte[] decrypt(byte[] container, char[] passphrase, byte[] pepper) {
        if (container.length < HEADER_LEN)
            throw new IllegalArgumentException("Geçersiz dosya (çok kısa).");
        for (int i = 0; i < 4; i++)
            if (container[i] != MAGIC[i])
                throw new IllegalArgumentException("Geçersiz dosya (KRPT imzası yok).");
        if (container[4] != VERSION)
            throw new IllegalArgumentException("Desteklenmeyen sürüm: " + container[4]);

        boolean bound = container[15] == 1;
        byte[] header = Arrays.copyOfRange(container, 0, HEADER_LEN);
        byte[] salt = Arrays.copyOfRange(container, 16, 32);
        byte[] nonce = Arrays.copyOfRange(container, 32, 44);
        byte[] ct = Arrays.copyOfRange(container, HEADER_LEN, container.length);

        byte[] key = deriveKey(passphrase, salt, bound ? pepper : null,
                MEM_KIB, ITERATIONS, PARALLELISM);
        try {
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"),
                    new GCMParameterSpec(TAG_BITS, nonce));
            c.updateAAD(header);
            return c.doFinal(ct);
        } catch (AEADBadTagException e) {
            throw new SecurityException(bound
                    ? "Çözme başarısız: yanlış parola, yetkisiz makine ya da dosya kurcalanmış."
                    : "Çözme başarısız: yanlış parola ya da dosya kurcalanmış.");
        } catch (Exception e) {
            throw new RuntimeException("Çözme hatası: " + e.getMessage(), e);
        } finally {
            Arrays.fill(key, (byte) 0);
        }
    }

    public static boolean isBound(byte[] container) {
        return container.length > 15 && container[15] == 1;
    }

    private static byte[] buildHeader(boolean bound, byte[] salt, byte[] nonce) {
        byte[] h = new byte[HEADER_LEN];
        System.arraycopy(MAGIC, 0, h, 0, 4);
        h[4] = VERSION;
        h[5] = KDF_ARGON2ID;
        putInt(h, 6, MEM_KIB);
        putInt(h, 10, ITERATIONS);
        h[14] = (byte) PARALLELISM;
        h[15] = (byte) (bound ? 1 : 0);
        System.arraycopy(salt, 0, h, 16, SALT_LEN);
        System.arraycopy(nonce, 0, h, 32, NONCE_LEN);
        return h;
    }

    private static byte[] deriveKey(char[] passphrase, byte[] salt, byte[] pepper,
                                    int memKib, int iters, int par) {
        Argon2Parameters.Builder pb = new Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
                .withVersion(Argon2Parameters.ARGON2_VERSION_13)
                .withSalt(salt)
                .withMemoryAsKB(memKib)
                .withIterations(iters)
                .withParallelism(par);
        if (pepper != null && pepper.length > 0) pb.withSecret(pepper);
        Argon2BytesGenerator gen = new Argon2BytesGenerator();
        gen.init(pb.build());
        byte[] key = new byte[KEY_LEN];
        gen.generateBytes(passphrase, key);
        return key;
    }

    private static void putInt(byte[] b, int off, int v) {
        b[off] = (byte) (v >>> 24);
        b[off + 1] = (byte) (v >>> 16);
        b[off + 2] = (byte) (v >>> 8);
        b[off + 3] = (byte) v;
    }
}
