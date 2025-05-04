package org.bungeeChat.managers;

import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;
import org.bungeeChat.BungeeChat;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class ConfigurationManager {
    private final BungeeChat plugin;
    private Configuration config;
    private Configuration messages;
    private Configuration antiMessages;

    public ConfigurationManager(BungeeChat plugin) {
        this.plugin = plugin;
        loadConfigs();
    }

    private void loadConfigs() {
        try {
            // 创建插件数据文件夹
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdir();
            }

            // 加载主配置文件
            File configFile = new File(plugin.getDataFolder(), "config.yml");
            if (!configFile.exists()) {
                try (InputStream input = plugin.getResourceAsStream("config.yml")) {
                    Files.copy(input, configFile.toPath());
                }
            }
            config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(configFile);

            // 加载消息文件
            File messagesFile = new File(plugin.getDataFolder(), "messages.yml");
            if (!messagesFile.exists()) {
                try (InputStream input = plugin.getResourceAsStream("messages.yml")) {
                    Files.copy(input, messagesFile.toPath());
                }
            }
            messages = loadYamlWithUTF8(messagesFile);

            // 加载反垃圾消息文件
            File antiMessagesFile = new File(plugin.getDataFolder(), "antimessage.yml");
            if (!antiMessagesFile.exists()) {
                try (InputStream input = plugin.getResourceAsStream("antimessage.yml")) {
                    Files.copy(input, antiMessagesFile.toPath());
                }
            }
            antiMessages = ConfigurationProvider.getProvider(YamlConfiguration.class).load(antiMessagesFile);

        } catch (IOException e) {
            plugin.getLogger().severe("加载配置文件时出错: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void reloadConfigs() {
        loadConfigs();
    }

    public Configuration getConfig() {
        return config;
    }

    public Configuration getMessages() {
        return messages;
    }

    public Configuration getAntiMessages() {
        return antiMessages;
    }


    private Configuration loadYamlWithUTF8(File file) throws IOException {
        return ConfigurationProvider.getProvider(YamlConfiguration.class)
                .load(new InputStreamReader(
                        new FileInputStream(file),
                        StandardCharsets.UTF_8
                ));
    }
}