package com.optiautofine;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.FMLCommonHandler;
import net.minecraftforge.common.MinecraftForge;
import com.optiautofine.client.OptiAutoFineGuiHandler;
import java.io.File;

@Mod(modid = OptiAutoFine.MODID, name = OptiAutoFine.NAME, version = OptiAutoFine.VERSION)
public class OptiAutoFine
{
    public static final String MODID = "optiautofine";
    public static final String NAME = "OptiAutoFine";
    public static final String VERSION = "1.0.0";

    @EventHandler
    public void preInit(FMLPreInitializationEvent event)
    {
        File configDir = event.getModConfigurationDirectory();
        OptiAutoFineConfig.load(configDir);
    }

    @EventHandler
    public void init(FMLInitializationEvent event)
    {
        FMLLog.info("[%s] Initialized.", NAME);

        if (FMLCommonHandler.instance().getSide().isClient()) {
            MinecraftForge.EVENT_BUS.register(new OptiAutoFineGuiHandler());
        }
    }
}
