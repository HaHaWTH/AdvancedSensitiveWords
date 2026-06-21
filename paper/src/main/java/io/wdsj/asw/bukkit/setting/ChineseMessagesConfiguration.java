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
        plugin.messageOnViolationReset = "<gradient:#22d3ee:#4ade80><bold>ASW</bold></gradient> <dark_gray>| <green>已重置所有玩家的违规次数。";
        plugin.messageOnCommandStatus = """
                <gradient:#22d3ee:#4ade80><bold>AdvancedSensitiveWords</bold></gradient> <dark_gray>| <aqua>插件状态
                <dark_gray>  版本 <aqua>%version% <dark_gray>| <gray>MC %mc_version%
                <dark_gray>  系统 <gray>%platform% %bit% <dark_gray>| <gray>Java %java_version% (%java_vendor%)
                <dark_gray>  初始化 <aqua>%init% <dark_gray>| <gray>模式 <aqua>%mode%
                <dark_gray>  已过滤 <aqua>%num% <dark_gray>| <gray>平均耗时 <aqua>%ms%""";
        plugin.messageOnAiStatus = """
                <gradient:#22d3ee:#4ade80><bold>AdvancedSensitiveWords</bold></gradient> <dark_gray>| <aqua>AI 检测状态
                <dark_gray>  已启用 <aqua>%enabled% <dark_gray>| <gray>模型 <aqua>%model% <dark_gray>| <gray>接口 <aqua>%api_mode%
                <dark_gray>  已提交 <aqua>%submitted% <dark_gray>| <gray>丢弃 <aqua>%dropped% <dark_gray>| <gray>失败 <aqua>%failed%
                <dark_gray>  无效 <aqua>%invalid% <dark_gray>| <gray>已执行 <aqua>%enforced%
                <dark_gray>  队列 <aqua>%active% 活跃 <gray>/ <aqua>%queued% 排队 <gray>/ <aqua>%pool_size% 线程
                <dark_gray>  通知/处罚阈值 <aqua>%thresholds%""";
        plugin.commandTest.testResultTrue = """
                <gradient:#22d3ee:#4ade80><bold>鉴定报告</bold></gradient>
                <dark_gray>  原消息 <red>%original_msg%
                <dark_gray>  过滤后 <green>%processed_msg%
                <dark_gray>  匹配项 <aqua>%censored_list%""";
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
        plugin.invalidViolationModule = "<red>未知违规模块，请选择 chat、ai、book、sign、anvil 或 item。";
        plugin.noSuchPlayer = "<red>找不到对应玩家.";
        plugin.noticeOperator = "<gradient:#22d3ee:#4ade80><bold>ASWNotify</bold></gradient> <dark_gray>| <gray>玩家 <red>%player% <gray>触发 <aqua>%type% <gray>检测 <dark_gray>| <gray>VL <aqua>%violation%\n<dark_gray>  原消息 <white>%message%\n<dark_gray>  匹配项 <aqua>%censored_list%";
        plugin.noticeOperatorProxy = "<gradient:#22d3ee:#4ade80><bold>ASWNotify</bold></gradient> <dark_gray>| <gray>玩家 <red>%player% <gray>在 <aqua>%server_name% <gray>触发 <aqua>%type% <gray>检测 <dark_gray>| <gray>VL <aqua>%violation%\n<dark_gray>  原消息 <white>%message%\n<dark_gray>  匹配项 <aqua>%censored_list%";
        plugin.aiObservation = "<gradient:#22d3ee:#4ade80><bold>ASWObserve</bold></gradient> <dark_gray>| <gray>玩家 <red>%player% <gray>被 AI 分类为 <aqua>%category% <dark_gray>| <gray>置信度 <aqua>%confidence%\n<dark_gray>  原消息 <white>%message%";
        plugin.aiObservationProxy = "<gradient:#22d3ee:#4ade80><bold>ASWObserve</bold></gradient> <dark_gray>| <gray>玩家 <red>%player% <gray>在 <aqua>%server_name% <gray>被 AI 分类为 <aqua>%category% <dark_gray>| <gray>置信度 <aqua>%confidence%\n<dark_gray>  原消息 <white>%message%";
        plugin.updateAvailable = "<gradient:#22d3ee:#4ade80><bold>ASWNotify</bold></gradient> <dark_gray>| <yellow>发现可用更新 <dark_gray>| <gray>最新 <aqua>%latest_version% <dark_gray>| <gray>当前 <aqua>%current_version%";
        plugin.messageOnCommandInfo = """
                <gradient:#22d3ee:#4ade80><bold>AdvancedSensitiveWords</bold></gradient> <dark_gray>| <aqua>玩家违规信息
                <dark_gray>  玩家 <aqua>%player%
                <dark_gray>  聊天 <aqua>%chat_violation% <dark_gray>| <gray>AI <aqua>%ai_violation% <dark_gray>| <gray>书本 <aqua>%book_violation%
                <dark_gray>  告示牌 <aqua>%sign_violation% <dark_gray>| <gray>铁砧 <aqua>%anvil_violation% <dark_gray>| <gray>物品 <aqua>%item_violation%
                <dark_gray>  总 VL <gradient:#fbbf24:#fb7185><bold>%violation%</bold></gradient>""";
        plugin.messageOnCommandReset = "<gradient:#22d3ee:#4ade80><bold>ASW</bold></gradient> <dark_gray>| <green>已重置玩家 <aqua>%player% <green>的 <aqua>%module% <green>VL。";
    }
}
