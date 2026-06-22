# AdvancedSensitiveWords

AdvancedSensitiveWords is a Paper moderation plugin built around DFA word matching, event-based content inspection, modular violation levels, and optional LLM-assisted chat review.

> **2.x is a breaking release.** It targets Paper only, uses Gradle with Kotlin DSL, and generates a new kebab-case configuration model. Do not reuse pre-2.0 configuration files without reviewing every setting.

## Requirements

| Component | Requirement                                            |
| --- |--------------------------------------------------------|
| Server | Paper 1.21.11                                          |
| Java | Java 21                                                |
| Proxy (optional) | Velocity with the matching Velocity jar             |
| Optional integrations | TrChat, PacketEvents, PlaceholderAPI, floodgate, AuthMe |

Spigot, CraftBukkit, and BungeeCord are not supported since version 2.x.

## Installation

1. Download the Paper artifact from the release, or build `paper:shadowJar` locally.
2. Put the Paper jar in the server `plugins/` directory and start Paper once.
3. Configure `plugins/AdvancedSensitiveWords/config.yml` and the generated `messages_en.yml` or `messages_zhcn.yml`.
4. Run `/asw reload all` after changing dictionaries or `/asw reload config` after changing only configuration.

For Velocity notifications and proxy commands, install the Velocity artifact on the proxy, set `plugin.hook-velocity: true` on Paper, then restart both sides. The proxy module does not filter chat by itself.

## What 2.x Filters

- **Chat and commands**: Paper `AsyncChatEvent`, command preprocessing, cross-message chat context, configurable replacement or cancellation, fake messages on cancellation, and TrChat fake-message/shadowban compatibility.
- **Books**: event-based writable-book checking, optional cross-page checking in cancel mode, and a bounded cache for processed book content.
- **Signs**: per-line, multi-line, and recent-sign context checking. In cancel mode, optional PacketEvents fake view can show the author their attempted text while other players retain the real, clean sign view.
- **Anvils and items**: rename-result filtering plus item display-name and lore filtering using Adventure components.
- **Player names and broadcasts**: login rejection for blocked names and optional broadcast filtering.
- **Optional LLM chat review**: asynchronous, cost-gated review for messages that did not match DFA or chat context. It is disabled by default and never retracts chat; it can notify, record, increment the separate AI VL, and run configured actions after a validated response.

## Quick Configuration

Configuration is generated in lower kebab-case. The generated file is the authoritative list of defaults and inline comments.

```yaml
plugin:
  language: en
  enable-chat-check: true
  enable-sign-edit-check: true

chat:
  method: CANCEL # REPLACE or CANCEL
  fake-message-on-cancel: false
  context-check: true
```

`REPLACE` changes matched text with the configured replacement. `CANCEL` rejects the affected interaction. Fake chat messages and sign fake view are cancellation-only features.

### Command Argument Rules

`chat.command-white-list` also defines which command arguments are inspected. With the default `invert-command-white-list: true`, listed command paths are inspected and commands outside the list are skipped.

```yaml
chat:
  invert-command-white-list: true
  command-white-list:
    - "[default:include] /msg [ignore:1]"
    - "[default:include] /bc [ignore:1,-1]"
    - "[default:ignore] /mail send [include:2..]"
```

Arguments are numbered after the command path, starting at `1`. `-1` is the final argument; `2..` means the second argument through the end. `include` and `ignore` directives are processed in order. Ignored arguments split detection segments, so a blocked word cannot match across a skipped player name, server name, count, or other parameter.

### Punishments and Violation Levels

Each filter module has its own `punishment` list and its own VL: `CHAT`, `AI`, `BOOK`, `SIGN`, `ANVIL`, and `ITEM`. Commands share the `CHAT` VL. The manual default list at `plugin.manual-punishment` is the exception: its `VL` conditions use the total across modules.

```yaml
chat:
  punishment:
    - "COMMAND|kick %player% Blocked content|VL>2"
    - "SHADOW|60|VL>5"
```

Supported action types are `COMMAND`, `COMMAND_PROXY`, `DAMAGE`, `HOSTILE`, `EFFECT`, and `SHADOW`. Use `%player%` or `%PLAYER%` in command actions. Empty lists keep detection, logging, notification, and VL counting active while disabling automatic actions.

## Optional LLM Moderation

Enable LLM review only after configuring a compatible provider and API key. Paper loads LangChain4j libraries through `plugin.yml`, so the server needs network access or an existing Paper library cache during the first startup.

```yaml
ai:
  enabled: true
  base-url: https://api.deepseek.com
  api-mode: CHAT_COMPLETIONS # CHAT_COMPLETIONS, RESPONSES, or ANTHROPIC_MESSAGES
  api-key-environment: DEEPSEEK_API_KEY
  model-name: deepseek-v4-flash
```

