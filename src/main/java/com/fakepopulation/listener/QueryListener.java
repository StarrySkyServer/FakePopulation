package com.fakepopulation.listener;

import cn.nukkit.event.EventHandler;
import cn.nukkit.event.EventPriority;
import cn.nukkit.event.Listener;
import cn.nukkit.event.server.QueryRegenerateEvent;
import com.fakepopulation.Main;

public class QueryListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onQueryRegenerate(QueryRegenerateEvent event) {
        Main.fakePlayerManager.apply(event);
    }
}