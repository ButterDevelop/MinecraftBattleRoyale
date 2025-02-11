package com.butterdevelop.battleroyale;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Команда /kitselect для выбора команды
 */
public class KitSelectionCommand implements CommandExecutor {

    private final GameManager gameManager;

    public KitSelectionCommand(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Эту команду можно использовать только в игре.");
            return true;
        }

        // Не даём пользователю выбирать кит-набор во время игры
        if (gameManager.isGameStarted()) {
            sender.sendMessage(ChatColor.RED + "Невозможно выбрать кит-набор во время игры.");
            return true;
        }

        KitSelectionGUI.openKitSelection(player);
        return true;
    }
}
