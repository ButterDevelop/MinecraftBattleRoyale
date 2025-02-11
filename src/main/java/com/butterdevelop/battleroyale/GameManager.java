package com.butterdevelop.battleroyale;

import org.bukkit.*;
import org.bukkit.WorldBorder;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.io.File;
import java.util.*;

/**
 * Менеджер игры: создание миров, распределение игроков, настройка границ, запуск подъёма воздуха и т.д.
 */
public class GameManager {

    private final Object lock = new Object();

    public static String worldLobbyName;
    public static String worldArenaName;
    public static String netherArenaName;
    public static String endArenaName;

    private static double borderMinimumSize;
    private static double borderMaximumSize;
    private static int    borderShrinkSeconds;
    private static int    borderShrinkToZeroSeconds;

    private static int    secondsBeforeArena;
    private static int    secondsAfterArena;

    private final JavaPlugin plugin;

    private final Set<UUID> waitingPlayers;
    private final Set<UUID> playingPlayers;

    private final Set<UUID> votedForStartingPlayers;
    private final Set<UUID> votedForEndingPlayers;

    public  BukkitRunnable prepareGameTask        = null;
    private BukkitRunnable airTask                = null;
    private BukkitRunnable startGameTask          = null;
    private BukkitRunnable startCountdownTask     = null;
    private BukkitRunnable damageAllPlayersTask   = null;
    private BukkitRunnable fireworksTask          = null;
    private BukkitRunnable lateGameListenerTask   = null;
    private BukkitRunnable glowPlayersTask        = null;
    private BukkitRunnable damageNotOverworldTask = null;

    private final ScoreboardManager scoreboardManager;

    // Карта назначений: игрок -> название команды
    private final Map<UUID, String>  teamAssignments = new HashMap<>();

    // Доступные команды, созданные в Scoreboard
    private final Map<String, TeamInfo> availableTeams = new HashMap<>();

    private boolean gameStarted = false;
    private boolean gameWinning = false;

    private World arenaWorld;
    private World netherWorld;
    private World endWorld;

    public GameManager(JavaPlugin plugin) {
        this.plugin                  = plugin;
        this.waitingPlayers          = new HashSet<>();
        this.playingPlayers          = new HashSet<>();
        this.votedForStartingPlayers = new HashSet<>();
        this.votedForEndingPlayers   = new HashSet<>();
        this.scoreboardManager = new ScoreboardManager(plugin);

        // Чтение значений из конфига
        worldLobbyName  = plugin.getConfig().getString("worlds.lobby", "lobby");
        worldArenaName  = plugin.getConfig().getString("worlds.arena", "battleroyale");
        netherArenaName = plugin.getConfig().getString("worlds.nether", "battleroyale_nether");
        endArenaName    = plugin.getConfig().getString("worlds.end", "battleroyale_the_end");

        borderMinimumSize         = plugin.getConfig().getDouble("border.minimumSize", 10.0);
        borderMaximumSize         = plugin.getConfig().getDouble("border.maximumSize", 2000.0);
        borderShrinkSeconds       = plugin.getConfig().getInt("border.shrinkSeconds", 3600);
        borderShrinkToZeroSeconds = plugin.getConfig().getInt("border.shrinkToZeroSeconds", 60);

        secondsBeforeArena = plugin.getConfig().getInt("arena.secondsBeforeArena", 60);
        secondsAfterArena  = plugin.getConfig().getInt("arena.secondsAfterArena", 60);

        // Регистрация команд (Scoreboard teams)
        registerTeams();
    }

    public ScoreboardManager getScoreboardManager() {
        return scoreboardManager;
    }

    // Метод регистрации команд из конфига (или дефолтные, если их нет)
    private void registerTeams() {
        Scoreboard scoreboard = Objects.requireNonNull(plugin.getServer().getScoreboardManager()).getMainScoreboard();
        if (plugin.getConfig().isList("teams")) {
            List<?> teamsList = plugin.getConfig().getList("teams");
            for (Object obj : Objects.requireNonNull(teamsList)) {
                if (obj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> teamMap = (Map<String, Object>) obj;
                    String name = (String) teamMap.get("name");
                    String colorStr = (String) teamMap.get("color");

                    ChatColor color;
                    try {
                        color = ChatColor.valueOf(colorStr.toUpperCase());
                    } catch (Exception e) {
                        color = ChatColor.WHITE;
                    }
                    Team sbTeam = scoreboard.getTeam(name);
                    if (sbTeam == null) {
                        sbTeam = scoreboard.registerNewTeam(name);
                    }

                    // Настройки для команды
                    sbTeam.setColor(color);
                    sbTeam.setDisplayName(name);
                    sbTeam.setCanSeeFriendlyInvisibles(true);
                    sbTeam.setAllowFriendlyFire(true);

                    TeamInfo teamInfo = new TeamInfo(name, color, sbTeam);
                    availableTeams.put(name, teamInfo);
                }
            }
        } else {
            createDefaultTeams(scoreboard);
        }
    }

    private void createDefaultTeams(Scoreboard scoreboard) {
        String[] defaultTeamNames = {"Красные", "Синие", "Зелёные"};
        ChatColor[] defaultColors = {ChatColor.RED, ChatColor.BLUE, ChatColor.GREEN};
        for (int i = 0; i < defaultTeamNames.length; i++) {
            String teamName = defaultTeamNames[i];
            Team sbTeam = scoreboard.getTeam(teamName);
            if (sbTeam == null) {
                sbTeam = scoreboard.registerNewTeam(teamName);
            }
            sbTeam.setPrefix(defaultColors[i].toString());
            TeamInfo teamInfo = new TeamInfo(teamName, defaultColors[i], sbTeam);
            availableTeams.put(teamName, teamInfo);
        }
    }

