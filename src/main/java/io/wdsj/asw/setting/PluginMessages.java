package io.wdsj.asw.setting;

import ch.jalu.configme.Comment;
import ch.jalu.configme.SettingsHolder;
import ch.jalu.configme.configurationdata.CommentsConfiguration;
import ch.jalu.configme.properties.Property;

import static ch.jalu.configme.properties.PropertyInitializer.newProperty;

public class PluginMessages implements SettingsHolder {
    @Comment({"玩家发送敏感消息时候回复的假消息(只有玩家本人能看见)",
            "内置变量%integrated_player% %integrated_message% (支持PlaceHolderAPI)"
    })
    public static final Property<String> CHAT_FAKE_MESSAGE = newProperty("Chat.fakeMessage", "<%integrated_player%> %integrated_message%");
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
    @Comment("插件重载消息")
    public static final Property<String> MESSAGE_ON_COMMAND_RELOAD = newProperty("Plugin.messageOnCommandReload", "&aAdvancedSensitiveWords has been reloaded.");
    @Comment("插件帮助菜单")
    public static final Property<String> MESSAGE_ON_COMMAND_HELP = newProperty("Plugin.messageOnCommandHelp", "&bAdvancedSensitiveWords&r---&b帮助菜单\n   &7/asw reload&7: &a重新加载过滤词库和插件配置\n   &7/asw status&7: &a显示插件状态菜单\n   &7/asw test <待测消息>: &a运行敏感词测试\n   &7/asw help&7: &a显示帮助信息");
    @Comment("插件状态菜单")
    public static final Property<String> MESSAGE_ON_COMMAND_STATUS = newProperty("Plugin.messageOnCommandStatus", "&bAdvancedSensitiveWords&r---&b插件状态(%VERSION%)(MC %MC_VERSION%)\n   &7系统信息: &b%PLATFORM% %BIT% (Java %JAVA_VERSION% -- %JAVA_VENDOR%)\n   &7初始化: %INIT%\n   &7API状态: %API_STATUS%\n   &7当前模式: %MODE%\n   &7已过滤消息数: &a%NUM%\n   &7近20次处理平均耗时: %MS%");
    @Comment("敏感词测试返回")
    public static final Property<String> MESSAGE_ON_COMMAND_TEST = newProperty("Plugin.commandTest.testResultTrue", "&b一眼丁真, 鉴定为敏感词(鉴定报告)\n   &7原消息: &c%ORIGINAL_MSG%\n   &7过滤后消息: &a%PROCESSED_MSG%\n   &7敏感词列表: &b%CENSORED_LIST%");
    @Comment("敏感词测试通过")
    public static final Property<String> MESSAGE_ON_COMMAND_TEST_PASS = newProperty("Plugin.commandTest.testResultPass", "&a待测消息中没有敏感词喵~");
    @Comment("敏感词测试参数不足")
    public static final Property<String> MESSAGE_ON_COMMAND_TEST_NOT_ENOUGH = newProperty("Plugin.commandTest.testArgNotEnough", "&c参数不足, 请使用 &7/asw test <待测消息>");
    @Comment("敏感词测试未初始化")
    public static final Property<String> MESSAGE_ON_COMMAND_TEST_NOT_INIT = newProperty("Plugin.commandTest.testNotInit", "&c插件还没有初始化完毕喵");
    @Comment("没有权限执行该指令")
    public static final Property<String> NO_PERMISSION = newProperty("Plugin.noPermission", "&c你没有权限执行该指令.");

    @Override
    public void registerComments(CommentsConfiguration conf) {
        conf.setComment("", "AdvancedSensitiveWords 插件消息配置");
        conf.setComment("Plugin", "插件消息");
        conf.setComment("Plugin.commandTest", "敏感词测试消息(不计入已过滤消息)");
        conf.setComment("Chat", "聊天检测消息");
        conf.setComment("Book", "书检测消息");
        conf.setComment("Sign", "告示牌检测消息");
        conf.setComment("Anvil", "铁砧重命名检测消息");
        conf.setComment("Name", "玩家名检测消息");
    }

    private PluginMessages() {
    }
}
