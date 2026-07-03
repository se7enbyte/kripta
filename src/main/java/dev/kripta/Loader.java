package dev.kripta;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarInputStream;
import java.util.jar.JarEntry;
import java.io.ByteArrayOutputStream;

/**
 * Sifreli bir JAR'i BELLEKTE cozup calistirir. Cozulmus baytlar diske
 * hicbir zaman yazilmaz; siniflar bellekten dogrudan yuklenir.
 *
 * Kullanim:
 *   java dev.kripta.Loader <sifreli.jar.enc> <AnaSinif> --key <ANAHTAR> [prog args...]
 *
 * Not: Anahtar burada komut satirindan geliyor. Gercek bir pakette anahtar
 * loader icine gomulu olur -> iste tam da bu yuzden "kirilamaz" degildir:
 * anahtar dagitilan kodun icindedir.
 */
public final class Loader {

    public static void main(String[] args) throws Exception {
        if (args.length < 4) {
            System.err.println("Kullanim: Loader <sifreli.jar.enc> <AnaSinif> --key <ANAHTAR> [args...]");
            System.exit(1);
        }
        Path encJar = Path.of(args[0]);
        String mainClass = args[1];
        byte[] key = null;
        int progArgsStart = args.length;
        for (int i = 2; i < args.length; i++) {
            if (args[i].equals("--key") && i + 1 < args.length) {
                key = args[i + 1].getBytes(StandardCharsets.UTF_8);
                progArgsStart = i + 2;
                break;
            }
        }
        if (key == null) throw new IllegalArgumentException("--key gerekli");

        // 1) Sifreli metni oku ve bellekte coz
        String enc = Files.readString(encJar, StandardCharsets.UTF_8);
        byte[] jarBytes = Codec.decode(enc, key);

        // 2) JAR icindeki tum siniflari bellege cikar
        Map<String, byte[]> classes = new HashMap<>();
        try (JarInputStream jis = new JarInputStream(new ByteArrayInputStream(jarBytes))) {
            JarEntry e;
            while ((e = jis.getNextJarEntry()) != null) {
                if (e.getName().endsWith(".class")) {
                    String name = e.getName().replace('/', '.').replaceAll("\\.class$", "");
                    classes.put(name, readAll(jis));
                }
            }
        }

        // 3) Bellekten yukleyen ClassLoader
        ClassLoader memLoader = new ClassLoader(Loader.class.getClassLoader()) {
            @Override
            protected Class<?> findClass(String name) throws ClassNotFoundException {
                byte[] b = classes.get(name);
                if (b == null) throw new ClassNotFoundException(name);
                return defineClass(name, b, 0, b.length);
            }
        };

        // 4) Ana sinifi calistir
        Class<?> cls = Class.forName(mainClass, true, memLoader);
        Method main = cls.getMethod("main", String[].class);
        String[] progArgs = new String[Math.max(0, args.length - progArgsStart)];
        System.arraycopy(args, progArgsStart, progArgs, 0, progArgs.length);
        main.invoke(null, (Object) progArgs);
    }

    private static byte[] readAll(java.io.InputStream in) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) != -1) bos.write(buf, 0, n);
        return bos.toByteArray();
    }
}