    public Map<String, TeamInfo> getAvailableTeams() {
        return availableTeams;
    }

    public void addWaitingPlayer(UUID playerId) {
        for (UUID waitingPlayerId : waitingPlayers) {
            if (waitingPlayerId.equals(playerId)) {
                return;
            }
        }
        waitingPlayers.add(playerId);
    }

    public void removeWaitingPlayer(UUID playerId) {
        waitingPlayers.remove(playerId);
    }

    public Set<UUID> getWaitingPlayers() {
        return waitingPlayers;
    }

    public void addPlayingPlayer(UUID playerId) {
        synchronized (lock) {
            playingPlayers.add(playerId);
        }
    }

    public void removePlayingPlayer(UUID playerId) {
        playingPlayers.remove(playerId);
    }

    public Set<UUID> getPlayingPlayers() {
        synchronized (lock) {
            return playingPlayers;
        }
    }

    public boolean containsPlayingPlayer(UUID playerId) {
        synchronized (lock) {
            return playingPlayers.contains(playerId);
        }
    }

    public void addVotedForStartPlayer(UUID playerId) {
        votedForStartingPlayers.add(playerId);
    }

    public void removeVotedForStartPlayer(UUID playerId) {
        votedForStartingPlayers.remove(playerId);
    }

    public Set<UUID> getVotedForStartPlayers() {
        return votedForStartingPlayers;
    }

    public void addVotedForEndPlayer(UUID playerId) {
        votedForEndingPlayers.add(playerId);
    }

    public void removeVotedForEndPlayer(UUID playerId) {
        votedForEndingPlayers.remove(playerId);
    }

    public Set<UUID> getVotedForEndPlayers() {
        return votedForEndingPlayers;
    }

    public boolean isGameStarted() {
        return gameStarted;
    }

    public boolean isGameWon() {
        return gameWinning;
    }

    /**
     * Назначает игроку команду:
     * – Удаляет его из старой команды (если была);
     * – Добавляет его в Scoreboard-команду, обновляет никнейм (с префиксом цвета);
     * – Сохраняет назначение в локальной карте.
     */
    public void setTeam(UUID playerId, String teamName) {
        Player player = Bukkit.getPlayer(playerId);
        if (player == null) {
            return;
        }

        // Если игрок уже состоит в другой команде — удаляем его оттуда
        String oldTeam = teamAssignments.get(playerId);
        if (oldTeam != null) {
            Scoreboard scoreboard = Objects.requireNonNull(plugin.getServer().getScoreboardManager()).getMainScoreboard();
            Team oldSbTeam = scoreboard.getTeam(oldTeam);
            if (oldSbTeam != null) {
                oldSbTeam.removeEntry(player.getName());
            }
        }
        teamAssignments.put(playerId, teamName);
        Scoreboard scoreboard = Objects.requireNonNull(plugin.getServer().getScoreboardManager()).getMainScoreboard();
        Team newSbTeam = scoreboard.getTeam(teamName);
        if (newSbTeam != null) {
            newSbTeam.addEntry(player.getName());
        }
        // Обновляем отображение имени игрока (добавляем цвет)
        TeamInfo teamInfo = availableTeams.get(teamName);
        if (teamInfo != null) {
            player.setDisplayName(teamInfo.getColor() + player.getName());
            player.sendMessage(ChatColor.GREEN + "Вы присоединились к команде " +
                    teamInfo.getColor() + ChatColor.BOLD + teamName + ChatColor.GREEN + "!");
        }
    }

    /**
     * Возвращает имя команды, к которой принадлежит игрок, или null, если игрок не состоит в команде.
     *
     * @param playerId Игрок
     * @return имя команды или null
     */
    public String getTeam(UUID playerId) {
        return teamAssignments.get(playerId);
    }

    /**
     * Удаляет игрока из системы назначения команд.
     *
     * @param playerId Игрок
     */
    public void removeTeam(UUID playerId) {
        Player player = Bukkit.getPlayer(playerId);
        if (player == null) {
            return;
        }

        String teamName = teamAssignments.remove(playerId);
        if (teamName != null) {
            Scoreboard scoreboard = Objects.requireNonNull(plugin.getServer().getScoreboardManager()).getMainScoreboard();
            Team sbTeam = scoreboard.getTeam(teamName);
            if (sbTeam != null) {
                sbTeam.removeEntry(player.getName());
            }
        }
    }

    /**
     * Возвращает список имён игроков, состоящих в указанной команде.
     */
    public List<String> getTeamPlayers(String teamName) {
        List<String> players = new ArrayList<>();
        for (Map.Entry<UUID, String> entry : teamAssignments.entrySet()) {
            if (entry.getValue().equals(teamName)) {
                Player player = Bukkit.getPlayer(entry.getKey());
                if (player != null) {
                    players.add(player.getName());
                }
            }
        }
        return players;
    }

