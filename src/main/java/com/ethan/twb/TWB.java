package com.ethan.twb;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class TWB extends JavaPlugin implements Listener {
    String mode = this.getConfig().getString("mode");

    @Override
    public void onEnable() {
        // Plugin startup logic
        // Primary config
        if(!getDataFolder().exists()){getDataFolder().mkdir();}
        getConfig().options().copyDefaults();
        saveDefaultConfig();

        Bukkit.getPluginManager().registerEvents(this, this);
    }

    // When a player moves outside the configured world border, teleport that player back inside.
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event){
        Player player = event.getPlayer();
        player.getVehicle();
        int x = player.getLocation().getBlockX();
        int z = player.getLocation().getBlockZ();
        int x_new = x;
        int z_new = z;
        // If the player is out of bounds, change x_new and/or z_new to reflect their current position
        if (x >= getConfig().getInt("corner-one.x")){
            x_new = getConfig().getInt("corner-one.x") - 10;
        }
        if (x <= getConfig().getInt("corner-two.x")){
            x_new = getConfig().getInt("corner-two.x") + 10;
        }
        if (z >= getConfig().getInt("corner-one.z")){
            z_new = getConfig().getInt("corner-one.z") - 10;
        }
        if (z <= getConfig().getInt("corner-two.z")){
            z_new = getConfig().getInt("corner-two.z") + 10;
        }

        if (x_new == x && z_new == z){return;}
        // If the player is out of bounds, teleport them based on mode.
        player.sendMessage(ChatColor.RED + "You have reached the world's border.");
        if (x_new != x && z_new != z){
            Location location = player.getLocation();
            // If the mode is "teleport" instead of teleporting the player 10 blocks back from the offending axis,
            // teleport the player 10 blocks away from the opposite side of the axis.
            if (mode.equals("teleport")){
                if (x >= getConfig().getInt("corner-one.x")){
                    location.setX(getConfig().getInt("corner-two.x") + 10);
                    x_new = getConfig().getInt("corner-two.x") + 10;
                }else if (x <= getConfig().getInt("corner-two.x")){
                    location.setX(getConfig().getInt("corner-one.x") - 10);
                    x_new = getConfig().getInt("corner-one.x") - 10;
                }
            }else if (mode.equals("block")){
                location.setX(x_new);
            }
            location.setZ(z_new);
            Block block = player.getWorld().getHighestBlockAt(x_new, z_new);
            location.setY(block.getY() + 1);
            teleportPlayer(player, location, player.getVehicle());
        } else if (x_new != x){
            Location location = player.getLocation();
            if (mode.equals("teleport")){
                if (x >= getConfig().getInt("corner-one.x")){
                    location.setX(getConfig().getInt("corner-two.x") + 10);
                    x_new = getConfig().getInt("corner-two.x") + 10;
                }else if (x <= getConfig().getInt("corner-two.x")){
                    location.setX(getConfig().getInt("corner-one.x") - 10);
                    x_new = getConfig().getInt("corner-one.x") - 10;
                }
            }else if (mode.equals("block")){
                location.setX(x_new);
            }
            Block block = player.getWorld().getHighestBlockAt(x_new, z_new);
            location.setY(block.getY() + 1);
            teleportPlayer(player, location, player.getVehicle());
        } else {
            Location location = player.getLocation();
            location.setZ(z_new);
            Block block = player.getWorld().getHighestBlockAt(x_new, z_new);
            location.setY(block.getY() + 1);
            teleportPlayer(player, location, player.getVehicle());
        }
    }

    public void teleportPlayer(Player player, Location location, Entity entity){
        // If player is riding a vehicle, teleport the vehicle too
        if (entity == null){
            player.teleport(location);
            return;
        }
        player.leaveVehicle();
        entity.teleport(location);
        player.teleport(location);
        entity.addPassenger(player);
    }
}
