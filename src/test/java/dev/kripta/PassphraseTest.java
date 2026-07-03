package dev.kripta;

import java.util.Arrays;

/**
 * Test runner'da tty yok -> System.console()==null; bu yuzden read() env
 * fallback yolunu test eder. KRIPTA_KEY set edilerek calistirilmali.
 */
public final class PassphraseTest {
    public static void main(String[] args) {
        int fail = 0;
        char[] got = Passphrase.read("Parola: ");
        if (!Arrays.equals(got, "envparola".toCharArray())) {
            System.out.println("✗ env fallback beklenen degeri vermedi: " + new String(got));
            fail++;
        } else {
            System.out.println("  ✓ KRIPTA_KEY env fallback çalışıyor");
        }
        if (fail != 0) System.exit(1);
    }
}
