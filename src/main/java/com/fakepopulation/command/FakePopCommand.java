package com.fakepopulation.command;

import cn.nukkit.Player;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import cn.nukkit.utils.TextFormat;
import com.fakepopulation.Main;
import com.fakepopulation.config.FakePlayerEntry;
import com.fakepopulation.form.FakePopForm;
import com.fakepopulation.validator.NameRejection;

import java.util.List;

public class FakePopCommand extends Command {

    private static final String USAGE =
            "/fakepop <ui|status|list|add|remove|enable|disable|clear|on|off|reload>";

    public FakePopCommand() {
        super("fakepop", "FakePopulation 控制命令", USAGE, new String[]{"fp"});
        this.setPermission("fakepop.use");
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        if (!this.testPermission(sender)) {
            return true;
        }

        String sub = args.length == 0
                ? (sender instanceof Player ? "ui" : "status")
                : args[0].toLowerCase();
        switch (sub) {
            case "ui" -> ui(sender);
            case "status" -> sendStatus(sender);
            case "list" -> list(sender);
            case "add" -> add(sender, args);
            case "remove" -> remove(sender, args);
            case "enable" -> setEnabled(sender, args, true);
            case "disable" -> setEnabled(sender, args, false);
            case "clear" -> clear(sender);
            case "on" -> toggle(sender, true);
            case "off" -> toggle(sender, false);
            case "reload" -> reload(sender);
            default -> sender.sendMessage(TextFormat.YELLOW + "用法: " + USAGE);
        }
        return true;
    }

    private boolean requireAdmin(CommandSender sender) {
        if (!sender.hasPermission("fakepop.admin")) {
            sender.sendMessage(TextFormat.RED + "你没有权限执行该操作");
            return false;
        }
        return true;
    }

    /**
     * 把子命令之后的参数拼回完整名字，并剥掉外层引号。
     * <p>
     * 带空格的名字在命令里用引号括起（如 add "Major Nelson"）。不同来源的参数切分方式
     * 可能不同：可能整体作为一个参数传入，也可能按空格拆成多个，这里两种都兼容。
     */
    private static String joinName(String[] args) {
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            if (i > 1) {
                sb.append(' ');
            }
            sb.append(args[i]);
        }
        String name = sb.toString().trim();
        if (name.length() >= 2 && name.startsWith("\"") && name.endsWith("\"")) {
            name = name.substring(1, name.length() - 1);
        }
        return name;
    }

    private void ui(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(TextFormat.YELLOW + "控制台无法打开表单，请用 /fakepop status");
            return;
        }
        if (!requireAdmin(sender)) {
            return;
        }
        FakePopForm.mainMenu(player);
    }

    private void add(CommandSender sender, String[] args) {
        if (!requireAdmin(sender)) {
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(TextFormat.YELLOW + "用法: /fakepop add <名字>（含空格请加引号，如 add \"Major Nelson\"）");
            return;
        }

        String name = joinName(args);
        NameRejection rejection = Main.fakePlayerManager.add(name);
        if (rejection != null) {
            sender.sendMessage(TextFormat.RED + "添加失败：" + rejection.message());
            return;
        }

        Main.refresh();
        sender.sendMessage(TextFormat.GREEN + "已添加假玩家 " + name
                + "，当前显示人数 " + Main.fakePlayerManager.displayCount());
    }

    private void remove(CommandSender sender, String[] args) {
        if (!requireAdmin(sender)) {
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(TextFormat.YELLOW + "用法: /fakepop remove <名字>（含空格请加引号）");
            return;
        }

        String name = joinName(args);
        if (!Main.fakePlayerManager.remove(name)) {
            sender.sendMessage(TextFormat.RED + "名单中没有 " + name);
            return;
        }

        Main.refresh();
        sender.sendMessage(TextFormat.GREEN + "已移除假玩家 " + name
                + "，当前显示人数 " + Main.fakePlayerManager.displayCount());
    }

    private void setEnabled(CommandSender sender, String[] args, boolean enabled) {
        if (!requireAdmin(sender)) {
            return;
        }
        String action = enabled ? "enable" : "disable";
        if (args.length < 2) {
            sender.sendMessage(TextFormat.YELLOW + "用法: /fakepop " + action + " <名字>（含空格请加引号）");
            return;
        }

        String name = joinName(args);
        if (!Main.fakePlayerManager.setEnabled(name, enabled)) {
            sender.sendMessage(TextFormat.RED + "名单中没有 " + name);
            return;
        }

        Main.refresh();
        sender.sendMessage(TextFormat.GREEN + "已" + (enabled ? "启用" : "禁用") + " " + name
                + "，当前显示人数 " + Main.fakePlayerManager.displayCount());
    }

    private void clear(CommandSender sender) {
        if (!requireAdmin(sender)) {
            return;
        }

        int removed = Main.fakePlayerManager.fakePlayers().size();
        Main.fakePlayerManager.clear();
        Main.refresh();
        sender.sendMessage(TextFormat.GREEN + "已清空 " + removed + " 个假玩家");
    }

    private void toggle(CommandSender sender, boolean enabled) {
        if (!requireAdmin(sender)) {
            return;
        }

        Main.config.enabled(enabled);
        Main.config.save();
        Main.refresh();
        sender.sendMessage(TextFormat.GREEN + "总开关已" + (enabled ? "开启" : "关闭")
                + "，当前显示人数 " + Main.fakePlayerManager.displayCount());
    }

    private void list(CommandSender sender) {
        List<FakePlayerEntry> entries = Main.fakePlayerManager.fakePlayers();
        if (entries.isEmpty()) {
            sender.sendMessage(TextFormat.YELLOW + "假玩家名单为空");
            return;
        }

        sender.sendMessage(TextFormat.GOLD + "===== 假玩家名单 (" + entries.size() + ") =====");
        for (FakePlayerEntry entry : entries) {
            sender.sendMessage(TextFormat.WHITE + " - " + entry.name()
                    + (entry.enabled() ? TextFormat.GREEN + " [启用]" : TextFormat.GRAY + " [禁用]"));
        }
    }

    private void reload(CommandSender sender) {
        if (!requireAdmin(sender)) {
            return;
        }

        Main.config.load();
        Main.fakePlayerManager.loadFromConfig();
        Main.refresh();
        sender.sendMessage(TextFormat.GREEN + "配置已重载，当前显示人数 " + Main.fakePlayerManager.displayCount());
    }

    private void sendStatus(CommandSender sender) {
        int real = Main.nkServer.getOnlinePlayers().size();
        boolean on = Main.config.enabled();

        sender.sendMessage(TextFormat.GOLD + "===== FakePopulation =====");
        sender.sendMessage(TextFormat.WHITE + "总开关: " + (on ? TextFormat.GREEN + "开启" : TextFormat.RED + "关闭"));
        sender.sendMessage(TextFormat.WHITE + "真实在线: " + real
                + "，假玩家(启用/总数): " + Main.fakePlayerManager.enabledCount()
                + "/" + Main.fakePlayerManager.fakePlayers().size()
                + "，上限 " + Main.config.maxFakePlayers());
        sender.sendMessage(TextFormat.WHITE + "当前显示人数: " + Main.fakePlayerManager.displayCount()
                + " / " + Main.nkServer.getMaxPlayers());
        sender.sendMessage(TextFormat.WHITE + "游戏内玩家列表(TAB): "
                + (Main.config.showInTab() ? TextFormat.GREEN + "开启" : TextFormat.RED + "关闭"));
    }
}