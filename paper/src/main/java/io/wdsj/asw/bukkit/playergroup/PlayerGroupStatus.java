package io.wdsj.asw.bukkit.playergroup;

public record PlayerGroupStatus(
        PlayerGroup group,
        PlayerGroupSource source,
        PlayerActivitySnapshot activity,
        double threshold
) {
}
