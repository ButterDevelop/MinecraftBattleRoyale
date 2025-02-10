package com.butterdevelop.battleroyale;

import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;

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
            GameManager.preparePlayer(player.getUniqueId());
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
        GameManager.preparePlayer(player.getUniqueId());

        // Добавляем игрока в список ожидания
        plugin.getGameManager().addWaitingPlayer(player.getUniqueId());
    }
}
