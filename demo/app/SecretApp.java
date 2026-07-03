public class SecretApp {
    public static void main(String[] args) {
        String who = args.length > 0 ? args[0] : "dunya";
        System.out.println("Gizli algoritma calisti! Merhaba, " + who);
        System.out.println("2 + 2 = " + secret(2, 2));
    }
    static int secret(int a, int b) { return (a * 7 + b) ^ 42; }
}
