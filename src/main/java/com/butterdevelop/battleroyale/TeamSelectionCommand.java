package com.butterdevelop.battleroyale;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Команда /teamselect для выбора команды
 */
public class TeamSelectionCommand implements CommandExecutor {

    private final GameManager gameManager;

    public TeamSelectionCommand(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Эту команду можно использовать только в игре.");
            return true;
        }

        TeamSelectionGUI.openTeamSelection(player, gameManager);
        return true;
    }
}
