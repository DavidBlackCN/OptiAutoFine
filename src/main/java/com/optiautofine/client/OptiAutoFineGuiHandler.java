package com.optiautofine.client;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraftforge.client.event.GuiOpenEvent;

import com.optiautofine.loader.OptiAutoFineLoadingPlugin;

public class OptiAutoFineGuiHandler
{
    private static boolean shown;

    @SubscribeEvent
    public void onGuiOpen(GuiOpenEvent event)
    {
        if (shown) {
            return;
        }

        if (!(event.gui instanceof GuiMainMenu)) {
            return;
        }

        int status = OptiAutoFineLoadingPlugin.getStatus();
        if (status == OptiAutoFineLoadingPlugin.STATUS_NONE) {
            return;
        }

        shown = true;
        event.gui = OptiAutoFineNoticeScreen.fromStatus(status);
    }
}
