package tw.origincraft.mcbanTwVelocity.api;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.NotNull;
import tw.origincraft.mcbanTwVelocity.Main;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URLConnection;
import java.util.UUID;

import static java.nio.charset.StandardCharsets.UTF_8;

public class McBanTw {
    private static final @NotNull Gson GSON = new Gson();

    public boolean isBanned(UUID uuid) throws IOException {
        if (Main.config.getBoolean("test")) {
            if (uuid.toString().equals(Main.config.getString("test-player"))) {
                writeBanRecord(uuid.toString());
                return true;
            }
        }
        // 先從本地快取查詢
        if (Main.bannedPlayerSave != null && Main.bannedPlayerSave.getBoolean(uuid.toString())) {
            return true;
        }

        String apiUrl = Main.config.getString("api-url");
        String serverId = Main.config.getString("server-id");

        URI uri = URI.create(apiUrl
                .replace("%server%", serverId)
                .replace("%player%", uuid.toString()));

        URLConnection connection = uri.toURL().openConnection();
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(10000);
        connection.setDoInput(true);
        connection.setDoOutput(false);
        JsonObject result;
        try (InputStreamReader input = new InputStreamReader(connection.getInputStream(), UTF_8)) {
            result = GSON.fromJson(input, JsonObject.class);
        }
        boolean banned = result.get("banned").getAsBoolean();

        // 如果被封鎖就寫入快取檔
        if (banned && Main.bannedPlayerSave != null) {
            writeBanRecord(uuid.toString());
        }

        return banned;
    }

    private void writeBanRecord(String uuid) {
        Main.bannedPlayerSave.set(uuid, true);
        try {
            Main.bannedPlayerSave.save();
        } catch (IOException e) {
            // 寫入失敗就記錄，但不影響回傳結果
            e.printStackTrace();
        }
    }
}
