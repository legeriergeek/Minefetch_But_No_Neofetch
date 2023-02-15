package org.wamwoowam.PowerMacInfo;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.json.JSONObject;

import java.io.File;
import java.io.BufferedReader;
import java.io.FileReader;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;

public class InfoCollector {
    private static final String LOOKUP_API_FORMAT = "https://di-api.reincubate.com/v1/apple-serials/%s/";

    //x86: /proc/device-tree seems to be OpenFirmware on PowerPC
    //private static final Path DEVICE_TREE = Path.of("/proc/device-tree");

    //x86: WINDFARM is a PowerPC (G5) exclusive kernel driver.. a lot relies on this
    //private static final Path WINDFARM = Path.of("/sys/devices/platform/windfarm.0");
    private final List<CPUInfo> cpus;
    private final List<MemoryBankInfo> memBanks;
    private final List<String> gpus;
    private final List<SensorInfo> sensors;
    private String displayModel;
    private String diskCache;

    private boolean hasWindfarm;

    private int addressCells;
    private int sizeCells;

    private boolean debug;

    public InfoCollector() {
        this.cpus = new ArrayList<>();
        this.memBanks = new ArrayList<>();
        this.gpus = new ArrayList<>();
        this.sensors = new ArrayList<>();
    }

    public boolean init(Plugin plugin) {
//        if (!Files.exists(DEVICE_TREE)) return false;

        //hasWindfarm = Files.exists(WINDFARM);

        //x86 Debugging
        debug = true;

        //x86 Debugging
        if (debug) {Bukkit.getLogger().info("Initial Initialization Passed!");}


        try {
//            addressCells = ByteBuffer.wrap(Files.readAllBytes(DEVICE_TREE.resolve("#address-cells"))).getInt();
//            sizeCells = ByteBuffer.wrap(Files.readAllBytes(DEVICE_TREE.resolve("#size-cells"))).getInt();

            //x86 Debugging
            if (debug) {Bukkit.getLogger().info("Address Cells Passed!");}

            readCPUInfo();

            //x86 Debugging
            if (debug) {Bukkit.getLogger().info("CPU Init Passed!");}

            readMemoryInfo();

            //x86 Debugging
            if (debug) {Bukkit.getLogger().info("Memory init Passed!");}

            readDiskInfo();

            //x86 Debugging
            if (debug) {Bukkit.getLogger().info("Disk init Passed!");}

            readGPUInfo();

            //x86 Debugging
            if (debug) {Bukkit.getLogger().info("GPU init Passed!");}

            //x86 Debugging
            if (debug) {Bukkit.getLogger().info("Info Collection Passed!");}

            var scheduler = Bukkit.getScheduler();
            // updates disk stats every 15 seconds on a separate thread
            scheduler.runTaskTimerAsynchronously(plugin, this::readDiskInfo, 0L, 20L * 15L);
            // updates sensors every second on a separate thread
            // scheduler.runTaskTimerAsynchronously(plugin, this::readSensors, 0L, 20L);
        } catch (IOException e) {
            return false;
        }

//        try {
//            var client = HttpClient.newHttpClient();
//            var request = HttpRequest.newBuilder(URI.create(String.format(LOOKUP_API_FORMAT, getSerialNumber()))).timeout(Duration.ofSeconds(5)).build();
//
//            client.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenApply(response -> {
//                if (response.statusCode() != 200)
//                    throw new CompletionException(new Exception("Invalid response code!"));
//                return response;
//            }).thenApply(HttpResponse::body).thenAccept(body -> {
//                var json = new JSONObject(body);
//                displayModel = json.getJSONObject("configurationCode").getString("skuHint");
//            });
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }

        return true;
    }

//    public String getModel() {
//        try {
//            return clean(Files.readString(DEVICE_TREE.resolve("model")));
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//    }

//    public String getDisplayModel() {
//        return displayModel;
//    }

