package nya.clockwork04.Minefetch;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.plugin.Plugin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class NeoParser {

    boolean debug = false;

    ChatColor neocolor;

    private final List<String> neofetch;
    private final HashMap<String, String> neomap;

    public NeoParser() {
        this.neofetch = new ArrayList<>();
        this.neomap = new HashMap<>();
    }

    public boolean init(Plugin plugin) {
        plugin.getLogger().info("Getting simulated neofetch output (custom Java-based parser)");

        readNeofetch();
        parseNeofetch();
        distroDetect();

        var scheduler = Bukkit.getScheduler();
        scheduler.runTaskTimerAsynchronously(plugin, this::updateNeofetch, 0L, 20L * 60L);

        plugin.getLogger().info("NeoParser Initialized with fake neofetch! Nya~ >w<");
        return true;
    }

    private void distroDetect() {
        String distroString = neomap.get("OS");
        if (distroString.contains("Arch")) {
            neocolor = ChatColor.AQUA;
        } else if (distroString.contains("Debian")) {
            neocolor = ChatColor.RED;
        } else if (distroString.contains("Gentoo")) {
            neocolor = ChatColor.DARK_PURPLE;
        } else {
            neocolor = ChatColor.BLUE;
        }
    }

    public ChatColor getNeocolor() {
        return this.neocolor;
    }

    public void updateNeofetch() {
        readNeofetch();
        parseNeofetch();
    }

    public void readNeofetch() {
        neofetch.clear();

        String user = System.getProperty("user.name");
        String hostname = "unknown";
        try {
            hostname = InetAddress.getLocalHost().getHostName();
        } catch (Exception ignored) {}

        neofetch.add(user + "@" + hostname);
        neofetch.add("OS: " + System.getProperty("os.name"));
        neofetch.add("Kernel: " + System.getProperty("os.version"));
        neofetch.add("Java: " + System.getProperty("java.version"));

        long uptimeSeconds = java.lang.management.ManagementFactory.getRuntimeMXBean().getUptime() / 1000;
        long hours = uptimeSeconds / 3600;
        long minutes = (uptimeSeconds % 3600) / 60;
        long seconds = uptimeSeconds % 60;
        neofetch.add("Uptime: " + String.format("%02dh %02dm %02ds", hours, minutes, seconds));

        long totalMem = Runtime.getRuntime().totalMemory() / (1024 * 1024);
        long freeMem = Runtime.getRuntime().freeMemory() / (1024 * 1024);
        long usedMem = totalMem - freeMem;
        neofetch.add("Memory: " + usedMem + "MiB / " + totalMem + "MiB");

        if (debug) {
            for (String line : neofetch) {
                Bukkit.getLogger().info(line);
            }
        }
    }

    public void parseNeofetch() {
        neomap.clear();
        neomap.put("hostname", neofetch.get(0));
        for (String outputLine : this.neofetch) {
            int colonIndex = outputLine.indexOf(":");
            if (colonIndex != -1) {
                String key = outputLine.substring(0, colonIndex).trim();
                String value = outputLine.substring(colonIndex + 1).trim();
                neomap.put(key, value);
            }
        }

        if (debug) {
            Bukkit.getLogger().info("Neofetch Parsed!");
        }
    }

    public String getNeofetch(String value) {
        return neomap.get(value);
    }
}
