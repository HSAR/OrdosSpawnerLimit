package org.landofordos.ordosspawnerlimit;

import java.io.BufferedOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StreamCorruptedException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.plugin.java.JavaPlugin;

public class OrdosSpawnerLimit extends JavaPlugin implements Listener {

    // Important plugin objects
    private static Server server;
    private static Logger logger;
    private FileConfiguration config;
    //
    private boolean verbose;
    //
    private boolean limitSpawnerSpawns;
    private int spawnLimit;
    private List<SpawnerData> spawners = null;
    //
    private boolean limitNaturalSpawns;
    private CircularArrayRing<SpawnerData> spawnList = null;
    private int denyVolume;
    private int denyVal;
    //
    private File spawnerFile;

    // map of spawner blocks and time remaining

    public void onDisable() {
        saveSpawnerData();
        logger.info("Disabled.");
    }

    public void onEnable() {
        // static reference to this plugin and the server
        // plugin = this;
        server = getServer();
        // start the logger
        logger = getLogger();
        // save config to default location if not already there
        this.saveDefaultConfig();
        //
        // ====== CONFIG LOAD START ======
        //
        // set config var
        config = this.getConfig();
        // first-run initialisation, if necessary
        final boolean firstrun = config.getBoolean("firstrun");
        if (firstrun) {
            // Whatever first run initialisation is required
            spawners = new ArrayList<>();
            saveSpawnerData();

            config.set("firstrun", false);
            this.saveConfig();
            if (verbose) {
                logger.info("First-run initialisation complete.");
            }
        }
        // verbose logging? retrieve value from config file.
        verbose = config.getBoolean("verboselogging");
        if (verbose) {
            logger.info("Verbose logging enabled.");
        } else {
            logger.info("Verbose logging disabled.");
        }
        // plugin effects enabled? retrieve values from config file.
        limitSpawnerSpawns = config.getBoolean("limitSpawners");
        // spawner limit? retrieve value from config file.
        spawnLimit = config.getInt("spawnLimit");
        logger.info("Limiting spawners to " + spawnLimit);
        spawnerFile = new File(this.getDataFolder() + "/spawnerdata.dat");
        if (!spawnerFile.exists()) {
            spawners = new ArrayList<>();
            saveSpawnerData();
        }
        // natural spawn area limit? retrieve values from config file.
        limitNaturalSpawns = config.getBoolean("limitNatural");
        int spawnListSize = config.getInt("spawnListSize");
        denyVolume = config.getInt("denyVolume");
        if ((spawnListSize > 0) && (denyVolume > 0)) {
            spawnList = new CircularArrayRing<>(spawnListSize);
            denyVal = denyVolume / 2;
        } else {
            logger.log(Level.SEVERE, "Invalid configuration values for natural spawn limit, disabling feature.");
            limitNaturalSpawns = false;
        }
        // ============
        //
        // ====== CONFIG LOAD FINISH ======
        //
        // initialise spawner data
        loadSpawnerData();
        if (spawners == null) {
            spawners = new ArrayList<>();
            saveSpawnerData();
        }
        // register events
        server.getPluginManager().registerEvents(this, this);
    }

