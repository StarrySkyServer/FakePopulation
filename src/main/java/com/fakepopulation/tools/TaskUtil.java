package com.fakepopulation.tools;

import cn.nukkit.scheduler.AsyncTask;
import cn.nukkit.scheduler.TaskHandler;

import static com.fakepopulation.Main.nkServer;
import static com.fakepopulation.Main.plugin;


public class TaskUtil {
    // 同步   Sync(() -> {});
    public static TaskHandler Sync(Runnable logic) {
        return nkServer.getScheduler().scheduleTask(plugin, new AsyncTask() {
            @Override
            public void onRun() {
                logic.run();
            }
        });
    }

    // 异步   Async(() -> {});
    public static TaskHandler Async(Runnable logic) {
        return nkServer.getScheduler().scheduleAsyncTask(plugin, new AsyncTask() {
            @Override
            public void onRun() {
                logic.run();
            }
        });
    }

    // 延迟   Delayed(() -> {}, 20, true);
    public static TaskHandler Delayed(Runnable logic, int delay, boolean asynchronous) {
        return nkServer.getScheduler().scheduleDelayedTask(plugin, logic, delay, asynchronous);
    }

    // 重复   Repeating(() -> {}, 20, true);
    public static TaskHandler Repeating(Runnable logic, int delay, boolean asynchronous) {
        return nkServer.getScheduler().scheduleRepeatingTask(plugin, logic, delay, asynchronous);
    }
}