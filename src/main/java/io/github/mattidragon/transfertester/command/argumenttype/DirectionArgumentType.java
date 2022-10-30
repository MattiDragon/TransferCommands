package io.github.mattidragon.transfertester.command.argumenttype;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.command.argument.EnumArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.util.math.Direction;

public class DirectionArgumentType extends EnumArgumentType<Direction> {
    protected DirectionArgumentType() {
        super(Direction.CODEC, Direction::values);
    }

    public static DirectionArgumentType direction() {
        return new DirectionArgumentType();
    }

    public static Direction getDirection(CommandContext<ServerCommandSource> context, String id) {
        return context.getArgument(id, Direction.class);
    }
}
