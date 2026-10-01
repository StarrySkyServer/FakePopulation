package com.fakepopulation.manager;

import cn.nukkit.event.server.QueryRegenerateEvent;
import com.fakepopulation.Main;
import com.fakepopulation.config.FakePlayerEntry;
import com.fakepopulation.validator.FakeNameValidator;
import com.fakepopulation.validator.NameRejection;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class FakePlayerManager {

    private final List<FakePlayerEntry> fakePlayers = new ArrayList<>();

    /**
     * 从配置读取名单，非法条目直接跳过并在控制台告警。
     * <p>
     * 手改 config.yml 可以绕过命令校验，所以加载时这一遍不能省。
     */
    public void loadFromConfig() {
        fakePlayers.clear();
        List<FakePlayerEntry> configured = Main.config.fakePlayers();
        if (configured == null) {
            return;
        }

        Set<String> accepted = new HashSet<>();
        for (FakePlayerEntry entry : configured) {
            if (entry == null) {
                continue;
            }
            String name = entry.name() == null ? "" : entry.name().trim();

            NameRejection rejection = FakeNameValidator.checkName(name);
            if (rejection == null && !accepted.add(name.toLowerCase(Locale.ROOT))) {
                rejection = NameRejection.DUPLICATE;
            }
            if (rejection == null && FakeNameValidator.isFull(fakePlayers.size())) {
                rejection = NameRejection.LIMIT;
            }
            if (rejection != null) {
                Main.logger.warning("已跳过非法假玩家 [" + name + "]：" + rejection.message());
                continue;
            }

            entry.name(name);
            fakePlayers.add(entry);
        }
    }

    public List<FakePlayerEntry> fakePlayers() {
        return fakePlayers;
    }

    public FakePlayerEntry find(String name) {
        if (name == null) {
            return null;
        }
        for (FakePlayerEntry entry : fakePlayers) {
            if (entry.name() != null && entry.name().equalsIgnoreCase(name)) {
                return entry;
            }
        }
        return null;
    }

    public boolean contains(String name) {
        return find(name) != null;
    }

    public NameRejection add(String rawName) {
        return add(rawName, true);
    }

    /** 返回 null 表示添加成功，否则为拒绝原因。 */
    public NameRejection add(String rawName, boolean enabled) {
        String name = rawName == null ? "" : rawName.trim();

        NameRejection rejection = FakeNameValidator.checkName(name);
        if (rejection != null) {
            return rejection;
        }
        if (contains(name)) {
            return NameRejection.DUPLICATE;
        }
        if (FakeNameValidator.isFull(fakePlayers.size())) {
            return NameRejection.LIMIT;
        }

        fakePlayers.add(new FakePlayerEntry().name(name).enabled(enabled));
        save();
        return null;
    }

    public boolean remove(String name) {
        FakePlayerEntry entry = find(name);
        if (entry == null) {
            return false;
        }
        fakePlayers.remove(entry);
        save();
        return true;
    }

    public boolean setEnabled(String name, boolean enabled) {
        FakePlayerEntry entry = find(name);
        if (entry == null) {
            return false;
        }
        entry.enabled(enabled);
        save();
        return true;
    }

    public void clear() {
        fakePlayers.clear();
        save();
    }

    public void save() {
        Main.config.fakePlayers(new ArrayList<>(fakePlayers));
        Main.config.save();
    }

    public int enabledCount() {
        return enabledPlayers().size();
    }

    public List<FakePlayerEntry> enabledPlayers() {
        List<FakePlayerEntry> result = new ArrayList<>();
        for (FakePlayerEntry entry : fakePlayers) {
            if (entry.enabled()) {
                result.add(entry);
            }
        }
        return result;
    }

    /** 当前对外显示的人数（受总开关影响）。 */
    public int displayCount() {
        int real = Main.nkServer.getOnlinePlayers().size();
        return Main.config.enabled() ? real + enabledCount() : real;
    }

    /**
     * 把伪造人数写进 query 事件。
     * <p>
     * 服务端每 512 tick 重建该事件，本方法也会被定时任务重复调用，因此必须幂等：
     * 真实人数从 server 实时取，而不是从 event 里取（event 里可能已是上一轮改过的值）。
     * <p>
     * 这里只改「显示用」的人数，不碰 server 的真实容量，假玩家不占用任何玩家位。
     */
    public void apply(QueryRegenerateEvent event) {
        int real = Main.nkServer.getOnlinePlayers().size();

        event.setPlayerCount(Main.config.enabled() ? real + enabledCount() : real);
        event.setMaxPlayerCount(Main.nkServer.getMaxPlayers());
    }
}