package org.wamwoowam.PowerMacInfo;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class NeoParser {

    //x86 Debugging
    boolean debug = true;

    private final List<String> neofetch;
    private final HashMap neomap;

    public NeoParser() {
        this.neofetch = new ArrayList<>();
        this.neomap = new HashMap<>();
    }


    public boolean init(Plugin plugin) {
        readNeofetch();
        parseNeofetch();

        //Scheduled to rerun and reparse every minutes since uptime should.. update.. every once in a while
        //Bukkit scheduler has.. cursed syntax, below is an Initial Delay of 0 Ticks, then 20 Ticks times 60 which means this task runs every minute (60 Seconds)
        var scheduler = Bukkit.getScheduler();
        scheduler.runTaskTimerAsynchronously(plugin, this::updateNeofetch, 0L, 20L * 60L);
        //i mean i guess it makes sense.. this is a minecraft plugin and minecraft uses ticks.. funny this is required though to get real time
        return true;
    }

    //kill me
    public void updateNeofetch(){
        readNeofetch();
        parseNeofetch();
    }

    public void readNeofetch() {
        try {
            neofetch.clear();

            Process process = Runtime.getRuntime().exec("neofetch --off --stdout");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;

            while ((line = reader.readLine()) != null) {

                //x86 Debugging
                if (debug) {
                    Bukkit.getLogger().info(line);
                }

                neofetch.add(line);
            }
            //x86 Debugging
            if (debug) {
                Bukkit.getLogger().info("Neofetch Read!");
            }

        } catch (IOException e) {
            throw new RuntimeException();
        }
    }

    public void parseNeofetch(){
        neomap.put("hostname", neofetch.get(0));
        for (String outputLine : this.neofetch) {
            int colonIndex = outputLine.indexOf(":");
            if (colonIndex != -1) {
                String key = outputLine.substring(0, colonIndex).trim();
                String value = outputLine.substring(colonIndex + 1).trim();
                neomap.put(key, value);

//            } else if(index == 0){
//                neomap.put("hostname", outputLine);
//
//                //x86 Debugging
//                if (debug){Bukkit.getLogger().info("Found Hostname! " + outputLine + "\"");}

            }
        }
        //x86 Debugging
        if (debug){Bukkit.getLogger().info("Neofetch Parsed!");}
    }

    public String getNeofetch(String value) {
        return neomap.get(value).toString();
    }
}
