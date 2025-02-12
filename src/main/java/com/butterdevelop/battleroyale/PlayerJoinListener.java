package com.butterdevelop.battleroyale;

import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.*;

/**
 * При входе игрока:
 * – Телепортируем его в мир лобби (предполагается, что существует мир "lobby"; если нет – используется первый мир);
 * – Добавляем его в список ожидания для игры.
 */
public class PlayerJoinListener implements Listener {

    private final BattleRoyalePlugin plugin;

    public PlayerJoinListener(BattleRoyalePlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Если игрок вошёл в мир вне арены - выбрасываем его
     * @param event Событие смены мира игроком
     */
    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        World  world  = player.getWorld();

        // Если игрок появился не в мире лобби вне игры - перемещаем его в лобби
        if (!world.getName().equals(GameManager.worldLobbyName) && !plugin.getGameManager().isGameStarted()) {
            plugin.getGameManager().preparePlayer(player.getUniqueId());
        }
    }

    /**
     * Если пользователь умрёт в мире лобби, то предметы без перезахода не появятся. Решаем эту проблему
     * @param event Событие возрождения игрока
     */
    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        World  world  = player.getWorld();

        // Если игрок появился в лобби (то есть скорее всего и умер в лобби)
        if (world.getName().equals(GameManager.worldLobbyName)) {
            // Подготавливаем игрока
            plugin.getGameManager().preparePlayer(player.getUniqueId());
        }
    }

    /**
     * Выполняем действия при заходе игрока на сервер
     * @param event Событие входа игрока на сервер
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        World  world  = player.getWorld();

        // Если игрок появился не в лобби
        if (!world.getName().equals(GameManager.worldLobbyName)) {
            // Сообщаем пользователю о перемещении его в другой мир
            player.sendMessage(ChatColor.YELLOW + "Вы были перемещены в лобби Battle Royale!");
        }

        // Подготавливаем игрока
        plugin.getGameManager().preparePlayer(player.getUniqueId());

        // Добавляем игрока в список ожидания
        plugin.getGameManager().addWaitingPlayer(player.getUniqueId());
    }

    /**
     * Если пользователя кикнул сервер, в принципе, это то же самое, что и при его выходе
     * @param event Событие кика игрока
     */
    @EventHandler
    public void onPlayerKick(PlayerKickEvent event) {
        final Player player = event.getPlayer();
        playerLeaves(player);
    }

    /**
     * Выполняем действия при выходе игрока с сервера
     * @param event Событие выхода игрока с сервера
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        final Player player = event.getPlayer();
        playerLeaves(player);
    }

    /**
     * Отдельная функция, обрабатывающая, что игрока нет на сервере
     * @param player Игрок
     */
    public void playerLeaves(final Player player) {
        // Если мы не на арене, то выходим
        String worldName = player.getWorld().getName();
        if (worldName.equals(GameManager.worldLobbyName) || (
                !worldName.equals(GameManager.worldArenaName) &&
                        !worldName.equals(GameManager.netherArenaName) &&
                        !worldName.equals(GameManager.endArenaName))) {
            return;
        }

        // Эффект молнии бьёт на месте смерти игрока, если он был частью игры
        if (plugin.getGameManager().containsPlayingPlayer(player.getUniqueId())) {
            player.getWorld().strikeLightningEffect(player.getLocation());
        }

        // Удаляем все эндерпёрлы игрока
        player.getEnderPearls().forEach(Entity::remove);

        // Убираем человека из игроков
        plugin.getGameManager().removePlayingPlayer(player.getUniqueId());

        // Убираем человека из ожидающих игроков
        plugin.getGameManager().removeWaitingPlayer(player.getUniqueId());

        // Убираем человека из команды
        plugin.getGameManager().removeTeam(player.getUniqueId());

        // Если не осталась одна команда, то есть уже был выигрыш
        if (!plugin.getGameManager().isGameWon()) {
            // Проверяем на победу
            plugin.getGameManager().checkForWin();
        }
    }
}
