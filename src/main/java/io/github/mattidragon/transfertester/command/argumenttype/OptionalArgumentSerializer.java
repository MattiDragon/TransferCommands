package io.github.mattidragon.transfertester.command.argumenttype;

import com.google.gson.JsonObject;
import com.mojang.brigadier.arguments.ArgumentType;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.ArgumentTypes;
import net.minecraft.command.argument.serialize.ArgumentSerializer;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.registry.Registry;

import java.util.function.Function;

public class OptionalArgumentSerializer implements ArgumentSerializer<OptionalArgumentType<?>, OptionalArgumentSerializer.Properties> {
    @Override
    public void writePacket(Properties properties, PacketByteBuf buf) {
        var delegate = properties.delegate.apply(null); // Hope a deserialized one doesn't get re-serialized
        buf.writeIdentifier(Registry.COMMAND_ARGUMENT_TYPE.getId(ArgumentTypes.get(delegate)));
        writeArgumentType(ArgumentTypes.get(delegate), delegate, buf);
        buf.writeChar(properties.wildcard);
    }

    // Dummy method to ensure all generics match.
    private <A extends ArgumentType<?>, T extends ArgumentTypeProperties<A>, S extends ArgumentSerializer<A, T>> void writeArgumentType(S serializer, A type, PacketByteBuf buf) {
        serializer.writePacket(serializer.getArgumentTypeProperties(type), buf);
    }

    @Override
    public Properties fromPacket(PacketByteBuf buf) {
        var serializer = Registry.COMMAND_ARGUMENT_TYPE.get(buf.readIdentifier());
        if (serializer == null)
            throw new IllegalArgumentException("Missing argument serializer");
        var props = serializer.fromPacket(buf);

        return new Properties(props::createType, buf.readChar());
    }

    @Override
    public void writeJson(Properties properties, JsonObject json) {
        json.addProperty("wildcard", String.valueOf(properties.wildcard));
        var delegate = properties.delegate.apply(null);
        var delegateJson = new JsonObject();
        writeArgumentType(ArgumentTypes.get(delegate), delegate, delegateJson);
        json.add("delegate", delegateJson);
    }

    // Dummy method to ensure all generics match.
    private <A extends ArgumentType<?>, T extends ArgumentTypeProperties<A>, S extends ArgumentSerializer<A, T>> void writeArgumentType(S serializer, A type, JsonObject json) {
        serializer.writeJson(serializer.getArgumentTypeProperties(type), json);
    }

    @Override
    public Properties getArgumentTypeProperties(OptionalArgumentType<?> argumentType) {
        return new Properties((__) -> argumentType.delegate, argumentType.wildcard);
    }

    public class Properties implements ArgumentTypeProperties<OptionalArgumentType<?>> {
        final Function<CommandRegistryAccess, ArgumentType<?>> delegate;
        final char wildcard;

        public Properties(Function<CommandRegistryAccess, ArgumentType<?>> delegate, char wildcard) {
            this.delegate = delegate;
            this.wildcard = wildcard;
        }

        @Override
        public OptionalArgumentType<?> createType(CommandRegistryAccess registryAccess) {
            return OptionalArgumentType.optional(delegate.apply(registryAccess), wildcard);
        }

        @Override
        public ArgumentSerializer<OptionalArgumentType<?>, ?> getSerializer() {
            return OptionalArgumentSerializer.this;
        }
    }
}
