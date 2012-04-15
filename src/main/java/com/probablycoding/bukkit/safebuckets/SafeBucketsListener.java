package com.probablycoding.bukkit.safebuckets;

import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;

public class SafeBucketsListener implements Listener {
    private final SafeBuckets plugin;
    private final int BEDROCK = 7;
    private final byte MAGIC_DATA = 2;

    SafeBucketsListener(SafeBuckets instance) {
        plugin = instance;
    }

    private boolean needsMagicData(World world, int x, int z) {
        for (int i = 1; i < 256; i++) {
            if (plugin.isSafeBlock(world.getName(), x, i, z)) {
                return true;
            }
        }

        return false;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPhysics(BlockPhysicsEvent event) {
        Block block = event.getBlock();
        if (event.getBlock().isLiquid()) {
            // using bedrock at Y=0 to short circuit lookups
            Block bedrock = block.getWorld().getBlockAt(block.getX(), 0, block.getY());
            if (bedrock.getTypeId() != BEDROCK || bedrock.getData() != MAGIC_DATA) {
                if (plugin.isSafeBlock(block.getWorld().getName(), block.getX(), block.getY(), block.getZ())) {
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Block block = event.getBlockPlaced();

        // create magic data when someone places bedrock at Y=0
        if (block.getY() == 0 && block.getTypeId() == BEDROCK) {
            if (needsMagicData(block.getWorld(), block.getX(), block.getZ())) {
                block.setData(MAGIC_DATA);
            }
        }

        // only stop tracking if source blocks were placed - this makes rollbacks work
        if (block.isLiquid()) {
            plugin.removeSafeBlock(block.getWorld().getName(), block.getX(), block.getY(), block.getZ());

            Block bedrock = block.getWorld().getBlockAt(block.getX(), 0, block.getZ());
            if (bedrock.getTypeId() == BEDROCK && needsMagicData(block.getWorld(), block.getX(), block.getZ())) {
                bedrock.setData(MAGIC_DATA);
            } else if (bedrock.getTypeId() == BEDROCK) {
                bedrock.setData((byte)0);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerBucketEmpty(PlayerBucketEmptyEvent event) {
        Block block = event.getBlockClicked().getRelative(event.getBlockFace());
        plugin.addSafeBlock(block.getWorld().getName(), block.getX(), block.getY(), block.getZ());

        Block bedrock = block.getWorld().getBlockAt(block.getX(), 0, block.getZ());
        if (bedrock.getTypeId() == BEDROCK && bedrock.getData() != MAGIC_DATA) {
            bedrock.setData(MAGIC_DATA);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerBucketFill(PlayerBucketFillEvent event) {
        Block block = event.getBlockClicked().getRelative(event.getBlockFace());
        plugin.removeSafeBlock(block.getWorld().getName(), block.getX(), block.getY(), block.getZ());

        Block bedrock = block.getWorld().getBlockAt(block.getX(), 0, block.getZ());
        if (bedrock.getTypeId() == BEDROCK && needsMagicData(block.getWorld(), block.getX(), block.getZ())) {
            bedrock.setData(MAGIC_DATA);
        } else if (bedrock.getTypeId() == BEDROCK) {
            bedrock.setData((byte)0);
        }
    }
}
