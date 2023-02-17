# Minefetch
 Spigot plugin that parses the output from neofetch, the current JVM to display information, and load about the current system.

 The code for this plugin is downright terrible, I barely know Java and most of the more complex math I asked ChatGPT to do but its universal as far as I can tell and does indeed work on anything `neofetch --off --stdout` does.

 Plugin heavily based on [PowerMacInfo](https://github.com/WamWooWam/PowerMacInfo)  in fact this was supposed to be a port to x86 machines, but it mostly uses "Windfarm" (A PowerMac G5 exclusive kernel driver) and OpenFirmware so parsing neofetch was easier to implement, and I'm too smooth brain to rewrite it in Java.

<img style="width: 75%" src=screenshots/minefetch1.png>

## Requirements
 - A Minecraft server running on Linux
 - Java 11+
 - Spigot compatible server (I used PaperMC)
 - Minecraft 1.12+
 - Neofetch

## Commands
 - `/neofetch` Parsed output from neofetch printed to the sender.
 - `/loadfetch` Output mostly from the JVM about CPU and RAM usage with cool graphs.