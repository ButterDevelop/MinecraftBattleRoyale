package com.butterdevelop.battleroyale;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public class BattleRoyalePlugin extends JavaPlugin {

    private GameManager gameManager;

    @Override
    public void onEnable() {
        // Сохраняем дефолтный config.yml (если его ещё нет)
        saveDefaultConfig();

        // Инициализируем менеджер игры
        gameManager = new GameManager(this);

        // Регистрируем слушателей событий
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerDeathListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerPortalListener(this), this);
        getServer().getPluginManager().registerEvents(new TeamSelectionListener(gameManager), this);
        getServer().getPluginManager().registerEvents(new KitSelectionGUI(this), this);
        getServer().getPluginManager().registerEvents(new PluginItemsListener(gameManager), this);

        // Регистрируем команды
        Objects.requireNonNull(getCommand("startgame")).setExecutor(new StartGameCommand(this));
        Objects.requireNonNull(getCommand("endgame")).setExecutor(new EndGameCommand(this));
        Objects.requireNonNull(getCommand("votestart")).setExecutor(new VoteStartCommand(this));
        Objects.requireNonNull(getCommand("voteend")).setExecutor(new VoteEndCommand(this));
        // Регистрируем команду для выбора команды (открытие GUI)
        Objects.requireNonNull(getCommand("teamselect")).setExecutor(new TeamSelectionCommand(gameManager));
        Objects.requireNonNull(getCommand("kitselect")).setExecutor(new KitSelectionCommand());
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
        getLogger().info("BattleRoyalePlugin выключён!");
    }

    public GameManager getGameManager() {
        return gameManager;
    }
}
