package io.github.mattidragon.transfertester;

import io.github.mattidragon.transfertester.command.argumenttype.DirectionArgumentType;
import io.github.mattidragon.transfertester.command.TransferCommand;
import io.github.mattidragon.transfertester.command.argumenttype.OptionalArgumentSerializer;
import io.github.mattidragon.transfertester.command.argumenttype.OptionalArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.ArgumentTypeRegistry;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.serialize.ConstantArgumentSerializer;
import net.minecraft.util.Identifier;

public class TransferTester implements ModInitializer {
    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register(new TransferCommand());

        ArgumentTypeRegistry.registerArgumentType(new Identifier("transfer_tester", "direction"), DirectionArgumentType.class, ConstantArgumentSerializer.of(DirectionArgumentType::direction));
        // Fuck generics and intellij
        //noinspection RedundantCast,unchecked
        ArgumentTypeRegistry.registerArgumentType(new Identifier("transfer_tester", "optional"), (Class<? extends OptionalArgumentType<?>>) (Object) OptionalArgumentType.class, new OptionalArgumentSerializer());
    }
}
