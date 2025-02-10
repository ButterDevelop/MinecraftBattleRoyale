package com.butterdevelop.battleroyale;

import org.bukkit.ChatColor;
import org.bukkit.scoreboard.Team;

// Класс с информацией о команде
public class TeamInfo {
    private final String    name;
    private final ChatColor color;
    private final Team      sbTeam;

    public TeamInfo(String name, ChatColor color, Team sbTeam) {
        this.name     = name;
        this.color    = color;
        this.sbTeam   = sbTeam;
    }

    public String getName() {
        return name;
    }

    public ChatColor getColor() {
        return color;
    }

    public Team getScoreboardTeam() {
        return sbTeam;
    }
}