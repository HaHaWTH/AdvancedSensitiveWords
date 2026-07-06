package io.wdsj.asw.bukkit.manage.playergroup;

public record PlayerGroupStatus(
        PlayerGroup group,
        PlayerGroupSource source,
        PlayerActivitySnapshot activity,
        double threshold
) {
}
