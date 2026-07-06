package io.wdsj.asw.bukkit.manage.playergroup;

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

    public static PlayerActivitySnapshot withScore(PlayerActivitySnapshot snapshot, double score) {
        return new PlayerActivitySnapshot(
                score,
                snapshot.playTimeHours(),
                snapshot.minedBlocks(),
                snapshot.movedBlocks(),
                snapshot.mobKills(),
                snapshot.usedItems(),
                snapshot.brokenItems(),
                snapshot.craftedItems(),
                snapshot.damageDealt(),
                snapshot.damageTaken(),
                snapshot.deaths(),
                snapshot.enchantedItems(),
                snapshot.fishCaught(),
                snapshot.villagerTrades()
        );
    }

    public PlayerActivitySnapshot merge(PlayerActivitySnapshot other) {
        return new PlayerActivitySnapshot(
                score + other.score(),
                playTimeHours + other.playTimeHours(),
                minedBlocks + other.minedBlocks(),
                movedBlocks + other.movedBlocks(),
                mobKills + other.mobKills(),
                usedItems + other.usedItems(),
                brokenItems + other.brokenItems(),
                craftedItems + other.craftedItems(),
                damageDealt + other.damageDealt(),
                damageTaken + other.damageTaken(),
                deaths + other.deaths(),
                enchantedItems + other.enchantedItems(),
                fishCaught + other.fishCaught(),
                villagerTrades + other.villagerTrades()
        );
    }
}
