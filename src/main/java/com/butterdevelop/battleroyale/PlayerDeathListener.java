package com.butterdevelop.battleroyale;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * При смерти игрока переводит его в режим наблюдателя через некоторое время.
 */
public class PlayerDeathListener implements Listener {

    private final BattleRoyalePlugin plugin;

    public PlayerDeathListener(BattleRoyalePlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Перехватываем событие смерти игрока, определяем победу
     * @param event Событие смерти игрока
     */
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        // Определяем игрока
        final Player player = event.getEntity();

        // Если мы не на арене, то выходим
        if (player.getWorld().getName().equals(GameManager.worldLobbyName)) {
            return;
        }

        // Увеличиваем количество смертей у погибшего
        plugin.getGameManager().getScoreboardManager().addDeath(player);
        // Увеличиваем убийства у киллера
        final Player killer = event.getEntity().getKiller();
        if (killer != null) {
            plugin.getGameManager().getScoreboardManager().addKill(killer);
        }

        // Эффект молнии бьёт на месте смерти игрока
        player.getWorld().strikeLightningEffect(player.getLocation());

        // Удаляем все эндерпёрлы игрока
        player.getEnderPearls().forEach(Entity::remove);

        // Отложенные действия, чтобы дать увидеть анимацию смерти
        new BukkitRunnable() {
            @Override
            public void run() {
                // Сразу же возрождаем игрока
                event.getEntity().spigot().respawn();

                // Кидаем его в режим наблюдателя
                player.setGameMode(GameMode.SPECTATOR);
                player.sendMessage(ChatColor.RED + "Вы погибли. Теперь вы в режиме наблюдателя.");
            }
        }.runTaskLater(plugin, 15L);

        // Убираем человека из игроков
        plugin.getGameManager().removePlayingPlayer(player.getUniqueId());

        // Если не осталась одна команда, то есть уже был выигрыш
        if (!plugin.getGameManager().isGameWon()) {
            // Проверяем на победу
            plugin.getGameManager().checkForWin();
        }
    }
}