Before a request, ASW requires that direct DFA and chat-context checks miss, then applies message-length, entropy, per-player cooldown, in-flight, concurrency, and queue limits. LLM output is strictly parsed locally before any follow-up action.

Each category has independent notification and punishment confidence thresholds and actions under `ai.category-policy`. LLM requests and responses are audited in `plugins/AdvancedSensitiveWords/llm-history/`. Treat this directory as sensitive operational data.

`ai.server-context-can-override` is a server-owner policy switch. When enabled, `ai.server-context` is inserted into the trusted system policy and is intentionally omitted from the user JSON payload. Keep policy text administrator-controlled; never place player input, credentials, or private data there.

## Commands

| Command | Purpose |
| --- | --- |
| `/asw help [query]` | Show command help. |
| `/asw status` | Show general plugin status. |
| `/asw ai status` | Show LLM runtime counters, queue state, model, API mode, and category policies. |
| `/asw reload [all\|config]` | Reload configuration, or configuration plus dictionaries. |
| `/asw test <text...>` | Test text against the DFA filter. |
| `/asw word add/remove <word> [word...]` | Mutate the blocked-word list for the current runtime only. |
| `/asw allow add/remove <word> [word...]` | Mutate the allowed-word list for the current runtime only. |
| `/asw player info <online-player>` | Show per-module and total VL. |
| `/asw player reset <online-player> [module]` | Reset all or one module VL. |
| `/asw player punish <online-player> [method...]` | Execute configured manual punishment or one supplied action. |
| `/asw teleport <world-id> <x> <y> <z>` | Teleport a staff member to a reported location. |

`/asw` and `/advancedsensitivewords` are equivalent. Runtime word-list mutations are discarded on a full dictionary reload or server restart.

## Permissions

| Permission | Default | Purpose |
| --- | --- | --- |
| `advancedsensitivewords.bypass` | false | Bypass filtering. |
| `advancedsensitivewords.notice` | op | Receive staff notifications. |
| `advancedsensitivewords.update` | op | Receive update notices. |
| `advancedsensitivewords.command.*` | false | Parent node for all management commands. |
| `advancedsensitivewords.command.help` | op | Use help. |
| `advancedsensitivewords.command.status` | op | Use general status. |
| `advancedsensitivewords.command.ai.status` | op | Use AI status. |
| `advancedsensitivewords.command.reload.all` | op | Reload configuration and dictionaries. |
| `advancedsensitivewords.command.reload.config` | op | Reload configuration only. |
| `advancedsensitivewords.command.test` | op | Use DFA test. |
| `advancedsensitivewords.command.word.add` | op | Add blocked words at runtime. |
| `advancedsensitivewords.command.word.remove` | op | Remove blocked words at runtime. |
| `advancedsensitivewords.command.allow.add` | op | Add allowed words at runtime. |
| `advancedsensitivewords.command.allow.remove` | op | Remove allowed words at runtime. |
| `advancedsensitivewords.command.player.info` | op | Inspect player VL. |
| `advancedsensitivewords.command.player.reset` | op | Reset player VL. |
| `advancedsensitivewords.command.player.punish` | op | Apply manual punishment. |

## Integrations and API

- **TrChat**: compatibility for fake chat messages and shadowban display. TrChat remains responsible for its own formatting pipeline.
- **PacketEvents**: optional and soft-required only for sign fake view. Without it, sign cancel behavior remains active without a fake view.
- **PlaceholderAPI**: enable `plugin.enable-placeholder` to expose `%asw_version%`, `%asw_total_filtered%`, `%asw_is_shadow%`, and `%asw_violation_count%`.
- **Floodgate / AuthMe**: optional Bedrock-name and authentication-state handling.

Other Paper plugins can access the shadowban API without depending on implementation classes:

```java
import io.wdsj.asw.bukkit.api.AdvancedSensitiveWordsApi;
import java.time.Duration;

AdvancedSensitiveWordsApi.shadowBan().shadow(player, Duration.ofMinutes(5));
```

`AsyncModerationResponseEvent` is fired asynchronously after an LLM response. Event handlers may observe, cancel ASW follow-up, or replace the validated result, but must schedule Bukkit entity/world work themselves.

## Building From Source

```powershell
.\gradlew.bat --no-daemon build
```

## License

AdvancedSensitiveWords is licensed under the [GNU AGPL-3.0](LICENSE).

## Links

- [Online word list](https://github.com/HaHaWTH/ASW-OnlineWordList)
- [bStats](https://bstats.org/plugin/bukkit/AdvancedSensitiveWords/20661)
