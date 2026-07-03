package dev.kripta;

/** Normal (debugger'siz) calismada debuggerPresent()==false olmali. */
public final class GuardTest {
    public static void main(String[] args) {
        boolean present = Guard.debuggerPresent();
        if (present) {
            System.out.println("✗ debugger yokken true döndü");
            System.exit(1);
        }
        System.out.println("  ✓ debugger yok, false döndü");
    }
}
