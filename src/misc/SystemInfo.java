package misc;

import java.io.PrintStream;

public class SystemInfo {

    public static void print(PrintStream out) {
        out.println("# System Information");
        out.print(gather());
        out.println("# -----------------------------------------------------------------------");
    }

    public static String gather() {
        StringBuilder sb = new StringBuilder();

        // JVM and Java version
        sb.append("  Java:      ").append(System.getProperty("java.version"))
          .append(" (").append(System.getProperty("java.vm.name")).append(")\n");

        // OS
        sb.append("  OS:        ").append(System.getProperty("os.name"))
          .append(" ").append(System.getProperty("os.version"))
          .append(" (").append(System.getProperty("os.arch")).append(")\n");

        // CPU cores
        sb.append("  CPU cores: ").append(Runtime.getRuntime().availableProcessors())
          .append(" (logical)\n");

        // JVM memory
        Runtime rt = Runtime.getRuntime();
        long maxMB = rt.maxMemory()   / (1024 * 1024);
        long totMB = rt.totalMemory() / (1024 * 1024);
        sb.append("  JVM RAM:   max=").append(maxMB).append(" MB")
          .append(", initial=").append(totMB).append(" MB\n");

        // Mac-specific cache/core info via sysctl
        if (System.getProperty("os.name").toLowerCase().contains("mac")) {
            sb.append(sysctlInfo("hw.perflevel0.physicalcpu",  "  P-cores:  "));
            sb.append(sysctlInfo("hw.perflevel1.physicalcpu",  "  E-cores:  "));
            sb.append(sysctlInfo("hw.memsize",                 "  RAM:      ", 1024*1024*1024, "GB"));
            sb.append(sysctlInfo("hw.perflevel0.l1icachesize", "  P L1i:    ", 1024, "KB"));
            sb.append(sysctlInfo("hw.perflevel0.l1dcachesize", "  P L1d:    ", 1024, "KB"));
            sb.append(sysctlInfo("hw.perflevel0.l2cachesize",  "  P L2:     ", 1024*1024, "MB"));
            sb.append(sysctlInfo("hw.perflevel1.l1icachesize", "  E L1i:    ", 1024, "KB"));
            sb.append(sysctlInfo("hw.perflevel1.l1dcachesize", "  E L1d:    ", 1024, "KB"));
            sb.append(sysctlInfo("hw.perflevel1.l2cachesize",  "  E L2:     ", 1024*1024, "MB"));
            sb.append(sysctlInfo("hw.cachelinesize",           "  Cacheline:", 1, "B"));
        }

        return sb.toString();
    }

    private static String sysctlRaw(String key) {
        try {
            Process p = Runtime.getRuntime().exec(new String[]{"sysctl", "-n", key});
            return new String(p.getInputStream().readAllBytes()).trim();
        } catch (Exception e) { return ""; }
    }

    private static String sysctlInfo(String key, String label) {
        String val = sysctlRaw(key);
        if (val.isEmpty()) return "";
        return label + " " + val + "\n";
    }

    private static String sysctlInfo(String key, String label, long divisor, String unit) {
        String val = sysctlRaw(key);
        if (val.isEmpty()) return "";
        try {
            long num = Long.parseLong(val) / divisor;
            return label + " " + num + " " + unit + "\n";
        } catch (NumberFormatException e) { return ""; }
    }
}