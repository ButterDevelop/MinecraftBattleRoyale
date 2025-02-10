package com.butterdevelop.battleroyale;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerPortalEvent;

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
}
