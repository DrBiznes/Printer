package me.jamino.printer.entity;

import me.jamino.printer.data.ImageReference;
import me.jamino.printer.data.PrintMode;
import me.jamino.printer.registry.ModDataComponents;
import me.jamino.printer.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public final class PrintedImageEntity extends HangingEntity {
    private static final EntityDataAccessor<String> CONTENT_ID = SynchedEntityData.defineId(
            PrintedImageEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> TITLE = SynchedEntityData.defineId(
            PrintedImageEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> PIXEL_WIDTH = SynchedEntityData.defineId(
            PrintedImageEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> PIXEL_HEIGHT = SynchedEntityData.defineId(
            PrintedImageEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> MONOCHROME = SynchedEntityData.defineId(
            PrintedImageEntity.class, EntityDataSerializers.BOOLEAN);

    public PrintedImageEntity(EntityType<? extends PrintedImageEntity> type, Level level) {
        super(type, level);
    }

    public PrintedImageEntity(EntityType<? extends PrintedImageEntity> type, Level level, BlockPos pos,
                              Direction direction, ImageReference reference) {
        super(type, level, pos);
        setReference(reference);
        setDirection(direction);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(CONTENT_ID, "");
        builder.define(TITLE, "");
        builder.define(PIXEL_WIDTH, 128);
        builder.define(PIXEL_HEIGHT, 128);
        builder.define(MONOCHROME, false);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
        if (accessor.equals(PIXEL_WIDTH) || accessor.equals(PIXEL_HEIGHT)) recalculateBoundingBox();
        super.onSyncedDataUpdated(accessor);
    }

    public void setReference(ImageReference reference) {
        entityData.set(CONTENT_ID, reference.contentId());
        entityData.set(TITLE, reference.title());
        entityData.set(PIXEL_WIDTH, reference.width());
        entityData.set(PIXEL_HEIGHT, reference.height());
        entityData.set(MONOCHROME, reference.mode() == PrintMode.MONOCHROME);
        recalculateBoundingBox();
    }

    public ImageReference getReference() {
        return new ImageReference(entityData.get(CONTENT_ID), entityData.get(PIXEL_WIDTH),
                entityData.get(PIXEL_HEIGHT), entityData.get(TITLE),
                entityData.get(MONOCHROME) ? PrintMode.MONOCHROME : PrintMode.COLOR);
    }

    public int blocksWide() { return getReference().blocksWide(); }
    public int blocksHigh() { return getReference().blocksHigh(); }

    @Override
    protected AABB calculateBoundingBox(BlockPos pos, Direction direction) {
        int width = blocksWide();
        int height = blocksHigh();
        Vec3 center = Vec3.atCenterOf(pos).relative(direction, -0.46875);
        double horizontalOffset = width % 2 == 0 ? 0.5 : 0.0;
        double verticalOffset = height % 2 == 0 ? 0.5 : 0.0;
        Vec3 adjusted = center.relative(direction.getCounterClockWise(), horizontalOffset)
                .relative(Direction.UP, verticalOffset);
        Direction.Axis axis = direction.getAxis();
        return AABB.ofSize(adjusted, axis == Direction.Axis.X ? 0.0625 : width,
                height, axis == Direction.Axis.Z ? 0.0625 : width);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        ImageReference reference = getReference();
        tag.putString("ContentId", reference.contentId());
        tag.putString("Title", reference.title());
        tag.putInt("PixelWidth", reference.width());
        tag.putInt("PixelHeight", reference.height());
        tag.putBoolean("Monochrome", reference.mode() == PrintMode.MONOCHROME);
        tag.putByte("Facing", (byte) direction.get2DDataValue());
        super.addAdditionalSaveData(tag);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        setReference(new ImageReference(tag.getString("ContentId"), Math.max(1, tag.getInt("PixelWidth")),
                Math.max(1, tag.getInt("PixelHeight")), tag.getString("Title"),
                tag.getBoolean("Monochrome") ? PrintMode.MONOCHROME : PrintMode.COLOR));
        direction = Direction.from2DDataValue(tag.getByte("Facing"));
        super.readAdditionalSaveData(tag);
        setDirection(direction);
    }

    @Override
    public void dropItem(@Nullable Entity breaker) {
        if (!level().getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS)) return;
        playSound(SoundEvents.PAINTING_BREAK, 1.0F, 1.0F);
        if (breaker instanceof Player player && player.hasInfiniteMaterials()) return;
        ItemStack stack = new ItemStack(ModItems.IMAGE.get());
        stack.set(ModDataComponents.IMAGE_REFERENCE.get(), getReference());
        spawnAtLocation(stack);
    }

    @Override public void playPlacementSound() { playSound(SoundEvents.PAINTING_PLACE, 1.0F, 1.0F); }
    @Override public void moveTo(double x, double y, double z, float yaw, float pitch) { setPos(x, y, z); }
    @Override public void lerpTo(double x, double y, double z, float yaw, float pitch, int steps) { setPos(x, y, z); }
    @Override public Vec3 trackingPosition() { return Vec3.atLowerCornerOf(pos); }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket(ServerEntity serverEntity) {
        return new ClientboundAddEntityPacket(this, direction.get3DDataValue(), getPos());
    }

    @Override
    public void recreateFromPacket(ClientboundAddEntityPacket packet) {
        super.recreateFromPacket(packet);
        setDirection(Direction.from3DDataValue(packet.getData()));
    }

    @Override
    public ItemStack getPickResult() {
        ItemStack stack = new ItemStack(ModItems.IMAGE.get());
        stack.set(ModDataComponents.IMAGE_REFERENCE.get(), getReference());
        return stack;
    }
}
