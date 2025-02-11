package com.butterdevelop.battleroyale;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Команда /voteend для голосования на окончание игры.
 */
public class VoteEndCommand implements CommandExecutor {

    private final BattleRoyalePlugin plugin;

    public VoteEndCommand(BattleRoyalePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        Player player = Bukkit.getPlayer(sender.getName());

        if (plugin.getGameManager().getVotedForEndPlayers().contains(player)) {
            sender.sendMessage(ChatColor.RED + "Вы уже проголосовали за окончание игры!");
            return true;
        }

        if (!plugin.getGameManager().isGameStarted()) {
            sender.sendMessage(ChatColor.RED + "Игра не запущена!");
            return true;
        }

        if (player == null) {
            return true;
        }

        // Добавляем игрока в проголосовавшие за окончание игры
        plugin.getGameManager().addVotedForEndPlayer(player.getUniqueId());

        Bukkit.broadcastMessage(ChatColor.GREEN + "Игрок " + ChatColor.BOLD + sender.getName() + ChatColor.GREEN + " проголосовал за окончание игры (" +
                plugin.getGameManager().getVotedForEndPlayers().size() + "/" + plugin.getGameManager().getWaitingPlayers().size() +
                "). Это можно сделать с помощью команды " + ChatColor.BOLD + "/voteend" +
                ChatColor.GREEN + ", если, к примеру, попался не лучший сид.");

        // Если все игроки согласились окончить игру
        if (plugin.getGameManager().getVotedForEndPlayers().size() == plugin.getGameManager().getWaitingPlayers().size()) {
            Bukkit.broadcastMessage(ChatColor.AQUA + "" + ChatColor.BOLD + "Игроки проголосовали за окончание игры. Игра объявлена ничьей.");

            // Отнимаем игру из статистики (не засчитываем её)
            plugin.getGameManager().getPlayingPlayers().forEach(playerId -> {
                Player p = Bukkit.getPlayer(playerId);
                if (p != null) {
                    plugin.getGameManager().getScoreboardManager().removeGame(p);
                }
            });

            // Завершаем игру
            plugin.getGameManager().endGame();
        }

        return true;
    }
}
