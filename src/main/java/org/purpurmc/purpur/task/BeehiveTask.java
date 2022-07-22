package org.purpurmc.purpur.task;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_18_R2.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_18_R2.scheduler.MinecraftInternalPlugin;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginBase;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;

public class BeehiveTask implements PluginMessageListener {
    public static final ResourceLocation BEEHIVE_C2S = new ResourceLocation("purpur", "beehive_c2s");
    public static final ResourceLocation BEEHIVE_S2C = new ResourceLocation("purpur", "beehive_s2c");

    private static BeehiveTask instance;

    public static BeehiveTask instance() {
        if (instance == null) {
            instance = new BeehiveTask();
        }
        return instance;
    }

    private final PluginBase plugin = new MinecraftInternalPlugin();

    private BeehiveTask() {
    }

    public void register() {
        Bukkit.getMessenger().registerOutgoingPluginChannel(this.plugin, BEEHIVE_S2C.toString());
        Bukkit.getMessenger().registerIncomingPluginChannel(this.plugin, BEEHIVE_C2S.toString(), this);
    }

    public void unregister() {
        Bukkit.getMessenger().unregisterOutgoingPluginChannel(this.plugin, BEEHIVE_S2C.toString());
        Bukkit.getMessenger().unregisterIncomingPluginChannel(this.plugin, BEEHIVE_C2S.toString());
    }

    @Override
    public void onPluginMessageReceived(@NotNull String channel, Player player, byte[] bytes) {
        ByteArrayDataInput in = in(bytes);
        long packedPos = in.readLong();
        BlockPos pos = BlockPos.of(packedPos);

        ServerPlayer serverPlayer = ((CraftPlayer) player).getHandle();

        BlockEntity blockEntity = serverPlayer.level.getBlockEntity(pos);
        if (!(blockEntity instanceof BeehiveBlockEntity beehive)) {
            return;
        }

        ByteArrayDataOutput out = out();

        out.writeInt(beehive.getOccupantCount());
        out.writeLong(packedPos);

        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.wrappedBuffer(out.toByteArray()));
        serverPlayer.connection.send(new ClientboundCustomPayloadPacket(BEEHIVE_S2C, buf));
    }

    @SuppressWarnings("UnstableApiUsage")
    private static ByteArrayDataOutput out() {
        return ByteStreams.newDataOutput();
    }

    @SuppressWarnings("UnstableApiUsage")
    private static ByteArrayDataInput in(byte[] bytes) {
        return ByteStreams.newDataInput(bytes);
    }
}