package com.butterdevelop.battleroyale;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

/**
 * Команда /startgame для ручного запуска игры.
 */
public class StartGameCommand implements CommandExecutor {

    private final BattleRoyalePlugin plugin;

    public StartGameCommand(BattleRoyalePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.isOp()) {
            sender.sendMessage(ChatColor.RED + "Вы не администратор, чтобы использовать эту команду!");
            return true;
        }

        if (plugin.getGameManager().isGameStarted() || plugin.getGameManager().prepareGameTask != null) {
            sender.sendMessage(ChatColor.RED + "Игра уже запущена!");
            return true;
        }
        plugin.getGameManager().startGame();
        sender.sendMessage(ChatColor.GREEN + "Игра начала запуск!");
        return true;
    }
}
