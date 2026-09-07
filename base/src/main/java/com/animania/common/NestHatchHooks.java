package com.animania.common;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Addon-owned nest hatching callbacks without introducing Base -> addon dependencies. */
public final class NestHatchHooks {
    @FunctionalInterface
    public interface Handler {
        /** @return true after an egg was hatched and consumed. */
        boolean tryHatch(ServerLevel level, BlockPos pos, AnimaniaBlocks.NestEntity nest, RandomSource random);
    }

    private static final Map<String, Handler> HANDLERS = new LinkedHashMap<>();

    private NestHatchHooks() {
    }

    public static synchronized void register(String addonId, Handler handler) {
        if (addonId == null || addonId.isBlank() || handler == null) return;
        HANDLERS.put(addonId, handler);
    }

    public static void tryHatch(ServerLevel level, BlockPos pos, AnimaniaBlocks.NestEntity nest,
                                RandomSource random) {
        final List<Handler> handlers;
        synchronized (NestHatchHooks.class) {
            handlers = List.copyOf(HANDLERS.values());
        }
        for (Handler handler : handlers) {
            if (handler.tryHatch(level, pos, nest, random)) return;
        }
    }
}
