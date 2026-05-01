package com.optiautofine.client;

import java.util.ArrayList;
import java.util.List;
import java.io.File;
import java.awt.Desktop;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.Minecraft;
import cpw.mods.fml.common.FMLCommonHandler;

import com.optiautofine.loader.OptiAutoFineLoadingPlugin;
import com.optiautofine.OptiAutoFineConfig;

public class OptiAutoFineNoticeScreen extends GuiScreen
{
    private final String title;
    private final List<String> lines;

    private OptiAutoFineNoticeScreen(String title, List<String> lines)
    {
        this.title = title;
        this.lines = lines;
    }

    public static OptiAutoFineNoticeScreen fromStatus(int status)
    {
        List<String> lines = new ArrayList<String>();
        String title;

        if (status == OptiAutoFineLoadingPlugin.STATUS_DOWNLOAD_OK) {
            title = OptiAutoFineConfig.titleSuccess;
            lines.add(OptiAutoFineConfig.lineSuccess1);
            lines.add(OptiAutoFineConfig.lineSuccess2);
        } else {
            title = OptiAutoFineConfig.titleFail;
            lines.add(OptiAutoFineConfig.lineFail1);
            lines.add(OptiAutoFineConfig.lineFail2);
        }

        if (OptiAutoFineConfig.showDetails) {
            String detail = OptiAutoFineLoadingPlugin.getStatusDetail();
            if (detail != null && detail.length() > 0) {
                if (status == OptiAutoFineLoadingPlugin.STATUS_DOWNLOAD_OK) {
                    lines.add(String.format(OptiAutoFineConfig.successDetailFormat, detail));
                } else {
                    lines.add(OptiAutoFineConfig.resolveFailureDetail(detail));
                }
            }
        }

        return new OptiAutoFineNoticeScreen(title, lines);
    }

    @Override
    public void initGui()
    {
        this.buttonList.clear();
        int centerX = this.width / 2 - 100;
        int baseY = this.height - 80;
        this.buttonList.add(new GuiButton(1, centerX, baseY, 200, 20, OptiAutoFineConfig.buttonOpenMods));
        this.buttonList.add(new GuiButton(2, centerX, baseY + 24, 200, 20, OptiAutoFineConfig.buttonQuit));
        this.buttonList.add(new GuiButton(0, centerX, baseY + 48, 200, 20, OptiAutoFineConfig.buttonContinue));
    }

    @Override
    protected void actionPerformed(GuiButton button)
    {
        if (button.id == 0) {
            this.mc.displayGuiScreen(new GuiMainMenu());
            return;
        }

        if (button.id == 1) {
            openModsFolder();
            return;
        }

        if (button.id == 2) {
            FMLCommonHandler.instance().exitJava(0, false);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks)
    {
        this.drawDefaultBackground();
        this.drawCenteredString(this.fontRendererObj, title, this.width / 2, 40, 0xFFFFFF);

        int y = 70;
        for (String line : lines) {
            this.drawCenteredString(this.fontRendererObj, line, this.width / 2, y, 0xE0E0E0);
            y += 12;
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void openModsFolder()
    {
        if (!Desktop.isDesktopSupported()) {
            return;
        }

        File mcDir = Minecraft.getMinecraft().mcDataDir;
        if (mcDir == null) {
            return;
        }

        File modsDir = new File(mcDir, "mods");
        try {
            Desktop.getDesktop().open(modsDir);
        } catch (Exception ex) {
        }
    }
}
