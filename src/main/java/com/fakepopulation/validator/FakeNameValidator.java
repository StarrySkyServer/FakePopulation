package com.fakepopulation.validator;

import cn.nukkit.Server;
import com.fakepopulation.Main;

import java.util.List;
import java.util.Locale;

/**
 * 假玩家名校验，规则对齐 Xbox 现代玩家代号（modern gamertag）。
 * <p>
 * 与名单当前状态无关的规则放在 {@link #checkName}，名单查重与容量由调用方处理，
 * 这样添加时和加载配置时能复用同一套规则。
 * <p>
 * 字符范围依据 Microsoft GDK《现代玩家代号的 UTF-8 字符范围》：玩家可自建字符为
 * 空格、撇号、0-9、A-Z、a-z，以及拉丁补充 / 拉丁扩展 A / CJK 等语言区块。
 */
public final class FakeNameValidator {

    /** Xbox 唯一玩家代号上限为 15 个字符（2026 年由 12 放宽而来） */
    private static final int MIN_LENGTH = 3;
    private static final int MAX_LENGTH = 15;

    private FakeNameValidator() {
    }

    /** 返回 null 表示通过。 */
    public static NameRejection checkName(String name) {
        if (name == null || name.isEmpty()) {
            return NameRejection.EMPTY;
        }
        if (!name.equals(name.trim())) {
            return NameRejection.SPACING;
        }
        if (name.length() < MIN_LENGTH || name.length() > MAX_LENGTH) {
            return NameRejection.LENGTH;
        }
        if (!isAllowedCharset(name)) {
            return NameRejection.CHARSET;
        }
        if (name.contains("  ")) {
            return NameRejection.SPACING;
        }

        Server server = Main.nkServer;
        if (server.getPlayerExact(name) != null) {
            return NameRejection.ONLINE;
        }
        // lookupName 内部按小写查已注册玩家索引，天然忽略大小写
        if (server.lookupName(name).isPresent()) {
            return NameRejection.REGISTERED;
        }
        if (isReserved(name)) {
            return NameRejection.RESERVED;
        }
        return null;
    }

    public static boolean isFull(int currentSize) {
        return currentSize >= Main.config.maxFakePlayers();
    }

    /** 逐字符判断是否落在 Xbox 现代玩家代号允许的范围内。 */
    private static boolean isAllowedCharset(String name) {
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (c == ' ' || c == '\'') {
                continue;
            }
            if (c >= '0' && c <= '9') {
                continue;
            }
            if (c >= 'A' && c <= 'Z') {
                continue;
            }
            if (c >= 'a' && c <= 'z') {
                continue;
            }
            if (c >= '\u00C0' && c <= '\u00F6') {
                continue;
            }
            if (c >= '\u00F8' && c <= '\u00FF') {
                continue;
            }
            if (c >= '\u0100' && c <= '\u017F') {
                continue;
            }
            if (c >= '\u4E00' && c <= '\u9FFF') {
                continue;
            }
            return false;
        }
        return true;
    }

    private static boolean isReserved(String name) {
        List<String> reserved = Main.config.reservedNames();
        if (reserved == null || reserved.isEmpty()) {
            return false;
        }
        String lower = name.toLowerCase(Locale.ROOT);
        for (String item : reserved) {
            if (item != null && item.trim().toLowerCase(Locale.ROOT).equals(lower)) {
                return true;
            }
        }
        return false;
    }
}