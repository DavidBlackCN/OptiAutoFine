package com.optiautofine.loader;

import cpw.mods.fml.relauncher.IFMLLoadingPlugin;
import cpw.mods.fml.relauncher.FMLRelaunchLog;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;
import net.minecraftforge.common.config.Configuration;

import com.optiautofine.OptiAutoFineConfig;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Map;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@IFMLLoadingPlugin.Name("OptiAutoFine")
@IFMLLoadingPlugin.MCVersion("1.7.10")
public class OptiAutoFineLoadingPlugin implements IFMLLoadingPlugin
{
    private static final String OPTIFINE_BASE_URL = "https://optifine.net/";
    private static final String OPTIFINE_DIRECT_URL_PREFIX = "https://optifine.net/download?f=";
    private static final String OPTIFINE_ADLOADX_URL_PREFIX = "https://optifine.net/adloadx?f=";
    private static final String OPTIFINED_BASE_URL = "https://optifined.net/";
    private static final String OPTIFINED_ADLOADX_URL_PREFIX = "https://optifined.net/adloadx.php?f=";
    private static final String BMCLAPI_OPTIFINE_URL_PREFIX = "https://bmclapi2.bangbang93.com/optifine/";
    private static final int CONNECT_TIMEOUT = 10000;
    private static final int READ_TIMEOUT = 30000;
    private static final int ADLOADX_READ_LIMIT = 1024 * 1024;
    private static final Pattern HREF_PATTERN = Pattern.compile("href\\s*=\\s*([\"'])(.*?)\\1", Pattern.CASE_INSENSITIVE);
    private static final Pattern OPTIFINE_FILE_PATTERN = Pattern.compile("^OptiFine_([^_]+)_(.+)_([^_]+)\\.jar$");
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
        File optiJar = new File(modsDir, optiFile);
        if (!optiJar.exists()) {
            if (!downloadOptiFine(optiJar, optiFile)) {
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

    private static boolean downloadOptiFine(File target, String optiFile)
    {
        File tempFile = new File(target.getParentFile(), target.getName() + ".part");
        String[] failureDetail = new String[] { DETAIL_DOWNLOAD_FAILED };
        if (tempFile.exists() && !tempFile.delete()) {
            FMLRelaunchLog.warning("[OptiAutoFine] Could not delete temp file: %s", tempFile.getAbsolutePath());
        }

        if (tryDownloadCandidate(tempFile, "official direct", buildOfficialDirectUrl(optiFile), failureDetail)) {
            return moveDownloadedFile(tempFile, target);
        }

        String randomizedUrl = resolveRandomizedOptiFineUrl(optiFile, OPTIFINE_ADLOADX_URL_PREFIX, OPTIFINE_BASE_URL, "official randomized");
        if (randomizedUrl != null && tryDownloadCandidate(tempFile, "official randomized", randomizedUrl, failureDetail)) {
            return moveDownloadedFile(tempFile, target);
        }

        String mirrorRandomizedUrl = resolveRandomizedOptiFineUrl(optiFile, OPTIFINED_ADLOADX_URL_PREFIX, OPTIFINED_BASE_URL, "OptiFine mirror randomized");
        if (mirrorRandomizedUrl != null && tryDownloadCandidate(tempFile, "OptiFine mirror randomized", mirrorRandomizedUrl, failureDetail)) {
            return moveDownloadedFile(tempFile, target);
        }

        String bmclApiUrl = buildBmclApiUrl(optiFile);
        if (bmclApiUrl != null && tryDownloadCandidate(tempFile, "BMCLAPI mirror", bmclApiUrl, failureDetail)) {
            return moveDownloadedFile(tempFile, target);
        }

        FMLRelaunchLog.severe("[OptiAutoFine] All OptiFine download attempts failed.");
        setStatusFailed(failureDetail[0]);
        return false;
    }

    private static boolean tryDownloadCandidate(File tempFile, String sourceName, String downloadUrl, String[] failureDetail)
    {
        if (tempFile.exists() && !tempFile.delete()) {
            FMLRelaunchLog.warning("[OptiAutoFine] Could not delete temp file before trying %s: %s", sourceName, tempFile.getAbsolutePath());
        }

        FMLRelaunchLog.info("[OptiAutoFine] Downloading OptiFine from %s: %s", sourceName, downloadUrl);

        InputStream input = null;
        OutputStream output = null;
        boolean downloadFailed = false;
        try {
            URLConnection connection = openConnection(downloadUrl);
            input = connection.getInputStream();
            output = new FileOutputStream(tempFile);

            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) != -1) {
                output.write(buffer, 0, read);
            }
        } catch (IOException ex) {
            FMLRelaunchLog.warning("[OptiAutoFine] OptiFine download from %s failed: %s", sourceName, ex);
            failureDetail[0] = DETAIL_DOWNLOAD_FAILED;
            downloadFailed = true;
        } finally {
            closeQuietly(input);
            closeQuietly(output);
        }
        if (downloadFailed) {
            deleteTempFile(tempFile);
            return false;
        }

        if (tempFile.length() == 0) {
            FMLRelaunchLog.warning("[OptiAutoFine] OptiFine download from %s returned an empty file.", sourceName);
            deleteTempFile(tempFile);
            failureDetail[0] = DETAIL_DOWNLOAD_EMPTY;
            return false;
        }

        if (!isValidJar(tempFile)) {
            FMLRelaunchLog.warning("[OptiAutoFine] OptiFine download from %s was not a valid jar.", sourceName);
            deleteTempFile(tempFile);
            failureDetail[0] = DETAIL_DOWNLOAD_FAILED;
            return false;
        }

        return true;
    }

