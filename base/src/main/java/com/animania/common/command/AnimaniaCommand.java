package com.animania.common.command;

import com.animania.api.interfaces.IConvertable;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.RegisterCommandsEvent;

import java.util.ArrayList;
import java.util.Map;
import java.util.WeakHashMap;

/** Modern guarded implementation of the legacy {@code /animania} command. */
public final class AnimaniaCommand {
    private static final long CONFIRM_WINDOW_MILLIS = 20_000L;
    private static final Map<MinecraftServer, Long> CONFIRMATIONS = new WeakHashMap<>();

    private AnimaniaCommand() { }

    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("animania")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("tovanilla").executes(context -> execute(context.getSource()))));
    }

    private static int execute(CommandSourceStack source) {
        MinecraftServer server = source.getServer();
        long now = System.currentTimeMillis();
        Long expires = CONFIRMATIONS.get(server);
        if (expires == null || expires < now) {
            CONFIRMATIONS.put(server, now + CONFIRM_WINDOW_MILLIS);
            source.sendFailure(Component.translatable("commands.animania.tovanilla.warning"));
            return 0;
        }
        CONFIRMATIONS.remove(server);
        int converted = 0;
        for (ServerLevel level : server.getAllLevels()) {
            ArrayList<Entity> loaded = new ArrayList<>();
            for (Entity entity : level.getAllEntities()) loaded.add(entity);
            for (Entity entity : loaded) {
                if (!(entity instanceof IConvertable convertable)) continue;
                Entity replacement = convertable.convertToVanilla();
                if (replacement == null) continue;
                // Keep the source until Forge confirms that the replacement
                // entered the world. Events or UUID conflicts may reject it.
                if (replaceAfterSuccessfulSpawn(level, entity, replacement)) converted++;
            }
        }
        int convertedCount = converted;
        source.sendSuccess(() -> Component.translatable("commands.animania.tovanilla.success", convertedCount), true);
        return converted;
    }

    /** Transactional replacement primitive shared with the Forge regression test. */
    public static boolean replaceAfterSuccessfulSpawn(ServerLevel level, Entity source, Entity replacement) {
        if (level == null || source == null || replacement == null || !source.isAlive()) return false;
        if (!level.addFreshEntity(replacement)) return false;
        source.discard();
        return true;
    }
}
