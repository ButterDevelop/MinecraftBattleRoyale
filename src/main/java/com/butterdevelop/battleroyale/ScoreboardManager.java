package com.butterdevelop.battleroyale;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.*;

import java.io.*;
import java.util.*;

public class ScoreboardManager {
    private final JavaPlugin plugin;
    private final Map<UUID, Integer> kills       = new HashMap<>();
    private final Map<UUID, Integer> deaths      = new HashMap<>();
    private final Map<UUID, Integer> wins        = new HashMap<>();
    private final Map<UUID, Integer> gamesPlayed = new HashMap<>();

    private final File statsFile;

    public ScoreboardManager(JavaPlugin plugin) {
        this.plugin    = plugin;
        this.statsFile = new File(plugin.getDataFolder(), "player_stats.dat");

        loadStats(); // Загружаем статистику при старте сервера
    }

    /**
     * Показывает временный Scoreboard на 3 секунды за каждого игрока.
     */
    public void showTemporaryScoreboard(Player player) {
        Scoreboard scoreboard = Objects.requireNonNull(Bukkit.getScoreboardManager()).getNewScoreboard();
        Objective objective = scoreboard.registerNewObjective("tempStats", Criteria.DUMMY, ChatColor.LIGHT_PURPLE + "📊 Ваша статистика");
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        objective.getScore(ChatColor.GOLD  + "Убийства (K): "  + getKills(player)).setScore(4);
        objective.getScore(ChatColor.RED   + "Смерти (D): " + getDeaths(player)).setScore(3);
        objective.getScore(ChatColor.GREEN + "Победы (W): "   + getWins(player)).setScore(2);
        objective.getScore(ChatColor.AQUA  + "Игры (G): "  + getGamesPlayed(player)).setScore(1);

        player.setScoreboard(scoreboard);

        // Убираем Scoreboard через (3 * кол-во игроков) секунд
        new BukkitRunnable() {
            @Override
            public void run() {
                player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
            }
        }.runTaskLater(plugin, 10 * 20L);
    }

    /**
     * Добавляет победу игроку.
     */
    public void addWin(Player player) {
        wins.put(player.getUniqueId(), getWins(player) + 1);
        saveStats();
    }

    /**
     * Добавляет убийство игроку.
     */
    public void addKill(Player player) {
        kills.put(player.getUniqueId(), getKills(player) + 1);
        saveStats();
    }

    /**
     * Добавляет смерть игроку.
     */
    public void addDeath(Player player) {
        deaths.put(player.getUniqueId(), getDeaths(player) + 1);
        saveStats();
    }

    /**
     * Увеличивает количество сыгранных игр.
     */
    public void addGame(Player player) {
        gamesPlayed.put(player.getUniqueId(), getGamesPlayed(player) + 1);
        saveStats();
    }

    // Геттеры с дефолтными значениями
    public int getKills(Player player) {
        return kills.getOrDefault(player.getUniqueId(), 0);
    }

    public int getDeaths(Player player) {
        return deaths.getOrDefault(player.getUniqueId(), 0);
    }

    public int getWins(Player player) {
        return wins.getOrDefault(player.getUniqueId(), 0);
    }

    public int getGamesPlayed(Player player) {
        return gamesPlayed.getOrDefault(player.getUniqueId(), 0);
    }

    /**
     * Сохраняет статистику в файл
     */
    private void saveStats() {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(statsFile))) {
            out.writeObject(kills);
            out.writeObject(deaths);
            out.writeObject(wins);
            out.writeObject(gamesPlayed);
        } catch (IOException e) {
            plugin.getLogger().severe("Ошибка сохранения статистики: " + e.getMessage());
        }
    }

    /**
     * Загружает статистику из файла
     */
    private void loadStats() {
        if (!statsFile.exists()) return;

        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(statsFile))) {
            kills.putAll((Map<UUID, Integer>) in.readObject());
            deaths.putAll((Map<UUID, Integer>) in.readObject());
            wins.putAll((Map<UUID, Integer>) in.readObject());
            gamesPlayed.putAll((Map<UUID, Integer>) in.readObject());
        } catch (IOException | ClassNotFoundException e) {
            plugin.getLogger().severe("Ошибка загрузки статистики: " + e.getMessage());
        }
    }
}
