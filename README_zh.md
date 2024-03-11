# AdvancedSensitiveWords(高级敏感词)
您Minecraft服务器的一站式终极反脏话/敏感词解决方案!

[![Available on SpigotMC](https://img.shields.io/badge/Available%20on%20SpigotMC-orange?style=for-the-badge&logo=SpigotMC&logoColor=FFFFFF)](https://www.spigotmc.org/resources/advancedsensitivewords.115484/)
![Available on GitHub](https://img.shields.io/badge/Available%20on%20GitHub-black?style=for-the-badge&logo=GitHub&logoColor=FFFFFF)
[![Available on Modrinth](https://img.shields.io/badge/Available%20on%20Modrinth-darkgreen?style=for-the-badge&logo=Modrinth&logoColor=FFFFFF)](https://modrinth.com/plugin/advancedsensitivewords)

[English](https://github.com/hahawth/AdvancedSensitiveWords)
[简体中文](https://github.com/hahawth/AdvancedSensitiveWords/blob/main/README_zh.md)
<p align="center">
  <img src="logo.webp" alt="logo" width="128" height="128"/>
</p>

Logo 由 GPT-4 生成

[![CodeFactor](https://www.codefactor.io/repository/github/hahawth/advancedsensitivewords/badge)](https://www.codefactor.io/repository/github/hahawth/advancedsensitivewords)
[![Made with Java](https://img.shields.io/badge/Made%20with-Java-blue.svg)](https://www.java.com/)
[![Love from Earth](https://img.shields.io/badge/Love%20%E2%9D%A4%EF%B8%8F-red.svg?v=202007241736)](https://github.com/hahawth/AdvancedSensitiveWords/stargazers)
[![](https://jitpack.io/v/HaHaWTH/AdvancedSensitiveWords.svg)](https://jitpack.io/#HaHaWTH/AdvancedSensitiveWords)

[![](https://img.shields.io/github/downloads/HaHaWTH/AdvancedSensitiveWords/total?style=for-the-badge)](https://github.com/HaHaWTH/AdvancedSensitiveWords/releases) [![](https://img.shields.io/github/license/HaHaWTH/AdvancedSensitiveWords?style=for-the-badge)](https://github.com/HaHaWTH/AdvancedSensitiveWords/blob/master/LICENSE)![Visitors](https://api.visitorbadge.io/api/visitors?path=https%3A%2F%2Fgithub.com%2FHaHaWTH%2FAdvancedSensitiveWords&label=Repo%20Views&labelColor=%23d9e3f0&countColor=%232ccce4&labelStyle=upper)

## 功能
1. 使用DFA(确定性有穷自动机) 算法
2. 预配置 简洁明了,开箱即用
3. 高质量的超大默认敏感词库 (60000+ 敏感词)
4. 运行在数据包层, 不会干扰其他聊天插件 (在2c2g的服务器上能做到3.2w qps)
5. 高度自定义的配置
6. 支持告示牌检测
7. 支持铁砧检测
8. 支持对书的检测
9. 支持玩家名检测
10. **聊天上下文检测✨**
11. **告示牌跨行检测**
12. **缓存书内容, 提升效率**
13. 基岩版支持
14. 兼容主流登录插件 (AuthMe, CatSeedLogin 等)
15. 支持检测Emoji等其他Unicode字符
16. 中文支持
17. 基于自定义数据结构的高速处理
18. 支持加载自定义在线词库 ([我们的在线词库](https://github.com/HaHaWTH/ASW-OnlineWordList))
19. Folia兼容
20. **假消息支持(灵感来自 [Bilibili 阿瓦隆系统](https://github.com/freedom-introvert/Research-on-Avalon-System-in-Bilibili-Comment-Area))**

**我们的目标: 干掉ChatSentry!(迫真)**

## 指令

`/asw help` - 显示帮助

`/asw reload` - 重载配置和词库

`/asw status` - 显示当前插件状态

`/asw test <文本>` - 测试插件检测效果

## 权限

`advancedsensitivewords.bypass` - 绕过检测

`advancedsensitivewords.reload` - 允许使用重载配置和词库命令 (op默认)

`advancedsensitivewords.status` - 允许使用status命令 (op默认)

`advancedsensitivewords.test` - 允许使用test命令 (op默认)

**更多详细信息，请前往[Wiki](https://github.com/HaHaWTH/AdvancedSensitiveWords/wiki)进行查看**

## 统计数据
[![](https://img.shields.io/bstats/servers/20661?label=Spigot%20Servers&style=for-the-badge)](https://bstats.org/plugin/bukkit/AdvancedSensitiveWords/20661)

[![](https://img.shields.io/bstats/players/20661?label=Online%20Players&style=for-the-badge)](https://bstats.org/plugin/bukkit/AdvancedSensitiveWords/20661)

## 开发者
AdvancedSensitiveWords 提供了一个 [简单的事件](./src/main/java/io/wdsj/asw/event/ASWFilterEvent.java) 便于开发者进行扩展.

你只需要引入下面的依赖就可以编写扩展插件辣!
```xml
<repository>
    <id>jitpack.io</id>
    <url>https://jitpack.io</url>
</repository>
```

```xml
<dependency>
    <groupId>com.github.HaHaWTH</groupId>
    <artifactId>AdvancedSensitiveWords</artifactId>
    <version>LATEST</version>
    <scope>provided</scope>
</dependency>
```

## 赞助
如果您喜欢这个项目, 请我喝杯迎宾酒罢(喜)!

[爱发电](https://afdian.net/a/114514woxiuyuan)
