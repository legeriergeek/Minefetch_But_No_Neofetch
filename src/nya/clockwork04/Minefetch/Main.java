package nya.clockwork04.Minefetch;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.management.ManagementFactory;
import com.sun.management.OperatingSystemMXBean;

import java.text.DecimalFormat;
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
        // Initialize Loadometer
        load = new Loadometer();
        if (!load.init(this)) {
            this.getLogger().severe("Loadometer Failed to Initialize!");
            Bukkit.getPluginManager().disablePlugin(this);
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
            ChatColor neocolor = neofetch.getNeocolor();

            //Fetch hostname by always assuming it's the first line in the neofetch.. this is stupid
            MessageQueue.add(String.format(neocolor + "--- %s%s%s---", ChatColor.WHITE, neofetch.getNeofetch("hostname"), neocolor));

            MessageQueue.add(String.format((neocolor + "OS: " + ChatColor.BOLD + ChatColor.WHITE + neofetch.getNeofetch("OS"))));
            //It might not have a HOST line in the neofetch if it cant grab the model, cant have it crashing can we
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

            String usage = (ChatColor.BOLD + "USED: ");

            // Loadfetch Banner
            MessageQueue.add(String.format(ChatColor.BLUE + "--- %s%s%s---", ChatColor.WHITE, "Loadfetch ", ChatColor.BLUE));

            // Add CPU Load
            MessageQueue.add(ChatColor.BLUE + "" + ChatColor.BOLD + "CPU: " +ChatColor.WHITE + neofetch.getNeofetch("CPU"));
            MessageQueue.add(ChatColor.BLUE + usage +ChatColor.WHITE + load.barBuilder(load.getCPULoad()));

            // Add Memory Load
            MessageQueue.add(ChatColor.BLUE + "" + ChatColor.BOLD + "RAM: " + ChatColor.WHITE + neofetch.getNeofetch("Memory"));
            MessageQueue.add(ChatColor.BLUE + usage + ChatColor.WHITE + load.barBuilder(load.getMEMLoad())  + " (JVM)");

            // Add World Size
            MessageQueue.add(ChatColor.BOLD + "World size: " + ChatColor.RESET + Math.round(load.getWorldSize()) + " MB");

            //Calc TPS color and round to 2 decimal places
            ChatColor tpsColor;
            double tps = TPS.getTPS();
            DecimalFormat df = new DecimalFormat("0.00");

            if (tps >= 18.0){
                tpsColor = ChatColor.GREEN;
            } else if (tps <= 13.0){
                tpsColor = ChatColor.RED;
            } else {
                tpsColor = ChatColor.YELLOW;
            }

            // Add Server TPS
            MessageQueue.add(ChatColor.BOLD + "Server TPS: " + tpsColor + df.format(tps) + " TPS");

            String[] array = new String[MessageQueue.size()];
            MessageQueue.toArray(array);

            sender.sendMessage(array);

        }
        return true;
    }
}
