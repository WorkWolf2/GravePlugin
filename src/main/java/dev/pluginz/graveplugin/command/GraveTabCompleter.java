package dev.pluginz.graveplugin.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GraveTabCompleter implements TabCompleter {

    public GraveTabCompleter() {}

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        List<String> commands = new ArrayList<>();

        if (args.length == 1) {
            if (sender.hasPermission("btgraves.reload")) {
                commands.add("reload");
            }
            commands.add("info");
            commands.add("collect");
            StringUtil.copyPartialMatches(args[0], commands, completions);
        }

        Collections.sort(completions);
        return completions;
    }
}
