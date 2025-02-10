package com.butterdevelop.battleroyale;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class PluginItemsListener implements Listener {

    private final GameManager gameManager;

    public PluginItemsListener(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    @EventHandler
    public void onPlayerUseItem(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item == null || (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK)) return;

        if (item.hasItemMeta() && item.getItemMeta() != null && item.getItemMeta().hasDisplayName()) {
            String displayName = ChatColor.stripColor(item.getItemMeta().getDisplayName());

            switch (item.getType()) {
                case COMPASS:
                    if (displayName.equals("Выбор команды")) {
                        event.setCancelled(true);
                        TeamSelectionGUI.openTeamSelection(player, gameManager);
                    }
                    break;

                case PAPER:
                    if (displayName.equals("Статистика")) {
                        event.setCancelled(true);
                        player.performCommand("stats");
                    }
                    break;

                case AMETHYST_SHARD:
                    if (displayName.equals("Голосовать за старт")) {
                        event.setCancelled(true);
                        player.performCommand("votestart");
                    }
                    break;

                case CHEST:
                    if (displayName.equals("Выбрать кит")) {
                        event.setCancelled(true);
                        KitSelectionGUI.openKitSelection(player);
                    }
                    break;
            }
        }
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        ItemStack item = event.getItemDrop().getItemStack();

        if (item.hasItemMeta() && item.getItemMeta() != null && item.getItemMeta().hasDisplayName()) {
            String displayName = ChatColor.stripColor(item.getItemMeta().getDisplayName());

            // Запрещаем выбрасывать все три предмета
            if (displayName.equals("Выбор команды") ||
                    displayName.equals("Статистика") ||
                    displayName.equals("Голосовать за старт")) {
                event.setCancelled(true);
            }
        }
    }
}
