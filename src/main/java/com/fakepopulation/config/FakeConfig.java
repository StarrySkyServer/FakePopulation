package com.fakepopulation.config;

import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.Comment;
import eu.okaeri.configs.annotation.CustomKey;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Accessors(fluent = true)
public class FakeConfig extends OkaeriConfig {

    @Comment("总开关：false 时完全回落到真实人数，假玩家名单保留不删")
    @CustomKey("enabled")
    private boolean enabled = true;

    @Comment("假玩家名单数量上限")
    @CustomKey("max-fake-players")
    private int maxFakePlayers = 200;

    @Comment("刷新服务器列表广告串的间隔（tick，20 = 1 秒）")
    @CustomKey("refresh-interval")
    private int refreshInterval = 20;

    @Comment("把假玩家名广播到游戏内玩家列表（暂停菜单 / TAB）。注意：游戏内服务器列表条目协议不含玩家名，无法显示")
    @CustomKey("show-in-tab")
    private boolean showInTab = true;

    @Comment("保留名黑名单：这些名字不允许作为假玩家（忽略大小写）")
    @CustomKey("reserved-names")
    private List<String> reservedNames = new ArrayList<>();

    @Comment("假玩家名单：显示人数 = 真实在线人数 + 启用中的假玩家数量")
    @CustomKey("fake-players")
    private List<FakePlayerEntry> fakePlayers = new ArrayList<>();

    @Comment("StarrySkyLink 集成：把假玩家名单注入它的查询回复。需在 StarrySkyLink 的 config.yml 里"
            + "用 %fakeplayers% 调用假人名、%fakepopnum% 调用含假玩家的人数")
    @CustomKey("starryskylink-integration")
    private boolean starrySkyLinkIntegration = false;

    @Comment("Tips 集成：把 Tips 的 {online} / {maxplayer} 改成含假玩家的人数（两边各加假玩家数量）")
    @CustomKey("tips-integration")
    private boolean tipsIntegration = false;
}