package io.wdsj.asw.bukkit.playergroup;

public record PlayerActivitySnapshot(
        double score,
        double playTimeHours,
        long minedBlocks,
        double movedBlocks,
        long mobKills,
        long usedItems,
        long brokenItems,
        long craftedItems,
        long damageDealt,
        long damageTaken,
        long deaths,
        long enchantedItems,
        long fishCaught,
        long villagerTrades
) {
    public static PlayerActivitySnapshot empty() {
        return new PlayerActivitySnapshot(0.0D, 0.0D, 0L, 0.0D, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L);
    }
}
