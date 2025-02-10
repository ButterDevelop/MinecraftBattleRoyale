package com.butterdevelop.battleroyale;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Команда /stats - выводит статистику всех игроков в чат и показывает временный Scoreboard
 */
public class StatsCommand implements CommandExecutor {

    private final GameManager gameManager;

    public StatsCommand(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Эту команду может использовать только игрок!");
            return true;
        }

        player.sendMessage(ChatColor.DARK_PURPLE + "📊 Статистика игроков:");

        int maxStatLength = 0;
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            int currentStatLength = String.format("K: %-3d | D: %-3d | W: %-3d | G: %-3d",
                    gameManager.getScoreboardManager().getKills(onlinePlayer),
                    gameManager.getScoreboardManager().getDeaths(onlinePlayer),
                    gameManager.getScoreboardManager().getWins(onlinePlayer),
                    gameManager.getScoreboardManager().getGamesPlayed(onlinePlayer)).length();
            maxStatLength = Math.max(maxStatLength, currentStatLength);
        }

        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            String formattedStats = String.format(ChatColor.GOLD + "K: %-3d" + ChatColor.GRAY + " | " +
                            ChatColor.RED    + "D: %-3d" + ChatColor.GRAY + " | " +
                            ChatColor.GREEN  + "W: %-3d" + ChatColor.GRAY + " | " +
                            ChatColor.AQUA   + "G: %-3d" + ChatColor.GRAY + " | " +
                            ChatColor.YELLOW + "%-"      + (maxStatLength + 1) + "s",
                    gameManager.getScoreboardManager().getKills(onlinePlayer),
                    gameManager.getScoreboardManager().getDeaths(onlinePlayer),
                    gameManager.getScoreboardManager().getWins(onlinePlayer),
                    gameManager.getScoreboardManager().getGamesPlayed(onlinePlayer),
                    onlinePlayer.getName());

            player.sendMessage(formattedStats);
        }

        // Показываем временный Scoreboard
        gameManager.getScoreboardManager().showTemporaryScoreboard(player);

        return true;
    }
}
