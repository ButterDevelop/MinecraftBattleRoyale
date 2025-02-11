package com.butterdevelop.battleroyale;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

/**
 * Контролируем взаимодействие игрока в лобби
 */
public class PlayerInteractListener implements Listener {

    public PlayerInteractListener() {
    }

    /**
     * Не даём использовать в лобби сундуки, раздатчики, выбрасыватели, кровати, бочки, шалкеры, печки и т.п.
     * @param event Событие взаимодействия игрока
     */
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        // Отключаем эндер-сундуки в принципе во избежание проблем
        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock != null && clickedBlock.getType() == Material.ENDER_CHEST && event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "Эндер-сундуки в этом режиме отключены. А зачем они в теории могут понадобиться?");
            return;
        }

        // Если мы находимся в мире лобби и выполнено интересующее нас действие
        if (player.getWorld().getName().equals(GameManager.worldLobbyName) &&
                (event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.LEFT_CLICK_BLOCK)
        ) {
            if (clickedBlock != null) {
                // Проверка на запрещённые блоки
                boolean shouldCancel = clickedBlock.getType().equals(Material.CHEST) ||
                        clickedBlock.getType().equals(Material.CHEST_MINECART) ||
                        clickedBlock.getType().equals(Material.HOPPER) ||
                        clickedBlock.getType().equals(Material.HOPPER_MINECART) ||
                        clickedBlock.getType().equals(Material.DISPENSER) ||
                        clickedBlock.getType().equals(Material.DROPPER) ||
                        clickedBlock.getType().equals(Material.FURNACE) ||
                        clickedBlock.getType().equals(Material.SHULKER_BOX) ||
                        clickedBlock.getType().equals(Material.BARREL) ||
                        clickedBlock.getType().toString().endsWith("BED") ||
                        clickedBlock.getType().equals(Material.PAINTING);

                // Если всё же нужно отменить действие
                if (shouldCancel) {
                    player.sendMessage(ChatColor.RED + "Вы не можете взаимодействовать с этим блоком в мире лобби.");
                    event.setCancelled(true);
                }
            }
        }
    }
}
