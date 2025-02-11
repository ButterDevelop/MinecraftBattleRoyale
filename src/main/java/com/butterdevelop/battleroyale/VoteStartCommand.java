package com.butterdevelop.battleroyale;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Команда /votestart для голосования на окончание игры.
 */
public class VoteStartCommand implements CommandExecutor {

    private final BattleRoyalePlugin plugin;

    public VoteStartCommand(BattleRoyalePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        Player player = Bukkit.getPlayer(sender.getName());

        if (player == null) {
            return true;
        }

        if (plugin.getGameManager().getVotedForStartPlayers().contains(player.getUniqueId())) {
            sender.sendMessage(ChatColor.RED + "Вы уже проголосовали за начало игры!");
            return true;
        }

        if (plugin.getGameManager().isGameStarted() || plugin.getGameManager().prepareGameTask != null) {
            sender.sendMessage(ChatColor.RED + "Игра уже запущена!");
            return true;
        }

        // Добавляем игрока в проголосовавшие за начало игры
        plugin.getGameManager().addVotedForStartPlayer(player.getUniqueId());

        Bukkit.broadcastMessage(ChatColor.GREEN + "Игрок " + ChatColor.BOLD + sender.getName() + ChatColor.GREEN + " проголосовал за начало игры (" +
                plugin.getGameManager().getVotedForStartPlayers().size() + "/" + plugin.getGameManager().getWaitingPlayers().size() +
                "). Это можно сделать с помощью команды " + ChatColor.BOLD + "/votestart");

        // Если все игроки согласились начать игру
        if (plugin.getGameManager().getVotedForStartPlayers().size() == Bukkit.getOnlinePlayers().size()) {
            Bukkit.broadcastMessage(ChatColor.AQUA + "" + ChatColor.BOLD + "Игроки проголосовали за начало игры. Игра начинается.");

            // Начинаем игру
            plugin.getGameManager().startGame();
        }

        return true;
    }
}
