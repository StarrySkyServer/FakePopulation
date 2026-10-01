package com.fakepopulation.form;

import cn.nukkit.Player;
import cn.nukkit.form.element.ElementInput;
import cn.nukkit.form.element.ElementToggle;
import cn.nukkit.utils.TextFormat;
import com.fakepopulation.Main;
import com.fakepopulation.config.FakePlayerEntry;
import com.fakepopulation.form.easy_form.Custom;
import com.fakepopulation.form.easy_form.Modal;
import com.fakepopulation.form.easy_form.Simple;
import com.fakepopulation.validator.NameRejection;

import java.util.List;

/**
 * 四张表单：主菜单 / 添加 / 名单管理（分页）/ 单条详情。
 * <p>
 * 所有回调都跑在主线程（不使用 asyncShow），因为改完名单要立刻刷新服务端状态。
 */
public class FakePopForm {

    private static final int PAGE_SIZE = 20;
    private static final String PERMISSION = "fakepop.admin";

    public static void mainMenu(Player player) {
        if (!player.hasPermission(PERMISSION)) {
            player.sendMessage(TextFormat.RED + "你没有权限打开管理面板");
            return;
        }

        boolean on = Main.config.enabled();
        int real = Main.nkServer.getOnlinePlayers().size();
        int enabled = Main.fakePlayerManager.enabledCount();
        int total = Main.fakePlayerManager.fakePlayers().size();

        String content = TextFormat.GOLD + "总开关: " + (on ? TextFormat.GREEN + "开启" : TextFormat.RED + "关闭")
                + "\n" + TextFormat.WHITE + "真实在线: " + real
                + "\n" + TextFormat.WHITE + "假玩家: " + enabled + " 启用 / " + total + " 总数"
                + "\n" + TextFormat.WHITE + "当前显示: " + Main.fakePlayerManager.displayCount()
                + " / " + Main.nkServer.getMaxPlayers();

        Simple form = new Simple("FakePopulation", content);
        form.add(on ? "关闭总开关" : "开启总开关", () -> {
            Main.config.enabled(!on);
            Main.config.save();
            Main.refresh();
            mainMenu(player);
        });
        form.add("添加假玩家", () -> addForm(player));
        form.add("名单管理 (" + total + ")", () -> manageForm(player, 0));
        form.add("刷新", () -> {
            Main.refresh();
            mainMenu(player);
        });
        form.show(player);
    }

    private static void addForm(Player player) {
        Custom form = new Custom("添加假玩家");
        form.add("name", new ElementInput("假玩家名字", "3-16 位，仅字母 / 数字 / 下划线", ""));
        form.add("enabled", new ElementToggle("添加后立即启用", true));
        form.setSubmit(() -> {
            String name = form.getInputRes("name");
            boolean enabled = form.getToggleRes("enabled");

            NameRejection rejection = Main.fakePlayerManager.add(name, enabled);
            if (rejection != null) {
                Modal.tipsModal(player, TextFormat.RED + "添加失败：" + rejection.message(),
                        () -> addForm(player));
                return;
            }

            Main.refresh();
            Modal.tipsModal(player,
                    TextFormat.GREEN + "已添加 " + name.trim()
                            + "\n当前显示人数 " + Main.fakePlayerManager.displayCount(),
                    () -> mainMenu(player));
        });
        form.show(player);
    }

    private static void manageForm(Player player, int page) {
        List<FakePlayerEntry> entries = Main.fakePlayerManager.fakePlayers();
        if (entries.isEmpty()) {
            Modal.tipsModal(player, TextFormat.YELLOW + "假玩家名单为空", () -> mainMenu(player));
            return;
        }

        int pages = (entries.size() + PAGE_SIZE - 1) / PAGE_SIZE;
        int current = Math.max(0, Math.min(page, pages - 1));
        int from = current * PAGE_SIZE;
        int to = Math.min(from + PAGE_SIZE, entries.size());

        Simple form = new Simple("假玩家名单",
                TextFormat.WHITE + "共 " + entries.size() + " 个，第 " + (current + 1) + "/" + pages + " 页");

        for (int i = from; i < to; i++) {
            FakePlayerEntry entry = entries.get(i);
            form.add(entry.name() + (entry.enabled() ? " [启用]" : " [禁用]"),
                    () -> detailForm(player, entry.name()));
        }
        if (current > 0) {
            form.add("« 上一页", () -> manageForm(player, current - 1));
        }
        if (current < pages - 1) {
            form.add("下一页 »", () -> manageForm(player, current + 1));
        }
        form.add("返回主菜单", () -> mainMenu(player));
        form.show(player);
    }

    private static void detailForm(Player player, String name) {
        FakePlayerEntry entry = Main.fakePlayerManager.find(name);
        if (entry == null) {
            Modal.tipsModal(player, TextFormat.RED + "该假玩家已不存在", () -> manageForm(player, 0));
            return;
        }

        Simple form = new Simple("假玩家: " + entry.name(),
                TextFormat.WHITE + "状态: " + (entry.enabled() ? TextFormat.GREEN + "启用" : TextFormat.RED + "禁用"));

        form.add(entry.enabled() ? "禁用" : "启用", () -> {
            Main.fakePlayerManager.setEnabled(entry.name(), !entry.enabled());
            Main.refresh();
            detailForm(player, entry.name());
        });
        form.add("删除", () -> Modal.confirmModal(player,
                TextFormat.RED + "确定要删除 " + entry.name() + " 吗？",
                () -> detailForm(player, entry.name()),
                () -> {
                    Main.fakePlayerManager.remove(entry.name());
                    Main.refresh();
                    manageForm(player, 0);
                }));
        form.add("返回名单", () -> manageForm(player, 0));
        form.show(player);
    }
}