    private static boolean moveDownloadedFile(File tempFile, File target)
    {
        if (!moveFile(tempFile, target)) {
            FMLRelaunchLog.severe("[OptiAutoFine] Failed to move OptiFine jar into place.");
            setStatusFailed(DETAIL_MOVE_FAILED);
            return false;
        }

        FMLRelaunchLog.info("[OptiAutoFine] OptiFine downloaded to %s", target.getAbsolutePath());
        return true;
    }

    private static URLConnection openConnection(String downloadUrl) throws IOException
    {
        URLConnection connection = new URL(downloadUrl).openConnection();
        connection.setRequestProperty("User-Agent", "Mozilla/5.0");
        connection.setConnectTimeout(CONNECT_TIMEOUT);
        connection.setReadTimeout(READ_TIMEOUT);
        if (connection instanceof HttpURLConnection) {
            HttpURLConnection httpConnection = (HttpURLConnection) connection;
            httpConnection.setInstanceFollowRedirects(true);
            int responseCode = httpConnection.getResponseCode();
            if (responseCode >= 400) {
                throw new IOException("HTTP " + responseCode);
            }
        }
        return connection;
    }

    private static String resolveRandomizedOptiFineUrl(String optiFile, String adloadUrlPrefix, String baseUrl, String sourceName)
    {
        String adloadUrl = adloadUrlPrefix + encodeQueryValue(optiFile);
        String html;
        try {
            FMLRelaunchLog.info("[OptiAutoFine] Resolving %s download link: %s", sourceName, adloadUrl);
            html = readUrlToString(adloadUrl);
        } catch (IOException ex) {
            FMLRelaunchLog.warning("[OptiAutoFine] Failed to resolve %s download link: %s", sourceName, ex);
            return null;
        }

        Matcher matcher = HREF_PATTERN.matcher(html);
        while (matcher.find()) {
            String href = unescapeHtml(matcher.group(2));
            if (href.indexOf("downloadx") == -1) {
                continue;
            }

            try {
                URL resolved = new URL(new URL(baseUrl), href);
                if (isValidRandomizedOptiFineUrl(resolved, optiFile, new URL(baseUrl).getHost())) {
                    FMLRelaunchLog.info("[OptiAutoFine] Resolved %s download link: %s", sourceName, resolved.toString());
                    return resolved.toString();
                }
            } catch (IOException ex) {
                FMLRelaunchLog.warning("[OptiAutoFine] Ignoring invalid OptiFine download link: %s", href);
            }
        }

        FMLRelaunchLog.warning("[OptiAutoFine] No valid %s download link found.", sourceName);
        return null;
    }

