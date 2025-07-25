package net.xmx.vortex.init;

import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.xmx.vortex.init.registry.CommandRegistry;
import net.xmx.vortex.init.registry.ModRegistries;
import net.xmx.vortex.network.NetworkHandler;
import net.xmx.vortex.builtin.BuiltInPhysicsRegistry;
import net.xmx.vortex.natives.NativeJoltInitializer;
import net.xmx.vortex.physics.constraint.serializer.registry.ConstraintSerializerRegistry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(VxMainClass.MODID)
public class VxMainClass {
    public static final String MODID = "vortex";
    public static final Logger LOGGER = LogManager.getLogger();

    public static final IEventBus eventBus = FMLJavaModLoadingContext.get().getModEventBus();
    public static final IEventBus forgeEventBus = MinecraftForge.EVENT_BUS;


    public VxMainClass() {

        ModRegistries.register(eventBus);
        RegisterEvents.register(eventBus, forgeEventBus);

        MinecraftForge.EVENT_BUS.addListener(this::onRegisterCommands);
        MinecraftForge.EVENT_BUS.addListener(this::onRegisterClientCommands);
        eventBus.addListener(this::commonSetup);
        eventBus.addListener(this::clientSetup);

    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            BuiltInPhysicsRegistry.register();
            ConstraintSerializerRegistry.registerDefaults();
            NetworkHandler.register();

            try {
                NativeJoltInitializer.initialize();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

        });
    }

    private void clientSetup(final FMLClientSetupEvent event) {
        event.enqueueWork(() -> {

            RegisterEvents.registerClient(forgeEventBus, MinecraftForge.EVENT_BUS);
            BuiltInPhysicsRegistry.registerClientRenderers();
        });
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        CommandRegistry.registerCommon(event.getDispatcher());
    }

    private void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        CommandRegistry.registerClient(event.getDispatcher());
    }
}