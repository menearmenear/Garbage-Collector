package com.menear.garbagecollector.ui;

import com.menear.garbagecollector.GarbageCollectorPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class GuiListener implements Listener {
    private final GarbageCollectorPlugin plugin;

    public GuiListener(GarbageCollectorPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        if (title.equals(SellGui.title())) {
            plugin.getSellGui().onClick(event);
        } else if (title.equals(ShopGui.title())) {
            plugin.getShopGui().onClick(event);
        } else if (title.equals(CollectionGui.title())) {
            plugin.getCollectionGui().onClick(event);
        } else if (title.equals(RecycleGui.title())) {
            plugin.getRecycleGui().onClick(event);
        } else if (title.equals(ZoneGui.title())) {
            plugin.getZoneGui().onClick(event);
        } else if (title.equals(MissionGui.title())) {
            plugin.getMissionGui().onClick(event);
        } else if (title.equals(AchievementGui.title())) {
            plugin.getAchievementGui().onClick(event);
        } else if (title.equals(LeaderboardGui.title())) {
            plugin.getLeaderboardGui().onClick(event);
        } else {
            plugin.getShopGui().onClick(event);
        }
    }
}