package com.dooji.unslab;

import net.minecraft.block.Block;
import net.minecraft.block.SlabBlock;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class UnslabMapping {
    private static final Map<Block, Block> slabToBlockMap = new HashMap<>();
    private static final Set<String> namespaces = Registries.BLOCK.getIds().stream()
            .map(Identifier::getNamespace)
            .collect(Collectors.toSet());

    public static void initialize() {
        for (Block block : Registries.BLOCK) {
            if (block instanceof SlabBlock slabBlock) {
                Identifier slabId = Registries.BLOCK.getId(block);

                Block fullBlock = mapMinecraftWoodSlabs(slabId);

                if (fullBlock == null) {
                    String baseName = removeSlabSuffix(slabId);

                    fullBlock = searchAllNamespacesForExactBlock(baseName);

                    if (fullBlock == null) {
                        fullBlock = tryPluralizationAcrossNamespaces(baseName);
                    }

                    if (fullBlock == null) {
                        fullBlock = searchAllNamespacesForExactBlock(baseName + "_block");
                    }
                }

                if (fullBlock != null && isValidBlock(fullBlock)) {
                    addMapping(slabBlock, fullBlock);
                } else {
                    Unslab.LOGGER.warn("[Unslab] No matching full block found for slab: {}", slabId);
                }
            }
        }

        Unslab.LOGGER.info("[Unslab] Finished mapping slabs to blocks. Total mappings: {}", slabToBlockMap.size());
    }

    private static Block mapMinecraftWoodSlabs(Identifier slabId) {
        if (!"minecraft".equals(slabId.getNamespace())) {
            return null;
        }

        String path = slabId.getPath();
        String planksName = path.replace("_slab", "_planks");

        return switch (path) {
            case "oak_slab", "spruce_slab", "birch_slab", "jungle_slab", "acacia_slab", "dark_oak_slab",
                 "mangrove_slab", "cherry_slab", "crimson_slab", "warped_slab" ->
                    Registries.BLOCK.get(Identifier.of("minecraft", planksName));
            default -> null;
        };
    }

    private static String removeSlabSuffix(Identifier slabId) {
        String slabName = slabId.getPath();

        if (slabName.endsWith("_slab")) {
            return slabName.substring(0, slabName.length() - "_slab".length());
        }

        return slabName;
    }

    private static Block searchAllNamespacesForExactBlock(String name) {
        for (String namespace : namespaces) {
            Block block = Registries.BLOCK.get(Identifier.of(namespace, name));

            if (isValidBlock(block)) {
                return block;
            }
        }

        return null;
    }

    private static Block tryPluralizationAcrossNamespaces(String baseName) {
        String[] components = baseName.split("_");

        for (int i = 0; i < components.length; i++) {
            String[] modifiedComponents = components.clone();
            modifiedComponents[i] = components[i] + "s";

            String pluralizedName = String.join("_", modifiedComponents);
            Block block = searchAllNamespacesForExactBlock(pluralizedName);

            if (block != null) {
                return block;
            }
        }

        for (int i = 0; i < components.length; i++) {
            for (int j = i + 1; j < components.length; j++) {
                String[] modifiedComponents = components.clone();
                modifiedComponents[i] = components[i] + "s";
                modifiedComponents[j] = components[j] + "s";

                String pluralizedName = String.join("_", modifiedComponents);
                Block block = searchAllNamespacesForExactBlock(pluralizedName);

                if (block != null) {
                    return block;
                }
            }
        }

        return null;
    }

    private static void addMapping(SlabBlock slabBlock, Block fullBlock) {
        slabToBlockMap.put(slabBlock, fullBlock);
    }

    private static boolean isValidBlock(Block block) {
        Identifier blockId = Registries.BLOCK.getId(block);

        return !blockId.getPath().equals("air");
    }

    public static Map<Block, Block> getSlabToBlockMap() {
        return slabToBlockMap;
    }
}