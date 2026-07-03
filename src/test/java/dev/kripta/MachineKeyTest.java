package dev.kripta;

import java.util.Arrays;

/** MachineKey: parmak izi deterministik ve 32 bayt olmali. */
public final class MachineKeyTest {
    public static void main(String[] args) {
        byte[] a = MachineKey.fingerprint();
        byte[] b = MachineKey.fingerprint();
        int fail = 0;
        if (a.length != 32) { System.out.println("✗ uzunluk 32 degil: " + a.length); fail++; }
        else System.out.println("  ✓ uzunluk 32");
        if (!Arrays.equals(a, b)) { System.out.println("✗ deterministik degil"); fail++; }
        else System.out.println("  ✓ deterministik");
        boolean allZero = true;
        for (byte x : a) if (x != 0) { allZero = false; break; }
        if (allZero) { System.out.println("✗ parmak izi tamamen sıfır"); fail++; }
        else System.out.println("  ✓ sıfır değil");
        System.out.println("isDegenerate=" + MachineKey.isDegenerate());
        if (fail != 0) System.exit(1);
    }
}
