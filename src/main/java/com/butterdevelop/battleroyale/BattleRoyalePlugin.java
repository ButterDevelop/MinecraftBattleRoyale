package com.butterdevelop.battleroyale;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Objects;
import java.util.UUID;

public class BattleRoyalePlugin extends JavaPlugin {

    private GameManager gameManager;
    private HashMap<UUID, PermissionAttachment> permissions;

    @Override
    public void onEnable() {
        // Сохраняем дефолтный config.yml (если его ещё нет)
        saveDefaultConfig();

        // Инициализируем менеджер игры
        gameManager = new GameManager(this);

        // Инициализируем разрешения
        permissions = new HashMap<>();

        // Регистрируем слушателей событий
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerDeathListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerPortalListener(this), this);
        getServer().getPluginManager().registerEvents(new TeamSelectionListener(gameManager), this);
        getServer().getPluginManager().registerEvents(new KitSelectionGUI(this), this);
        getServer().getPluginManager().registerEvents(new PluginItemsListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerInteractListener(), this);

        // Регистрируем команды
        Objects.requireNonNull(getCommand("startgame")).setExecutor(new StartGameCommand(this));
        Objects.requireNonNull(getCommand("endgame")).setExecutor(new EndGameCommand(this));
        Objects.requireNonNull(getCommand("votestart")).setExecutor(new VoteStartCommand(this));
        Objects.requireNonNull(getCommand("voteend")).setExecutor(new VoteEndCommand(this));
        // Регистрируем команду для выбора команды (открытие GUI)
        Objects.requireNonNull(getCommand("teamselect")).setExecutor(new TeamSelectionCommand(gameManager));
        Objects.requireNonNull(getCommand("kitselect")).setExecutor(new KitSelectionCommand(gameManager));
        Objects.requireNonNull(getCommand("stats")).setExecutor(new StatsCommand(gameManager));

        getLogger().info("BattleRoyalePlugin включён!");

        // Добавляем всех игроков, которые на сервере, в список ждущих
        Bukkit.getOnlinePlayers().forEach(player -> gameManager.addWaitingPlayer(player.getUniqueId()));
    }

    @Override
    public void onDisable() {
        if (gameManager != null) {
            gameManager.shutdown();
        }
        if (permissions != null) {
            permissions.clear();
        }
        getLogger().info("BattleRoyalePlugin выключён!");
    }

    public GameManager getGameManager() {
        return gameManager;
    }

    public void setPermission(Player player, String permission, boolean value) {
        if (!this.isEnabled()) {
            return;
        }

        PermissionAttachment attachment = player.addAttachment(this);
        permissions.put(player.getUniqueId(), attachment);

        PermissionAttachment playerPerms = permissions.get(player.getUniqueId());
        playerPerms.setPermission(permission, value);
    }
}
