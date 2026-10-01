package com.fakepopulation.integration;

import com.fakepopulation.Main;

import java.lang.reflect.Method;

/**
 * 覆盖 Tips 的 {@code {online}} / {@code {maxplayer}}，让它们把假玩家算进去。
 * <p>
 * Tips 暴露公开静态 API {@code tip.utils.Api.addVariable(name, value)}，值写入
 * {@code VariableManager.otherVariables}，而 {@code toMessage()} 最后才
 * {@code variables.putAll(otherVariables)}，所以能覆盖内置的 {online}/{maxplayer}。
 * <p>
 * 纯反射调用，不依赖 Tips 存在：只有开关打开且插件在场时才注入，否则直接跳过。
 */
public final class TipsHook {

    private static final String API_CLASS = "tip.utils.Api";
    private static final String TOKEN_ONLINE = "{online}";
    private static final String TOKEN_MAX_PLAYER = "{maxplayer}";

    private static Method addVariable;

    private TipsHook() {
    }

    public static void apply() {
        if (!Main.config.tipsIntegration()) {
            return;
        }
        if (Main.nkServer.getPluginManager().getPlugin("Tips") == null) {
            return;
        }

        // 总开关关闭时假玩家不计数，两个变量都回落到真实值
        int fake = Main.config.enabled() ? Main.fakePlayerManager.enabledCount() : 0;

        try {
            Method method = addVariableMethod();
            method.invoke(null, TOKEN_ONLINE, String.valueOf(Main.nkServer.getOnlinePlayers().size() + fake));
            method.invoke(null, TOKEN_MAX_PLAYER, String.valueOf(Main.nkServer.getMaxPlayers() + fake));
        } catch (ReflectiveOperationException | RuntimeException e) {
            Main.logger.warning("Tips 集成注入失败：" + e.getMessage());
        }
    }

    private static Method addVariableMethod() throws ReflectiveOperationException {
        if (addVariable == null) {
            addVariable = Class.forName(API_CLASS).getMethod("addVariable", String.class, String.class);
        }
        return addVariable;
    }
}