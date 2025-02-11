package com.butterdevelop.battleroyale;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class KitSelectionGUI implements Listener {

    public static KitManager kitManager;

    public static final int INVENTORY_INITIAL_SLOTS_AMOUNT = 18;

    public KitSelectionGUI(BattleRoyalePlugin plugin) {
        kitManager = new KitManager(plugin);
    }

    public static void openKitSelection(Player player) {
        Inventory inventory = Bukkit.createInventory(null, INVENTORY_INITIAL_SLOTS_AMOUNT,
                ChatColor.DARK_GRAY + "" + ChatColor.BOLD + "Выбор набора");

        int index = 0;
        for (String kitName : kitManager.getAvailableKits().keySet()) {
            List<ItemStack> kitItems = kitManager.getAvailableKits().get(kitName);

            ItemStack kitItem = new ItemStack(kitItems.getFirst());
            kitItem.setAmount(1);
            ItemMeta meta = kitItem.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + kitName);

                // Подсвечиваем выбор
                String playerKit = kitManager.getPlayerKit(player.getUniqueId());
                if (playerKit != null && playerKit.equals(kitName)) {
                    meta.addEnchant(Enchantment.UNBREAKING, 3, true);
                    meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                }

                // Добавляем описание наборов
                List<String> lore = new ArrayList<>();
                lore.add(" ");
                lore.add(ChatColor.GRAY + "" + ChatColor.BOLD + "Предметы набора:");
                kitItems.forEach(item -> {
                    if (item.getItemMeta() != null) {
                        lore.add(ChatColor.GRAY + item.getItemMeta().getDisplayName() + " (x" + item.getAmount() + ")");
                    }
                });

                // Выставляем описание предмета
                meta.setLore(lore);

                // Выставляем метаданные для предмета
                kitItem.setItemMeta(meta);
            }
            inventory.setItem(index++, kitItem);
        }

        // Пропуск предметов для красоты
        for (int i = 0; i < INVENTORY_INITIAL_SLOTS_AMOUNT - index; i++) {
            ItemStack template = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
            ItemMeta meta = template.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(" ");
                meta.setMaxStackSize(1);
                template.setItemMeta(meta);
            }
            inventory.addItem(template);
        }

        // Открываем интерфейс
        player.openInventory(inventory);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getView().getTitle().equals(ChatColor.DARK_GRAY + "" + ChatColor.BOLD + "Выбор набора")) {
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
