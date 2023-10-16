package com.ethan.twb;

import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.craft.PilotedCraft;
import net.countercraft.movecraft.events.CraftPreTranslateEvent;
import net.countercraft.movecraft.util.hitboxes.HitBox;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
        // Return if player is piloting a craft. First if statement catches if Movecraft is not active.
        if (CraftManager.getInstance() != null){
            if (CraftManager.getInstance().getCraftByPlayer(player) != null){return;}
        }


        int x = player.getLocation().getBlockX();
        int z = player.getLocation().getBlockZ();
        int xNew = x;
        int zNew = z;
        int xTrash = x;

        // If the player is out of bounds, change xNew and/or zNew to reflect their current position
        // Check if player is in the nether
        if (player.getWorld().getEnvironment().equals(World.Environment.NETHER)){
            if (x >= cornerOneX/8){xNew = xTrash = cornerOneX/8 - buffer;}
            if (x <= cornerTwoX/8){xNew = xTrash = cornerTwoX/8 + buffer;}
            if (z >= cornerOneZ/8){zNew = xTrash = cornerOneZ/8 - buffer;}
            if (z <= cornerTwoZ/8){zNew = xTrash = cornerTwoZ/8 + buffer;}
        } else{
            if (x >= cornerOneX){xNew = xTrash = cornerOneX - buffer;}
            if (x <= cornerTwoX){xNew = xTrash = cornerTwoX + buffer;}
            if (z >= cornerOneZ){zNew = xTrash = cornerOneZ - buffer;}
            if (z <= cornerTwoZ){zNew = xTrash = cornerTwoZ + buffer;}
        }

        if (xNew == x && zNew == z){return;}
        // If the player is out of bounds, teleport them based on mode.
        player.sendMessage(ChatColor.RED + "You have reached the world's border.");
        if (xNew != x && zNew != z){
            // If the mode is "teleport" instead of teleporting the player 10 blocks back from the offending axis,
            // teleport the player 10 blocks away from the opposite side of the axis.
            if (mode.equals("teleport")){
                if (x >= cornerOneX){
                    xTrash = xNew;
                    xNew = cornerTwoX + buffer;
                }else if (x <= cornerTwoX){
                    xTrash = xNew;
                    xNew = cornerOneX - buffer;
                }
            }
            Block block = findSafeBlock(player, xNew, player.getLocation().getBlockY(), zNew);
            if (block != null){
                teleportPlayer(player, block.getLocation(), player.getVehicle());
            } else{
                Location location = player.getLocation();
                location.setX(xTrash);
                teleportPlayer(player, location, player.getVehicle());
                player.sendMessage(ChatColor.RED + "No safe blocks found to teleport to");
            }
        } else if (xNew != x){
            if (mode.equals("teleport")){
                if (x >= cornerOneX){
                    xTrash = xNew;
                    xNew = cornerTwoX + buffer;
                }else if (x <= cornerTwoX){
                    xTrash = xNew;
                    xNew = cornerOneX - buffer;
                }
            }
            Block block = findSafeBlock(player, xNew, player.getLocation().getBlockY(), zNew);
            if (block != null){
                teleportPlayer(player, block.getLocation(), player.getVehicle());
            } else{
                Location location = player.getLocation();
                location.setX(xTrash);
                teleportPlayer(player, location, player.getVehicle());
                player.sendMessage(ChatColor.RED + "No safe blocks found to teleport to");
            }
        } else {
            Block block = findSafeBlock(player, xNew, player.getLocation().getBlockY(), zNew);
            if (block != null){
                teleportPlayer(player, block.getLocation(), player.getVehicle());
            } else{
                Location location = player.getLocation();
                location.setX(xTrash);
                teleportPlayer(player, location, player.getVehicle());
                player.sendMessage(ChatColor.RED + "No safe blocks found to teleport to");
            }

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
            return;
        }
        player.leaveVehicle();
        entity.teleport(location);
        player.teleport(location);
        entity.addPassenger(player);
    }

    List<Material> unsafeBlocks = Arrays.asList(Material.AIR, Material.LAVA, Material.FIRE);

    public Block findSafeBlock(Player player, int newX, int oldY, int newZ){
        World world = player.getWorld();
        int maxHeight = world.getMaxHeight();
        int minHeight = world.getMinHeight();
        List<Block> safeBlocks = new ArrayList<>();
        for (int i = minHeight; i <= maxHeight; i++){
            Block block = world.getBlockAt(newX, i, newZ);
            if (unsafeBlocks.contains(block.getType())){continue;}
            // Check two blocks above for air
            if (!block.getRelative(BlockFace.UP).getType().equals(Material.AIR)
                    || !block.getRelative(0, 2, 0).getType().equals(Material.AIR)){continue;}
            if (world.getEnvironment().equals(World.Environment.NETHER) && block.getY() == 127 && block.getType().equals(Material.BEDROCK)){continue;}
            safeBlocks.add(block);
        }
        Block safe = null;
        for (Block block : safeBlocks){
            if (safe == null){safe = block;}
            int blockY = block.getY();
            int safeY = safe.getY();
            if (Math.abs(oldY - blockY) < Math.abs(oldY - safeY)){safe = block;}
        }
        if (safe != null){safe = safe.getRelative(BlockFace.UP);}
        return safe;
    }
}
