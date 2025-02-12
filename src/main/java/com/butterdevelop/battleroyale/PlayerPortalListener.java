package com.butterdevelop.battleroyale;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Работаем с порталами подстать режиму игры
 */
public class PlayerPortalListener implements Listener {

    private final BattleRoyalePlugin plugin;

    public PlayerPortalListener(BattleRoyalePlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Выключаем край, убираем деление на 8 в координатах адского мира,
     * и кидаем пользователя в измерение арены (например, battleroyale_nether, а не world_nether)
     * @param event Событие смены мира
     */
    @EventHandler
    public void onPlayerPortal(PlayerPortalEvent event) {
        // Достаём нужные миры
        World fromWorld = event.getFrom().getWorld();
        World toWorld   = event.getTo() != null ? event.getTo().getWorld() : null;

        // Проверяем на null
        if (fromWorld == null || toWorld == null) {
            return;
        }

        // Достаём имена миров для сравнения
        String fromWorldName = fromWorld.getName();

        // Выключаем край
        if (fromWorldName.equals(GameManager.worldArenaName) && toWorld.getEnvironment() == World.Environment.THE_END) {
            event.getPlayer().sendMessage(ChatColor.RED + "Мир края недоступен в текущей версии Battle Royale.");
            event.setCancelled(true);
            return;
        }

        // Из обычного мира в адский - выставляем правильный мир и координаты
        if (fromWorldName.equals(GameManager.worldArenaName) && toWorld.getEnvironment() == World.Environment.NETHER) {
            Location getFrom = event.getFrom();
            Location getTo   = event.getTo();
            event.setTo(new Location(Bukkit.getWorld(GameManager.netherArenaName), getFrom.getX(), getTo.getY(), getFrom.getZ()));
            return;
        }

        // Из адского мира в обычный
        if (fromWorldName.equals(GameManager.netherArenaName) && toWorld.getEnvironment() == World.Environment.NORMAL) {
            Location getFrom = event.getFrom();
            Location getTo = event.getTo();
            event.setTo(new Location(Bukkit.getWorld(GameManager.worldArenaName), getFrom.getX(), getTo.getY(), getFrom.getZ()));
            return;
        }
    }


    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        // Проверим, стоит ли игрока кинуть в режим наблюдателя
        Player player    = event.getPlayer();
        String worldName = player.getWorld().getName();

        // Если это мир лобби, то выходим
        if (worldName.equals(GameManager.worldLobbyName)) {
            // Приветствуем игрока
            player.sendMessage(ChatColor.GREEN + "" + ChatColor.BOLD + "Добро пожаловать в лобби " + ChatColor.UNDERLINE + "Battle Royale!");

            plugin.getGameManager().preparePlayer(player.getUniqueId());
        } else {
            // Если игрок попал в мир арены, при этом не играет,
            // то стоит кинуть его в режим наблюдателя, если он уже не в нём (на всякий случай)
            if ((worldName.equals(GameManager.worldArenaName) ||
                 worldName.equals(GameManager.netherArenaName) ||
                 worldName.equals(GameManager.endArenaName)
                ) &&
                !plugin.getGameManager().containsPlayingPlayer(player.getUniqueId()) &&
                player.getGameMode() != GameMode.SPECTATOR
            ) {
                // Даём эффект ночного зрения, невидимости и неуязвимости игроку для удобства
                PotionEffect nightVisionPotionEffect = new PotionEffect(PotionEffectType.NIGHT_VISION, PotionEffect.INFINITE_DURATION,
                        Integer.MAX_VALUE, false, false);
                player.addPotionEffect(nightVisionPotionEffect);

                // Кидаем игрока в режим наблюдателя
                player.setGameMode(GameMode.SPECTATOR);

                if (plugin.getGameManager().isGameStarted()) {
                    // Сообщаем игроку об этом
                    player.sendMessage(ChatColor.YELLOW + "Вы попали в мир игры, но при этом не участвуете в ней. Вам был выдан режим наблюдателя.");
                } else {
                    plugin.getGameManager().preparePlayer(player.getUniqueId());
                }
            }
        }
    }
}
