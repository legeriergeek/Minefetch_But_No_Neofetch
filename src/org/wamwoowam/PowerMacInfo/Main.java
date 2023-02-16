package org.wamwoowam.PowerMacInfo;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.management.ManagementFactory;
import com.sun.management.OperatingSystemMXBean;

import java.util.ArrayList;

public class Main extends JavaPlugin {
    private NeoParser neofetch;
    private Loadometer load;

    @Override
    public void onEnable() {

        // Initialize Neofetch Parser
        neofetch = new NeoParser();
        if (!neofetch.init(this)) {
            this.getLogger().severe("NeoParser Failed to Initialize! Is Neofetch installed?");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        // Initialize Neofetch Parser
        load = new Loadometer();
        if (!load.init(this)) {
            this.getLogger().severe("Loadometer Failed to Initialize!");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
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

        if (command.getName().equalsIgnoreCase("loadfetch")) {

            // Add MessageQueue so messages arent sent in a burst
            var MessageQueue = new ArrayList<String>();

            // Loadfetch Banner
            MessageQueue.add(String.format(ChatColor.BLUE + "--- %s%s%s---", ChatColor.WHITE, "LoadFetch ", ChatColor.BLUE));

            // Add CPU Load
            MessageQueue.add(ChatColor.BLUE + "CPU: " +ChatColor.WHITE + neofetch.getNeofetch("CPU"));
            MessageQueue.add(ChatColor.WHITE + load.barBuilder(load.getCPULoad(), false));

            // Add Memory Load
            MessageQueue.add(String.format((ChatColor.BLUE + "Memory: " + ChatColor.BOLD + ChatColor.WHITE + neofetch.getNeofetch("Memory"))));
            MessageQueue.add(ChatColor.WHITE + load.barBuilder(load.getMEMLoad(), false));

            // Add World Size
            MessageQueue.add("World size: " + Math.round(load.getWorldSize()) + " MB");

            // Add Server TPS
            MessageQueue.add("Server TPS: " + Math.round(TPS.getTPS()) + " TPS");

            String[] array = new String[MessageQueue.size()];
            MessageQueue.toArray(array);

            sender.sendMessage(array);

        }

        return true;
    }
}