    private static boolean isValidRandomizedOptiFineUrl(URL url, String optiFile, String expectedHost)
    {
        String path = url.getPath();
        if (!"https".equalsIgnoreCase(url.getProtocol())) {
            return false;
        }
        if (!expectedHost.equalsIgnoreCase(url.getHost())) {
            return false;
        }
        if (path == null || (!path.endsWith("/downloadx") && !path.endsWith("/downloadx.php"))) {
            return false;
        }

        String file = parseQueryParameter(url.getQuery(), "f");
        String token = parseQueryParameter(url.getQuery(), "x");
        return optiFile.equals(file) && token != null && token.length() > 0;
    }

    private static String buildOfficialDirectUrl(String optiFile)
    {
        return OPTIFINE_DIRECT_URL_PREFIX + encodeQueryValue(optiFile);
    }

    private static String buildBmclApiUrl(String optiFile)
    {
        Matcher matcher = OPTIFINE_FILE_PATTERN.matcher(optiFile);
        if (!matcher.matches()) {
            FMLRelaunchLog.warning("[OptiAutoFine] Could not parse OptiFine filename for BMCLAPI mirror: %s", optiFile);
            return null;
        }

        String minecraftVersion = matcher.group(1);
        String type = matcher.group(2);
        String patch = matcher.group(3);
        if (!isSafePathSegment(minecraftVersion) || !isSafePathSegment(type) || !isSafePathSegment(patch)) {
            FMLRelaunchLog.warning("[OptiAutoFine] OptiFine filename contains unsafe mirror path segments: %s", optiFile);
            return null;
        }

        return BMCLAPI_OPTIFINE_URL_PREFIX + minecraftVersion + "/" + type + "/" + patch;
    }

    private static boolean isSafePathSegment(String value)
    {
        if (value == null || value.length() == 0 || value.indexOf('/') != -1 || value.indexOf('\\') != -1 || value.indexOf("..") != -1) {
            return false;
        }
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (!(c >= 'A' && c <= 'Z') && !(c >= 'a' && c <= 'z') && !(c >= '0' && c <= '9') && c != '.' && c != '_' && c != '-') {
                return false;
            }
        }
        return true;
    }

    private static boolean isValidJar(File file)
    {
        JarFile jarFile = null;
        try {
            jarFile = new JarFile(file);
            return true;
        } catch (IOException ex) {
            return false;
        } finally {
            if (jarFile != null) {
                try {
                    jarFile.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    private static String readUrlToString(String url) throws IOException
    {
        InputStream input = null;
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try {
            URLConnection connection = openConnection(url);
            input = connection.getInputStream();
            byte[] buffer = new byte[4096];
            int read;
            int total = 0;
            while ((read = input.read(buffer)) != -1) {
                total += read;
                if (total > ADLOADX_READ_LIMIT) {
                    throw new IOException("response too large");
                }
                output.write(buffer, 0, read);
            }
        } finally {
            closeQuietly(input);
        }
        return output.toString("UTF-8");
    }

    private static String parseQueryParameter(String query, String key)
    {
        if (query == null) {
            return null;
        }

        String[] parts = query.split("&");
        for (int i = 0; i < parts.length; i++) {
            int equalsIndex = parts[i].indexOf('=');
            String parameterKey;
            String parameterValue;
            if (equalsIndex == -1) {
                parameterKey = decodeQueryValue(parts[i]);
                parameterValue = "";
            } else {
                parameterKey = decodeQueryValue(parts[i].substring(0, equalsIndex));
                parameterValue = decodeQueryValue(parts[i].substring(equalsIndex + 1));
            }
            if (key.equals(parameterKey)) {
                return parameterValue;
            }
        }
        return null;
    }

    private static String encodeQueryValue(String value)
    {
        try {
            return URLEncoder.encode(value, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            return value;
        }
    }

    private static String decodeQueryValue(String value)
    {
        try {
            return URLDecoder.decode(value, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            return value;
        }
    }

    private static String unescapeHtml(String value)
    {
        return value.replace("&amp;", "&").replace("&#38;", "&");
    }

    private static void deleteTempFile(File tempFile)
    {
        if (tempFile.exists() && !tempFile.delete()) {
            FMLRelaunchLog.warning("[OptiAutoFine] Could not delete temp file: %s", tempFile.getAbsolutePath());
        }
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
