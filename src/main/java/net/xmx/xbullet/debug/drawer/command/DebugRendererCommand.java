package net.xmx.xbullet.debug.drawer.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.xmx.xbullet.debug.drawer.DebugGlobalRenderer; // WICHTIG: Geänderter Import
import net.xmx.xbullet.debug.drawer.data.VisualizationType;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class DebugRendererCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // Der Befehl kann jetzt auch von der Konsole ausgeführt werden
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("debugrenderer")
                .requires(source -> source.hasPermission(2))
                .executes(DebugRendererCommand::showStatus)
                .then(Commands.literal("toggle")
                        .then(Commands.argument("type", StringArgumentType.word())
                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                                        Arrays.stream(VisualizationType.values()).map(Enum::name), builder))
                                .executes(DebugRendererCommand::toggle)
                        )
                )
                .then(Commands.literal("off")
                        .executes(DebugRendererCommand::deactivate)
                );
        dispatcher.register(root);
    }

    private static int showStatus(CommandContext<CommandSourceStack> context) {
        Set<VisualizationType> activeTypes = DebugGlobalRenderer.getActiveVisualizations();
        if (activeTypes.isEmpty()) {
            context.getSource().sendSuccess(() -> Component.literal("Globaler Physics Debug ist deaktiviert."), false);
        } else {
            String activeList = activeTypes.stream().map(Enum::name).collect(Collectors.joining(", "));
            context.getSource().sendSuccess(() -> Component.literal("Aktive globale Debug-Ansichten: " + activeList), false);
        }

        context.getSource().sendSuccess(() -> Component.literal("Verfügbare Typen: " +
                Arrays.stream(VisualizationType.values()).map(Enum::name).collect(Collectors.joining(", "))), false);
        return 1;
    }

    private static int deactivate(CommandContext<CommandSourceStack> context) {
        DebugGlobalRenderer.deactivateAll();
        context.getSource().sendSuccess(() -> Component.literal("Alle globalen Physics Debug-Ansichten deaktiviert."), true);
        return 1;
    }

    private static int toggle(CommandContext<CommandSourceStack> context) {
        String typeStr = StringArgumentType.getString(context, "type").toUpperCase();
        try {
            VisualizationType type = VisualizationType.valueOf(typeStr);
            boolean nowActive = DebugGlobalRenderer.toggle(type);
            if (nowActive) {
                context.getSource().sendSuccess(() -> Component.literal(type.name() + " global aktiviert."), true);
            } else {
                context.getSource().sendSuccess(() -> Component.literal(type.name() + " global deaktiviert."), true);
            }
        } catch (IllegalArgumentException e) {
            context.getSource().sendFailure(Component.literal("Unbekannter Visualisierungstyp: " + typeStr));
            return 0;
        }
        return 1;
    }
}