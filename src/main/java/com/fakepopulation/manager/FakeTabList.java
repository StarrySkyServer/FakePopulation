package com.fakepopulation.manager;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.entity.data.Skin;
import cn.nukkit.network.protocol.PlayerListPacket;
import com.fakepopulation.Main;
import com.fakepopulation.config.FakePlayerEntry;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 把假玩家写进游戏内玩家列表（暂停菜单 / TAB）。
 * <p>
 * 基岩版服务器列表条目本身不含玩家名，名字只能通过 PlayerListPacket 展示，
 * 所以这里是「游戏内看到假玩家名」的唯一出口。
 */
public class FakeTabList {

    /** 已经发给客户端的假玩家名，用于名单变化时精确撤回 */
    private static final Set<String> sent = new LinkedHashSet<>();

    private FakeTabList() {
    }

    public static UUID uuidOf(String name) {
        return UUID.nameUUIDFromBytes(("FakePopulation:" + name.toLowerCase()).getBytes(StandardCharsets.UTF_8));
    }

    private static long entityIdOf(String name) {
        // 高位固定，避免与服务器真实实体的运行时 ID 冲突
        return 0x7F00000000000000L | (uuidOf(name).getLeastSignificantBits() & 0x00FFFFFFFFFFFFFFL);
    }

    private static PlayerListPacket packet(byte type, Collection<String> names) {
        PlayerListPacket pk = new PlayerListPacket();
        pk.type = type;
        List<PlayerListPacket.Entry> entries = new ArrayList<>(names.size());
        for (String name : names) {
            entries.add(new PlayerListPacket.Entry(uuidOf(name), entityIdOf(name), name, Skin.NO_PERSONA_SKIN, ""));
        }
        pk.entries = entries.toArray(new PlayerListPacket.Entry[0]);
        return pk;
    }

    private static List<String> enabledNames() {
        List<String> names = new ArrayList<>();
        for (FakePlayerEntry entry : Main.fakePlayerManager.enabledPlayers()) {
            names.add(entry.name());
        }
        return names;
    }

    private static List<Player> onlinePlayers() {
        return new ArrayList<>(Main.nkServer.getOnlinePlayers().values());
    }

    /** 名单或开关变化后重新同步：先撤掉旧条目，再写入当前启用的条目。 */
    public static void resync() {
        if (!Main.config.showInTab()) {
            removeAll();
            return;
        }

        List<Player> players = onlinePlayers();
        if (players.isEmpty()) {
            sent.clear();
            return;
        }

        if (!sent.isEmpty()) {
            Server.broadcastPacket(players, packet(PlayerListPacket.TYPE_REMOVE, sent));
            sent.clear();
        }

        List<String> names = enabledNames();
        if (!names.isEmpty()) {
            Server.broadcastPacket(players, packet(PlayerListPacket.TYPE_ADD, names));
            sent.addAll(names);
        }
    }

    /** 新玩家加入后补发当前假玩家条目。 */
    public static void addFor(Player player) {
        if (!Main.config.showInTab()) {
            return;
        }
        List<String> names = enabledNames();
        if (names.isEmpty()) {
            return;
        }
        player.dataPacket(packet(PlayerListPacket.TYPE_ADD, names));
        sent.addAll(names);
    }

    public static void removeAll() {
        if (sent.isEmpty()) {
            return;
        }
        List<Player> players = onlinePlayers();
        if (!players.isEmpty()) {
            Server.broadcastPacket(players, packet(PlayerListPacket.TYPE_REMOVE, sent));
        }
        sent.clear();
    }
}