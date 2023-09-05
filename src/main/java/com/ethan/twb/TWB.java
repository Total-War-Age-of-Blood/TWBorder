package com.ethan.twb;

import net.countercraft.movecraft.Movecraft;
import net.countercraft.movecraft.MovecraftChunk;
import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.MovecraftRotation;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.craft.PilotedCraft;
import net.countercraft.movecraft.events.CraftPreTranslateEvent;
import net.countercraft.movecraft.events.CraftTranslateEvent;
import net.countercraft.movecraft.processing.MovecraftWorld;
import net.countercraft.movecraft.util.hitboxes.HitBox;
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
    public final int cornerOneX = getConfig().getInt("corner-one.x");
    public final int cornerOneZ = getConfig().getInt("corner-one.z");
    public final int cornerTwoX = getConfig().getInt("corner-two.x");
    public final int cornerTwoZ = getConfig().getInt("corner-two.z");
    public final int buffer = getConfig().getInt("buffer");


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
        // Return if player is piloting a craft
        if (CraftManager.getInstance().getCraftByPlayer(player) != null){return;}

        int x = player.getLocation().getBlockX();
        int z = player.getLocation().getBlockZ();
        int xNew = x;
        int zNew = z;

        // If the player is out of bounds, change xNew and/or zNew to reflect their current position
        if (x >= cornerOneX){xNew = cornerOneX - buffer;}
        if (x <= cornerTwoX){xNew = cornerTwoX + buffer;}
        if (z >= cornerOneZ){zNew = cornerOneZ - buffer;}
        if (z <= cornerTwoZ){zNew = cornerTwoZ + buffer;}

        if (xNew == x && zNew == z){return;}
        // If the player is out of bounds, teleport them based on mode.
        player.sendMessage(ChatColor.RED + "You have reached the world's border.");
        if (xNew != x && zNew != z){
            Location location = player.getLocation();
            // If the mode is "teleport" instead of teleporting the player 10 blocks back from the offending axis,
            // teleport the player 10 blocks away from the opposite side of the axis.
            if (mode.equals("teleport")){
                if (x >= cornerOneX){
                    location.setX(cornerTwoX + buffer);
                    xNew = cornerTwoX + buffer;
                }else if (x <= cornerTwoX){
                    location.setX(cornerOneX - buffer);
                    xNew = cornerOneX - buffer;
                }
            }else if (mode.equals("block")){
                location.setX(xNew);
            }
            location.setZ(zNew);
            Block block = player.getWorld().getHighestBlockAt(xNew, zNew);
            location.setY(block.getY() + 1);
            teleportPlayer(player, location, player.getVehicle());
        } else if (xNew != x){
            Location location = player.getLocation();
            if (mode.equals("teleport")){
                if (x >= cornerOneX){
                    location.setX(cornerTwoX + buffer);
                    xNew = cornerTwoX + buffer;
                }else if (x <= cornerTwoX){
                    location.setX(cornerOneX - buffer);
                    xNew = cornerOneX - buffer;
                }
            }else if (mode.equals("block")){
                location.setX(xNew);
            }
            Block block = player.getWorld().getHighestBlockAt(xNew, zNew);
            location.setY(block.getY() + 1);
            teleportPlayer(player, location, player.getVehicle());
        } else {
            Location location = player.getLocation();
            location.setZ(zNew);
            Block block = player.getWorld().getHighestBlockAt(xNew, zNew);
            location.setY(block.getY() + 1);
            teleportPlayer(player, location, player.getVehicle());
        }
    }

    // When a movecraft leaves the world border, teleport the craft.
    @EventHandler
    public void onTranslate(CraftPreTranslateEvent event){
        Craft craft = event.getCraft();
        // Craft Movement
        HitBox hitBox = craft.getHitBox();
        int dX = event.getDx();
        int dZ = event.getDz();

        // Craft Future Position
        int xLength = hitBox.getXLength();
        int zLength = hitBox.getZLength();
        int maxX = hitBox.getMaxX() + dX;
        int maxZ = hitBox.getMaxZ() + dZ;
        int minX = hitBox.getMinX() + dX;
        int minZ = hitBox.getMinZ() + dZ;

        // Check if craft is outside play area.
        boolean gMaxX = false;
        boolean gMaxZ = false;
        boolean lMinX = false;
        boolean lMinZ = false;
        if (maxX >= cornerOneX){gMaxX = true;}
        if (maxZ >= cornerOneZ){gMaxZ = true;}
        if (minX <= cornerTwoX){lMinX = true;}
        if (minZ <= cornerTwoX){lMinZ = true;}
        // Return if craft is not out of bounds
        if (!gMaxZ && !gMaxX && !lMinX && !lMinZ){return;}
        // maxX = east, minX = west, maxZ = south, minZ = north
        int newX = 0;
        int newZ = 0;
        // If mode is block, we need to move the craft one craft length + buffer away from relevant borders
        if (mode.equals("block")){
            if (gMaxX){newX = -xLength - buffer;}
            if (gMaxZ){newZ = -zLength - buffer;}
            if (lMinX){newX = xLength + buffer;}
            if (lMinZ){newZ = zLength + buffer;}
            // Change the displacements for the event
            event.setDx(newX);
            event.setDz(newZ);
            if (!(craft instanceof PilotedCraft)){return;}
            ((PilotedCraft) craft).getPilot().sendMessage(ChatColor.RED + "Your craft bounces off the world border.");
            event.setPlayingFailSound(true);
        } else if (mode.equals("teleport")) {
            // If the craft is hitting just north or just south border, use block behavior
            if (gMaxZ && !gMaxX && !lMinX){
                newZ = -zLength - buffer;
                event.setDz(newZ);
                if (!(craft instanceof PilotedCraft)){return;}
                ((PilotedCraft) craft).getPilot().sendMessage(ChatColor.RED + "Your craft bounces off the world border.");
                event.setPlayingFailSound(true);
                return;
            }
            if (lMinZ && !gMaxX && !lMinX){
                newZ = zLength + buffer;
                event.setDz(newZ);
                if (!(craft instanceof PilotedCraft)){return;}
                ((PilotedCraft) craft).getPilot().sendMessage(ChatColor.RED + "Your craft bounces off the world border.");
                event.setPlayingFailSound(true);
                return;
            }

            // If the craft hits east or west, put it on the other side of the map
            newX = Math.abs(cornerOneX - cornerTwoX) - buffer - xLength;
            if (gMaxX){
                event.setDx(-newX);
                event.setDz(0);
            } else if (lMinX) {
                event.setDx(newX);
                event.setDz(0);
            }
        } else{System.out.println("Improper mode");}
    }

    public void teleportPlayer(Player player, Location location, Entity entity){
        // If player is riding a vehicle, teleport the vehicle too
        if (entity == null){
            player.teleport(location);
            System.out.println("Teleporting to");
            return;
        }
        player.leaveVehicle();
        entity.teleport(location);
        player.teleport(location);
        entity.addPassenger(player);
    }
}