//    public String getSerialNumber() {
//        try {
//            var rawSerial = Files.readAllBytes(DEVICE_TREE.resolve("serial-number"));
//            int x = 0;
//            while (rawSerial[x] != 0) x++;
//
//            while (rawSerial[x] == 0) x++;
//
//            int y = x;
//            while (rawSerial[y] != 0) y++;
//
//            return new String(rawSerial, x, y - x);
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//    }

    public String getCPU() {
        return String.format("%dx %s @ %s", cpus.size(), cpus.get(0).getName(), cpus.get(0).getClock());
    }

    public String[] getCPUs() {
        var array = new String[cpus.size()];
        for (int i = 0; i < cpus.size(); i++) {
            var cpu = cpus.get(i);
            array[i] = cpu.getIndex() + ": " + cpu.getName() + " @ " + cpu.getClock();
        }

        return array;
    }

    public String getMemory() {
        var groups = memBanks.stream().collect(Collectors.groupingBy((MemoryBankInfo p) -> p.getSize() + " " + p.getDIMMSpeed()));

        var groupStrings = new ArrayList<String>();
        for (var group : groups.values()) {
            groupStrings.add(String.format("%dx%s %s", group.size(), group.get(0).getSizeString(), group.get(0).getDIMMSpeed()));
        }

        long totalSize = 0;
        for (var bank : memBanks)
            totalSize += bank.getSize();

        return String.format("%s %s (%s)", Util.formatSize(totalSize), memBanks.get(0).getDIMMType(), String.join(", ", groupStrings));
    }

    public String[] getMemoryBanks() {
        var array = new String[memBanks.size()];
        for (int i = 0; i < memBanks.size(); i++) {
            var mem = memBanks.get(i);

            array[i] = String.format("%d: %s %s %s", i, mem.getSizeString(), mem.getDIMMType(), mem.getDIMMSpeed());
        }

        return array;
    }

    public String getDisk() {
        return diskCache;
    }

    public String[] getGPUs() {
        String[] array = new String[gpus.size()];
        gpus.toArray(array);

        return array;
    }

    public String getTemps() {
        if (sensors.size() == 0) return "unavailable";

        // average temperature, total power
        double cpuTemp = 0;
        double cpuPower = 0;

        for (var sensor : sensors) {
            if (sensor.getLocation() == SensorLocation.CPU) {
                if (sensor.getType() == SensorType.TEMPERATURE) cpuTemp += Double.parseDouble(sensor.getValue());
                if (sensor.getType() == SensorType.POWER) cpuPower += Double.parseDouble(sensor.getValue());
            }
        }

        cpuTemp /= cpus.size();

        return String.format("%.2f°C (%.2fW)", cpuTemp, cpuPower);
    }

    private void readCPUInfo() throws IOException {
        cpus.clear();

        var lines = Files.readAllLines(Path.of("/proc/cpuinfo"));

        int idx = -1;
        int cpuMhz = 0;
        String cpuName = null;
        for (int i = 0; i < lines.size(); i++) {
            var line = lines.get(i);

            //x86 Debugging
            if (debug) {Bukkit.getLogger().info("Got" + i + "lines.");}

            if (line.isBlank() && idx != -1) {
                cpus.add(new CPUInfo(idx, cpuName, cpuMhz));
                idx = -1;
                continue;
            }


            //x86: Seems .indexOf cant handle.. emptiness which doesn't occur in PowerPC cpuinfo listings
            if (line.startsWith("power")) {
                continue;
            }

            String value = line.substring(line.indexOf(':') + 2);

            //x86 Debugging
            if (debug) {Bukkit.getLogger().info(value);}

            if (line.startsWith("processor")) {
                idx = Integer.parseInt(value);
            }

            //x86: changed "cpu" to "model name" to accommodate architectural difference in /proc/cpuinfo

            if (line.startsWith("model name")) {
                cpuName = value;

                //x86 Debugging
                if (debug) {Bukkit.getLogger().info("Found " + value + " Microprocessor!");}

                var x = cpuName.indexOf(',');

                //x86: removes "Altivec supported" from PowerPC cpuinfo readout.. i think.. no point in removing ;3
                if (x != -1) {
                    cpuName = cpuName.substring(0, x);
                }

                if (cpuName.matches("740/750")) cpuName = String.format("PowerPC G3 (%s)", cpuName);
                else if (cpuName.matches("^74.+")) cpuName = String.format("PowerPC G4 (%s)", cpuName);
                else if (cpuName.matches("^(PPC)*9.+")) cpuName = String.format("PowerPC G5 (%s)", cpuName);
            }

            //x86: changed "clock" to "cpu MHz" to accommodate architectural difference in /proc/cpuinfo

            if (line.startsWith("cpu MHz")) {
                cpuMhz = Integer.parseInt(value.substring(0, value.indexOf('.')));
            }

            //x86: Has no display model

//            if (line.startsWith("detected as")) {
//                if (value.indexOf('(') != -1)
//                    displayModel = value.substring(value.indexOf('(') + 1, value.indexOf(')'));
//                else displayModel = value;
//            }
        }
    }


    //x86: Uses output from device tree to gather ram information and add to the memBanks arraylist, we dont have OpenFirmware :'3
    private void readMemoryInfo() throws IOException {

        //TODO: x86: This all relies on OpenFirmware, try parsing /proc/meminfo?.. for now
        return;

//        memBanks.clear();
//
//        var addressBytes = addressCells * 4;
//        var sizeBytes = sizeCells * 4;
//
//        // TODO: is it always memory@0,0?
//        var mem = Files.readAllBytes(DEVICE_TREE.resolve("memory@0,0/reg"));
//        var dimmSpeeds = Files.readString(DEVICE_TREE.resolve("memory@0,0/dimm-speeds")).split("\0");
//        var dimmTypes = Files.readString(DEVICE_TREE.resolve("memory@0,0/dimm-types")).split("\0");
//
//        var stride = (addressCells + sizeCells) * 4;
//        var count = mem.length / stride;
//
//        assert dimmSpeeds.length == count;
//        assert dimmTypes.length == count;
//
//        for (int i = 0; i < count; i += 1) {
//            var idx = i * stride;
//            var addressBuf = ByteBuffer.wrap(mem, idx, addressBytes);
//            var sizeBuff = ByteBuffer.wrap(mem, idx + addressBytes, sizeBytes);
//
//            long address = getAddress(addressBuf);
//            long size = getSize(sizeBuff);
//
//            memBanks.add(new MemoryBankInfo(address, size, dimmTypes[i], dimmSpeeds[i]));
//        }
    }

    private void readDiskInfo() {
        try {
            var p = Runtime.getRuntime().exec("df -h -x aufs -x tmpfs -x overlay -x drvfs -x devtmpfs --total");
            var is = new BufferedReader(new InputStreamReader(p.getInputStream()));

            String line;
            while ((line = is.readLine()) != null) {
                if (!line.startsWith("total")) continue;

                String[] split = line.replaceAll("\\s+", "_").split("_");
                diskCache = String.format("%sB/%sB", split[2], split[1]);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void readGPUInfo() {
        try {
            gpus.clear();
            var p = Runtime.getRuntime().exec("lspci");
            var is = new BufferedReader(new InputStreamReader(p.getInputStream()));

            String line;
            while ((line = is.readLine()) != null) {
                if (!line.contains("VGA compatible controller")) continue;

                line = line.substring(10);

                var idx = line.indexOf(':') + 2;
                var endIdx = line.indexOf('(');
                if (endIdx == -1) endIdx = line.length();

                gpus.add(line.substring(idx, endIdx));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void readSensors() {

        //x86: We dont have windfarm.
        // TODO: Add support for RAPL sysfs interface

        // TODO: we only support windfarm currently
//        try {
//            if (sensors.size() == 0 && hasWindfarm) {
//                for (int i = 0; i < cpus.size(); i++) {
//                    addSensorIfExists(SensorType.TEMPERATURE, SensorLocation.CPU, "cpu-temp-" + i);
//                    addSensorIfExists(SensorType.POWER, SensorLocation.CPU, "cpu-power-" + i);
//                    addSensorIfExists(SensorType.CURRENT, SensorLocation.CPU, "cpu-current-" + i);
//                    addSensorIfExists(SensorType.VOLTAGE, SensorLocation.CPU, "cpu-voltage-" + i);
//                    addSensorIfExists(SensorType.FAN_SPEED, SensorLocation.CPU, "cpu-rear-fan-" + i);
//                    addSensorIfExists(SensorType.FAN_SPEED, SensorLocation.CPU, "cpu-front-fan-" + i);
//                }
//
//                addSensorIfExists(SensorType.FAN_SPEED, SensorLocation.DISK, "drive-bay-fan");
//                addSensorIfExists(SensorType.TEMPERATURE, SensorLocation.DISK, "hd-temp");
//                addSensorIfExists(SensorType.FAN_SPEED, SensorLocation.GENERIC, "backside-fan");
//                addSensorIfExists(SensorType.TEMPERATURE, SensorLocation.GENERIC, "backside-temp");
//                addSensorIfExists(SensorType.FAN_SPEED, SensorLocation.SLOTS, "slots-fan");
//                addSensorIfExists(SensorType.POWER, SensorLocation.SLOTS, "slots-power");
//            }
//
//            for (var sensor : sensors)
//                sensor.update();
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
    }


    //x86: Thanks, ChatGPT
    //x86: should read CPU temperature, CPU power consumption since last poweron, and current CPU wattage using Intel RAPL,
    public static double getTemperature() throws IOException {
        File file = new File("/sys/class/thermal/thermal_zone0/temp");
        BufferedReader reader = new BufferedReader(new FileReader(file));
        String line = reader.readLine();
        reader.close();
        double temp = Double.parseDouble(line) / 1000.0;
        return temp;
    }
    public static double getPowerUsageHistorical() throws IOException {
        File file = new File("/sys/class/powercap/intel-rapl/intel-rapl:0/energy_uj");
        BufferedReader reader = new BufferedReader(new FileReader(file));
        String line = reader.readLine();
        reader.close();
        double energy = Double.parseDouble(line);
        double power = energy / 1000000.0; // convert from microjoules to watts
        return power;
    }

    public static double getPowerUsage() throws IOException {
        final String RAPL_ROOT = "/sys/class/powercap/intel-rapl";
        final String RAPL_PKG = "intel-rapl:0";

        Path path = Path.of(RAPL_ROOT, RAPL_PKG, "energy_uj");
        long energyMicrojoules = Long.parseLong(Files.readString(path).trim());
        double energyJoules = energyMicrojoules / 1000000.0;
        Path maxPath = Path.of(RAPL_ROOT, RAPL_PKG, "max_energy_range_uj");
        long maxMicrojoules = Long.parseLong(Files.readString(maxPath).trim());
        double maxJoules = maxMicrojoules / 1000000.0;
        double powerUsage = energyJoules * 1.0 / maxJoules;
        return powerUsage;
    }

//    private void addSensorIfExists(SensorType type, SensorLocation location, String sensor) {
//        var path = WINDFARM.resolve(sensor);
//
//        if (Files.exists(path)) sensors.add(new SensorInfo(type, location, path));
//    }

    // cleans a string retrieved from the Device Tree
    private String clean(String src) {
        return src.substring(0, src.length() - 1);
    }

    private long getSize(ByteBuffer sizeBuff) {
        long size;
        if (sizeCells == 2) size = sizeBuff.getLong();
        else size = (long) sizeBuff.getInt() & 0xffffffffL;
        return size;
    }

    private long getAddress(ByteBuffer addressBuf) {
        long address;
        if (addressCells == 2) address = addressBuf.getLong();
        else address = addressBuf.getInt() & 0xffffffffL;
        return address;
    }
}
