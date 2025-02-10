package com.butterdevelop.battleroyale;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class KitSelectionGUI implements Listener {

    public static KitManager kitManager;

    public KitSelectionGUI(BattleRoyalePlugin plugin) {
        kitManager = new KitManager(plugin);
    }

    public static void openKitSelection(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 9, ChatColor.DARK_PURPLE + "Выбор набора");

        int index = 0;
        for (String kitName : kitManager.getAvailableKits().keySet()) {
            ItemStack kitItem = new ItemStack(kitManager.getAvailableKits().get(kitName).getFirst());
            kitItem.setAmount(1);
            ItemMeta meta = kitItem.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.YELLOW + kitName);

                // Подсвечиваем выбор
                String playerKit = kitManager.getPlayerKit(player.getUniqueId());
                if (playerKit != null && playerKit.equals(kitName)) {
                    meta.addEnchant(Enchantment.UNBREAKING, 3, true);
                    meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                }
                kitItem.setItemMeta(meta);
            }
            inventory.setItem(index++, kitItem);
        }

        player.openInventory(inventory);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getView().getTitle().equals(ChatColor.DARK_PURPLE + "Выбор набора")) {
            event.setCancelled(true);

            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem != null && clickedItem.hasItemMeta() && clickedItem.getItemMeta() != null && clickedItem.getItemMeta().hasDisplayName()) {
                String kitName = ChatColor.stripColor(clickedItem.getItemMeta().getDisplayName());

                // Убираем выбор или наоборот делаем
                if (clickedItem.getItemMeta().hasEnchants()) {
                    kitManager.removePlayerKit(player);
                } else {
                    kitManager.setPlayerKit(player, kitName);
                }
                player.closeInventory();
            }
        }
    }
}
