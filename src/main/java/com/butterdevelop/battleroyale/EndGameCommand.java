package com.butterdevelop.battleroyale;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

/**
 * Команда /endgame для ручной остановки игры.
 */
public class EndGameCommand implements CommandExecutor {

    private final BattleRoyalePlugin plugin;

    public EndGameCommand(BattleRoyalePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.isOp()) {
            sender.sendMessage(ChatColor.RED + "Вы не администратор, чтобы использовать эту команду!");
            return true;
        }

        if (!plugin.getGameManager().isGameStarted()) {
            sender.sendMessage(ChatColor.RED + "Игра не запущена!");
            return true;
        }
        plugin.getGameManager().endGame();
        sender.sendMessage(ChatColor.GREEN + "Игра остановлена!");
        return true;
    }
}
