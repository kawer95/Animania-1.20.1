package com.animania.network;

import com.animania.Animania;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.concurrent.atomic.AtomicBoolean;

/** Minimal validated channel reserved for future client hints; authoritative state uses entity data. */
public final class AnimaniaNetwork {
    private static final String PROTOCOL = "1";
    private static final AtomicBoolean REGISTERED = new AtomicBoolean();
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(Animania.MOD_ID, "main"), () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    private AnimaniaNetwork() {
    }

    public static void register() {
        if (REGISTERED.compareAndSet(false, true)) {
            CHANNEL.registerMessage(0, RequestAnimalSnapshotPacket.class, RequestAnimalSnapshotPacket::encode,
                    RequestAnimalSnapshotPacket::decode, RequestAnimalSnapshotPacket::handle);
            CHANNEL.registerMessage(1, CarriedAnimalSyncPacket.class, CarriedAnimalSyncPacket::encode,
                    CarriedAnimalSyncPacket::decode, CarriedAnimalSyncPacket::handle);
        }
    }

    /** Broadcast the authoritative carry state so every client can render it. */
    public static void syncCarried(net.minecraft.server.level.ServerPlayer player) {
        if (player == null) return;
        syncCarried(player, net.minecraftforge.network.PacketDistributor.ALL.noArg());
    }

    /** Send one subject's carry state to one newly tracking client. */
    public static void syncCarriedTo(net.minecraft.server.level.ServerPlayer subject,
                                     net.minecraft.server.level.ServerPlayer recipient) {
        if (subject == null || recipient == null) return;
        syncCarried(subject, net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> recipient));
    }

    private static void syncCarried(net.minecraft.server.level.ServerPlayer player,
                                    net.minecraftforge.network.PacketDistributor.PacketTarget target) {
        boolean carrying = com.animania.common.entity.AnimaniaAnimalEntity.hasCarriedAnimal(player);
        CHANNEL.send(target,
                new CarriedAnimalSyncPacket(player.getUUID(), carrying,
                        carrying ? com.animania.common.entity.AnimaniaAnimalEntity.carriedAnimalType(player) : "",
                        carrying ? com.animania.common.entity.AnimaniaAnimalEntity.carriedAnimalData(player)
                                : new net.minecraft.nbt.CompoundTag()));
    }
}
