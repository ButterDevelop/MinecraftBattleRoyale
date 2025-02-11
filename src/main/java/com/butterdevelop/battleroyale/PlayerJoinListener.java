package com.butterdevelop.battleroyale;

import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

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

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Определяем игрока
        final Player player = event.getPlayer();

        // Если мы не на арене, то выходим
        String worldName = player.getWorld().getName();
        if (worldName.equals(GameManager.worldLobbyName) || (
                !worldName.equals(GameManager.worldArenaName) &&
                !worldName.equals(GameManager.netherArenaName) &&
                !worldName.equals(GameManager.endArenaName))) {
            return;
        }

        // Эффект молнии бьёт на месте смерти игрока
        player.getWorld().strikeLightningEffect(player.getLocation());

        // Удаляем все эндерпёрлы игрока
        player.getEnderPearls().forEach(Entity::remove);

        // Убираем человека из игроков
        plugin.getGameManager().removePlayingPlayer(player.getUniqueId());

        // Если не осталась одна команда, то есть уже был выигрыш
        if (!plugin.getGameManager().isGameWon()) {
            // Проверяем на победу
            plugin.getGameManager().checkForWin();
        }
    }
}
