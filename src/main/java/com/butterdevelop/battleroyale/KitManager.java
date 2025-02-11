package com.butterdevelop.battleroyale;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemRarity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

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
        ItemStack potionTurtlePower = createItem(Material.SPLASH_POTION, ChatColor.DARK_AQUA + "Зелье Танка", 1);
        PotionMeta potionTurtlePowerMeta = (PotionMeta)potionTurtlePower.getItemMeta();
        PotionEffect potionTurtlePowerEffect1 = new PotionEffect(PotionEffectType.RESISTANCE, 20 * 20, 2, false, true);
        PotionEffect potionTurtlePowerEffect2 = new PotionEffect(PotionEffectType.SLOWNESS, 20 * 20, 3, false, true);
        if (potionTurtlePowerMeta != null) {
            potionTurtlePowerMeta.setColor(Color.WHITE);
            potionTurtlePowerMeta.addCustomEffect(potionTurtlePowerEffect1, false);
            potionTurtlePowerMeta.addCustomEffect(potionTurtlePowerEffect2, false);
            potionTurtlePower.setItemMeta(potionTurtlePowerMeta);
        }
        ItemStack leatherChestplate = createItem(Material.LEATHER_CHESTPLATE, ChatColor.BLUE + "Броня Танка");
        leatherChestplate.addEnchantment(Enchantment.PROTECTION, 4);
        kits.put("Танк", Arrays.asList(
                createItem(Material.SHIELD, ChatColor.BLUE + "Щит Танка"),
                leatherChestplate,
                potionTurtlePower
        ));

        // Набор "Инженер"
        kits.put("Инженер", Arrays.asList(
                createItem(Material.PISTON, ChatColor.GREEN + "Поршень Инженера", 4),
                createItem(Material.STICKY_PISTON, ChatColor.GREEN + "Липкий поршень Инженера", 4),
                createItem(Material.SLIME_BLOCK, ChatColor.GREEN + "Блоки слизи Инженера", 4),
                createItem(Material.HONEY_BLOCK, ChatColor.GREEN + "Блоки мёда Инженера", 4),
                createItem(Material.OBSERVER, ChatColor.GREEN + "Наблюдатель Инженера", 4)
        ));

        // Набор "Работяга"
        kits.put("Работяга", Arrays.asList(
                createItem(Material.STONE_AXE,     ChatColor.DARK_GRAY + "Топор Работяги",  1),
                createItem(Material.STONE_PICKAXE, ChatColor.DARK_GRAY + "Кирка Работяги",  1),
                createItem(Material.STONE_SHOVEL,  ChatColor.DARK_GRAY + "Лопата Работяги", 1)
        ));

        // Набор "Путешественник"
        ItemStack elytra = createItem(Material.ELYTRA, ChatColor.DARK_GREEN + "Крылья Путешественника", 1);
        elytra.setDurability((short)427);
        kits.put("Путешественник", Arrays.asList(
                elytra,
                createItem(Material.FIREWORK_ROCKET, ChatColor.DARK_GREEN + "Фейерверк Путешественника", 1)
        ));

        // Набор "Рико"
        kits.put("Рико", Arrays.asList(
                createItem(Material.TNT, ChatColor.DARK_RED + "TnT Рико", 5),
                createItem(Material.FLINT_AND_STEEL, ChatColor.DARK_RED + "Зажигалка Рико", 1)
        ));

        // Набор "Химик"
        ItemStack potionSpeed = createItem(Material.SPLASH_POTION, ChatColor.DARK_AQUA + "Зелье Химика", 1);
        PotionMeta potionSpeedMeta = (PotionMeta)potionSpeed.getItemMeta();
        PotionEffect potionSpeedEffect = new PotionEffect(PotionEffectType.SPEED, 8 * 60 * 20, 1, false, true);
        if (potionSpeedMeta != null) {
            potionSpeedMeta.setColor(Color.WHITE);
            potionSpeedMeta.addCustomEffect(potionSpeedEffect, false);
            potionSpeed.setItemMeta(potionSpeedMeta);
        }
        kits.put("Химик", Arrays.asList(
                potionSpeed,
                createItem(Material.GLASS_BOTTLE, ChatColor.DARK_AQUA + "Бутылочка Химика", 3)
        ));

        // Набор "Инджрих"
        ItemStack mace = createItem(Material.MACE, ChatColor.DARK_PURPLE + "Булава Инджриха", 1);
        mace.setDurability((short)499);
        kits.put("Инджрих", Arrays.asList(
                mace,
                createItem(Material.WIND_CHARGE, ChatColor.DARK_PURPLE + "Заряд ветра Инджриха", 5)
        ));

        // Набор "Антонио"
        ItemStack woodenHoe = createItem(Material.WOODEN_HOE, ChatColor.DARK_BLUE + "Деревянная мотыга из сейфа Антонио", 3);
        woodenHoe.addEnchantment(Enchantment.FORTUNE, 1);
        kits.put("Антонио", Arrays.asList(
                createItem(Material.CARROT, ChatColor.DARK_BLUE + "Морковки из сейфа Антонио", 8),
                createItem(Material.BONE_MEAL, ChatColor.DARK_BLUE + "Костная мука из сейфа Антонио", 3),
                woodenHoe
        ));

        // Набор "Обманка"
        ItemStack woodenPickaxe = createItem(Material.WOODEN_PICKAXE, ChatColor.RED + "Деревянная кирка-обманка", 1);
        woodenPickaxe.addEnchantment(Enchantment.SILK_TOUCH, 1);
        kits.put("Обманка", Arrays.asList(
                createItem(Material.ENDER_CHEST, ChatColor.RED + "Эндер-сундук-обманка", 1),
                woodenPickaxe
        ));

        // Набор "Дрессировщик"
        kits.put("Дрессировщик", Arrays.asList(
                createItem(Material.LEAD, ChatColor.YELLOW + "Поводок Дрессировщика", 1),
                createItem(Material.CAT_SPAWN_EGG, ChatColor.YELLOW + "Яйцо спавна кошки Дрессировщика", 4),
                createItem(Material.WOLF_SPAWN_EGG, ChatColor.YELLOW + "Яйцо спавна волка Дрессировщика", 4),
                createItem(Material.IRON_GOLEM_SPAWN_EGG, ChatColor.YELLOW + "Яйцо спавна железного голема Дрессировщика", 1)
        ));

        // Набор "Музыкант"
        kits.put("Музыкант", Arrays.asList(
                createItem(Material.JUKEBOX,        ChatColor.BLUE + "Бумбокс Музыканта", 1),
                createItem(Material.MUSIC_DISC_5,   ChatColor.BLUE + "Музыкальная пластинка Музыканта", 1),
                createItem(Material.MUSIC_DISC_11,  ChatColor.BLUE + "Музыкальная пластинка Музыканта", 1),
                createItem(Material.MUSIC_DISC_13,  ChatColor.BLUE + "Музыкальная пластинка Музыканта", 1),
                createItem(Material.MUSIC_DISC_CAT, ChatColor.BLUE + "Музыкальная пластинка Музыканта", 1)
        ));

        // Набор "Король"
        ItemStack kingHelmet     = createItem(Material.GOLDEN_HELMET,     ChatColor.GOLD + "Корона Короля",    1);
        kingHelmet.addEnchantment(Enchantment.BINDING_CURSE, 1);
        kingHelmet.addEnchantment(Enchantment.UNBREAKING,    3);

        ItemStack kingChestplate = createItem(Material.GOLDEN_CHESTPLATE, ChatColor.GOLD + "Нагрудник Короля", 1);
        kingChestplate.addEnchantment(Enchantment.BINDING_CURSE, 1);
        kingChestplate.addEnchantment(Enchantment.UNBREAKING,    3);

        ItemStack kingLeggins    = createItem(Material.GOLDEN_LEGGINGS,   ChatColor.GOLD + "Поножи Короля",    1);
        kingLeggins.addEnchantment(Enchantment.BINDING_CURSE, 1);
        kingLeggins.addEnchantment(Enchantment.UNBREAKING,    3);

        ItemStack kingBoots      = createItem(Material.GOLDEN_BOOTS,      ChatColor.GOLD + "Ботинки Короля",   1);
        kingBoots.addEnchantment(Enchantment.BINDING_CURSE, 1);
        kingBoots.addEnchantment(Enchantment.UNBREAKING,    3);

        kits.put("Король", Arrays.asList(
                createItem(Material.GOLD_INGOT, ChatColor.GOLD + "Золотой слиток Короля", 1),
                kingHelmet,
                kingChestplate,
                kingLeggins,
                kingBoots
        ));

        // Набор "Землекоп"
        ItemStack dirt = createItem(Material.DIRT, ChatColor.GREEN + "Земля Землекопа", 1);
        ItemMeta dirtMeta = dirt.getItemMeta();
        if (dirtMeta != null) {
            dirtMeta.setRarity(ItemRarity.EPIC);
            dirtMeta.addEnchant(Enchantment.KNOCKBACK, 5, true);
            dirt.setItemMeta(dirtMeta);
        }
        kits.put("Землекоп", Arrays.asList(
                createItem(Material.DIAMOND_SHOVEL, ChatColor.GREEN + "Лопата Землекопа", 1),
                dirt
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
