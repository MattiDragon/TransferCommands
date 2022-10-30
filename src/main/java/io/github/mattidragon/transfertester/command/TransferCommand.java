package io.github.mattidragon.transfertester.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import io.github.mattidragon.transfertester.command.argumenttype.DirectionArgumentType;
import io.github.mattidragon.transfertester.command.argumenttype.OptionalArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.command.argument.ItemStackArgument;
import net.minecraft.command.argument.ItemStackArgumentType;
import net.minecraft.command.argument.RegistryKeyArgumentType;
import net.minecraft.fluid.Fluid;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import team.reborn.energy.api.EnergyStorage;
import team.reborn.energy.api.EnergyStorageUtil;

import java.util.function.Predicate;

import static net.minecraft.server.command.CommandManager.*;

@SuppressWarnings("UnstableApiUsage")
public class TransferCommand implements CommandRegistrationCallback {
    private static final DynamicCommandExceptionType UNKNOWN_FLUID = new DynamicCommandExceptionType(fluid -> Text.translatable("commands.transfer.fluid.invalid", fluid));
    private static final SimpleCommandExceptionType MISSING_STORAGE = new SimpleCommandExceptionType(Text.translatable("commands.transfer.invalid_storage"));

    @Override
    public void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, RegistrationEnvironment environment) {
        dispatcher.register(literal("transfer")
                .requires(source -> source.hasPermissionLevel(2))
                .then(literal("insert")
                        .then(literal("fluid")
                                .then(argument("pos", BlockPosArgumentType.blockPos())
                                        .then(argument("side", DirectionArgumentType.direction())
                                                .then(argument("maxAmount", LongArgumentType.longArg(0))
                                                        .suggests(((context, builder) -> builder.suggest("81000").buildFuture()))
                                                        .then(argument("type", RegistryKeyArgumentType.registryKey(Registry.FLUID_KEY))
                                                                .executes(context -> executeInsertFluid(context, false))
                                                                .then(literal("simulate")
                                                                        .executes(context -> executeInsertFluid(context, true))))))))
                        .then(literal("item")
                                .then(argument("pos", BlockPosArgumentType.blockPos())
                                        .then(argument("side", DirectionArgumentType.direction())
                                                .then(argument("maxAmount", LongArgumentType.longArg(0))
                                                        .suggests(((context, builder) -> builder.suggest("64").suggest("1").buildFuture()))
                                                        .then(argument("type", ItemStackArgumentType.itemStack(registryAccess))
                                                                .executes(context -> executeInsertItem(context, false))
                                                                .then(literal("simulate")
                                                                        .executes(context -> executeInsertItem(context, true))))))))
                        .then(literal("energy")
                                .then(argument("pos", BlockPosArgumentType.blockPos())
                                        .then(argument("side", DirectionArgumentType.direction())
                                                .then(argument("maxAmount", LongArgumentType.longArg(0))
                                                        .suggests(((context, builder) -> builder.suggest("128").suggest("256").buildFuture()))
                                                        .executes(context -> executeInsertEnergy(context, false))
                                                        .then(literal("simulate")
                                                                .executes(context -> executeInsertEnergy(context, true))))))))
                .then(literal("extract")
                        .then(literal("fluid")
                                .then(argument("pos", BlockPosArgumentType.blockPos())
                                        .then(argument("side", DirectionArgumentType.direction())
                                                .then(argument("maxAmount", LongArgumentType.longArg(0))
                                                        .suggests(((context, builder) -> builder.suggest("81000").buildFuture()))
                                                        .then(argument("type", RegistryKeyArgumentType.registryKey(Registry.FLUID_KEY))
                                                                .suggests(((context, builder) -> builder.suggest("128").suggest("256").buildFuture()))
                                                                .executes(context -> executeExtractFluid(context, false))
                                                                .then(literal("simulate")
                                                                        .executes(context -> executeExtractFluid(context, true))))))))
                        .then(literal("item")
                                .then(argument("pos", BlockPosArgumentType.blockPos())
                                        .then(argument("side", DirectionArgumentType.direction())
                                                .then(argument("maxAmount", LongArgumentType.longArg(0))
                                                        .suggests(((context, builder) -> builder.suggest("64").suggest("1").buildFuture()))
                                                        .then(argument("type", ItemStackArgumentType.itemStack(registryAccess))
                                                                .executes(context -> executeExtractItem(context, false))
                                                                .then(literal("simulate")
                                                                        .executes(context -> executeExtractItem(context, true))))))))
                        .then(literal("energy")
                                .then(argument("pos", BlockPosArgumentType.blockPos())
                                        .then(argument("side", DirectionArgumentType.direction())
                                                .then(argument("maxAmount", LongArgumentType.longArg(0))
                                                        .executes(context -> executeExtractEnergy(context, false))
                                                        .then(literal("simulate")
                                                                .executes(context -> executeExtractEnergy(context, true))))))))
                .then(literal("move")
                        .then(literal("fluid")
                                .then(argument("pos1", BlockPosArgumentType.blockPos())
                                        .then(argument("side1", DirectionArgumentType.direction())
                                                .then(argument("pos2", BlockPosArgumentType.blockPos())
                                                        .then(argument("side2", DirectionArgumentType.direction())
                                                                .then(argument("maxAmount", OptionalArgumentType.optional(LongArgumentType.longArg(0), '*'))
                                                                        .suggests(((context, builder) -> builder.suggest("*").suggest("81000").buildFuture()))
                                                                        .then(argument("type", OptionalArgumentType.optional(RegistryKeyArgumentType.registryKey(Registry.FLUID_KEY), '*'))
                                                                                .executes(context -> executeMoveFluid(context, false))
                                                                                .then(literal("simulate")
                                                                                        .executes(context -> executeMoveFluid(context, true))))))))))
                        .then(literal("item")
                                .then(argument("pos1", BlockPosArgumentType.blockPos())
                                        .then(argument("side1", DirectionArgumentType.direction())
                                                .then(argument("pos2", BlockPosArgumentType.blockPos())
                                                        .then(argument("side2", DirectionArgumentType.direction())
                                                                .then(argument("maxAmount", OptionalArgumentType.optional(LongArgumentType.longArg(0), '*'))
                                                                        .suggests(((context, builder) -> builder.suggest("*").suggest("64").suggest("1").buildFuture()))
                                                                        .then(argument("type", OptionalArgumentType.optional(ItemStackArgumentType.itemStack(registryAccess), '*'))
                                                                                .executes(context -> executeMoveItem(context, false))
                                                                                .then(literal("simulate")
                                                                                        .executes(context -> executeMoveItem(context, true))))))))))
                        .then(literal("energy")
                                .then(argument("pos1", BlockPosArgumentType.blockPos())
                                        .then(argument("side1", DirectionArgumentType.direction())
                                                .then(argument("pos2", BlockPosArgumentType.blockPos())
                                                        .then(argument("side2", DirectionArgumentType.direction())
                                                                .then(argument("maxAmount", OptionalArgumentType.optional(LongArgumentType.longArg(0), '*'))
                                                                        .suggests(((context, builder) -> builder.suggest("*").suggest("128").suggest("256").buildFuture()))
                                                                        .executes(context -> executeMoveEnergy(context, false))
                                                                        .then(literal("simulate")
                                                                                .executes(context -> executeMoveEnergy(context, true)))))))))));
    }

    private int executeMoveItem(CommandContext<ServerCommandSource> context, boolean simulate) throws CommandSyntaxException {
        var pos1 = BlockPosArgumentType.getBlockPos(context, "pos1");
        var side1 = DirectionArgumentType.getDirection(context, "side1");
        var pos2 = BlockPosArgumentType.getBlockPos(context, "pos2");
        var side2 = DirectionArgumentType.getDirection(context, "side2");
        var maxAmount = OptionalArgumentType.<Long>getOptional(context, "maxAmount").orElse(Long.MAX_VALUE);
        var optionalItem = OptionalArgumentType.<ItemStackArgument>getOptional(context, "type");
        Predicate<ItemVariant> filter = optionalItem.isPresent() ? Predicate.isEqual(ItemVariant.of(optionalItem.get().createStack(1, false))) : __ -> true;

        var source = context.getSource();

        var storage1 = ItemStorage.SIDED.find(source.getWorld(), pos1, side1);
        if (storage1 == null) throw MISSING_STORAGE.create();

        var storage2 = ItemStorage.SIDED.find(source.getWorld(), pos2, side2);
        if (storage2 == null) throw MISSING_STORAGE.create();

        try (var t = Transaction.openOuter()) {
            var moved = StorageUtil.move(storage1, storage2, filter, maxAmount, t);

            if (simulate) {
                context.getSource().sendMessage(Text.translatable("commands.transfer.move.simulate.success", moved, "items"));
            } else {
                t.commit();
                context.getSource().sendMessage(Text.translatable("commands.transfer.move.success", moved, "items"));
            }
            return moved > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) moved;
        }
    }

    private int executeMoveFluid(CommandContext<ServerCommandSource> context, boolean simulate) throws CommandSyntaxException {
        var pos1 = BlockPosArgumentType.getBlockPos(context, "pos1");
        var side1 = DirectionArgumentType.getDirection(context, "side1");
        var pos2 = BlockPosArgumentType.getBlockPos(context, "pos2");
        var side2 = DirectionArgumentType.getDirection(context, "side2");
        var maxAmount = OptionalArgumentType.<Long>getOptional(context, "maxAmount").orElse(Long.MAX_VALUE);
        var optionalKey = OptionalArgumentType.<RegistryKey<Fluid>>getOptional(context, "type");
        Predicate<FluidVariant> filter;
        if (optionalKey.isPresent()) {
            var fluid = Registry.FLUID.get(optionalKey.get());
            if (fluid == null)
                throw UNKNOWN_FLUID.create(optionalKey.get().getValue());
            filter = Predicate.isEqual(FluidVariant.of(fluid));
        } else {
            filter = __ -> true;
        }
        var source = context.getSource();

        var storage1 = FluidStorage.SIDED.find(source.getWorld(), pos1, side1);
        if (storage1 == null) throw MISSING_STORAGE.create();

        var storage2 = FluidStorage.SIDED.find(source.getWorld(), pos2, side2);
        if (storage2 == null) throw MISSING_STORAGE.create();

        try (var t = Transaction.openOuter()) {
            var moved = StorageUtil.move(storage1, storage2, filter, maxAmount, t);

            if (simulate) {
                context.getSource().sendMessage(Text.translatable("commands.transfer.move.simulate.success", moved, "droplets"));
            } else {
                t.commit();
                context.getSource().sendMessage(Text.translatable("commands.transfer.move.success", moved, "droplets"));
            }
            return moved > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) moved;
        }
    }

    private int executeMoveEnergy(CommandContext<ServerCommandSource> context, boolean simulate) throws CommandSyntaxException {
        var pos1 = BlockPosArgumentType.getBlockPos(context, "pos1");
        var side1 = DirectionArgumentType.getDirection(context, "side1");
        var pos2 = BlockPosArgumentType.getBlockPos(context, "pos2");
        var side2 = DirectionArgumentType.getDirection(context, "side2");
        var maxAmount = OptionalArgumentType.<Long>getOptional(context, "maxAmount").orElse(Long.MAX_VALUE);
        var source = context.getSource();

        var storage1 = EnergyStorage.SIDED.find(source.getWorld(), pos1, side1);
        if (storage1 == null) throw MISSING_STORAGE.create();

        var storage2 = EnergyStorage.SIDED.find(source.getWorld(), pos2, side2);
        if (storage2 == null) throw MISSING_STORAGE.create();

        try (var t = Transaction.openOuter()) {
            var moved = EnergyStorageUtil.move(storage1, storage2, maxAmount, t);

            if (simulate) {
                context.getSource().sendMessage(Text.translatable("commands.transfer.move.simulate.success", moved, "energy"));
            } else {
                t.commit();
                context.getSource().sendMessage(Text.translatable("commands.transfer.move.success", moved, "energy"));
            }
            return moved > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) moved;
        }
    }

    private int executeExtractItem(CommandContext<ServerCommandSource> context, boolean simulate) throws CommandSyntaxException {
        var pos = BlockPosArgumentType.getBlockPos(context, "pos");
        var side = DirectionArgumentType.getDirection(context, "side");
        var maxAmount = LongArgumentType.getLong(context, "maxAmount");
        var item = ItemStackArgumentType.getItemStackArgument(context, "type").createStack(1, false);

        var source = context.getSource();
        try (var t = Transaction.openOuter()) {
            var storage = ItemStorage.SIDED.find(source.getWorld(), pos, side);
            if (storage == null)
                throw MISSING_STORAGE.create();

            var extracted = storage.extract(ItemVariant.of(item), maxAmount, t);
            if (simulate) {
                context.getSource().sendMessage(Text.translatable("commands.transfer.extract.simulate.success", extracted, item.getName()));
            } else {
                t.commit();
                context.getSource().sendMessage(Text.translatable("commands.transfer.extract.success", extracted, item.getName()));
            }
            return extracted > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) extracted;
        }
    }

    private int executeExtractFluid(CommandContext<ServerCommandSource> context, boolean simulate) throws CommandSyntaxException {
        var pos = BlockPosArgumentType.getBlockPos(context, "pos");
        var side = DirectionArgumentType.getDirection(context, "side");
        var maxAmount = LongArgumentType.getLong(context, "maxAmount");
        //noinspection unchecked
        var key = (RegistryKey<Fluid>) context.getArgument("type", RegistryKey.class);
        var fluid = Registry.FLUID.get(key);
        if (fluid == null)
            throw UNKNOWN_FLUID.create(key.getValue());

        var source = context.getSource();
        try (var t = Transaction.openOuter()) {
            var storage = FluidStorage.SIDED.find(source.getWorld(), pos, side);
            if (storage == null)
                throw MISSING_STORAGE.create();

            var extracted = storage.extract(FluidVariant.of(fluid), maxAmount, t);
            if (simulate) {
                context.getSource().sendMessage(Text.translatable("commands.transfer.extract.simulate.success", extracted, key.getValue()));
            } else {
                t.commit();
                context.getSource().sendMessage(Text.translatable("commands.transfer.extract.success", extracted, key.getValue()));
            }
            return extracted > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) extracted;
        }
    }

    private int executeExtractEnergy(CommandContext<ServerCommandSource> context, boolean simulate) throws CommandSyntaxException {
        var pos = BlockPosArgumentType.getBlockPos(context, "pos");
        var side = DirectionArgumentType.getDirection(context, "side");
        var maxAmount = LongArgumentType.getLong(context, "maxAmount");

        var source = context.getSource();
        try (var t = Transaction.openOuter()) {
            var storage = EnergyStorage.SIDED.find(source.getWorld(), pos, side);
            if (storage == null)
                throw MISSING_STORAGE.create();

            var extracted = storage.extract(maxAmount, t);
            if (simulate) {
                context.getSource().sendMessage(Text.translatable("commands.transfer.extract.simulate.success", extracted, "energy"));
            } else {
                t.commit();
                context.getSource().sendMessage(Text.translatable("commands.transfer.extract.success", extracted, "energy"));
            }
            return extracted > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) extracted;
        }
    }

    private int executeInsertItem(CommandContext<ServerCommandSource> context, boolean simulate) throws CommandSyntaxException {
        var pos = BlockPosArgumentType.getBlockPos(context, "pos");
        var side = DirectionArgumentType.getDirection(context, "side");
        var maxAmount = LongArgumentType.getLong(context, "maxAmount");
        var item = ItemStackArgumentType.getItemStackArgument(context, "type").createStack(1, false);

        var source = context.getSource();
        try (var t = Transaction.openOuter()) {
            var storage = ItemStorage.SIDED.find(source.getWorld(), pos, side);
            if (storage == null)
                throw MISSING_STORAGE.create();

            var inserted = storage.insert(ItemVariant.of(item), maxAmount, t);
            if (simulate) {
                context.getSource().sendMessage(Text.translatable("commands.transfer.insert.simulate.success", inserted, item.getName()));
            } else {
                t.commit();
                context.getSource().sendMessage(Text.translatable("commands.transfer.insert.success", inserted, item.getName()));
            }
            return inserted > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) inserted;
        }
    }

    private int executeInsertFluid(CommandContext<ServerCommandSource> context, boolean simulate) throws CommandSyntaxException {
        var pos = BlockPosArgumentType.getBlockPos(context, "pos");
        var side = DirectionArgumentType.getDirection(context, "side");
        var maxAmount = LongArgumentType.getLong(context, "maxAmount");
        //noinspection unchecked
        var key = (RegistryKey<Fluid>) context.getArgument("type", RegistryKey.class);
        var fluid = Registry.FLUID.get(key);
        if (fluid == null)
            throw UNKNOWN_FLUID.create(key.getValue());

        var source = context.getSource();
        try (var t = Transaction.openOuter()) {
            var storage = FluidStorage.SIDED.find(source.getWorld(), pos, side);
            if (storage == null)
                throw MISSING_STORAGE.create();

            var inserted = storage.insert(FluidVariant.of(fluid), maxAmount, t);
            if (simulate) {
                context.getSource().sendMessage(Text.translatable("commands.transfer.insert.simulate.success", inserted, key.getValue()));
            } else {
                t.commit();
                context.getSource().sendMessage(Text.translatable("commands.transfer.insert.success", inserted, key.getValue()));
            }
            return inserted > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) inserted;
        }
    }

    private int executeInsertEnergy(CommandContext<ServerCommandSource> context, boolean simulate) throws CommandSyntaxException {
        var pos = BlockPosArgumentType.getBlockPos(context, "pos");
        var side = DirectionArgumentType.getDirection(context, "side");
        var maxAmount = LongArgumentType.getLong(context, "maxAmount");

        var source = context.getSource();
        try (var t = Transaction.openOuter()) {
            var storage = EnergyStorage.SIDED.find(source.getWorld(), pos, side);
            if (storage == null)
                throw MISSING_STORAGE.create();

            var inserted = storage.insert(maxAmount, t);
            if (simulate) {
                context.getSource().sendMessage(Text.translatable("commands.transfer.insert.simulate.success", inserted, "energy"));
            } else {
                t.commit();
                context.getSource().sendMessage(Text.translatable("commands.transfer.insert.success", inserted, "energy"));
            }
            return inserted > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) inserted;
        }
    }
}
