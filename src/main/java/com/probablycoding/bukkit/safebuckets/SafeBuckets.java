package com.probablycoding.bukkit.safebuckets;

import java.io.*;
import java.util.HashMap;
import java.util.TreeSet;
import java.util.logging.Level;
import org.bukkit.plugin.java.JavaPlugin;

public class SafeBuckets extends JavaPlugin {
    private final SafeBucketsListener listener = new SafeBucketsListener(this);
    private HashMap<String, TreeSet<Long>> safeBlocks;

    private long packXYZ(int x, int y, int z) {
        return (((long) z << 32) & 0x00ffffff00000000L) | (((long) x << 8) & 0x00000000ffffff00L) | y;
    }

    public boolean isSafeBlock(String world, int x, int y, int z) {
        if (!safeBlocks.containsKey(world)) {
            return false;
        }

        Long hash = packXYZ(x, y, z);
        if (safeBlocks.get(world).contains(hash)) {
            return true;
        }

        return false;
    }

    public void addSafeBlock(String world, int x, int y, int z) {
        if (!safeBlocks.containsKey(world)) {
            safeBlocks.put(world, new TreeSet<Long>());
        }

        Long hash = packXYZ(x, y, z);
        safeBlocks.get(world).add(hash);
        saveSet();
    }

    public void removeSafeBlock(String world, int x, int y, int z) {
        if (!safeBlocks.containsKey(world)) {
            return;
        }

        Long hash = packXYZ(x, y, z);
        safeBlocks.get(world).remove(hash);
        saveSet();
    }

    public void saveSet() {
        File saveFile = new File(this.getDataFolder() + File.separator + "safeblocks.dat");
        saveFile.getParentFile().mkdirs();

        try {
            FileOutputStream fos = new FileOutputStream(saveFile);
            ObjectOutputStream out = new ObjectOutputStream(fos);
            out.writeObject(safeBlocks);
        } catch (Exception ex) {
            getLogger().log(Level.SEVERE, null, ex);
        }
    }

    public void loadSet() {
        File saveFile = new File(this.getDataFolder() + File.separator + "safeblocks.dat");
        if (saveFile.exists()) {
            try {
                FileInputStream fis = new FileInputStream(saveFile);
                ObjectInputStream in = new ObjectInputStream(fis);
                safeBlocks = (HashMap<String, TreeSet<Long>>) in.readObject();
                return;
            } catch (Exception ex) {
                getLogger().log(Level.SEVERE, null, ex);
            }
        }

        // if file doesn't exist or can't be loaded make new storage
        safeBlocks = new HashMap<String, TreeSet<Long>>();
    }

    @Override
    public void onDisable() {
        saveSet();
    }

    @Override
    public void onEnable() {
        this.getServer().getPluginManager().registerEvents(listener, this);
        loadSet();
    }
}
