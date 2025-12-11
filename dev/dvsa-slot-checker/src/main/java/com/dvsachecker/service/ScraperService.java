package com.dvsachecker.service;

import com.dvsachecker.config.AppConfig;
import com.dvsachecker.model.Slot;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ScraperService {
    private static final Logger logger = LoggerFactory.getLogger(ScraperService.class);

    private static final String BOOKING_SEARCH_URL = "https://driverpracticaltest.dvsa.gov.uk/application?execution=e1s2";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private final WebDriver driver;
    private final AppConfig config;
    private final Random random;

    public ScraperService(WebDriver driver) {
        this.driver = driver;
        this.config = AppConfig.getInstance();
        this.random = new Random();
    }

    public List<Slot> scrapeAvailableSlots() throws ScrapingException {
        List<Slot> allSlots = new ArrayList<>();
        List<String> testCenters = config.getTargetTestCenters();

        for (String testCenter : testCenters) {
            try {
                logger.info("Scraping slots for test center: {}", testCenter);
                List<Slot> centerSlots = scrapeTestCenter(testCenter);
                allSlots.addAll(centerSlots);

                simulateHumanDelay();

            } catch (Exception e) {
                logger.error("Failed to scrape test center: {}", testCenter, e);
            }
        }

        logger.info("Total slots found: {}", allSlots.size());
        return allSlots;
    }

    private List<Slot> scrapeTestCenter(String testCenter) throws ScrapingException {
        List<Slot> slots = new ArrayList<>();

        try {
            driver.get(BOOKING_SEARCH_URL);
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));

            WebElement changeTestCenterLink = wait.until(
                    ExpectedConditions.elementToBeClickable(By.linkText("Change test centre"))
            );
            changeTestCenterLink.click();

            WebElement testCenterInput = wait.until(
                    ExpectedConditions.presenceOfElementLocated(By.id("test-centre-search"))
            );
            testCenterInput.clear();
            testCenterInput.sendKeys(testCenter);

            WebElement searchButton = driver.findElement(By.cssSelector("button[type='submit']"));
            searchButton.click();

            WebElement testCenterResult = wait.until(
                    ExpectedConditions.elementToBeClickable(
                            By.xpath(String.format("//a[contains(text(), '%s')]", testCenter))
                    )
            );
            testCenterResult.click();

            LocalDate startDate = config.getSearchStartDate();
            LocalDate endDate = config.getSearchEndDate();

            List<WebElement> availableDates = driver.findElements(
                    By.cssSelector(".available-date, .slot-available")
            );

            for (WebElement dateElement : availableDates) {
                try {
                    String dateStr = dateElement.getAttribute("data-date");
                    if (dateStr == null || dateStr.isEmpty()) {
                        dateStr = dateElement.getText();
                    }

                    LocalDate slotDate = LocalDate.parse(dateStr, DATE_FORMATTER);

                    if (slotDate.isBefore(startDate) || slotDate.isAfter(endDate)) {
                        continue;
                    }

                    dateElement.click();
                    Thread.sleep(500);

                    List<Slot> dateSlots = extractTimeSlotsForDate(testCenter, slotDate);
                    slots.addAll(dateSlots);

                    driver.navigate().back();
                    Thread.sleep(300);

                } catch (Exception e) {
                    logger.warn("Failed to process date element", e);
                }
            }

        } catch (TimeoutException e) {
            logger.error("Timeout while scraping test center: {}", testCenter, e);
            throw new ScrapingException("Scraping timeout", e);
        } catch (NoSuchElementException e) {
            logger.error("Element not found while scraping: {}", testCenter, e);
            throw new ScrapingException("Element not found", e);
        } catch (Exception e) {
            logger.error("Unexpected error while scraping: {}", testCenter, e);
            throw new ScrapingException("Unexpected scraping error", e);
        }

        return slots;
    }

    private List<Slot> extractTimeSlotsForDate(String testCenter, LocalDate date) {
        List<Slot> slots = new ArrayList<>();

        try {
            List<WebElement> timeSlots = driver.findElements(
                    By.cssSelector(".time-slot, .slot-time, .available-time")
            );

            for (WebElement timeSlot : timeSlots) {
                try {
                    String timeStr = timeSlot.getText().trim();
                    String slotId = timeSlot.getAttribute("data-slot-id");

                    if (slotId == null || slotId.isEmpty()) {
                        slotId = generateSlotId(testCenter, date, timeStr);
                    }

                    LocalDateTime dateTime = parseDateTime(date, timeStr);

                    String testType = timeSlot.getAttribute("data-test-type");
                    if (testType == null || testType.isEmpty()) {
                        testType = "car";
                    }

                    Slot slot = new Slot(testCenter, dateTime, slotId, testType);
                    slots.add(slot);

                } catch (Exception e) {
                    logger.warn("Failed to parse time slot element", e);
                }
            }

        } catch (Exception e) {
            logger.warn("Failed to extract time slots for date: {}", date, e);
        }

        return slots;
    }

    private LocalDateTime parseDateTime(LocalDate date, String timeStr) {
        timeStr = timeStr.replaceAll("[^0-9:]", "").trim();

        try {
            return LocalDateTime.of(date,
                    java.time.LocalTime.parse(timeStr, TIME_FORMATTER));
        } catch (Exception e) {
            logger.warn("Failed to parse time: {}, using default", timeStr);
            return date.atTime(9, 0); // Default to 9am
        }
    }

    private String generateSlotId(String testCenter, LocalDate date, String time) {
        return String.format("%s-%s-%s",
                testCenter.replaceAll("[^a-zA-Z0-9]", ""),
                date.toString(),
                time.replaceAll("[^0-9]", ""));
    }

    private void simulateHumanDelay() {
        try {
            int baseDelay = 1000;
            int variance = random.nextInt(2000);
            Thread.sleep(baseDelay + variance);

            if (random.nextDouble() < 0.1) {
                Thread.sleep(random.nextInt(3000) + 2000);
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("Delay interrupted", e);
        }
    }

    public void performAntiDetectionActions() {
        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;

            int scrollAmount = random.nextInt(500) + 100;
            js.executeScript(String.format("window.scrollBy(0, %d)", scrollAmount));

            Thread.sleep(random.nextInt(500) + 200);

            js.executeScript(String.format("window.scrollBy(0, -%d)", scrollAmount / 2));

        } catch (Exception e) {
            logger.debug("Anti-detection action failed (non-critical)", e);
        }
    }

    public static class ScrapingException extends Exception {
        public ScrapingException(String message) {
            super(message);
        }

        public ScrapingException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}