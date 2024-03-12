# AdvancedSensitiveWords
If you trigger the sensitive word test you'll be penalised.
One-stop-shop **ultimate** anti-swear solution for your Minecraft server!

[![Available on SpigotMC](https://img.shields.io/badge/Available%20on%20SpigotMC-orange?style=for-the-badge&logo=SpigotMC&logoColor=FFFFFF)](https://www.spigotmc.org/resources/advancedsensitivewords.115484/)
![Available on GitHub](https://img.shields.io/badge/Available%20on%20GitHub-black?style=for-the-badge&logo=GitHub&logoColor=FFFFFF)
[![Available on Modrinth](https://img.shields.io/badge/Available%20on%20Modrinth-darkgreen?style=for-the-badge&logo=Modrinth&logoColor=FFFFFF)](https://modrinth.com/plugin/advancedsensitivewords)

[English](https://github.com/hahawth/AdvancedSensitiveWords)
[简体中文](https://github.com/hahawth/AdvancedSensitiveWords/blob/main/README_zh.md)
<p align="center">
  <img src="logo.webp" alt="logo" width="128" height="128"/>
</p>

Logo designed by GPT-4

[![CodeFactor](https://www.codefactor.io/repository/github/hahawth/advancedsensitivewords/badge)](https://www.codefactor.io/repository/github/hahawth/advancedsensitivewords)
[![Made with Java](https://img.shields.io/badge/Made%20with-Java-blue.svg)](https://www.java.com/)
[![Love from Earth](https://img.shields.io/badge/Love%20%E2%9D%A4%EF%B8%8F-red.svg?v=202007241736)](https://github.com/hahawth/AdvancedSensitiveWords/stargazers)
[![](https://jitpack.io/v/HaHaWTH/AdvancedSensitiveWords.svg)](https://jitpack.io/#HaHaWTH/AdvancedSensitiveWords)

[![](https://img.shields.io/github/downloads/HaHaWTH/AdvancedSensitiveWords/total?style=for-the-badge)](https://github.com/HaHaWTH/AdvancedSensitiveWords/releases) [![](https://img.shields.io/github/license/HaHaWTH/AdvancedSensitiveWords?style=for-the-badge)](https://github.com/HaHaWTH/AdvancedSensitiveWords/blob/master/LICENSE) ![Visitors](https://api.visitorbadge.io/api/visitors?path=https%3A%2F%2Fgithub.com%2FHaHaWTH%2FAdvancedSensitiveWords&label=Repo%20Views&labelColor=%23d9e3f0&countColor=%232ccce4&labelStyle=upper)

## Features
1. Using DFA(Deterministic Finite Automata) algorithm
2. Plug-and-play
3. Huge and high-quality default dictionary (Over 60,000 words)
4. Blazing fast by using packets (32,000+ queries per second on a 2-core server)
5. 100% compatibility with chat plugins (Tested over 30+ plugins)
6. Full-customizable
7. Sign check support
8. Anvil check support
9. Book check support
10. Player name check support
11. **Chat context check**
12. **Sign multiple lines check**
13. **Book check with ignore lines support and cache**
14. Bedrock player compatibility
15. Compatibility with main stream login plugins (AuthMe, CatSeedLogin etc.)
16. Emoji and other unicode support
17. Chinese support
18. Fast processing depending on our custom data structure
19. Online sensitive word list support ([Repository here](https://github.com/HaHaWTH/ASW-OnlineWordList))
20. Folia supported
21. OP notifications on player swore
22. Custom punishments (Effect, command, hostile, etc.)
23. Fake message support (Inspired by [Bilibili Avalon System](https://github.com/freedom-introvert/Research-on-Avalon-System-in-Bilibili-Comment-Area))

**Features above make us unique in the anti-swear plugins!**

## Commands

`/asw help` - Show help message

`/asw reload` - Re-initialize the DFA dict and reload configurations

`/asw status` - Show the status of the AdvancedSensitiveWords

`/asw test <text>` - Test the AdvancedSensitiveWords filter with given text

## Permissions

`advancedsensitivewords.bypass` - Bypass the AdvancedSensitiveWords message filter

`advancedsensitivewords.reload` - Allows you to use the /asw reload command

`advancedsensitivewords.status` - Allows you to use the /asw status command

`advancedsensitivewords.test` - Allows you to use the /asw test command

`advancedsensitivewords.help` - Allows you to use the /asw help command

`advancedsensitivewords.notice` - Retrieve the notification when players swore

**For more info, please visit [our Wiki](https://github.com/HaHaWTH/AdvancedSensitiveWords/wiki)**

## Statistics
[![](https://img.shields.io/bstats/servers/20661?label=Spigot%20Servers&style=for-the-badge)](https://bstats.org/plugin/bukkit/AdvancedSensitiveWords/20661)

[![](https://img.shields.io/bstats/players/20661?label=Online%20Players&style=for-the-badge)](https://bstats.org/plugin/bukkit/AdvancedSensitiveWords/20661)

## For developers
AdvancedSensitiveWords offers a [simple event](./src/main/java/io/wdsj/asw/event/ASWFilterEvent.java) for developers.

To use this, you just need to import the plugin binary file as a dependency in your pom.xml
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

## Sponsor
If you like this project, you can sponsor me on [Afdian](https://afdian.net/a/114514woxiuyuan).
