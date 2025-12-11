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

package dev.pluginz.graveplugin.command;

import dev.pluginz.graveplugin.GravePlugin;
import dev.pluginz.graveplugin.manager.GraveInventoryManager;
import dev.pluginz.graveplugin.manager.GraveManager;
import dev.pluginz.graveplugin.util.Grave;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Iterator;
import java.util.UUID;

public class GraveCommand implements CommandExecutor {
    private final GravePlugin plugin;
    private final GraveManager graveManager;
    private final GraveInventoryManager graveInventoryManager;
    private final String prefix;

    public GraveCommand(GravePlugin plugin) {
        this.plugin = plugin;
        this.graveManager = plugin.getGraveManager();
        this.graveInventoryManager = plugin.getGraveInventoryManager();
        this.prefix = plugin.getPluginPrefix();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "info":
                handleInfo(sender, args);
                return true;
            case "reload":
                return handleReload(sender);
            case "open":
                return handleOpen(sender, args);
            case "collect":
                return this.handleCollect(sender);
            default:
                sendHelp(sender);
                return true;
        }
    }

    private void handleInfo(CommandSender sender, String[] args) {
        plugin.sendPluginMessages(sender, "title");
        sender.sendMessage(ChatColor.GREEN + "Author: BTPluginz");
        sender.sendMessage(ChatColor.GREEN + "Version: " + plugin.getVersion());
        if (plugin.getConfigManager().isCheckVersion() && sender.hasPermission("btgraves.admin")) {
            if (plugin.isNewVersion()) {
                sender.sendMessage(ChatColor.YELLOW + "A new version is available! Update at: " + ChatColor.UNDERLINE + "https://modrinth.com/plugin/bt-graves/version/" + plugin.getVersionInfo().latestVersion);
            } else {
                sender.sendMessage(ChatColor.GREEN + "You are using the latest version!");
            }
        } else {
            sender.sendMessage(ChatColor.GOLD +  "Version checking is disabled!");
        }
        TextComponent githubLink = new TextComponent(ChatColor.DARK_GRAY + "" + ChatColor.UNDERLINE + "GitHub");
        githubLink.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://github.com/BT-Pluginz/GravePlugin"));
        githubLink.setUnderlined(true);
        sender.spigot().sendMessage(githubLink);

        TextComponent discordLink = new TextComponent(ChatColor.BLUE + "" + ChatColor.UNDERLINE + "Discord");
        discordLink.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://discord.pluginz.dev"));
        discordLink.setUnderlined(true);
        sender.spigot().sendMessage(discordLink);

        sender.sendMessage("If you have any issues please report them on GitHub or on our the Discord.");
        plugin.sendPluginMessages(sender, "line");
    }

    public boolean handleReload(CommandSender sender) {
        if (sender.hasPermission("btgraves.reload")) {
            plugin.getConfigManager().reloadConfig();
            sender.sendMessage(prefix + "The config reloaded.");
        } else {
            sender.sendMessage(prefix + ChatColor.RED +  "You do not have permission to reload the config.");
        }
        return true;
    }


    public boolean handleOpen(CommandSender sender, String[] args) {
        if (!sender.hasPermission("btgraves.admin.open")) {
            sender.sendMessage(prefix + ChatColor.RED + "You do not have permission to open graves.");
            return true;
        }
        if (!(sender instanceof Player)) {
            sender.sendMessage(prefix + ChatColor.RED + "Only players can open graves.");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(prefix + ChatColor.RED + "Please provide a grave ID.");
            return true;
        }

        String graveId = args[1];
        Player player = (Player) sender;
        plugin.getGraveInventoryManager().openGrave(player, UUID.fromString(graveId));

        return true;
    }

    private boolean handleCollect(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.only-player"))); // solo i giocatori possono usare
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("btgraves.collect")) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.no-permission")));
            return true;
        }

        Grave nearestGrave = this.findNearestPlayerGrave(player);

        if (nearestGrave == null) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.no-graves-near")));
            return true;
        }

        double distance = player.getLocation().distance(nearestGrave.getLocation());
        this.graveInventoryManager.openGrave(player, nearestGrave.getGraveId());

        player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.click")));
        return true;
    }

    private Grave findNearestPlayerGrave(Player player) {
        Grave nearestGrave = null;
        double nearestDistance = 10.0D;
        double MAX_DISTANCE = 10.0D;

        for (Grave grave : this.graveManager.getGraves().values()) {
            if (grave.getPlayerName().equals(player.getName()) && !grave.isExpired() && grave.getLocation().getWorld().equals(player.getWorld())) {
                double distance = player.getLocation().distance(grave.getLocation());

                if (distance < MAX_DISTANCE && distance < nearestDistance) {
                    nearestDistance = distance;
                    nearestGrave = grave;
                }
            }
        }

        return nearestGrave;
    }

    private boolean addItemToInventoryOrDrop(Player player, ItemStack item) {
        HashMap<Integer, ItemStack> leftOver = player.getInventory().addItem(new ItemStack[]{item});
        if (leftOver.isEmpty()) {
            return true;
        } else {

            for (ItemStack leftOverItem : leftOver.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), leftOverItem);
            }

            return false;
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(prefix + "Grave Plugin Commands:");
        sender.sendMessage(prefix + "/grave info - Show plugin information");
        sender.sendMessage(prefix + "/grave list - List all active graves");
        sender.sendMessage(prefix + "/grave list <player> - List graves for a specific player");
    }
}
