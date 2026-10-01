package com.fakepopulation.task;

import cn.nukkit.scheduler.Task;
import com.fakepopulation.Main;
import com.fakepopulation.integration.StarrySkyLinkHook;
import com.fakepopulation.integration.TipsHook;

public class RefreshTask extends Task {

    @Override
    public void onRun(int currentTick) {
        if (Main.nkServer == null) {
            return;
        }
        // RakNet 广告串只在 Network.updateName() 时刷新，否则游戏内服务器列表不会更新人数
        Main.fakePlayerManager.apply(Main.nkServer.getQueryInformation());
        Main.nkServer.getNetwork().updateName();
        // 真实玩家进出会改变名单，所以注入内容也要跟着重算
        StarrySkyLinkHook.apply();
        TipsHook.apply();
    }
}