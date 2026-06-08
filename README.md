# OptiAutoFine

A Minecraft 1.7.10 mod, automatically downloads OptiFine on first Forge launch. Originally designed for use on maps by TUW Team

Every text and message can be edited by config `optiautofine.cfg`, this is used for language localization.

No plans to support higher versions, as higher versions have better alternatives for Optifine.

> [!NOTE]
> **v1.1 Changelog**
> Added fallback download support for OptiFine when the official direct link fails.  
> Added support for resolving randomized downloadx.php mirror links with f and x request parameters.  
> Added BMCLAPI mirror fallback for OptiFine downloads based on the configured OptiFine filename.  
> Improved download validation to ensure the downloaded file is a valid JAR instead of saving empty files or HTML error pages.  
> Improved logging for each download source and fallback attempt.  

<details>

<summary>Default configuration</summary>

```cfg
# Configuration file

general {
    # OptiFine jar filename to download. [default: OptiFine_1.7.10_HD_U_E7.jar]
    S:optifineFile=OptiFine_1.7.10_HD_U_E7.jar
}


ui {
    # Continue button text. [default: Continue]
    S:buttonContinue=Continue

    # Open mods button text. [default: Open Mods Folder]
    S:buttonOpenMods=Open Mods Folder

    # Quit button text. [default: Quit]
    S:buttonQuit=Quit

    # Detail when download is empty. [default: Downloaded file is empty.]
    S:detailDownloadEmpty=Downloaded file is empty.

    # Detail when download fails. [default: Download failed.]
    S:detailDownloadFailed=Download failed.

    # Detail when mcDir is missing. [default: Minecraft directory not available.]
    S:detailMcDirMissing=Minecraft directory not available.

    # Detail when mods directory cannot be created. [default: Failed to create mods directory.]
    S:detailModsDirFailed=Failed to create mods directory.

    # Detail when moving the jar fails. [default: Failed to move OptiFine jar into place.]
    S:detailMoveFailed=Failed to move OptiFine jar into place.

    # Detail when reason is unknown. [default: Unknown error.]
    S:detailUnknown=Unknown error.

    # Failure line 1. [default: OptiFine could not be downloaded.]
    S:lineFail1=OptiFine could not be downloaded.

    # Failure line 2. [default: Check the log for details.]
    S:lineFail2=Check the log for details.

    # Success line 1. [default: OptiFine was downloaded to your mods folder.]
    S:lineSuccess1=OptiFine was downloaded to your mods folder.

    # Success line 2. [default: Please restart Minecraft to load it.]
    S:lineSuccess2=Please restart Minecraft to load it.

    # Show detail line. [default: true]
    B:showDetails=true

    # Success detail format, uses %s. [default: File: %s]
    S:successDetailFormat=File: %s

    # Title when download fails. [default: OptiFine Download Failed]
    S:titleFail=OptiFine Download Failed

    # Title when download succeeds. [default: OptiFine Installed]
    S:titleSuccess=OptiFine Installed
}
```

</details>

---

一个 Minecraft 1.7.10 模组，在第一次 Forge 启动时会自动下载 OptiFine，最初设计为TUW团队的地图使用。

每条文本和消息都可以通过配置 `optiautofine.cfg` 进行编辑，可用于语言本地化。

没有计划支持更高版本，因为高版本有更好的Optifine替代品。

> [!NOTE]
> **v1.1 更新日志**
> 在官方直连失败时，新增了OptiFine的镜像下载支持。  
> 增加了解析随机downloadx.php镜像链路（`f`和`x`请求参数）的支持。  
> 添加了基于配置的的 BMCLAPI 镜像回退。  
> 改进了下载验证，以确保下载的文件是有效的 JAR，而不是保存空文件或 HTML 错误页面。  
> 改进了每个下载源和回退尝试的日志记录。

<details>

<summary>示例中文配置</summary>

```cfg
# Configuration file

general {
    # OptiFine jar filename to download. [default: OptiFine_1.7.10_HD_U_E7.jar]
    S:optifineFile=OptiFine_1.7.10_HD_U_E7.jar
}


ui {
    # Continue button text. [default: Continue]
    S:buttonContinue=继续

    # Open mods button text. [default: Open Mods Folder]
    S:buttonOpenMods=打开 Mods 文件夹

    # Quit button text. [default: Quit]
    S:buttonQuit=退出游戏

    # Detail when download is empty. [default: Downloaded file is empty.]
    S:detailDownloadEmpty=下载的文件为空！

    # Detail when download fails. [default: Download failed.]
    S:detailDownloadFailed=§c下载失败！

    # Detail when mcDir is missing. [default: Minecraft directory not available.]
    S:detailMcDirMissing=§cMinecraft 目录不存在！

    # Detail when mods directory cannot be created. [default: Failed to create mods directory.]
    S:detailModsDirFailed=§c创建 Mods 目录失败！

    # Detail when moving the jar fails. [default: Failed to move OptiFine jar into place.]
    S:detailMoveFailed=§c未能将 OptiFine 移动到位！

    # Detail when reason is unknown. [default: Unknown error.]
    S:detailUnknown=§c未知错误！

    # Failure line 1. [default: OptiFine could not be downloaded.]
    S:lineFail1=§c无法下载 OptiFine！

    # Failure line 2. [default: Check the log for details.]
    S:lineFail2=§c请检查游戏日志！

    # Success line 1. [default: OptiFine was downloaded to your mods folder.]
    S:lineSuccess1=§a§lOptiFine 已成功安装！

    # Success line 2. [default: Please restart Minecraft to load it.]
    S:lineSuccess2=§d§l请重启游戏以重新加载 OptiFine！

    # Show detail line. [default: true]
    B:showDetails=true

    # Success detail format, uses %s. [default: File: %s]
    S:successDetailFormat=文件名: %s

    # Title when download fails. [default: OptiFine Download Failed]
    S:titleFail=§6OptiAutoFine 提醒你：

    # Title when download succeeds. [default: OptiFine Installed]
    S:titleSuccess=§6OptiAutoFine 提醒你：
}
```

</details>