    /**
     * Готовим игрока: чистим инвентарь, снимаем эффекты
     * @param playerId Игрок
     */
    public void preparePlayer(UUID playerId) {
        Player player = Bukkit.getPlayer(playerId);
        if (player == null) {
            return;
        }

        // Определяем мир лобби
        World lobby = Bukkit.getWorld(GameManager.worldLobbyName);
        if (lobby == null) {
            // Если мира "lobby" нет, используем первый доступный мир
            lobby = Bukkit.getWorlds().getFirst();
            player.sendMessage(ChatColor.DARK_RED + "Не удалось найти мир для лобби!");
        }

        // Настройки мира лобби, если потенциально он ещё не настроен
        if (lobby.getDifficulty() != Difficulty.PEACEFUL) {
            Bukkit.broadcastMessage(ChatColor.BLUE + "Обнаружено, что мир лобби не настроен, поэтому плагин его настроил.");

            lobby.setPVP(false);
            lobby.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
            lobby.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
            lobby.setGameRule(GameRule.DO_MOB_SPAWNING, false);
            lobby.setGameRule(GameRule.DO_TILE_DROPS, false);
            lobby.setGameRule(GameRule.DO_FIRE_TICK, false);
            lobby.setGameRule(GameRule.DO_VINES_SPREAD, false);
            lobby.setGameRule(GameRule.DO_ENTITY_DROPS, false);
            lobby.setGameRule(GameRule.DO_IMMEDIATE_RESPAWN, true);
            lobby.setGameRule(GameRule.FALL_DAMAGE, false);
            lobby.setGameRule(GameRule.SPECTATORS_GENERATE_CHUNKS, false);
            lobby.setGameRule(GameRule.DROWNING_DAMAGE, false);
            lobby.setGameRule(GameRule.FREEZE_DAMAGE, false);
            lobby.setGameRule(GameRule.MOB_GRIEFING, false);
            lobby.setTime(3000);
            lobby.setDifficulty(Difficulty.PEACEFUL);
        }

        // Перемещаем игрока в лобби
        player.teleport(lobby.getSpawnLocation());

        // Убираем игроку команду
        removeTeam(player.getUniqueId());

        // Очищаем все эффекты
        player.getActivePotionEffects().forEach(potionEffect -> player.removePotionEffect(potionEffect.getType()));

        // Очищаем игроку инвентарь
        player.getInventory().clear();

        // Очищаем игроку уровень опыта
        player.setExp(0);
        player.setLevel(0);

        // Ставим игроку режим приключений
        player.setGameMode(GameMode.ADVENTURE);

        // Выдаём предметы, которые нужны для плагина
        setPluginItems(player);
    }

    /**
     * Выдача предметов, которые выполняют команды
     * @param player Игрок, которому выдадут предметы
     */
    public static void setPluginItems(Player player) {
        // Компас для выбора команды
        ItemStack teamSelector = new ItemStack(Material.COMPASS, 1);
        ItemMeta teamMeta = teamSelector.getItemMeta();
        if (teamMeta != null) {
            teamMeta.setMaxStackSize(99);
            teamMeta.setDisplayName(ChatColor.GREEN + "" + ChatColor.BOLD + "Выбор команды");
            teamMeta.setLore(Collections.singletonList(ChatColor.YELLOW + "Нажмите ПКМ, чтобы выбрать команду"));
            teamSelector.setItemMeta(teamMeta);
        }

        // Часы для просмотра статистики
        ItemStack statsItem = new ItemStack(Material.PAPER, 1);
        ItemMeta statsMeta = statsItem.getItemMeta();
        if (statsMeta != null) {
            statsMeta.setMaxStackSize(99);
            statsMeta.setDisplayName(ChatColor.AQUA + "" + ChatColor.BOLD + "Статистика");
            statsMeta.setLore(Collections.singletonList(ChatColor.YELLOW + "Нажмите ПКМ, чтобы посмотреть статистику"));
            statsItem.setItemMeta(statsMeta);
        }

        // Осколок аметиста для голосования за старт игры
        ItemStack voteStartItem = new ItemStack(Material.AMETHYST_SHARD, 1);
        ItemMeta voteMeta = voteStartItem.getItemMeta();
        if (voteMeta != null) {
            voteMeta.setMaxStackSize(99);
            voteMeta.setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + "Голосовать за старт");
            voteMeta.setLore(Collections.singletonList(ChatColor.YELLOW + "Нажмите ПКМ, чтобы проголосовать за старт игры"));
            voteStartItem.setItemMeta(voteMeta);
        }

        // Осколок аметиста для голосования за старт игры
        ItemStack chooseKitItem = new ItemStack(Material.CHEST, 1);
        ItemMeta chooseKitMeta = chooseKitItem.getItemMeta();
        if (chooseKitMeta != null) {
            chooseKitMeta.setMaxStackSize(99);
            chooseKitMeta.setDisplayName(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "Выбрать кит");
            chooseKitMeta.setLore(Collections.singletonList(ChatColor.YELLOW + "Нажмите ПКМ, чтобы выбрать кит"));
            chooseKitItem.setItemMeta(chooseKitMeta);
        }

