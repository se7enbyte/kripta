package dev.kripta;

import java.io.Console;
import java.util.Arrays;

/**
 * Parolayi guvenli edinir. Interaktif: gizli prompt (ekranda gorunmez).
 * Non-interaktif (pipe/CI): KRIPTA_KEY ortam degiskeni. Parola argv'de asla
 * gecmez -> ps ciktisinda ve shell gecmisinde gorunmez.
 */
public final class Passphrase {
    private Passphrase() {}

    public static char[] read(String prompt) {
        Console con = System.console();
        if (con != null) {
            return con.readPassword("%s", prompt);
        }
        String env = System.getenv("KRIPTA_KEY");
        if (env != null && !env.isEmpty()) {
            return env.toCharArray();
        }
        throw new IllegalStateException(
                "İnteraktif terminal yok ve KRIPTA_KEY tanımlı değil.");
    }

    public static char[] readConfirmed() {
        Console con = System.console();
        if (con == null) {
            return read("Parola: ");
        }
        while (true) {
            char[] a = con.readPassword("Parola: ");
            char[] b = con.readPassword("Parola (tekrar): ");
            boolean ok = a.length > 0 && Arrays.equals(a, b);
            Arrays.fill(b, '\0');
            if (ok) return a;
            Arrays.fill(a, '\0');
            System.out.println("Parolalar uyuşmadı ya da boş. Tekrar deneyin.");
        }
    }
}
