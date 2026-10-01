package com.fakepopulation.listener;

import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.player.PlayerJoinEvent;
import com.fakepopulation.Main;
import com.fakepopulation.manager.FakeTabList;

public class PlayerJoinListener implements Listener {

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        // 延迟 1 秒再补发，避免被服务端登录流程里的玩家列表覆盖
        Main.nkServer.getScheduler().scheduleDelayedTask(Main.plugin,
                () -> FakeTabList.addFor(event.getPlayer()), 20);
    }
}