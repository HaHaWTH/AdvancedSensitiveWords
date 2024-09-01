package io.wdsj.asw.bukkit.setting;

import ch.jalu.configme.Comment;
import ch.jalu.configme.SettingsHolder;
import ch.jalu.configme.configurationdata.CommentsConfiguration;
import ch.jalu.configme.properties.Property;

import static ch.jalu.configme.properties.PropertyInitializer.newProperty;

public class PluginMessages implements SettingsHolder {
    @Comment("玩家发送敏感消息时候的提示")
    public static final Property<String> MESSAGE_ON_CHAT = newProperty("Chat.messageOnChat", "&c请勿在聊天中发送敏感词汇.");
    @Comment("玩家写入敏感消息时的提示")
    public static final Property<String> MESSAGE_ON_SIGN = newProperty("Sign.messageOnSign", "&c请勿在告示牌中写入敏感词汇.");
    @Comment("玩家在铁砧重命名时写入敏感消息的提示")
    public static final Property<String> MESSAGE_ON_ANVIL_RENAME = newProperty("Anvil.messageOnAnvilRename", "&c请勿在铁砧中写入敏感词汇.");
    @Comment("玩家在书中写入敏感消息的提示")
    public static final Property<String> MESSAGE_ON_BOOK = newProperty("Book.messageOnBook", "&c请勿在书中写入敏感词汇.");
    @Comment("玩家名包含敏感词时的消息")
    public static final Property<String> MESSAGE_ON_NAME = newProperty("Name.messageOnName", "&c您的用户名包含敏感词,请修改您的用户名或联系管理员.");
    @Comment("玩家物品包含敏感词时的消息")
    public static final Property<String> MESSAGE_ON_ITEM = newProperty("Item.messageOnItem", "&c您的物品包含敏感词.");
    @Comment("插件重载消息")
    public static final Property<String> MESSAGE_ON_COMMAND_RELOAD = newProperty("Plugin.messageOnCommandReload", "&aAdvancedSensitiveWords 已重新加载.");
    @Comment("违规次数重置消息")
    public static final Property<String> MESSAGE_ON_VIOLATION_RESET = newProperty("Plugin.messageOnViolationReset", "&a&l已重置所有玩家的违规次数!");
    @Comment("插件帮助菜单")
    public static final Property<String> MESSAGE_ON_COMMAND_HELP = newProperty("Plugin.messageOnCommandHelp", "&bAdvancedSensitiveWords&r---&b帮助菜单\n   &7/asw reload&7: &a重新加载过滤词库和插件配置\n   &7/asw reloadconfig: &a重新加载插件配置\n   &7/asw add <敏感词>: &a添加敏感词\n   &7/asw remove <敏感词>: &a移除敏感词\n   &7/asw status: &a显示插件状态菜单\n   &7/asw test <待测消息>: &a运行敏感词测试\n   &7/asw help: &a显示帮助信息\n   &7/asw info <玩家名称>: &a获取玩家违规次数\n   &7/asw punish <玩家名称> [惩罚]: &a手动惩罚玩家, 不填惩罚将使用配置文件内容");
    @Comment("插件状态菜单")
    public static final Property<String> MESSAGE_ON_COMMAND_STATUS = newProperty("Plugin.messageOnCommandStatus", "&bAdvancedSensitiveWords&r---&b插件状态(%version%)(MC %mc_version%)\n   &7系统信息: &b%platform% %bit% (Java %java_version% -- %java_vendor%)\n   &7初始化: %init%\n   &7当前模式: %mode%\n   &7已过滤消息数: &a%num%\n   &7近20次处理平均耗时: %ms%");
    @Comment("敏感词测试返回")
    public static final Property<String> MESSAGE_ON_COMMAND_TEST = newProperty("Plugin.commandTest.testResultTrue", "&b一眼丁真, 鉴定为敏感词(鉴定报告)\n   &7原消息: &c%original_msg%\n   &7过滤后消息: &a%processed_msg%\n   &7敏感词列表: &b%censored_list%");
    @Comment("敏感词测试通过")
    public static final Property<String> MESSAGE_ON_COMMAND_TEST_PASS = newProperty("Plugin.commandTest.testResultPass", "&a待测消息中没有敏感词喵~");
    @Comment("敏感词测试未初始化")
    public static final Property<String> MESSAGE_ON_COMMAND_TEST_NOT_INIT = newProperty("Plugin.commandTest.testNotInit", "&c插件还没有初始化完毕喵");
    @Comment("解析方法出错")
    public static final Property<String> MESSAGE_ON_COMMAND_PUNISH_PARSE_ERROR = newProperty("Plugin.commandPunish.parseError", "&c解析方法出错, 请检查指令格式.");
    @Comment("已惩罚")
    public static final Property<String> MESSAGE_ON_COMMAND_PUNISH_SUCCESS = newProperty("Plugin.commandPunish.success", "&a成功惩罚玩家 %player%.");
    @Comment("敏感词添加成功")
    public static final Property<String> MESSAGE_ON_COMMAND_ADD_SUCCESS = newProperty("Plugin.commandAdd.success", "&a敏感词添加成功.");
    @Comment("操作繁忙")
    public static final Property<String> MESSAGE_ON_COMMAND_ADD_BUSY = newProperty("Plugin.commandAdd.busy", "&c操作繁忙, 请稍后再试");
    @Comment("敏感词移除成功")
    public static final Property<String> MESSAGE_ON_COMMAND_REMOVE_SUCCESS = newProperty("Plugin.commandRemove.success", "&a敏感词移除成功.");
    @Comment("操作繁忙")
    public static final Property<String> MESSAGE_ON_COMMAND_REMOVE_BUSY = newProperty("Plugin.commandRemove.busy", "&c操作繁忙, 请稍后再试");
    @Comment("没有权限执行该指令")
    public static final Property<String> NO_PERMISSION = newProperty("Plugin.noPermission", "&c你没有权限执行该指令.");
    @Comment("未知命令")
    public static final Property<String> UNKNOWN_COMMAND = newProperty("Plugin.unknownCommand", "&c未知命令, 请使用 &7/asw help");
    @Comment("命令参数不足")
    public static final Property<String> NOT_ENOUGH_ARGS = newProperty("Plugin.argsNotEnough", "&c参数不足, 请使用 &7/asw help");
    @Comment("找不到对应玩家")
    public static final Property<String> PLAYER_NOT_FOUND = newProperty("Plugin.noSuchPlayer", "&c找不到对应玩家");
    @Comment("管理员提醒消息")
    public static final Property<String> ADMIN_REMINDER = newProperty("Plugin.noticeOperator", "&f[&bASW&7Notify&f]&7玩家 &c%player% &7触发了敏感词检测(%type%)(原消息: %message%)");
    @Comment("跨服提醒消息")
    public static final Property<String> ADMIN_REMINDER_PROXY = newProperty("Plugin.noticeOperatorProxy", "&f[&bASW&7Notify&f]&7玩家 &c%player% (服务器: %server_name%) &7触发了敏感词检测(%type%)(原消息: %message%)");
    @Comment("更新可用")
    public static final Property<String> UPDATE_AVAILABLE = newProperty("Plugin.updateAvailable", "&f[&bASW&7Notify&f]&7插件有可用更新(%latest_version%), 当前正在运行: &b%current_version%, 请前往 &b%url% &7下载新版本.");
    @Comment("获取到玩家信息")
    public static final Property<String> MESSAGE_ON_PLAYER_INFO = newProperty("Plugin.database.playerInfo", "&bAdvancedSensitiveWords&r---&b玩家信息\n   &7玩家名称: &b%player%\n   &7违规次数: &a%num%");
    @Comment("获取玩家信息失败")
    public static final Property<String> MESSAGE_ON_PLAYER_INFO_FAIL = newProperty("Plugin.database.playerInfoFailed", "&c获取玩家信息失败, 请检查玩家名称是否正确");
    @Comment("玩家信息数据库已关闭")
    public static final Property<String> MESSAGE_ON_PLAYER_INFO_CLOSE = newProperty("Plugin.database.playerInfoClosed", "&c玩家信息数据库已禁用");


    @Override
    public void registerComments(CommentsConfiguration conf) {
        conf.setComment("", "AdvancedSensitiveWords 插件消息配置");
        conf.setComment("Plugin", "插件消息");
        conf.setComment("Plugin.commandTest", "敏感词测试消息(不计入已过滤消息)");
        conf.setComment("Plugin.commandPunish", "惩罚消息");
        conf.setComment("Plugin.commandAdd", "Add 指令相关");
        conf.setComment("Plugin.commandRemove", "Remove 指令相关");
        conf.setComment("Chat", "聊天检测消息");
        conf.setComment("Book", "书检测消息");
        conf.setComment("Sign", "告示牌检测消息");
        conf.setComment("Anvil", "铁砧重命名检测消息");
        conf.setComment("Name", "玩家名检测消息");
    }

    private PluginMessages() {
    }
}
