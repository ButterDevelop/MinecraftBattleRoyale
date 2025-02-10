package com.butterdevelop.battleroyale;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.Objects;

public class TeamSelectionGUI {

    public static final Material RANDOM_TEAM_MATERIAL = Material.ENCHANTING_TABLE;
    public static final String   INVENTORY_TITLE      = ChatColor.BOLD + "Выбор команды";

    public static final int INVENTORY_INITIAL_SLOTS_AMOUNT = 18;

    public static void openTeamSelection(Player player, GameManager gameManager) {
        // Создаём инвентарь размером INVENTORY_INITIAL_SLOTS_AMOUNT слотов (можно увеличить, если команд больше)
        Inventory inv = Bukkit.createInventory(null, INVENTORY_INITIAL_SLOTS_AMOUNT, INVENTORY_TITLE);

        // Получаем команду игрока
        String playerTeam = gameManager.getTeam(player.getUniqueId());

        int activeSlots = 0;
        // Для каждой доступной команды создаём предмет (блок Concrete соответствующего цвета)
        for (TeamInfo teamInfo : gameManager.getAvailableTeams().values()) {
            Material material = switch (teamInfo.getColor()) {
                case RED         -> Material.RED_CONCRETE;
                case BLUE        -> Material.BLUE_CONCRETE;
                case GREEN       -> Material.GREEN_CONCRETE;
                case YELLOW      -> Material.YELLOW_CONCRETE;
                case AQUA        -> Material.LIGHT_BLUE_CONCRETE;
                case GOLD        -> Material.ORANGE_CONCRETE;
                case DARK_PURPLE -> Material.PURPLE_CONCRETE;
                case GRAY        -> Material.GRAY_CONCRETE;
                default          -> RANDOM_TEAM_MATERIAL;
            };
            // Определяем материал по цвету (простой пример)
            ItemStack item = new ItemStack(material, 1);
            ItemMeta meta = item.getItemMeta();
            Objects.requireNonNull(meta).setDisplayName(ChatColor.BOLD + teamInfo.getName());

            // Отображаем как выбранное с помощью зачарования (если выбрано)
            if (playerTeam != null && playerTeam.equals(teamInfo.getName())) {
                meta.addEnchant(Enchantment.UNBREAKING, 3, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }

            // Формируем lore с информацией о игроках в команде
            List<String> lore = new java.util.ArrayList<>();
            List<String> teamPlayers = gameManager.getTeamPlayers(teamInfo.getName());
            if (teamPlayers.isEmpty()) {
                lore.add(ChatColor.GRAY + "В команде пока никого нет.");
            } else {
                lore.add(ChatColor.GRAY + "Игроки: ");
                teamPlayers.forEach(teamPlayer -> lore.add(ChatColor.GRAY + "" + ChatColor.BOLD + teamPlayer));
            }
            meta.setLore(lore);
            item.setItemMeta(meta);
            inv.addItem(item);

            // Увеличиваем счётчик слотов
            activeSlots++;
        }

        // Пропуск предметов для красоты
        for (int i = 0; i < INVENTORY_INITIAL_SLOTS_AMOUNT - activeSlots - 1; i++) {
            ItemStack template = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
            ItemMeta meta = template.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(" ");
                meta.setMaxStackSize(1);
                template.setItemMeta(meta);
            }
            inv.addItem(template);
        }

        // Добавляем кнопку для случайного распределения
        ItemStack item = new ItemStack(RANDOM_TEAM_MATERIAL, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            // Выставляем пустое имя для заполнителя
            meta.setDisplayName(ChatColor.ITALIC + "" + ChatColor.WHITE + "Случайная команда");

            // Отображаем как выбранное с помощью зачарования (если выбрано)
            if (playerTeam == null) {
                meta.addEnchant(Enchantment.UNBREAKING, 3, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }

            item.setItemMeta(meta);
        }
        inv.addItem(item);

        // Открываем инвентарь для игрока
        player.openInventory(inv);
    }
}
