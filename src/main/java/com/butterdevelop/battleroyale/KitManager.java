package com.butterdevelop.battleroyale;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class KitManager {

    private final File              kitsFile;
    private final FileConfiguration kitsConfig;

    private final Map<String, List<ItemStack>> kits         = new HashMap<>();
    private final Map<UUID, String>            selectedKits = new HashMap<>();

    public KitManager(JavaPlugin plugin) {
        this.kitsFile   = new File(plugin.getDataFolder(), "kits.yml");
        this.kitsConfig = YamlConfiguration.loadConfiguration(kitsFile);

        initializeKits();
        loadPlayerKits();
    }

    /**
     * Создаёт доступные наборы предметов.
     */
    private void initializeKits() {
        // Набор "Воин"
        kits.put("Воин", Arrays.asList(
                createItem(Material.IRON_SWORD, ChatColor.RED + "Меч Воина"),
                createItem(Material.LEATHER_HELMET, ChatColor.RED + "Шлем Воина")
        ));

        // Набор "Лучник"
        kits.put("Лучник", Arrays.asList(
                createItem(Material.BOW, ChatColor.YELLOW + "Лук Лучника"),
                createItem(Material.ARROW, ChatColor.YELLOW + "Стрелы", 16)
        ));

        // Набор "Танк"
        kits.put("Танк", Arrays.asList(
                createItem(Material.LEATHER_CHESTPLATE, ChatColor.BLUE + "Броня Танка"),
                createItem(Material.SHIELD, ChatColor.BLUE + "Щит Танка")
        ));

        // Набор "Инженер"
        kits.put("Инженер", Arrays.asList(
                createItem(Material.PISTON, ChatColor.GREEN + "Поршень Инженера", 4),
                createItem(Material.STICKY_PISTON, ChatColor.GREEN + "Липкий поршень Инженера", 4),
                createItem(Material.SLIME_BLOCK, ChatColor.GREEN + "Блоки слизи Инженера", 4)
        ));

        // Набор "Работяга"
        kits.put("Работяга", Arrays.asList(
                createItem(Material.STONE_AXE, ChatColor.GRAY + "Топор Работяги", 1),
                createItem(Material.STONE_PICKAXE, ChatColor.GRAY + "Кирка Работяги", 1),
                createItem(Material.STONE_SHOVEL, ChatColor.GRAY + "Лопата Работяги", 1)
        ));

        // Набор "Путешественник"
        ItemStack elytra = createItem(Material.ELYTRA, ChatColor.GRAY + "Крылья Путешественника", 1);
        elytra.setDurability((short)427);
        kits.put("Путешественник", Arrays.asList(
                elytra,
                createItem(Material.FIREWORK_ROCKET, ChatColor.GRAY + "Фейерверк Путешественника", 1)
        ));
    }

    /**
     * Создаёт предмет с заданным именем.
     */
    private ItemStack createItem(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * Создаёт предмет с заданным именем и количеством.
     */
    private ItemStack createItem(Material material, String name, int amount) {
        ItemStack item = createItem(material, name);
        item.setAmount(amount);
        return item;
    }

    /**
     * Сохраняет выбранный кит игрока в `kits.yml`.
     */
    public void setPlayerKit(Player player, String kitName) {
        if (kits.containsKey(kitName)) {
            selectedKits.put(player.getUniqueId(), kitName);
            kitsConfig.set("players." + player.getUniqueId(), kitName);
            saveKitsFile();
            player.sendMessage(ChatColor.GREEN + "Вы выбрали набор: " + ChatColor.BOLD + kitName);
        } else {
            player.sendMessage(ChatColor.RED + "Такого набора не существует!");
        }
    }

    /**
     * Загружает выбор китов из `kits.yml`.
     */
    private void loadPlayerKits() {
        if (kitsConfig.isConfigurationSection("players")) {
            for (String uuidStr : Objects.requireNonNull(kitsConfig.getConfigurationSection("players")).getKeys(false)) {
                UUID playerId = UUID.fromString(uuidStr);
                String kitName = kitsConfig.getString("players." + uuidStr);
                selectedKits.put(playerId, kitName);
            }
        }
    }

    /**
     * Сохраняет файл `kits.yml`.
     */
    private void saveKitsFile() {
        try {
            kitsConfig.save(kitsFile);
        } catch (IOException e) {
            Bukkit.getLogger().severe("Ошибка сохранения файла kits.yml: " + e.getMessage());
        }
    }

    /**
     * Возвращает выбранный кит игрока.
     */
    public String getPlayerKit(UUID playerId) {
        return selectedKits.get(playerId);
    }

    /**
     * Выдаёт предметы игроку по его выбранному киту.
     */
    public void giveKit(Player player) {
        String kitName = selectedKits.get(player.getUniqueId());
        if (kitName != null && kits.containsKey(kitName)) {
            player.getInventory().addItem(kits.get(kitName).toArray(new ItemStack[0]));
            player.sendMessage(ChatColor.GOLD + "Вы получили набор: " + ChatColor.BOLD + kitName);
        } else {
            player.sendMessage(ChatColor.YELLOW + "Вы не выбрали набор! Используйте меню выбора в следующий раз.");
        }
    }

    /**
     * Удаляет выбранный кит игрока.
     */
    public void removePlayerKit(Player player) {
        selectedKits.remove(player.getUniqueId());
        kitsConfig.set("players." + player.getUniqueId(), null);
        saveKitsFile();
        player.sendMessage(ChatColor.GREEN + "Вы убрали свой кит-набор.");
    }

    /**
     * Возвращает все доступные киты.
     */
    public Map<String, List<ItemStack>> getAvailableKits() {
        return kits;
    }
}
