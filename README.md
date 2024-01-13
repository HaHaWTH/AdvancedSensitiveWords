# AdvancedSensitiveWords
A one-stop-shop ultimate anti-swear solution for your Minecraft server!
(Compatible with spigot 1.8.8~1.20.4)
<p align="center">
  <img src="icon.webp" alt="logo" width="128" height="128"/>
</p>

[![CodeFactor](https://www.codefactor.io/repository/github/hahawth/advancedsensitivewords/badge)](https://www.codefactor.io/repository/github/hahawth/advancedsensitivewords)

## Features
1. Using DFA(Deterministic Finite Automata) algorithm
2. Plug-and-play
3. Huge and high-quality default dictionary(Over 60,000 words)
4. Blazing fast by using packets
5. Nice compatibility with chat plugins
6. Full-customizable
7. Books Anvils Signs support

## Commands

`/asw help` - Show help message

`/asw reload` - Re-initialize the DFA dict and reload configurations

## Permissions

`advancedsensitivewords.bypass` - Bypass the AdvancedSensitiveWords message filter

`advancedsensitivewords.reload` - Allows you to use the /asw reload command

## Statistics
![Graph](https://bstats.org/signatures/bukkit/AdvancedSensitiveWords.svg)

## For developers
AdvancedSensitiveWords offers a [simple event](./src/main/java/io/wdsj/asw/event/ASWFilterEvent.java) for developers 
to use this, you need to import the following dependency in your pom.xml:
```
        <dependency>
            <groupId>io.wdsj</groupId>
            <artifactId>asw</artifactId>
            <version>LATEST</version>
            <scope>system</scope>
            <systemPath>YOUR_PATH_TO_JAR</systemPath>
        </dependency>
```


