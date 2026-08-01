package net.wkhan.naturesaura_plus.data;

import com.mojang.datafixers.util.Either;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.util.random.SimpleWeightedRandomList;
import net.minecraft.util.random.WeightedEntry;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Queue;

public final class OreSpawnRules {
    public record OreSpawnValues(Reference2IntOpenHashMap<Block> baseBlockAndAuraDrain, SimpleWeightedRandomList<Block> outputOres) {}
    public static final HashMap<Pair<ResourceKey<DimensionType>, ResourceKey<Biome>>, OreSpawnValues> ORE_SPAWNS = new HashMap<>();

    public static final Queue<OreSpawnRule> oreRulesQueue = new ArrayDeque<>();
    public static void addOreSpawn(OreSpawnRule rule) {
        Pair<ResourceKey<DimensionType>, ResourceKey<Biome>> dimensionBiomePair = ImmutablePair.of(rule.dimensionType(), rule.biome());
        Reference2IntOpenHashMap<Block> baseBlockAndAuraDrain = new Reference2IntOpenHashMap<>(rule.baseBlockAndAuraDrain().size());
        baseBlockAndAuraDrain.defaultReturnValue(0);
        SimpleWeightedRandomList.Builder<Block> outputOres = new SimpleWeightedRandomList.Builder<>();

        for (Either<Block, TagKey<Block>> eitherBorBT : rule.baseBlockAndAuraDrain().keySet()) {
            int auraDrain = rule.baseBlockAndAuraDrain().get(eitherBorBT);
            eitherBorBT.ifLeft(block -> baseBlockAndAuraDrain.put(block, auraDrain))
                    .ifRight(blockTagKey -> ForgeRegistries.BLOCKS.tags().getTag(blockTagKey)
                            .forEach(block -> baseBlockAndAuraDrain.put(block, auraDrain)));
        }

        for (WeightedEntry.Wrapper<Either<Block, TagKey<Block>>> eitherBorBT : rule.outputOres().unwrap()) {
            int weight = eitherBorBT.getWeight().asInt();
            eitherBorBT.getData().ifLeft(block -> outputOres.add(block, weight))
                    .ifRight(blockTagKey -> ForgeRegistries.BLOCKS.tags().getTag(blockTagKey)
                                .forEach(block -> outputOres.add(block, weight)));
        }

        OreSpawnValues oreSpawnValues = new OreSpawnValues(baseBlockAndAuraDrain, outputOres.build());

        ORE_SPAWNS.merge(dimensionBiomePair, oreSpawnValues, (existing, incoming) -> {
            Reference2IntOpenHashMap<Block> newBaseBlockAndAuraDrain = new Reference2IntOpenHashMap<>(existing.baseBlockAndAuraDrain());
            newBaseBlockAndAuraDrain.putAll(incoming.baseBlockAndAuraDrain());

            SimpleWeightedRandomList.Builder<Block> newOutputOres = new SimpleWeightedRandomList.Builder<>();
            existing.outputOres().unwrap().forEach(entry -> newOutputOres.add(entry.getData(), entry.getWeight().asInt()));
            incoming.outputOres().unwrap().forEach(entry -> newOutputOres.add(entry.getData(), entry.getWeight().asInt()));
            return new OreSpawnValues(newBaseBlockAndAuraDrain, newOutputOres.build());
        });
    }
}
