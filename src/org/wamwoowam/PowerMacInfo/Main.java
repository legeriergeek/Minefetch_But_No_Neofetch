package org.wamwoowam.PowerMacInfo;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.management.ManagementFactory;
import com.sun.management.OperatingSystemMXBean;

import java.text.NumberFormat;
import java.util.ArrayList;

public class Main extends JavaPlugin {
    private InfoCollector info;
    private NeoParser neofetch;

    @Override
    public void onEnable() {

        // Initialize PowerMacInfo InfoCollector
        info = new InfoCollector();
        if (!info.init(this)) {
            this.getLogger().severe("This plugin only works on Power Macintosh platforms under Linux.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        // Initialize Neofetch Parser
        neofetch = new NeoParser();
        if (!neofetch.init(this)) {
            this.getLogger().severe("NeoParser Failed to Initialize! Is Neofetch installed?");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        this.getLogger().info("Hello! This is a " + info.getCPU());

        var cpus = info.getCPUs();
        for (String cpu : cpus)
            this.getLogger().info("CPU" + cpu);

        var memoryBanks = info.getMemoryBanks();
        for (String mem : memoryBanks)
            this.getLogger().info("RAM" + mem);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // /system command
        if (command.getName().equalsIgnoreCase("system")) {

            var list = new ArrayList<String>();
            list.add(String.format("--- %s%s%s ---", ChatColor.BOLD, "MineInfo", ChatColor.RESET));
            list.add(String.format("%sCPU:%s %s", ChatColor.BOLD, ChatColor.RESET, info.getCPU()));
            list.add(String.format("%sRAM:%s %s", ChatColor.BOLD, ChatColor.RESET, info.getMemory()));

            var gpus = info.getGPUs();
            if (gpus.length > 0) {
                if (gpus.length == 1) {
                    list.add(String.format("%sGPU:%s %s", ChatColor.BOLD, ChatColor.RESET, gpus[0]));
                } else {
                    for (int i = 0; i < gpus.length; i++) {
                        list.add(String.format("%sGPU%d:%s %s", ChatColor.BOLD, i, ChatColor.RESET, gpus[i]));
                    }
                }
            }

            list.add(String.format("%sDisk:%s %s", ChatColor.BOLD, ChatColor.RESET, info.getDisk()));
            //x86: Serial? what serial >w<
            // list.add(String.format("%sSerial:%s %s", ChatColor.BOLD, ChatColor.RESET, info.getSerialNumber()));
            list.add(String.format("%sTemp:%s %s", ChatColor.BOLD, ChatColor.RESET, info.getTemps()));

            String[] array = new String[list.size()];
            list.toArray(array);

            sender.sendMessage(array);
        }

        // /neofetch command
        if (command.getName().equalsIgnoreCase("neofetch")) {
            if (neofetch.getNeofetch("OS") == null){
                sender.sendMessage(ChatColor.RED + "Neofetch Failed!");
                this.getLogger().severe("Neofetch Failed, called by: " + sender.getName());
                return false;
            }

            OperatingSystemMXBean osBean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);
            double cpuUsage = osBean.getSystemCpuLoad() * 100;

            Runtime runtime = Runtime.getRuntime();
            long allocatedMemory = runtime.totalMemory();

            var MessageQueue = new ArrayList<String>();

            //TODO: Change NeoColor based on host distro.. like Neofetch.
            ChatColor neocolor = ChatColor.RED;

            //Fetch hostname by always assuming it's the first line in the neofetch.. this is stupid
            MessageQueue.add(String.format("--- %s%s%s---", ChatColor.WHITE, neofetch.getNeofetch("hostname"), ChatColor.RESET));

            MessageQueue.add(String.format((neocolor + "OS: " + ChatColor.BOLD + ChatColor.WHITE + neofetch.getNeofetch("OS"))));
            if(neofetch.getNeofetch("Host") != null){
                MessageQueue.add(String.format((neocolor + "Host: " + ChatColor.BOLD + ChatColor.WHITE + neofetch.getNeofetch("Host"))));
            }
            MessageQueue.add(String.format((neocolor + "Kernel: " + ChatColor.BOLD + ChatColor.WHITE + neofetch.getNeofetch("Kernel"))));
            MessageQueue.add(String.format((neocolor + "CPU: " + ChatColor.BOLD + ChatColor.WHITE + neofetch.getNeofetch("CPU") + " (" + Math.round(cpuUsage) + "%%)")));
            MessageQueue.add(String.format((neocolor + "GPU: " + ChatColor.BOLD + ChatColor.WHITE + neofetch.getNeofetch("GPU"))));
            MessageQueue.add(String.format((neocolor + "Memory: " + ChatColor.BOLD + ChatColor.WHITE + neofetch.getNeofetch("Memory") + " (" + (allocatedMemory / (1024 * 1024)) + " MiB Allocated)")));
            MessageQueue.add(String.format((neocolor + "Uptime: " + ChatColor.BOLD + ChatColor.WHITE + neofetch.getNeofetch("Uptime"))));

            String[] array = new String[MessageQueue.size()];
            MessageQueue.toArray(array);

            sender.sendMessage(array);

        }

        return true;
    }
}
