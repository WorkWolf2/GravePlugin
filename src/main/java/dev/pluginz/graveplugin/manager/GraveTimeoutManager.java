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
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public class GraveTimeoutManager {

    private final GravePlugin plugin;
    private GraveManager graveManager;
    private GraveInventoryManager graveInventoryManager;
    private GravePersistenceManager gravePersistenceManager;

    private long lastUpdateTime;

    public GraveTimeoutManager(GravePlugin plugin) {
        this.plugin = plugin;
        this.graveManager = plugin.getGraveManager();
        this.graveInventoryManager = plugin.getGraveInventoryManager();
        this.gravePersistenceManager = plugin.getGravePersistenceManager();
    }

    public void startGraveTimeoutTask() {
        lastUpdateTime = System.currentTimeMillis();
        plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            long currentTime = System.currentTimeMillis();
            long elapsedTime = currentTime - lastUpdateTime;
            lastUpdateTime = currentTime;
            Map<UUID, Grave> graves = graveManager.getGraves();

            for (Grave grave : graves.values()) {
                grave.incrementActiveTime(elapsedTime);

                if (grave.getMaxActiveTime() == -1) {
                    continue;
                }
                if (grave.getMaxActiveTime() != -1 && grave.getActiveTime() >= grave.getMaxActiveTime()) {
                    grave.setExpired(true);
                }

                if (grave.isExpired()) {
                    if (grave.getLocation().getWorld().isChunkLoaded(grave.getLocation().getBlockX() >> 4, grave.getLocation().getBlockZ() >> 4)) {
                        if (isPlayerNearby(grave.getLocation(), 50)) {
                            graveInventoryManager.dropGraveItems(grave);
                            graveManager.removeGrave(grave.getGraveId());
                            //iterator.remove();
                        }
                    }
                } else if (isPlayerNearby(grave.getLocation(), 50)) {
                    long remainingTime = grave.getMaxActiveTime() - grave.getActiveTime();
                    String coloredTime = getColoredTime(grave);
                    updateGraveName(grave.getGraveId(), coloredTime);
                }
            }
        }, 0L, 20L); // 20 ticks = 1 second

        plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            gravePersistenceManager.saveGraves();
        }, 0L, 200L); // 200 ticks = 10 seconds
    }

    public void updateGraveName(UUID graveId, String coloredTime) {
        Grave grave = graveManager.getGrave(graveId);
        if (grave != null) {
            Location location = grave.getLocation();
            World world = location.getWorld();
            if (world != null) {
                ArmorStand armorStand = (ArmorStand) Bukkit.getEntity(graveManager.getArmorStandIdFromGraveId(graveId));
                if (armorStand != null) {
                    String playerName = grave.getPlayerName();
                    armorStand.setCustomName("Tomba di " + playerName + " " + coloredTime);
                }
            }
        }
    }

    private boolean isPlayerNearby(Location location, double radius) {
        return location.getWorld().getPlayers().stream()
                .anyMatch(player -> player.getLocation().distanceSquared(location) <= radius * radius);
    }

    private static String getColoredTime(Grave grave) {
        long remainingTime = grave.getMaxActiveTime() - grave.getActiveTime();
        int remainingSeconds = (int) (remainingTime / 1000); // Convert milliseconds to seconds

        int hours = remainingSeconds / 3600;
        int minutes = (remainingSeconds % 3600) / 60;
        int seconds = remainingSeconds % 60;

        String time = String.format("%02dh %02dm %02ds", hours, minutes, seconds);

        double ratio = (double) grave.getActiveTime() / grave.getMaxActiveTime();
        ChatColor color;
        if (ratio < 0.33) {
            color = ChatColor.GREEN;
        } else if (ratio < 0.66) {
            color = ChatColor.GOLD;
        } else {
            color = ChatColor.RED;
        }
        return color + time;
    }
}
