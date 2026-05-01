package com.optiautofine.loader;

import cpw.mods.fml.relauncher.IFMLLoadingPlugin;
import cpw.mods.fml.relauncher.FMLRelaunchLog;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;
import net.minecraftforge.common.config.Configuration;

import com.optiautofine.OptiAutoFineConfig;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Map;

@IFMLLoadingPlugin.Name("OptiAutoFine")
@IFMLLoadingPlugin.MCVersion("1.7.10")
public class OptiAutoFineLoadingPlugin implements IFMLLoadingPlugin
{
    private static final String OPTIFINE_URL_PREFIX = "https://optifine.net/download?f=";
    private static final String DETAIL_MC_DIR = "mcdir_missing";
    private static final String DETAIL_MODS_DIR = "modsdir_failed";
    private static final String DETAIL_DOWNLOAD_FAILED = "download_failed";
    private static final String DETAIL_DOWNLOAD_EMPTY = "download_empty";
    private static final String DETAIL_MOVE_FAILED = "move_failed";
    public static final int STATUS_NONE = 0;
    public static final int STATUS_DOWNLOAD_OK = 1;
    public static final int STATUS_DOWNLOAD_FAILED = 2;

    private static int status = STATUS_NONE;
    private static String statusDetail;
    private static boolean attempted;

    @Override
    public String[] getASMTransformerClass()
    {
        return new String[0];
    }

    @Override
    public String getModContainerClass()
    {
        return null;
    }

    @Override
    public String getSetupClass()
    {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data)
    {
        if (attempted) {
            return;
        }
        attempted = true;

        File mcDir = (File) data.get("mcDir");
        if (mcDir == null) {
            mcDir = Launch.minecraftHome;
        }
        if (mcDir == null) {
            FMLRelaunchLog.severe("[OptiAutoFine] mcDir not available; cannot download OptiFine.");
            setStatusFailed(DETAIL_MC_DIR);
            return;
        }

        File modsDir = new File(mcDir, "mods");
        if (!modsDir.exists() && !modsDir.mkdirs()) {
            FMLRelaunchLog.severe("[OptiAutoFine] Failed to create mods directory: %s", modsDir.getAbsolutePath());
            setStatusFailed(DETAIL_MODS_DIR);
            return;
        }

        String optiFile = readOptiFineFile(mcDir);
        String optiUrl = OPTIFINE_URL_PREFIX + optiFile;
        File optiJar = new File(modsDir, optiFile);
        if (!optiJar.exists()) {
            if (!downloadOptiFine(optiJar, optiUrl)) {
                FMLRelaunchLog.severe("[OptiAutoFine] Download failed; OptiFine will not be available this session.");
                return;
            }
            setStatusOk(optiFile);
        }

        addToClasspath(optiJar);
    }

    @Override
    public String getAccessTransformerClass()
    {
        return null;
    }

    public static int getStatus()
    {
        return status;
    }

    public static String getStatusDetail()
    {
        return statusDetail;
    }

    private static boolean downloadOptiFine(File target, String downloadUrl)
    {
        File tempFile = new File(target.getParentFile(), target.getName() + ".part");
        if (tempFile.exists() && !tempFile.delete()) {
            FMLRelaunchLog.warning("[OptiAutoFine] Could not delete temp file: %s", tempFile.getAbsolutePath());
        }

        FMLRelaunchLog.info("[OptiAutoFine] Downloading OptiFine from %s", downloadUrl);

        InputStream input = null;
        OutputStream output = null;
        try {
            URLConnection connection = new URL(downloadUrl).openConnection();
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(30000);

            input = connection.getInputStream();
            output = new FileOutputStream(tempFile);

            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) != -1) {
                output.write(buffer, 0, read);
            }
        } catch (IOException ex) {
            FMLRelaunchLog.severe("[OptiAutoFine] Failed to download OptiFine: %s", ex);
            setStatusFailed(DETAIL_DOWNLOAD_FAILED);
            return false;
        } finally {
            closeQuietly(input);
            closeQuietly(output);
        }

        if (tempFile.length() == 0) {
            FMLRelaunchLog.severe("[OptiAutoFine] Downloaded file is empty.");
            setStatusFailed(DETAIL_DOWNLOAD_EMPTY);
            tempFile.delete();
            return false;
        }

        if (!moveFile(tempFile, target)) {
            FMLRelaunchLog.severe("[OptiAutoFine] Failed to move OptiFine jar into place.");
            setStatusFailed(DETAIL_MOVE_FAILED);
            return false;
        }

        FMLRelaunchLog.info("[OptiAutoFine] OptiFine downloaded to %s", target.getAbsolutePath());
        return true;
    }

    private static void addToClasspath(File jarFile)
    {
        try {
            LaunchClassLoader classLoader = Launch.classLoader;
            classLoader.addURL(jarFile.toURI().toURL());
            FMLRelaunchLog.info("[OptiAutoFine] Added OptiFine to classpath. If it is not detected, restart the game.");
        } catch (Exception ex) {
            FMLRelaunchLog.severe("[OptiAutoFine] Failed to add OptiFine to classpath. Restart required: %s", ex);
        }
    }

    private static boolean moveFile(File source, File target)
    {
        if (source.renameTo(target)) {
            return true;
        }

        InputStream input = null;
        OutputStream output = null;
        try {
            input = new FileInputStream(source);
            output = new FileOutputStream(target);

            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) != -1) {
                output.write(buffer, 0, read);
            }
        } catch (IOException ex) {
            FMLRelaunchLog.severe("[OptiAutoFine] Failed to copy OptiFine jar: %s", ex);
            return false;
        } finally {
            closeQuietly(input);
            closeQuietly(output);
        }

        if (!source.delete()) {
            FMLRelaunchLog.warning("[OptiAutoFine] Could not delete temp file: %s", source.getAbsolutePath());
        }

        return true;
    }

    private static void closeQuietly(InputStream input)
    {
        if (input != null) {
            try {
                input.close();
            } catch (IOException ignored) {
            }
        }
    }

    private static void closeQuietly(OutputStream output)
    {
        if (output != null) {
            try {
                output.close();
            } catch (IOException ignored) {
            }
        }
    }

    private static void setStatusOk(String message)
    {
        status = STATUS_DOWNLOAD_OK;
        statusDetail = message;
    }

    private static void setStatusFailed(String message)
    {
        status = STATUS_DOWNLOAD_FAILED;
        statusDetail = message;
    }

    private static String readOptiFineFile(File mcDir)
    {
        File configDir = new File(mcDir, "config");
        if (!configDir.exists() && !configDir.mkdirs()) {
            FMLRelaunchLog.warning("[OptiAutoFine] Could not create config directory: %s", configDir.getAbsolutePath());
        }
        File configFile = new File(configDir, "optiautofine.cfg");
        Configuration config = new Configuration(configFile);
        config.load();
        String value = config.getString("optifineFile", "general", OptiAutoFineConfig.DEFAULT_OPTIFINE_FILE,
                "OptiFine jar filename to download.");
        if (config.hasChanged()) {
            config.save();
        }
        return value;
    }
}
