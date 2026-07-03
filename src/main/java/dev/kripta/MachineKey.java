package dev.kripta;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HexFormat;
import java.util.List;

/**
 * Makineye ozgu deterministik bir parmak izi uretir. Bu parmak izi Argon2id'e
 * "pepper" olarak girer -> sifreli dosya sadece ayni makinede cozulur.
 *
 * DURUSTLUK: Parmak izi makinede turetildigi icin taklit edilebilir. Bu,
 * dosyayi baska makineye kopyalayip calistirmayi durdurur; kararli birini
 * durdurmaz (bkz. README).
 */
public final class MachineKey {
    private MachineKey() {}

    // Guvenli varsayilan: fingerprint() henuz cagrilmadiysa "degenerate/bilinmiyor"
    // kabul et. fingerprint() calisinca gercek deger (macs.isEmpty()) ile guncellenir.
    private static boolean degenerate = true;

    public static byte[] fingerprint() {
        List<String> parts = new ArrayList<>();
        List<String> macs = new ArrayList<>();
        try {
            for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (ni.isLoopback() || ni.isVirtual() || !ni.isUp()) continue;
                byte[] mac = ni.getHardwareAddress();
                if (mac != null && mac.length > 0) macs.add(HexFormat.of().formatHex(mac));
            }
        } catch (Exception ignore) {
            // ag arayuzleri okunamadi; fallback devrede
        }
        Collections.sort(macs);
        degenerate = macs.isEmpty();
        parts.addAll(macs);

        try {
            parts.add(InetAddress.getLocalHost().getHostName());
        } catch (Exception ignore) {
            // hostname yok
        }
        parts.add(System.getProperty("os.name", ""));
        parts.add(System.getProperty("os.arch", ""));

        String joined = String.join("|", parts);
        try {
            return MessageDigest.getInstance("SHA-256")
                    .digest(joined.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 yok: " + e.getMessage(), e);
        }
    }

    /** fingerprint() en az bir kez cagrildiktan sonra anlamli. */
    public static boolean isDegenerate() {
        return degenerate;
    }
}
