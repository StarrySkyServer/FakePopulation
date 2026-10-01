package com.fakepopulation;

import cn.nukkit.Server;
import cn.nukkit.plugin.PluginBase;
import cn.nukkit.plugin.PluginLogger;
import com.fakepopulation.command.FakePopCommand;
import com.fakepopulation.config.FakeConfig;
import com.fakepopulation.integration.StarrySkyLinkHook;
import com.fakepopulation.integration.TipsHook;
import com.fakepopulation.listener.PlayerJoinListener;
import com.fakepopulation.listener.QueryListener;
import com.fakepopulation.manager.FakePlayerManager;
import com.fakepopulation.manager.FakeTabList;
import com.fakepopulation.task.RefreshTask;
import eu.okaeri.configs.ConfigManager;
import eu.okaeri.configs.yaml.snakeyaml.YamlSnakeYamlConfigurer;

import java.io.File;

public class Main extends PluginBase {

    public static Main plugin;
    public static Server nkServer;
    public static PluginLogger logger;
    public static FakeConfig config;
    public static FakePlayerManager fakePlayerManager;

    @Override
    public void onLoad() {
        plugin = this;
        nkServer = getServer();
        logger = getLogger();
        String configPath = getDataFolder().getPath();

        config = ConfigManager.create(FakeConfig.class, it -> {
            it.configure(opt -> {
                opt.configurer(new YamlSnakeYamlConfigurer());
                opt.bindFile(new File(configPath + "/config.yml"));
                opt.removeOrphans(true);
            });
            it.saveDefaults();
            it.load(true);
        });

        fakePlayerManager = new FakePlayerManager();
        fakePlayerManager.loadFromConfig();

        logger.info(getName() + " 配置读取完成");
    }

    @Override
    public void onEnable() {
        nkServer.getPluginManager().registerEvents(new QueryListener(), this);
        nkServer.getPluginManager().registerEvents(new PlayerJoinListener(), this);

        nkServer.getCommandMap().register(getName(), new FakePopCommand());

        nkServer.getScheduler().scheduleRepeatingTask(this, new RefreshTask(),
                Math.max(1, config.refreshInterval()));

        // 立即生效一次，避免启动后到首次定时刷新之间仍显示真实人数
        refresh();

        logger.info(getName() + " 插件开启，当前显示人数 " + fakePlayerManager.displayCount());
    }

    /** 名单或开关变化后调用：重算人数、刷新广告串、重新同步游戏内玩家列表。 */
    public static void refresh() {
        fakePlayerManager.apply(nkServer.getQueryInformation());
        nkServer.getNetwork().updateName();
        FakeTabList.resync();
        StarrySkyLinkHook.apply();
        TipsHook.apply();
    }

    @Override
    public void onDisable() {
        FakeTabList.removeAll();
        logger.info(getName() + " 插件关闭");
    }
}