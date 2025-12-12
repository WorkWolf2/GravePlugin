/*
 * This file is part of BT's Graves, licensed under the MIT License.
 *
 *  Copyright (c) BT Pluginz <github@tubyoub.de>
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */

package dev.pluginz.graveplugin.manager;

import dev.pluginz.graveplugin.GravePlugin;
import dev.pluginz.graveplugin.util.Grave;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class GraveManager {
    private static GravePlugin plugin;
    private static GravePersistenceManager gravePersistenceManager;
    private Map<UUID, Grave> graves;


    public GraveManager(GravePlugin plugin) {
        this.plugin = plugin;
        this.graves = new HashMap<>();
    }
    public static void setPersistenceManager(GravePersistenceManager gravePersistenceManager) {
        GraveManager.gravePersistenceManager = gravePersistenceManager;
    }

    public UUID createGrave(Player player, Location location, ItemStack[] items, ItemStack[] armor, ItemStack offHand, int lvl, float xp) {
        ArmorStand armorStand = (ArmorStand) location.getWorld().spawnEntity(location, EntityType.ARMOR_STAND);
        armorStand.setVisible(false);
        armorStand.setGravity(false);
        armorStand.setCustomName("Grave di " + player.getName());
        armorStand.setCustomNameVisible(true);
        armorStand.setInvulnerable(true);
        if (plugin.getConfigManager().isSmallArmorStand()) {
            armorStand.setSmall(true);
        }

        UUID graveId = UUID.randomUUID();
        UUID armorStandId = armorStand.getUniqueId();

        long maxActiveTime = (long) plugin.getConfigManager().getGraveTimeout() * 60 * 1000;
        if(plugin.getConfigManager().getGraveTimeout() == -1) {
            maxActiveTime = -1;
        }

        Grave grave = new Grave(player.getName(), graveId, location, items, armor, offHand, armorStandId, maxActiveTime, false, lvl, xp);
        graves.put(graveId, grave);

        gravePersistenceManager.saveGraves();
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.successfull-grave")
                .replaceAll("%x%", String.valueOf(location.getX()))
                .replaceAll("%y%", String.valueOf(location.getY()))
                .replaceAll("%z%", String.valueOf(location.getZ()))
                ));
        return graveId;
    }

    public void removeGrave(UUID graveId) {
        Grave grave = this.getGraveFromGraveID(graveId);
        if (grave != null) {
            ArmorStand armorStand = (ArmorStand) grave.getLocation().getWorld().getEntitiesByClass(ArmorStand.class).stream()
                    .filter(entity -> entity.getUniqueId().equals(grave.getArmorStandId()))
                    .findFirst()
                    .orElse(null);
            if (armorStand != null) {
                Bukkit.getScheduler().runTaskLater(plugin, armorStand::remove, 1);
                armorStand.remove();
            }

            Block block = grave.getLocation().getBlock();
            if (block.getType() == Material.PLAYER_HEAD || block.getType() == Material.PLAYER_WALL_HEAD) {
                block.setType(Material.AIR);
            }

            graves.remove(graveId);
            gravePersistenceManager.saveGraves();
        }
    }

    public boolean isGraveArmorStand(UUID uuid) {
        return graves.containsKey(uuid);
    }

    public Grave getGraveFromUUID(UUID armorStandUUID) {
        for (Grave grave : graves.values()) {
            if (grave.getArmorStandId().equals(armorStandUUID)) {
                return grave;
            }
        }
        return null;
    }

    public UUID getArmorStandIdFromGraveId(UUID graveId) {
        Grave grave = graves.get(graveId);
        if (grave != null) {
            return grave.getArmorStandId();
        }
        return null;
    }

    public UUID getGraveIdFromArmorStand(UUID armorStandId) {
        for (Map.Entry<UUID, Grave> entry : graves.entrySet()) {
            if (entry.getValue().getArmorStandId().equals(armorStandId)) {
                return entry.getKey();
            }
        }
        return null;
    }

    public Grave getGraveFromGraveID(UUID graveId) {
        return graves.get(graveId);
    }

    public void addGrave(UUID id, Grave grave) {
        graves.put(id, grave);
    }

    public Grave getGrave(UUID id) {
        return graves.get(id);
    }

    public Map<UUID, Grave> getGraves() {
        return graves;
    }
}