package com.fakepopulation.integration;

import cn.nukkit.utils.Config;
import com.fakepopulation.Main;

import java.io.File;
import java.lang.reflect.Field;
import java.util.StringJoiner;

/**
 * 把假玩家名单注入 StarrySkyLink 的查询回复。
 * <p>
 * StarrySkyLink 没有占位符注册接口，{@code %players%}/{@code %playerNum%} 都是它内部从真实
 * 玩家算出来的，外部无法让它主动调用我们。所以这里走推送：读它 config.yml 里的
 * {@code QueryServerMessage} 模板，把 {@code %fakeplayers%}/{@code %fakepopnum%} 替换掉，
 * 再写回它的公开静态字段 {@code MyConfig.QueryServerMessage}。它每次查询都实时读该字段，
 * 所以替换立即生效；模板每次刷新都从文件重读（带 mtime 缓存），因此用户手改配置也能跟着变。
 * <p>
 * 不依赖 StarrySkyLink 存在：只有配置开关打开且插件在场时才会反射操作，否则直接跳过。
 */
public final class StarrySkyLinkHook {

    private static final String CONFIG_CLASS = "top.szzz666.StarrySkyLink.config.MyConfig";
    private static final String MESSAGE_FIELD = "QueryServerMessage";
    private static final String TOKEN_FAKE_PLAYERS = "%fakeplayers%";
    private static final String TOKEN_FAKE_COUNT = "%fakepopnum%";

    private static String cachedTemplate;
    private static long cachedModified = -1L;

    private StarrySkyLinkHook() {
    }

    public static void apply() {
        if (!Main.config.starrySkyLinkIntegration()) {
            return;
        }
        if (Main.nkServer.getPluginManager().getPlugin("StarrySkyLink") == null) {
            return;
        }

        String template = readTemplate();
        if (template == null) {
            return;
        }

        String injected = template
                .replace(TOKEN_FAKE_PLAYERS, fakeNames())
                .replace(TOKEN_FAKE_COUNT, String.valueOf(Main.fakePlayerManager.displayCount()));

        try {
            Field field = Class.forName(CONFIG_CLASS).getField(MESSAGE_FIELD);
            if (!injected.equals(field.get(null))) {
                field.set(null, injected);
            }
        } catch (ReflectiveOperationException | RuntimeException e) {
            Main.logger.warning("StarrySkyLink 集成注入失败：" + e.getMessage());
        }
    }

    /** 读取模板；文件没变时走缓存，避免每次刷新都解析 YAML。 */
    private static String readTemplate() {
        File file = new File(Main.plugin.getDataFolder().getParentFile(), "StarrySkyLink/config.yml");
        if (!file.isFile()) {
            return null;
        }

        long modified = file.lastModified();
        if (cachedTemplate != null && modified == cachedModified) {
            return cachedTemplate;
        }

        cachedTemplate = new Config(file, Config.YAML).getString("QueryServerMessage");
        cachedModified = modified;
        return cachedTemplate;
    }

    private static String fakeNames() {
        StringJoiner joiner = new StringJoiner(", ");
        Main.fakePlayerManager.enabledPlayers().forEach(entry -> joiner.add(entry.name()));
        return joiner.toString();
    }
}