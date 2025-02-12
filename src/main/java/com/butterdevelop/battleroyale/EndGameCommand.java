package com.butterdevelop.battleroyale;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
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
        // Нужно быть администратором
        if (!sender.isOp()) {
            sender.sendMessage(ChatColor.RED + "Вы не администратор, чтобы использовать эту команду!");
            return true;
        }

        // Если ещё не началась игра
        if (!plugin.getGameManager().isGameStarted()) {
            sender.sendMessage(ChatColor.RED + "Игра не запущена!");
            return true;
        }

        // Отнимаем игру из статистики (не засчитываем её)
        plugin.getGameManager().getPlayingPlayers().forEach(playerId -> {
            Player p = Bukkit.getPlayer(playerId);
            if (p != null) {
                plugin.getGameManager().getScoreboardManager().removeGame(p);
            }
        });

        // Останавливаем игру
        plugin.getGameManager().endGame();

        // Уведомляем пользователя
        Bukkit.broadcastMessage(ChatColor.GREEN + "Игра остановлена!");

        return true;
    }
}
