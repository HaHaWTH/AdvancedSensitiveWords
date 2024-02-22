# AdvancedSensitiveWords
One-stop-shop **ultimate** anti-swear solution for your Minecraft server!
<p align="center">
  <img src="logo.webp" alt="logo" width="128" height="128"/>
</p>

[![CodeFactor](https://www.codefactor.io/repository/github/hahawth/advancedsensitivewords/badge)](https://www.codefactor.io/repository/github/hahawth/advancedsensitivewords)

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

**Features above make us unique in the anti-swear plugins!**

## Commands

`/asw help` - Show help message

`/asw reload` - Re-initialize the DFA dict and reload configurations

`/asw status` - Show the status of the AdvancedSensitiveWords

`/asw test <text>` - Test the AdvancedSensitiveWords filter with given text

## Permissions

`advancedsensitivewords.bypass` - Bypass the AdvancedSensitiveWords message filter

`advancedsensitivewords.reload` - Allows you to use the /asw reload command

`advancedsensitivewords.status` - Allows you to use the /asw status command`

`advancedsensitivewords.test` - Allows you to use the /asw test command`

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
    <groupId>io.wdsj</groupId>
    <artifactId>asw</artifactId>
    <version>LATEST</version>
    <scope>provided</scope>
</dependency>
```
