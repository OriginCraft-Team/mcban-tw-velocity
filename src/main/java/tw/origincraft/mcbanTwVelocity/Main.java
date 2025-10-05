package tw.origincraft.mcbanTwVelocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Continuation;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PreLoginEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import dev.dejvokep.boostedyaml.YamlDocument;
import dev.dejvokep.boostedyaml.dvs.versioning.BasicVersioning;
import dev.dejvokep.boostedyaml.settings.dumper.DumperSettings;
import dev.dejvokep.boostedyaml.settings.general.GeneralSettings;
import dev.dejvokep.boostedyaml.settings.loader.LoaderSettings;
import dev.dejvokep.boostedyaml.settings.updater.UpdaterSettings;
import net.kyori.adventure.text.Component;
import org.slf4j.Logger;
import tw.origincraft.mcbanTwVelocity.api.McBanTw;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

@Plugin(id = "mcban-tw-velocity", name = "mcban-tw-velocity", version = BuildConstants.VERSION, authors = {"OriginCraft"})
public class Main {

    private final Logger logger;
    private final ProxyServer server;
    private final Path dataDirectory;
    private boolean enabled = true;
    public static YamlDocument config;
    public static YamlDocument bannedPlayerSave;
    McBanTw mcBanTw = new McBanTw();

    @Inject
    public Main(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.logger = logger;
        this.server = server;
        this.dataDirectory = dataDirectory;

        // 載入配置檔與已封鎖玩家儲存檔
        loadConfig();
        loadBannedPlayerSave();

        // 如果未啟用插件就關閉
        if (!config.getBoolean("ban-enable")) {
            shutdownThisPlugin();
        }

        logger.info("McBanTw Velocity 插件已啟動!");
    }

    @Subscribe
    public void onLogin(PreLoginEvent event, Continuation continuation) throws IOException {
        if (!enabled) {
            continuation.resume();
            return;
        }
        // TODO: 解決踢出邏輯
        if (mcBanTw.isBanned(event.getUniqueId())) {
            event.setResult(PreLoginEvent.PreLoginComponentResult.denied(Component.text(config.getString("ban-message"))));
            continuation.resume();
        } else {
            continuation.resume();
        }
    }

    private void loadConfig() {
        try {
            // 創建配置檔案 (注意 BasicVersioning key 不要包含冒號)
            config = YamlDocument.create(new File(dataDirectory.toFile(), "config.yml"),
                    Objects.requireNonNull(getClass().getResourceAsStream("/config.yml")),
                    GeneralSettings.DEFAULT,
                    LoaderSettings.builder().setAutoUpdate(true).build(),
                    DumperSettings.DEFAULT,
                    UpdaterSettings.builder().setVersioning(new BasicVersioning("file-version"))
                            .setOptionSorting(UpdaterSettings.OptionSorting.SORT_BY_DEFAULTS).build());
            config.update();
            config.save();
        } catch (IOException e) {
            // 如果讀取不到檔案就關機
            logger.error("Error loading config.yml", e);
            shutdownThisPlugin();
        }
    }

    private void loadBannedPlayerSave() {
        try {
            // 創建儲存用檔案 (只儲存被封鎖玩家的 UUID 作為 key, 值為 true)
            bannedPlayerSave = YamlDocument.create(new File(dataDirectory.toFile(), "bannedPlayerSave.yml"));
            bannedPlayerSave.save();
        } catch (IOException e) {
            // 如果讀取不到檔案就關機
            logger.error("Error loading bannedPlayerSave.yml", e);
            shutdownThisPlugin();
        }
    }

    private void shutdownThisPlugin() {
        this.enabled = false;
        logger.warn("McBanTw 插件功能已停用");
    }
}