    private void loadSpawnerData() {
        if (verbose) {
            logger.info("Loading data...");
        }
        ArrayList<SpawnerData> savedSpawners = null;
        // deserialise file
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(spawnerFile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        ObjectInputStream ois;
        boolean readSuccess = false;
        do {
            try {
                ois = new ObjectInputStream(fis);
                savedSpawners = (ArrayList<SpawnerData>) ois.readObject();
                // resource leak == !good
                ois.close();
                readSuccess = true;
            } catch (StreamCorruptedException sce) {
                try {
                    Thread.sleep(250);
                } catch (InterruptedException ignored) {
                }
            } catch (EOFException e) {
                // do nothing
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        } while (!readSuccess);
        spawners = savedSpawners;
        if (verbose) {
            logger.info("Data for " + spawners.size() + " spawners loaded.");
        }
    }

    private void saveSpawnerData() {
        try {
            if (verbose) {
                logger.info("Saving data...");
            }
            ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(spawnerFile)));
            oos.writeObject(spawners);
            // resource leak == !good
            oos.flush();
            oos.close();
            if (verbose) {
                logger.info("Data saved.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
        if (args.length == 1) {
            if (args[0].equals("reload")) {
                if (sender.hasPermission("ordosspawnerlimit.reloadconfig")) {
                    verbose = config.getBoolean("verboselogging");
                    if (verbose) {
                        logger.info("Verbose logging enabled.");
                    } else {
                        logger.info("Verbose logging disabled.");
                    }
                    // spawners
                    limitSpawnerSpawns = config.getBoolean("limitSpawners");
                    spawnLimit = config.getInt("spawnLimit");
                    // natural
                    limitNaturalSpawns = config.getBoolean("limitNatural");
                    int spawnListSize = config.getInt("spawnListSize");
                    denyVolume = config.getInt("denyVolume");
                    if ((spawnListSize > 0) && (denyVolume > 0)) {
                        // resize CAR is necessary
                        CircularArrayRing<SpawnerData> newSpawnList = new CircularArrayRing<>(spawnListSize);
                        if (spawnList.size() != newSpawnList.size()) {
                            newSpawnList.addAll(spawnList);
                            spawnList = newSpawnList;
                        }
                        denyVal = denyVolume / 2;
                    } else {
                        logger.log(Level.SEVERE, "Invalid configuration values for natural spawn limit, disabling feature.");
                        limitNaturalSpawns = false;
                    }
                    sender.sendMessage("Config values reloaded.");
                } else {
                    sender.sendMessage(ChatColor.RED + "You do not have permission to reload OrdosSpawnerLimit's config.");
                    return true;
                }
            }
            if (args[0].equals("toggle")) {
                if (args[0].equals("spawners")) {
                    if (sender.hasPermission("ordosspawnerlimit.ingametoggle")) {
                        limitSpawnerSpawns = !limitSpawnerSpawns;
                        // save to config
                        config.set("limitSpawners", limitSpawnerSpawns);
                    } else {
                        sender.sendMessage(ChatColor.RED + "You do not have permission to toggle OrdosSpawnerLimit.");
                        return true;
                    }
                }
                if (args[0].equals("natural")) {
                    if (sender.hasPermission("ordosspawnerlimit.ingametoggle")) {
                        limitNaturalSpawns = !limitNaturalSpawns;
                        // save to config
                        config.set("limitNatural", limitNaturalSpawns);
                    } else {
                        sender.sendMessage(ChatColor.RED + "You do not have permission to toggle OrdosSpawnerLimit.");
                        return true;
                    }
                }
                return false;
            }
        }
        return true;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (!event.isCancelled()) {
            Location spawnLocation = event.getLocation();
            if (limitSpawnerSpawns) {
                if (event.getSpawnReason().equals(SpawnReason.SPAWNER)) {
                    // check a 8x2x8 region for the spawner.
                    for (int x = -4; x < 5; x++) {
                        for (int y = -1; y < 2; y++) {
                            for (int z = -4; z < 5; z++) {
                                Block checkBlock = spawnLocation.getWorld().getBlockAt(
                                        new Location(spawnLocation.getWorld(), spawnLocation.getX() + x, spawnLocation.getY() + y,
                                                spawnLocation.getZ() + z));
                                // if spawner found check spawned creature matches creature spawned type.
                                if (checkBlock.getType().equals(Material.MOB_SPAWNER)) {
                                    CreatureSpawner spawner = (CreatureSpawner) checkBlock.getState();
                                    EntityType spawnType = spawner.getSpawnedType();
                                    if (spawnType.equals(event.getEntityType())) {
                                        SpawnerData spawnSpawner = new SpawnerData((int) spawnLocation.getX() + x,
                                                (int) spawnLocation.getY() + y, (int) spawnLocation.getZ() + z, spawnLimit);
                                        processNewSpawn(spawnSpawner, checkBlock);
                                    }
                                }
                            }
                        }
                    }
                    // if not found, cancel event.
                    logger.info("Unattached creature spawn detected. Event cancelled.");
                    event.setCancelled(true);
                    return;
                }
            }
            if (limitNaturalSpawns) {
                if (event.getSpawnReason().equals(SpawnReason.NATURAL)) {
                    // if (event.getSpawnReason().equals(SpawnReason.SPAWNER_EGG)) {
                    // check a cubic region for previous spawns, block if found
                    for (int x = -denyVal; x <= denyVal; x++) {
                        for (int y = -denyVal; y <= denyVal; y++) {
                            for (int z = -denyVal; z <= denyVal; z++) {
                                // if any of the positions has previously spawned a mob, block spawn
                                SpawnerData checkPosition = new SpawnerData((int) spawnLocation.getX() + x, (int) spawnLocation.getY() + y,
                                        (int) spawnLocation.getZ() + z, 1);
                                if (spawnList.contains(checkPosition)) {
                                    if (verbose) {
                                        // logger.info("Natural spawn blocked at " + checkPosition.toString() + ".");
                                    }
                                    event.setCancelled(true);
                                    return;
                                }
                            }
                        }
                    }
                    SpawnerData spawnPosition = new SpawnerData((int) spawnLocation.getX(), (int) spawnLocation.getY(),
                            (int) spawnLocation.getZ(), 1);
                    /*
                     * if (verbose) { logger.info("Natural spawn added at " + spawnPosition.toString() + "."); }
					 */
                    spawnList.add(spawnPosition);
                }
            }
        }
    }

    private void processNewSpawn(SpawnerData spawn, Block spawnerBlock) {
        // check for previous data
        if (!spawners.contains(spawn)) {
            spawners.add(spawn);
            if (verbose) {
                logger.info("Now tracking new spawner at " + spawn.toString() + ".");
            }
        }

        SpawnerData record = spawners.get(spawners.indexOf(spawn));

        record.setSpawnsRemaining(record.getSpawnsRemaining() - 1);
        if (verbose) {
            logger.info("Spawner at " + spawn.toString() + " spawned mob. "
                    + record.getSpawnsRemaining() + " spawns remain.");
        }

        // if value has reached 0 destroy spawner.
        if (record.getSpawnsRemaining() == 1) {
            spawnerBlock.setType(Material.AIR);
            logger.info("Spawner at " + spawn.toString() + " has been destroyed.");
            spawners.remove(record);
        }

        saveSpawnerData();
    }
}