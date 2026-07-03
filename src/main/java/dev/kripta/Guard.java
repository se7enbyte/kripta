package dev.kripta;

import java.lang.management.ManagementFactory;
import java.util.List;

/**
 * Basit anti-tamper: JVM'e bir debugger/agent takili mi diye bakar.
 *
 * DURUSTLUK: Bu kontrol atlatilabilir (JVM argumanlari gizlenebilir, bytecode
 * yamanabilir). Amaci gundelik kurcalamayi ve otomatik araclari caydirmaktir,
 * kararli birini durdurmak degil (bkz. README).
 */
public final class Guard {
    private Guard() {}

    public static boolean debuggerPresent() {
        try {
            List<String> jvmArgs = ManagementFactory.getRuntimeMXBean().getInputArguments();
            for (String a : jvmArgs) {
                String s = a.toLowerCase();
                if (s.contains("-agentlib:jdwp") || s.contains("-xrunjdwp")
                        || s.contains("-xdebug") || s.contains("-agentpath")) {
                    return true;
                }
            }
        } catch (Exception ignore) {
            // MXBean okunamadi -> temkinli davranmayip false don
        }
        return false;
    }

    public static void assertNoDebugger() {
        if (debuggerPresent()) {
            System.err.println("Güvenlik: debugger/agent tespit edildi, çalışma reddedildi.");
            System.exit(3);
        }
    }
}
