package com.dvsachecker.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

public class AppConfig {
    private static final Logger logger = LoggerFactory.getLogger(AppConfig.class);
    private static AppConfig instance;

    private final Properties properties;

    private AppConfig() {
        this.properties = new Properties();
        loadConfiguration();
    }

    public static synchronized AppConfig getInstance() {
        if (instance == null) {
            instance = new AppConfig();
        }
        return instance;
    }

    private void loadConfiguration() {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("application.properties")) {
            if (input == null) {
                throw new IOException("application.properties not found in classpath");
            }
            properties.load(input);
            logger.info("Configuration loaded successfully");
        } catch (IOException e) {
            logger.error("Failed to load configuration file", e);
            throw new RuntimeException("Configuration initialization failed", e);
        }
    }

    // DVSA Credentials - ONLY LICENSE NUMBER
    public String getDvsaLicenseNumber() {
        return properties.getProperty("dvsa.license.number");
    }

    public boolean hasDisability() {
        return Boolean.parseBoolean(properties.getProperty("dvsa.has.disability", "false"));
    }

    // Test Center Configuration
    public List<String> getTargetTestCenters() {
        String centers = properties.getProperty("dvsa.test.centers", "");
        return Arrays.asList(centers.split(","));
    }

    // Date Range Configuration
    public LocalDate getSearchStartDate() {
        String date = properties.getProperty("dvsa.search.start.date", LocalDate.now().toString());
        return LocalDate.parse(date);
    }

    public LocalDate getSearchEndDate() {
        String date = properties.getProperty("dvsa.search.end.date", LocalDate.now().plusMonths(3).toString());
        return LocalDate.parse(date);
    }

    // Polling Configuration
    public int getPollingIntervalSeconds() {
        return Integer.parseInt(properties.getProperty("polling.interval.seconds", "30"));
    }

    public int getPollingRandomnessSeconds() {
        return Integer.parseInt(properties.getProperty("polling.randomness.seconds", "10"));
    }

    // Selenium Configuration
    public boolean isHeadlessMode() {
        return Boolean.parseBoolean(properties.getProperty("selenium.headless", "true"));
    }

    public int getPageLoadTimeoutSeconds() {
        return Integer.parseInt(properties.getProperty("selenium.page.load.timeout", "30"));
    }

    public int getImplicitWaitSeconds() {
        return Integer.parseInt(properties.getProperty("selenium.implicit.wait", "10"));
    }

    // Notification Configuration
    public String getDiscordWebhookUrl() {
        return properties.getProperty("notification.discord.webhook");
    }

    public boolean isDiscordEnabled() {
        return Boolean.parseBoolean(properties.getProperty("notification.discord.enabled", "false"));
    }

    public String getTelegramBotToken() {
        return properties.getProperty("notification.telegram.token");
    }

    public String getTelegramChatId() {
        return properties.getProperty("notification.telegram.chat.id");
    }

    public boolean isTelegramEnabled() {
        return Boolean.parseBoolean(properties.getProperty("notification.telegram.enabled", "false"));
    }

    // Database Configuration
    public String getDatabasePath() {
        return properties.getProperty("database.path", "dvsa-slots.db");
    }
}



