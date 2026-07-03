package dev.kripta;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

/**
 * Sifreli JAR'i BELLEKTE cozup calistirir; cozulmus baytlar diske yazilmaz.
 *
 * Ek katman (per-class efemer sifreleme): JAR bellekte cozuldukten sonra her
 * .class ayri bir efemer AES-GCM anahtariyla yeniden sifrelenir; sinif yalnizca
 * findClass ile ilk yuklendigi an cozulur. Boylece tum program ayni anda acik
 * durmaz -> naif bellek taramasi zorlasir. (Efemer anahtar da bellekte oldugu
 * icin bu kararli bir tersine muhendisi durdurmaz; bkz. README.)
 */
public final class Loader {
    private Loader() {}

    public static void run(String encPath, String mainClass, char[] passphrase,
                           String[] progArgs) throws Exception {
        Guard.assertNoDebugger();

        byte[] container = Files.readAllBytes(Path.of(encPath));
        byte[] pepper = Crypto.isBound(container) ? MachineKey.fingerprint() : null;
        byte[] jarBytes = Crypto.decrypt(container, passphrase, pepper);

        // Efemer anahtar: bu calisma icin rastgele, sadece bellekte
        KeyGenerator kg = KeyGenerator.getInstance("AES");
        kg.init(256);
        SecretKey ephemeral = kg.generateKey();
        SecureRandom rng = new SecureRandom();

        Map<String, byte[]> sealed = new HashMap<>();   // sinif adi -> nonce||ct
        try (JarInputStream jis = new JarInputStream(new ByteArrayInputStream(jarBytes))) {
            JarEntry e;
            while ((e = jis.getNextJarEntry()) != null) {
                if (!e.getName().endsWith(".class")) continue;
                String name = e.getName().replace('/', '.').replaceAll("\\.class$", "");
                byte[] raw = readAll(jis);
                sealed.put(name, seal(raw, ephemeral, rng));
                java.util.Arrays.fill(raw, (byte) 0);
            }
        }
        java.util.Arrays.fill(jarBytes, (byte) 0);

        ClassLoader memLoader = new ClassLoader(Loader.class.getClassLoader()) {
            @Override
            protected Class<?> findClass(String name) throws ClassNotFoundException {
                byte[] blob = sealed.get(name);
                if (blob == null) throw new ClassNotFoundException(name);
                try {
                    byte[] b = unseal(blob, ephemeral);
                    return defineClass(name, b, 0, b.length);
                } catch (Exception ex) {
                    throw new ClassNotFoundException(name, ex);
                }
            }
        };

        Class<?> cls = Class.forName(mainClass, true, memLoader);
        Method main = cls.getMethod("main", String[].class);
        main.invoke(null, (Object) progArgs);
    }

    private static byte[] seal(byte[] data, SecretKey key, SecureRandom rng) throws Exception {
        byte[] nonce = new byte[12];
        rng.nextBytes(nonce);
        Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
        c.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, nonce));
        byte[] ct = c.doFinal(data);
        byte[] out = new byte[12 + ct.length];
        System.arraycopy(nonce, 0, out, 0, 12);
        System.arraycopy(ct, 0, out, 12, ct.length);
        return out;
    }

    private static byte[] unseal(byte[] blob, SecretKey key) throws Exception {
        byte[] nonce = java.util.Arrays.copyOfRange(blob, 0, 12);
        byte[] ct = java.util.Arrays.copyOfRange(blob, 12, blob.length);
        Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
        c.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, nonce));
        return c.doFinal(ct);
    }

    private static byte[] readAll(InputStream in) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) != -1) bos.write(buf, 0, n);
        return bos.toByteArray();
    }
}
