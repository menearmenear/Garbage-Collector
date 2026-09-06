package com.menear.garbagecollector.ui;

import com.menear.garbagecollector.GarbageCollectorPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

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
            // shop hub + all its sub-pages guard their own titles
            plugin.getShopGui().onClick(event);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (isGarbageView(event.getView().getTitle())) {
            event.setCancelled(true);
        }
    }

    private boolean isGarbageView(String title) {
        return title.equals(SellGui.title())
                || title.equals(CollectionGui.title())
                || title.equals(RecycleGui.title())
                || title.equals(ZoneGui.title())
                || title.equals(MissionGui.title())
                || title.equals(AchievementGui.title())
                || title.equals(LeaderboardGui.title())
                || ShopGui.isShopView(title);
    }
}