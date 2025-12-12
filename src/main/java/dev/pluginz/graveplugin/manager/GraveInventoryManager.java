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
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GraveInventoryManager {
    private final GravePlugin plugin;
    private GraveManager graveManager;

    private Map<Inventory, UUID> inventoryGraveMap;

    public GraveInventoryManager(GravePlugin plugin) {
        this.plugin = plugin;
        this.graveManager = plugin.getGraveManager();
        this.inventoryGraveMap = new HashMap<>();
    }

    public void openGrave(Player player, UUID graveId) {
        Grave grave = graveManager.getGraveFromGraveID(graveId);
        if (grave == null || grave.isExpired()) {
            return;
        }
        int inventorySize = 54;

        Inventory graveInventory = Bukkit.createInventory(null, inventorySize, "Tomba di " + player.getName());

        // Add Armor (First Row)
        int startIndex = 0;
        for (int i = 3; i >= 0; i--) {
            ItemStack armor = grave.getArmorItems()[i];
            if (armor != null && armor.getType() != Material.AIR) {
                graveInventory.setItem(startIndex++, armor);
            } else startIndex++;
        }
        ItemStack offHand = grave.getOffHand();
        if (offHand != null && offHand.getType() != Material.AIR) {
            graveInventory.setItem(8, offHand);
        }

        // Add Inventory (Rows 2-4)
        for (int i = 0; i < 27; i++) {
            ItemStack item = grave.getItems()[i + 9];
            if (item != null && item.getType() != Material.AIR) {
                graveInventory.setItem(i + 9, item);
            }
        }

        // Add Hotbar (Row 5)
        for (int i = 0; i < 9; i++) {
            ItemStack item = grave.getItems()[i];
            if (item != null && item.getType() != Material.AIR) {
                graveInventory.setItem(i + 36, item);
            }
        }

        ItemStack greenPane = createGlassPane(Material.GREEN_STAINED_GLASS_PANE, ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.restore")));
        ItemStack redPane = createGlassPane(Material.RED_STAINED_GLASS_PANE, ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.drop")));

        int expLevels = grave.getLvl();
        ItemStack expBottles;
        expBottles = new ItemStack(Material.EXPERIENCE_BOTTLE, 1);
        ItemMeta meta = expBottles.getItemMeta();
        meta.setDisplayName(ChatColor.GREEN + "Livelli: " + ChatColor.YELLOW + expLevels);
        expBottles.setItemMeta(meta);
        graveInventory.setItem(49, expBottles);


        graveInventory.setItem(45, greenPane);
        graveInventory.setItem(53, redPane);

        inventoryGraveMap.put(graveInventory, graveId);

        player.openInventory(graveInventory);
    }

    public void restoreInventory(Player player, Inventory inventory, UUID graveId) {
        new BukkitRunnable() {
            @Override
            public void run() {
                ItemStack[] armorItems = { inventory.getItem(0), inventory.getItem(1), inventory.getItem(2), inventory.getItem(3), inventory.getItem(8) };
                PlayerInventory inv = player.getInventory();
                for (int i = 0; i < armorItems.length; i++) {
                    ItemStack item = armorItems[i];
                    if (item == null || item.getType() == Material.AIR) {
                        continue;
                    }
                    switch (i) {
                        case 0:
                            if (inv.getHelmet() == null) {
                                inv.setHelmet(item);
                            } else {
                                player.getWorld().dropItemNaturally(player.getLocation(), item);
                            }
                            break;
                        case 1:
                            if (inv.getChestplate() == null) {
                                inv.setChestplate(item);
                            } else {
                                player.getWorld().dropItemNaturally(player.getLocation(), item);
                            }
                            break;
                        case 2:
                            if (inv.getLeggings() == null) {
                                inv.setLeggings(item);
                            } else {
                                player.getWorld().dropItemNaturally(player.getLocation(), item);
                            }
                            break;
                        case 3:
                            if (inv.getBoots() == null) {
                                inv.setBoots(item);
                            } else {
                                player.getWorld().dropItemNaturally(player.getLocation(), item);
                            }
                            break;
                        case 4:
                            if (inv.getItemInOffHand().getType() == Material.AIR) {
                                inv.setItemInOffHand(item);
                            } else {
                                player.getWorld().dropItemNaturally(player.getLocation(), item);
                            }
                            break;
                    }
                }
                for (int i = 9; i < 45; i++) {
                    ItemStack item = inventory.getItem(i);
                    if (item != null && item.getType() != Material.AIR) {
                        if (i >= 36 && i < 45) {
                            if (player.getInventory().getItem(i - 36) == null) {
                                player.getInventory().setItem(i - 36, item);
                            } else {
                                player.getWorld().dropItemNaturally(player.getLocation(), item);
                            }
                        } else {
                            if (player.getInventory().getItem(i) == null) {
                                player.getInventory().setItem(i, item);
                            } else {
                                player.getWorld().dropItemNaturally(player.getLocation(), item);
                            }
                        }
                    }
                }

                Grave grave = graveManager.getGrave(graveId);
                player.setLevel(player.getLevel() + grave.getLvl());

                float exp = player.getExp() + grave.getExp();
                if (exp < 0 || exp > 1) {
                    exp = 0;
                }
                player.setExp(exp);

                graveManager.removeGrave(graveId);
                inventoryGraveMap.remove(inventory);
            }
        }.runTask(plugin);
    }

    public void dropGraveItems(Grave grave) {
        World world = grave.getLocation().getWorld();
        if (world == null) return;

        // Drop main inventory items
        for (ItemStack item : grave.getItems()) {
            if (item != null) {
                world.dropItemNaturally(grave.getLocation(), item);
            }
        }

        // Calculate total XP
        int totalXp = calculateTotalXp(grave.getLvl(), grave.getExp());

        // Spawn a single XP orb with the total XP
        world.spawn(grave.getLocation(), ExperienceOrb.class, experienceOrb -> experienceOrb.setExperience(totalXp));
    }

    private int calculateTotalXp(int level, float expProgress) {
        int xp = 0;

        // XP for completed levels
        for (int i = 0; i < level; i++) {
            xp += getXpForLevel(i);
        }

        // Add XP progress towards next level
        xp += Math.round(expProgress * getXpForLevel(level));

        return xp;
    }

    private int getXpForLevel(int level) {
        if (level <= 15) {
            return 2 * level + 7;
        } else if (level <= 30) {
            return 5 * level - 38;
        } else {
            return 9 * level - 158;
        }
    }


    private ItemStack createGlassPane(Material material, String name) {
        ItemStack pane = new ItemStack(material);
        ItemMeta meta = pane.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            pane.setItemMeta(meta);
        }
        return pane;
    }

    public int calculateLevelFromExperience(double totalExp) {
        int level = 0;
        while (true) {
            int expForNextLevel;
            if (level <= 15) {
                expForNextLevel = 2 * level + 7;
            } else if (level <= 30) {
                expForNextLevel = 5 * level - 38;
            } else {
                expForNextLevel = 9 * level - 158;
            }

            totalExp -= expForNextLevel;

            if (totalExp < 0) {
                break;
            }

            level++;
        }
        return level;
    }

    public boolean isGraveInventory(InventoryView inventoryView) {
        return inventoryView.getTitle().startsWith("Tomba di ");
    }

    public UUID getGraveIdFromInventory(Inventory inventory) {
        return inventoryGraveMap.get(inventory);
    }
}