package com.optiautofine;

import java.io.File;

import net.minecraftforge.common.config.Configuration;

public final class OptiAutoFineConfig
{
    public static final String DEFAULT_OPTIFINE_FILE = "OptiFine_1.7.10_HD_U_E7.jar";

    public static String optifineFile = DEFAULT_OPTIFINE_FILE;
    public static String titleSuccess = "OptiFine Installed";
    public static String titleFail = "OptiFine Download Failed";
    public static String lineSuccess1 = "OptiFine was downloaded to your mods folder.";
    public static String lineSuccess2 = "Please restart Minecraft to load it.";
    public static String lineFail1 = "OptiFine could not be downloaded.";
    public static String lineFail2 = "Check the log for details.";
    public static String buttonOpenMods = "Open Mods Folder";
    public static String buttonQuit = "Quit";
    public static String buttonContinue = "Continue";
    public static String successDetailFormat = "File: %s";
    public static String detailMcDirMissing = "Minecraft directory not available.";
    public static String detailModsDirFailed = "Failed to create mods directory.";
    public static String detailDownloadFailed = "Download failed.";
    public static String detailDownloadEmpty = "Downloaded file is empty.";
    public static String detailMoveFailed = "Failed to move OptiFine jar into place.";
    public static String detailUnknown = "Unknown error.";
    public static boolean showDetails = true;

    private OptiAutoFineConfig()
    {
    }

    public static void load(File configDir)
    {
        File configFile = new File(configDir, "optiautofine.cfg");
        Configuration config = new Configuration(configFile);
        config.load();

        optifineFile = config.getString("optifineFile", "general", DEFAULT_OPTIFINE_FILE,
                "OptiFine jar filename to download.");

        titleSuccess = config.getString("titleSuccess", "ui", titleSuccess, "Title when download succeeds.");
        titleFail = config.getString("titleFail", "ui", titleFail, "Title when download fails.");
        lineSuccess1 = config.getString("lineSuccess1", "ui", lineSuccess1, "Success line 1.");
        lineSuccess2 = config.getString("lineSuccess2", "ui", lineSuccess2, "Success line 2.");
        lineFail1 = config.getString("lineFail1", "ui", lineFail1, "Failure line 1.");
        lineFail2 = config.getString("lineFail2", "ui", lineFail2, "Failure line 2.");
        buttonOpenMods = config.getString("buttonOpenMods", "ui", buttonOpenMods, "Open mods button text.");
        buttonQuit = config.getString("buttonQuit", "ui", buttonQuit, "Quit button text.");
        buttonContinue = config.getString("buttonContinue", "ui", buttonContinue, "Continue button text.");
        successDetailFormat = config.getString("successDetailFormat", "ui", successDetailFormat,
            "Success detail format, uses %s.");
        detailMcDirMissing = config.getString("detailMcDirMissing", "ui", detailMcDirMissing,
            "Detail when mcDir is missing.");
        detailModsDirFailed = config.getString("detailModsDirFailed", "ui", detailModsDirFailed,
            "Detail when mods directory cannot be created.");
        detailDownloadFailed = config.getString("detailDownloadFailed", "ui", detailDownloadFailed,
            "Detail when download fails.");
        detailDownloadEmpty = config.getString("detailDownloadEmpty", "ui", detailDownloadEmpty,
            "Detail when download is empty.");
        detailMoveFailed = config.getString("detailMoveFailed", "ui", detailMoveFailed,
            "Detail when moving the jar fails.");
        detailUnknown = config.getString("detailUnknown", "ui", detailUnknown,
            "Detail when reason is unknown.");
        showDetails = config.getBoolean("showDetails", "ui", showDetails, "Show detail line.");

        if (config.hasChanged()) {
            config.save();
        }
    }

    public static String resolveFailureDetail(String detailCode)
    {
        if ("mcdir_missing".equals(detailCode)) {
            return detailMcDirMissing;
        }
        if ("modsdir_failed".equals(detailCode)) {
            return detailModsDirFailed;
        }
        if ("download_failed".equals(detailCode)) {
            return detailDownloadFailed;
        }
        if ("download_empty".equals(detailCode)) {
            return detailDownloadEmpty;
        }
        if ("move_failed".equals(detailCode)) {
            return detailMoveFailed;
        }
        return detailUnknown;
    }
}