        // Выдаём предметы в инвентарь
        player.getInventory().setItem(0, teamSelector);  // Компас
        player.getInventory().setItem(1, statsItem);     // Бумага
        player.getInventory().setItem(2, voteStartItem); // Осколок аметиста
        player.getInventory().setItem(4, chooseKitItem); // Сундук
    }

    /**
     * Метод запуска игры:
     * – Создаёт аренный, адский и край миры;
     * – Телепортирует игроков в аренную;
     * – Настраивает границу и запускает сжатие;
     * – Запускает задачу подъёма воздуха.
     */
    public void startGame() {
        if (gameStarted || prepareGameTask != null) {
            plugin.getLogger().info("Игра уже запущена!");
            return;
        }
        if (waitingPlayers.isEmpty() || availableTeams.size() <= 1) {
            plugin.getLogger().info("Нет игроков для запуска игры!");
            return;
        }
        plugin.getLogger().info("Запуск игры!");

        // Возвращаем всех игроков на точку спавна в лобби
        World lobby = Bukkit.getWorld(worldLobbyName);
        if (lobby != null) {
            Bukkit.getOnlinePlayers().forEach(player -> {
                if (!player.getWorld().getName().equals(worldLobbyName)) {
                    preparePlayer(player.getUniqueId());
                }
            });
        }

        // Делаем действия в другом потоке, плюс отложенно (подготовка миров)
        prepareGameTask = new BukkitRunnable() {
            @Override
            public void run() {
                // Удаляем старые миры арены
                deleteArenaWorlds();

                // Создаём мир Края (End)
                Bukkit.broadcastMessage(ChatColor.GRAY + "" + ChatColor.ITALIC + "Создаём мир края...");
                createEndWorld();

                // Создаём мир Ада (Nether)
                Bukkit.broadcastMessage(ChatColor.GRAY + "" + ChatColor.ITALIC + "Создаём адский мир...");
                createNetherWorld();

                // Создаём аренный мир
                Bukkit.broadcastMessage(ChatColor.GRAY + "" + ChatColor.ITALIC + "Создаём обычный мир...");
                createArenaWorld();
            }
        };
        // 10 секунд задержка после телепортации игроков
        prepareGameTask.runTaskLater(plugin, 10 * 20L);

        // Делаем действия в другом потоке, плюс отложенно (старт игры)
        startGameTask = new BukkitRunnable() {
            @Override
            public void run() {
                gameStarted = true;
                gameWinning = false;

                // Настраиваем границу арены
                setupWorldBorder();

                // Назначаем командам игроков, у которых её нет
                assignPlayersToTeams();

                // Чистим все достижения игрокам, чтобы они появлялись снова
                revokeAllAdvancements();

                // Телепортируем команды на арену
                teleportTeamsToArena();

                // Запускаем проверку границы и затем задачу подъёма воздуха
                scheduleLateGame();

                // Проверка на всякий случай, если будет непредвиденная ошибка, чтобы арена не длилась вечно
                if (playingPlayers.isEmpty()) {
                    checkForWin();
                } else {
                    // Добавляем игру игрокам в статистику
                    for (UUID playerId : waitingPlayers) {
                        Player player = Bukkit.getPlayer(playerId);
                        if (player != null) {
                            scoreboardManager.addGame(player);
                        }
                    }
                }
            }
        };
        // secondsBeforeArena секунд задержка
        startGameTask.runTaskLater(plugin, secondsBeforeArena * 20L);

        // Старт отсчёта до арены
        startCountdownTask = new BukkitRunnable() {
            int countdown = secondsBeforeArena;

            @Override
            public void run() {
                Bukkit.broadcastMessage(ChatColor.AQUA + "До арены осталось " + ChatColor.BOLD + countdown + ChatColor.AQUA + " секунд!");
                if (--countdown <= 0) {
                    cancel();
                }
            }
        };
        startCountdownTask.runTaskTimer(plugin, 0L, 20L); // Запускаем таймер каждую секунду
    }

    /**
     * Распределяет всех игроков без команды по командам.
     * Сначала игроков закидывает в пустые команды, затем равномерно в остальные.
     */
    private void assignPlayersToTeams() {
        List<String> emptyTeams        = new ArrayList<>();
        Map<String, Integer> teamSizes = new HashMap<>();

        // Разделяем команды на пустые и заполненные
        for (String team : availableTeams.keySet()) {
            int size = (int) waitingPlayers.stream().filter(p -> team.equals(getTeam(p))).count();
            teamSizes.put(team, size);
            if (size == 0) {
                emptyTeams.add(team);
            }
        }

        List<UUID> playersWithoutTeam = waitingPlayers.stream()
                .filter(p -> getTeam(p) == null)
                .toList();

        // Сначала заполняем пустые команды
        Iterator<String> emptyTeamsIterator = emptyTeams.iterator();
        for (UUID playerId : playersWithoutTeam) {
            if (emptyTeamsIterator.hasNext()) {
                String team = emptyTeamsIterator.next();
                setTeam(playerId, team);
                teamSizes.put(team, teamSizes.getOrDefault(team, 0) + 1);
            }
        }

        // Затем равномерно распределяем оставшихся игроков по всем командам
        List<String> allTeams = new ArrayList<>(teamSizes.keySet());
        allTeams.sort(Comparator.comparingInt(teamSizes::get)); // Сортируем от меньших к большим

        for (UUID playerId : playersWithoutTeam) {
            if (getTeam(playerId) == null) {
                String smallestTeam = allTeams.getFirst();
                setTeam(playerId, smallestTeam);
                teamSizes.put(smallestTeam, teamSizes.get(smallestTeam) + 1);
                allTeams.sort(Comparator.comparingInt(teamSizes::get)); // Пересортируем после добавления игрока
            }
        }
    }

    /**
     * Останавливаем игру
     */
    public void endGame() {
        gameWinning = true;

        // Останавливаем все барьеры
        stopAllWorldBorders();

        // Сообщаем об окончании игры
        Bukkit.broadcastMessage(ChatColor.DARK_GREEN + "Игра окончена. Возвращение в лобби через " +
                ChatColor.BOLD + secondsAfterArena + ChatColor.DARK_GREEN + " секунд.");

        // Запускаем shutdown чуть позже, чтобы был перерыв до окончания
        new BukkitRunnable() {
            @Override
            public void run() {
                gameStarted = false;
                gameWinning = false;
                shutdown();
            }
        }.runTaskLater(plugin, secondsAfterArena * 20L);
    }

    /**
     * Проверяем, победила ли какая-либо команда.
     * Группируем выживших игроков по командам и, если осталась только одна команда,
     * объявляем её победителем.
     */
    public void checkForWin() {
        if (!gameStarted) {
            return;
        }

        // Группируем выживших игроков по имени команды.
        Map<String, Integer> aliveTeamCounts = new HashMap<>();
        for (UUID playerId : playingPlayers) {
            Player player = Bukkit.getPlayer(playerId);
            if (player == null) {
                continue;
            }

            if (player.isOnline() && player.getGameMode() == GameMode.SURVIVAL && !player.isDead()) {
                String team = teamAssignments.get(playerId);
                if (team != null) {
                    aliveTeamCounts.put(team, aliveTeamCounts.getOrDefault(team, 0) + 1);
                }
            }
        }

        // Если никого нет, то это ничья
        if (aliveTeamCounts.isEmpty()) {
            // Сообщение о ничьей для всех
            Bukkit.broadcastMessage(ChatColor.GREEN + "НИЧЬЯ! Никто не победил!");
            // Окончание игры
            endGame();
            return;
        }
        // Если одна команда осталась, то она победила
        if (aliveTeamCounts.size() == 1) {
            gameWinning = true;

            String winningTeam = aliveTeamCounts.keySet().iterator().next();
            Bukkit.broadcastMessage(ChatColor.GREEN + "Победила команда "
                    + ChatColor.BOLD + ChatColor.GOLD + winningTeam + ChatColor.GREEN + "!");

            StringBuilder builder = new StringBuilder();
            // Ищем игроков победившей команды
            for (String playerName : getTeamPlayers(winningTeam)) {
                builder.append(playerName).append(" ");

                // Создаём фейерверк рядом с победителями через секунду
                Player player = Bukkit.getPlayer(playerName);

                if (player != null) {
                    // Добавляем пользователю победу
                    scoreboardManager.addWin(player);

                    fireworksTask = new BukkitRunnable() {
                        int counter = 10;
                        @Override
                        public void run() {
                            spawnFirework(player.getLocation());
                            if (--counter <= 0) {
                                cancel();
                            }
                        }
                    };
                    fireworksTask.runTaskTimer(plugin, 20L, 20L);
                }
            }

            // Показываем самих игроков победившей команды
            Bukkit.broadcastMessage(ChatColor.GREEN + "Победившие игроки: " + ChatColor.BOLD + builder.toString().trim());

            // Окончание игры
            endGame();
        }
    }

    /**
     * Создаёт фейерверк
     * @param location Место, где создать фейерверк
     */
    public void spawnFirework(Location location) {
        if (location.getWorld() == null) {
            return;
        }

        Location fireworkLocation = location.clone();
        fireworkLocation.setY(fireworkLocation.getY() + 15);
        Firework fw = (Firework)location.getWorld().spawnEntity(fireworkLocation, EntityType.FIREWORK_ROCKET);

        FireworkMeta fwm = fw.getFireworkMeta();
        fwm.setPower(3);
        fwm.addEffect(FireworkEffect.builder().withColor(Color.ORANGE).flicker(true).trail(true).build());

        fw.setFireworkMeta(fwm);
        fw.detonate();
    }

    /**
     * Удаляем все миры арены
     */
    private void deleteArenaWorlds() {
        // Если мир уже существует, выгружаем и удаляем его
        arenaWorld = Bukkit.getWorld(worldArenaName);
        if (arenaWorld != null) {
            Bukkit.unloadWorld(arenaWorld, false);
            deleteWorldFolder(new File(Bukkit.getWorldContainer(), worldArenaName));
        }

        netherWorld = Bukkit.getWorld(netherArenaName);
        if (netherWorld != null) {
            Bukkit.unloadWorld(netherWorld, false);
            deleteWorldFolder(new File(Bukkit.getWorldContainer(), netherArenaName));
        }

        endWorld = Bukkit.getWorld(endArenaName);
        if (endWorld != null) {
            Bukkit.unloadWorld(endWorld, false);
            deleteWorldFolder(new File(Bukkit.getWorldContainer(), endArenaName));
        }
    }

    /**
     * Создаём обычный мир арены
     */
    private void createArenaWorld() {
        WorldCreator creator = new WorldCreator(worldArenaName);
        creator.environment(World.Environment.NORMAL);

        arenaWorld = creator.createWorld();
        if (arenaWorld != null) {
            arenaWorld.setSpawnLocation(0, arenaWorld.getHighestBlockYAt(0, 0), 0);
            arenaWorld.setTime(0);
            arenaWorld.setGameRule(GameRule.SPECTATORS_GENERATE_CHUNKS, false);
            arenaWorld.setGameRule(GameRule.SPAWN_CHUNK_RADIUS, 0);
            arenaWorld.setGameRule(GameRule.SPAWN_RADIUS, 0);
            arenaWorld.setSpawnFlags(true, true);
            arenaWorld.setAutoSave(false);
        }
        arenaWorld = Bukkit.getWorld(worldArenaName);
    }

    /**
     * Создаём адский мир арены
     */
    private void createNetherWorld() {
        WorldCreator creator = new WorldCreator(netherArenaName);
        creator.environment(World.Environment.NETHER);
        netherWorld = creator.createWorld();
        if (netherWorld != null) {
            netherWorld.setGameRule(GameRule.SPECTATORS_GENERATE_CHUNKS, false);
            netherWorld.setGameRule(GameRule.SPAWN_CHUNK_RADIUS, 0);
            netherWorld.setGameRule(GameRule.SPAWN_RADIUS, 0);
            netherWorld.setAutoSave(false);
        }
        netherWorld = Bukkit.getWorld(netherArenaName);
    }

    /**
     * Создаём мир края арены
     */
    private void createEndWorld() {
        WorldCreator creator = new WorldCreator(endArenaName);
        creator.environment(World.Environment.THE_END);
        endWorld = creator.createWorld();
        if (endWorld != null) {
            endWorld.setGameRule(GameRule.SPECTATORS_GENERATE_CHUNKS, false);
            endWorld.setGameRule(GameRule.SPAWN_CHUNK_RADIUS, 0);
            endWorld.setGameRule(GameRule.SPAWN_RADIUS, 0);
            endWorld.setAutoSave(false);
        }
        endWorld = Bukkit.getWorld(endArenaName);
    }

    /**
     * Забираем все достижения, чтобы они отображались во время игры
     */
    private void revokeAllAdvancements() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            Iterator<Advancement> iterator = Bukkit.getServer().advancementIterator();

            while (iterator.hasNext()) {
                Advancement advancement = iterator.next();
                AdvancementProgress progress = player.getAdvancementProgress(advancement);

                for (String criteria : progress.getAwardedCriteria()) {
                    progress.revokeCriteria(criteria);
                }
            }
        }
    }

    /**
     * Равномерно распределяет команды по территории арены.
     */
    private void teleportTeamsToArena() {
        Random random             = new Random();
        WorldBorder border        = arenaWorld.getWorldBorder();
        double borderSize         = border.getSize();
        double centerX            = arenaWorld.getSpawnLocation().getX();
        double centerZ            = arenaWorld.getSpawnLocation().getZ();
        double maxAvailableRadius = (borderSize / 2) - 20; // отступ от границы мира

        // Используйте часть доступного радиуса, чтобы гарантировать, что точки появления не будут слишком близко к границе.
        double spawnCircleRadius = maxAvailableRadius * 0.7;

        // Самая большая высота мира
        final int maxY = 256;

        // Создаём объекты нужных эффектов, которые выдадим игроку в начале арены (максимальный уровень, но лишь на время)
        PotionEffect resistancePotionEffect  = new PotionEffect(PotionEffectType.RESISTANCE, 120 * 20, Integer.MAX_VALUE, false, false);
        PotionEffect saturationPotionEffect  = new PotionEffect(PotionEffectType.SATURATION, 120 * 20, Integer.MAX_VALUE, false, false);
        PotionEffect slowFallingPotionEffect = new PotionEffect(PotionEffectType.SLOW_FALLING, 2 * 20, 4, false, false);

        for (UUID playerId : waitingPlayers) {
            Player player = Bukkit.getPlayer(playerId);
            if (player == null) {
                continue;
            }

            // Вдруг игрок вышел
            if (!player.isOnline()) {
                continue;
            }

            // Если пользователь умер, то сообщаем ему об этом
            if (player.isDead()) {
                player.sendMessage(ChatColor.RED + "Вы умерли, невозможно переместить вас на арену!");
                continue;
            }

            // Указываем, что игрок активен на арене
            addPlayingPlayer(playerId);
        }

        // Если слишком мало игроков или команд для запуска игры
        if (playingPlayers.size() <= 1 || availableTeams.size() <= 1) {
            Bukkit.broadcastMessage(ChatColor.YELLOW + "Слишком мало игроков или команд для начала игры! Отмена!");
            endGame();
            return;
        }

        // Создайте карту для хранения мест появления каждой команды.
        Map<String, Location> teamSpawnLocations = new HashMap<>();
        int teamCount = availableTeams.size();
        // Создайте случайное угловое смещение, чтобы немного изменить общую компоновку.
        double angleOffset = random.nextDouble() * 2 * Math.PI;

        int index = 0;
        for (String team : availableTeams.keySet()) {
            // Равномерно распределите команды по углам.
            double theta = angleOffset + (2 * Math.PI * index / teamCount);
            // Добавьте небольшое случайное радиальное отклонение от 0,9 до 1,1.
            double radialVariation = 0.9 + 0.2 * random.nextDouble();
            double teamRadius = spawnCircleRadius * radialVariation;

            double randomX = centerX + teamRadius * Math.cos(theta);
            double randomZ = centerZ + teamRadius * Math.sin(theta);

            Location teamSpawnLocation = new Location(arenaWorld, randomX, maxY, randomZ);
            teamSpawnLocations.put(team, teamSpawnLocation);
            index++;
        }

        // Телепортируем игроков в их командные точки
        for (UUID playerId : playingPlayers) {
            Player player = Bukkit.getPlayer(playerId);
            if (player == null) {
                continue;
            }

            String team = getTeam(playerId);
            if (team == null) {
                player.sendMessage(ChatColor.RED + "У вас не было команды к началу арены по какой-то причине. Невозможно телепортировать на арену.");
                continue;
            }

            Location spawnLocation = teamSpawnLocations.getOrDefault(team, new Location(arenaWorld, centerX, maxY, centerZ));

            // Очищаем инвентарь
            player.getInventory().clear();
            // Очищаем все эффекты
            player.getActivePotionEffects().forEach(potionEffect -> player.removePotionEffect(potionEffect.getType()));

            // Предзагрузка чанка
            arenaWorld.loadChunk(spawnLocation.getChunk());

            // Телепортируем игрока в точку команды
            player.teleport(spawnLocation);

            // Даём эффект бессмертия и сытости на время
            player.addPotionEffect(resistancePotionEffect);
            player.addPotionEffect(saturationPotionEffect);
            player.addPotionEffect(slowFallingPotionEffect);

            // Выдаём кит-набор игроку
            KitSelectionGUI.kitManager.giveKit(player);

            // Сообщение в чате
            player.sendMessage(ChatColor.BOLD + "" + ChatColor.GOLD + "Вы появляетесь на арене со своей командой!");
        }
    }

    /**
     * Настраивает границу арены: начальный размер M блоков, сжатие до NхN за K минут.
     */
    private void setupWorldBorder() {
        // Координаты спавна арены
        Location spawnLocation = arenaWorld.getSpawnLocation();

        // Граница мира для обычного мира
        WorldBorder borderWorld = arenaWorld.getWorldBorder();
        borderWorld.setCenter(spawnLocation);
        borderWorld.setSize(borderMaximumSize); // начальный размер
        // Сжимаем границу до условных borderMinimumSize за borderShrinkSeconds секунд
        borderWorld.setSize(borderMinimumSize, borderShrinkSeconds);

        // Граница мира для ада
        Location netherSpawnLocation = new Location(netherWorld, spawnLocation.getX(), spawnLocation.getY(), spawnLocation.getZ());
        WorldBorder borderNether = netherWorld.getWorldBorder();
        borderNether.setCenter(netherSpawnLocation);
        borderNether.setSize(borderMaximumSize); // начальный размер
        // Сжимаем границу до условных borderMinimumSize за borderShrinkSeconds секунд
        borderNether.setSize(1, borderShrinkSeconds);

        // Граница мира для края
        WorldBorder borderEnd = endWorld.getWorldBorder();
        borderEnd.setCenter(endWorld.getSpawnLocation());
        borderEnd.setSize(borderMaximumSize); // начальный размер
        // Сжимаем границу до условных borderMinimumSize за borderShrinkSeconds секунд
        borderEnd.setSize(1, borderShrinkSeconds);
    }

    /**
     * Останавливает все границы мира
     */
    private void stopAllWorldBorders() {
        // Граница мира для обычного мира
        WorldBorder borderWorld = arenaWorld.getWorldBorder();
        borderWorld.setSize(borderWorld.getSize());

        // Граница мира для ада
        WorldBorder borderNether = netherWorld.getWorldBorder();
        borderNether.setSize(borderNether.getSize());

        // Граница мира для края
        WorldBorder borderEnd = netherWorld.getWorldBorder();
        borderEnd.setSize(borderEnd.getSize());
    }

    /**
     * Запускает свечение для всех игроков, которые ещё играют
     */
    private void startPlayerGlowing() {
        Bukkit.broadcastMessage(ChatColor.LIGHT_PURPLE + "Все игроки были подсвечены.");

        PotionEffect glowingPotionEffect = new PotionEffect(PotionEffectType.GLOWING, PotionEffect.INFINITE_DURATION, Integer.MAX_VALUE, false, false);

        glowPlayersTask = new BukkitRunnable() {
            @Override
            public void run() {
                // Если некому выдавать эффект, то отменяем задачу
                if (playingPlayers.isEmpty()) {
                    cancel();
                }

                // Выдаём эффект свечения, если он ещё не выдан
                for (UUID playerId : playingPlayers) {
                    Player player = Bukkit.getPlayer(playerId);
                    if (player == null) {
                        continue;
                    }

                    // Выдаём пользователю эффект, если его нет
                    if (!player.hasPotionEffect(glowingPotionEffect.getType())) {
                        player.addPotionEffect(glowingPotionEffect);
                    }
                }
            }
        };
        glowPlayersTask.runTaskTimer(plugin, 0L, 20L); // проверка каждую секунду
    }

    /**
     * Начинаем наносить урон игрокам, которые не в обычном мире
     */
    private void startDamageNotOverworld() {
        damageNotOverworldTask = new BukkitRunnable() {
            @Override
            public void run() {
                // Если некому наносить урон, то отменяем задачу
                if (playingPlayers.isEmpty()) {
                    cancel();
                    return;
                }

                // Наносим урон
                playingPlayers.forEach(playerId -> {
                    Player player = Bukkit.getPlayer(playerId);
                    if (player != null) {
                        // Наносим урон пользователю, если он не в обычном мире
                        if (player.getWorld().getEnvironment() != World.Environment.NORMAL) {
                            AttributeInstance maxHealth = player.getAttribute(Attribute.MAX_HEALTH);
                            if (maxHealth != null) {
                                double health = Math.max(player.getHealth() - maxHealth.getValue() * 0.1, 1);
                                player.setHealth(health);
                                player.damage(1);
                                player.sendMessage(ChatColor.RED + "Барьер достаточно уменьшился. " +
                                        "Вам нужно вернуться в обычный мир, иначе будет наноситься урон.");
                            }
                        }
                    }
                });
            }
        };
        damageNotOverworldTask.runTaskTimer(plugin, 0L, 20L); // урон каждую секунду
    }

    /**
     * Запускает задачу, которая каждую секунду проверяет размер границы и,
     * как только граница станет минимальной, запускает события late-game.
     */
    private void scheduleLateGame() {
        lateGameListenerTask = new BukkitRunnable() {
            @Override
            public void run() {
                WorldBorder border = arenaWorld.getWorldBorder();
                if (border.getSize() <= borderMinimumSize) {
                    // Граница почти минимальна, запускаем уничтожение блоков
                    startAirRise();

                    // Запускаем свечение для игроков
                    startPlayerGlowing();

                    // Запускаем урон для всех, кто не в обычном мире
                    startDamageNotOverworld();

                    cancel();
                }
            }
        };
        lateGameListenerTask.runTaskTimer(plugin, 0L, 200L); // проверка каждые 10 секунд
    }

    /**
     * Уменьшаем размер барьера до 1, чтобы начать наносить всем урон
     */
    private void shrinkBorderToZero() {
        // Граница мира для обычного мира
        WorldBorder borderWorld = arenaWorld.getWorldBorder();
        // Сжимаем границу до 1
        borderWorld.setSize(1, borderShrinkToZeroSeconds);
    }

    /**
     * Наносим урон всем живым игрокам на арене, чтобы уже наконец выяснить победителя. Урон растёт с каждой секундой
     */
    private void damageAllPlayers() {
        Bukkit.broadcastMessage(ChatColor.DARK_GREEN + "Начинается нанесение урона всем оставшимся игрокам.");

        damageAllPlayersTask = new BukkitRunnable() {
            int counter = 1;

            @Override
            public void run() {
                Bukkit.getOnlinePlayers().forEach(player -> {
                    if (!player.getWorld().getName().equals(worldLobbyName) && player.getGameMode() == GameMode.SURVIVAL && !player.isDead()) {
                        player.damage(++counter);
                    }
                });
            }
        };
        damageAllPlayersTask.runTaskTimer(plugin, 0L, 20L);
    }

    /**
     * Запускает повторяющуюся задачу, которая убирает блоки снизу в центральной области арены.
     */
    private void startAirRise() {
        Bukkit.broadcastMessage(ChatColor.DARK_PURPLE + "Начинается удаление блоков с самого низа мира.");

        final int startY = -64; // стартовая высота для уничтожения (начало мира)
        airTask = new BukkitRunnable() {
            int currentY = startY;
            @Override
            public void run() {
                // Заполняем область 5x5 воздухом на уровне currentY
                Location center = arenaWorld.getSpawnLocation();
                int radius = (int)borderMinimumSize / 2;
                for (int x = -radius; x <= radius; x++) {
                    for (int z = -radius; z <= radius; z++) {
                        Location loc = new Location(arenaWorld, center.getBlockX() + x, currentY, center.getBlockZ() + z);
                        Block block = loc.getBlock();
                        // Устанавливаем только если блок не является твердым (на всякий случай)
                        block.setType(Material.AIR);
                    }
                }
                currentY++;
                // Останавливаем, если воздух достигнет определённой высоты (например, 256)
                if (currentY > 256) {
                    // Делаем border до 0, чтобы нанести урон всем
                    shrinkBorderToZero();

                    // Начинаем наносить урон всем игрокам
                    damageAllPlayers();

                    // Отменяем задачу
                    cancel();
                }
            }
        };
        airTask.runTaskTimer(plugin, 0L, 20L); // обновление каждую секунду
    }

    /**
     * Останавливает запущенные задачи при отключении плагина.
     */
    public void shutdown() {
        // Останавливаем все таски
        if (prepareGameTask != null) {
            prepareGameTask.cancel();
            prepareGameTask = null;
        }
        if (airTask != null) {
            airTask.cancel();
            airTask = null;
        }
        if (startGameTask != null) {
            startGameTask.cancel();
            startGameTask = null;
        }
        if (startCountdownTask != null) {
            startCountdownTask.cancel();
            startCountdownTask = null;
        }
        if (damageAllPlayersTask != null) {
            damageAllPlayersTask.cancel();
            damageAllPlayersTask = null;
        }
        if (fireworksTask != null) {
            fireworksTask.cancel();
            fireworksTask = null;
        }
        if (lateGameListenerTask != null) {
            lateGameListenerTask.cancel();
            lateGameListenerTask = null;
        }
        if (glowPlayersTask != null) {
            glowPlayersTask.cancel();
            glowPlayersTask = null;
        }
        if (damageNotOverworldTask != null) {
            damageNotOverworldTask.cancel();
            damageNotOverworldTask = null;
        }

        // Удаляем всех игроков из команд
        waitingPlayers.forEach(this::removeTeam);

        // Возвращаем игроков на их места
        waitingPlayers.forEach(this::preparePlayer);

        // Удаляем всех игроков из играющих
        synchronized (lock) {
            playingPlayers.clear();
        }

        // Очищаем голосования игроков
        votedForStartingPlayers.clear();
        votedForEndingPlayers.clear();
    }

    /**
     * Рекурсивно удаляет папку мира.
     */
    private void deleteWorldFolder(File path) {
        if (path.exists()) {
            File[] files = path.listFiles();
            if (files != null) {
                for (File f : files) {
                    deleteWorldFolder(f);
                }
            }
            try {
                if (path.delete()) {
                    plugin.getLogger().info("Файл удалён - " + path);
                } else {
                    plugin.getLogger().info("Не удалось удалить файл - " + path);
                }
            } catch (Exception e) {
                plugin.getLogger().info("deleteWorldFolder error: " + e.getMessage());
            }
        }
    }
}
