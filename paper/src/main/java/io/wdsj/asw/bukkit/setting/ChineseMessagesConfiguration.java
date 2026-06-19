package io.wdsj.asw.bukkit.setting;

import de.exlll.configlib.Configuration;

@Configuration
public final class ChineseMessagesConfiguration extends MessagesConfiguration {
    public ChineseMessagesConfiguration() {
        chat.messageOnChat = "<red>请勿在聊天中发送敏感词汇.";
        sign.messageOnSign = "<red>请勿在告示牌中写入敏感词汇.";
        anvil.messageOnAnvilRename = "<red>请勿在铁砧中写入敏感词汇.";
        book.messageOnBook = "<red>请勿在书中写入敏感词汇.";
        name.messageOnName = "<red>您的用户名包含敏感词,请修改您的用户名或联系管理员.";
        item.messageOnItem = "<red>您的物品包含敏感词.";
        plugin.messageOnCommandReload = "<green>AdvancedSensitiveWords 已重新加载.";
        plugin.messageOnViolationReset = "<aqua>ASW<gray>Notify >> <green>已重置所有玩家的违规次数!";
        plugin.messageOnCommandStatus = """
                <aqua>AdvancedSensitiveWords<reset>---<aqua>插件状态(%version%)(MC %mc_version%)
                   <gray>系统信息: <aqua>%platform% %bit% (Java %java_version% -- %java_vendor%)
                   <gray>初始化: %init%
                   <gray>当前模式: %mode%
                   <gray>已过滤消息数: %num%
                   <gray>近20次处理平均耗时: %ms%""";
        plugin.commandTest.testResultTrue = """
                <aqua>一眼丁真, 鉴定为敏感词(鉴定报告)
                   <gray>原消息: <red>%original_msg%
                   <gray>过滤后消息: <green>%processed_msg%
                   <gray>敏感词列表: <aqua>%censored_list%""";
        plugin.commandTest.testResultPass = "<green>待测消息中没有敏感词喵~";
        plugin.commandTest.testNotInit = "<red>插件还没有初始化完毕喵";
        plugin.commandPunish.parseError = "<red>解析方法出错, 请检查指令格式.";
        plugin.commandPunish.success = "<green>成功惩罚玩家 %player%.";
        plugin.commandAdd.success = "<green>敏感词添加成功.";
        plugin.commandRemove.success = "<green>敏感词移除成功.";
        plugin.commandWord.runtimeOnly = "<yellow>通过命令修改的词库仅在本次运行期间生效，重载过滤器或重启服务器后将丢失。";
        plugin.noPermission = "<red>你没有权限执行该指令.";
        plugin.unknownCommand = "<red>未知命令, 请使用 <gray>/asw help";
        plugin.argsNotEnough = "<red>参数不足, 请使用 <gray>/asw help";
        plugin.noSuchPlayer = "<red>找不到对应玩家.";
        plugin.noticeOperator = "<aqua>ASW<gray>Notify >> <gray>玩家 <red>%player% <gray>触发了敏感词检测(%type%)(VL: %violation%)(原消息: %message%) 敏感词列表: <aqua>%censored_list%";
        plugin.noticeOperatorProxy = "<aqua>ASW<gray>Notify >> <gray>玩家 <red>%player% (服务器: %server_name%) <gray>触发了敏感词检测(%type%)(VL: %violation%)(原消息: %message%) 敏感词列表: <aqua>%censored_list%";
        plugin.updateAvailable = "<aqua>ASW<gray>Notify >> <gray>插件有可用更新(%latest_version%), 当前正在运行: <aqua>%current_version%.";
        plugin.messageOnCommandInfo = """
                <aqua>AdvancedSensitiveWords<reset>---<aqua>玩家信息
                    <gray>玩家名称: <aqua>%player%
                    <gray>违规次数: <aqua>%violation%""";
        plugin.messageOnCommandReset = "<green>已重置玩家 %player% 的违规次数.";
    }
}
