package io.github.mattidragon.transfertester.command.argumenttype;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class OptionalArgumentType<T> implements ArgumentType<Optional<T>> {
    final ArgumentType<T> delegate;
    final char wildcard;

    private OptionalArgumentType(ArgumentType<T> delegate, char wildcard) {
        this.delegate = delegate;
        this.wildcard = wildcard;
    }

    public static <T> OptionalArgumentType<T> optional(ArgumentType<T> delegate, char wildcard) {
        return new OptionalArgumentType<>(delegate, wildcard);
    }

    public static <T> Optional<T> getOptional(final CommandContext<?> context, final String name) {
        //noinspection unchecked
        return context.getArgument(name, Optional.class);
    }

    @Override
    public Optional<T> parse(StringReader reader) throws CommandSyntaxException {
        if (reader.canRead() && reader.peek() == wildcard) {
            reader.read();
            return Optional.empty();
        }
        return Optional.of(delegate.parse(reader));
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        return delegate.listSuggestions(context, builder).thenApply(suggestions -> {
            var builder1 = new SuggestionsBuilder(builder.getInput(), builder.getStart());
            builder1.suggest(String.valueOf(wildcard));
            suggestions.getList().forEach(suggestion -> builder1.suggest(suggestion.getText(), suggestion.getTooltip()));
            return builder1.build();
        });
    }

    @Override
    public Collection<String> getExamples() {
        var list = new ArrayList<>(delegate.getExamples());
        list.add(String.valueOf(wildcard));
        return list;
    }
}
