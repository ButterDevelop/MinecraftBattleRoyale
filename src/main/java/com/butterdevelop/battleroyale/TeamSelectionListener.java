package com.butterdevelop.battleroyale;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.Objects;

public class TeamSelectionListener implements Listener {

    private final GameManager gameManager;

    public TeamSelectionListener(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTitle().equals(TeamSelectionGUI.INVENTORY_TITLE)) {
            event.setCancelled(true); // Отменяем стандартное поведение

            if (event.getCurrentItem() == null) {
                return;
            }

            Player player = (Player) event.getWhoClicked();

            if (event.getCurrentItem().getType() == TeamSelectionGUI.RANDOM_TEAM_MATERIAL) {
                player.sendMessage(ChatColor.GREEN + "Вы выбрали быть в случайной команде!");
                gameManager.removeTeam(player.getUniqueId());
                player.closeInventory();
                return;
            }

            if (!event.getCurrentItem().getType().toString().endsWith("CONCRETE")) {
                return;
            }

            if (event.getCurrentItem().getItemMeta() != null && event.getCurrentItem().getItemMeta().hasEnchants()) {
                player.sendMessage(ChatColor.GREEN + "Вы " + ChatColor.YELLOW + ChatColor.BOLD + "убрали" + ChatColor.GREEN +
                        " свой " + ChatColor.BOLD + "выбор команды" + ChatColor.GREEN + ". Тогда вы появитесь в случайной.");
                gameManager.removeTeam(player.getUniqueId());
                player.closeInventory();
                return;
            }

            String teamName = Objects.requireNonNull(event.getCurrentItem().getItemMeta()).getDisplayName();

            // Удаляем цветовые коды
            teamName = ChatColor.stripColor(teamName);
            gameManager.setTeam(player.getUniqueId(), teamName);
            player.closeInventory();
        }
    }
}
