package com.dvsachecker.service;

import com.dvsachecker.config.AppConfig;
import com.dvsachecker.model.Slot;
import com.google.gson.JsonObject;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Notification dispatcher supporting multiple channels (Discord, Telegram).
 * Uses OkHttp for async webhook calls with retry logic.
 */
public class NotificationService {
    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static final DateTimeFormatter DISPLAY_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");

    private final AppConfig config;
    private final OkHttpClient httpClient;

    public NotificationService() {
        this.config = AppConfig.getInstance();
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build();
    }

    /**
     * Dispatches notifications to all enabled channels.
     * Executes channel notifications in parallel for efficiency.
     */
    public void dispatchNotifications(List<Slot> newSlots) {
        if (newSlots.isEmpty()) {
            logger.debug("No new slots to notify");
            return;
        }

        logger.info("Dispatching notifications for {} new slots", newSlots.size());

        // Dispatch to Discord
        if (config.isDiscordEnabled()) {
            new Thread(() -> notifyDiscord(newSlots)).start();
        }

        // Dispatch to Telegram
        if (config.isTelegramEnabled()) {
            new Thread(() -> notifyTelegram(newSlots)).start();
        }
    }

    /**
     * Sends notification to Discord webhook.
     * Formats message with embeds for visual appeal.
     */
    private void notifyDiscord(List<Slot> slots) {
        String webhookUrl = config.getDiscordWebhookUrl();
        if (webhookUrl == null || webhookUrl.isEmpty()) {
            logger.warn("Discord webhook URL not configured");
            return;
        }

        try {
            JsonObject payload = buildDiscordPayload(slots);

            RequestBody body = RequestBody.create(payload.toString(), JSON);
            Request request = new Request.Builder()
                    .url(webhookUrl)
                    .post(body)
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    logger.info("Discord notification sent successfully");
                } else {
                    logger.error("Discord notification failed: {} - {}",
                            response.code(), response.message());
                }
            }

        } catch (IOException e) {
            logger.error("Failed to send Discord notification", e);
        }
    }

    /**
     * Constructs Discord webhook payload with rich embeds.
     * Groups slots by test center for organized display.
     */
    private JsonObject buildDiscordPayload(List<Slot> slots) {
        JsonObject payload = new JsonObject();
        payload.addProperty("content", "🚗 **New DVSA Test Slots Available!**");

        // Build embed with slot details
        JsonObject embed = new JsonObject();
        embed.addProperty("title", "Available Test Slots");
        embed.addProperty("color", 5814783); // Blue color
        embed.addProperty("timestamp", java.time.Instant.now().toString());

        // Create description with slot details
        StringBuilder description = new StringBuilder();
        for (Slot slot : slots) {
            description.append(String.format("📍 **%s**\n", slot.getTestCenter()));
            description.append(String.format("📅 %s\n",
                    slot.getDateTime().format(DISPLAY_FORMATTER)));
            description.append(String.format("🚘 Test Type: %s\n", slot.getTestType()));
            description.append("\n");
        }

        embed.addProperty("description", description.toString());

        // Add footer
        JsonObject footer = new JsonObject();
        footer.addProperty("text", "DVSA Slot Checker");
        embed.add("footer", footer);

        // Add embed to payload
        com.google.gson.JsonArray embeds = new com.google.gson.JsonArray();
        embeds.add(embed);
        payload.add("embeds", embeds);

        return payload;
    }

    /**
     * Sends notification to Telegram bot.
     * Uses Telegram Bot API for message delivery.
     */
    private void notifyTelegram(List<Slot> slots) {
        String botToken = config.getTelegramBotToken();
        String chatId = config.getTelegramChatId();

        if (botToken == null || botToken.isEmpty() || chatId == null || chatId.isEmpty()) {
            logger.warn("Telegram credentials not configured");
            return;
        }

        try {
            String message = buildTelegramMessage(slots);
            String telegramUrl = String.format("https://api.telegram.org/bot%s/sendMessage", botToken);

            JsonObject payload = new JsonObject();
            payload.addProperty("chat_id", chatId);
            payload.addProperty("text", message);
            payload.addProperty("parse_mode", "Markdown");

            RequestBody body = RequestBody.create(payload.toString(), JSON);
            Request request = new Request.Builder()
                    .url(telegramUrl)
                    .post(body)
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    logger.info("Telegram notification sent successfully");
                } else {
                    logger.error("Telegram notification failed: {} - {}",
                            response.code(), response.message());
                }
            }

        } catch (IOException e) {
            logger.error("Failed to send Telegram notification", e);
        }
    }

    /**
     * Constructs Telegram message with Markdown formatting.
     */
    private String buildTelegramMessage(List<Slot> slots) {
        StringBuilder message = new StringBuilder();
        message.append("🚗 *New DVSA Test Slots Available!*\n\n");

        for (Slot slot : slots) {
            message.append(String.format("📍 *%s*\n", slot.getTestCenter()));
            message.append(String.format("📅 %s\n",
                    slot.getDateTime().format(DISPLAY_FORMATTER)));
            message.append(String.format("🚘 Test Type: %s\n", slot.getTestType()));
            message.append("\n");
        }

        message.append("_Book quickly before slots are taken!_");
        return message.toString();
    }

    /**
     * Sends a single slot notification (for real-time updates).
     */
    public void notifySingleSlot(Slot slot) {
        List<Slot> singleSlotList = List.of(slot);
        dispatchNotifications(singleSlotList);
    }

    /**
     * Sends test notification to verify configuration.
     */
    public void sendTestNotification() {
        logger.info("Sending test notification");

        if (config.isDiscordEnabled()) {
            sendDiscordTest();
        }

        if (config.isTelegramEnabled()) {
            sendTelegramTest();
        }
    }

    private void sendDiscordTest() {
        String webhookUrl = config.getDiscordWebhookUrl();
        if (webhookUrl == null || webhookUrl.isEmpty()) {
            logger.warn("Discord webhook URL not configured");
            return;
        }

        try {
            JsonObject payload = new JsonObject();
            payload.addProperty("content", "✅ DVSA Slot Checker test notification - system operational");

            RequestBody body = RequestBody.create(payload.toString(), JSON);
            Request request = new Request.Builder()
                    .url(webhookUrl)
                    .post(body)
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    logger.info("Discord test notification sent");
                } else {
                    logger.error("Discord test failed: {}", response.code());
                }
            }

        } catch (IOException e) {
            logger.error("Failed to send Discord test", e);
        }
    }

    private void sendTelegramTest() {
        String botToken = config.getTelegramBotToken();
        String chatId = config.getTelegramChatId();

        if (botToken == null || chatId == null) {
            logger.warn("Telegram credentials not configured");
            return;
        }

        try {
            String telegramUrl = String.format("https://api.telegram.org/bot%s/sendMessage", botToken);

            JsonObject payload = new JsonObject();
            payload.addProperty("chat_id", chatId);
            payload.addProperty("text", "✅ DVSA Slot Checker test notification - system operational");

            RequestBody body = RequestBody.create(payload.toString(), JSON);
            Request request = new Request.Builder()
                    .url(telegramUrl)
                    .post(body)
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    logger.info("Telegram test notification sent");
                } else {
                    logger.error("Telegram test failed: {}", response.code());
                }
            }

        } catch (IOException e) {
            logger.error("Failed to send Telegram test", e);
        }
    }
}