package com.dvsachecker;

import com.dvsachecker.config.AppConfig;
import com.dvsachecker.repository.StateRepository;
import com.dvsachecker.scheduler.SlotPollingJob;
import com.dvsachecker.service.NotificationService;
import com.dvsachecker.service.ScraperService;
import com.dvsachecker.service.SessionManager;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Scanner;

/**
 * Main application entry point for DVSA Slot Checker.
 * Orchestrates WebDriver initialization, service setup, and Quartz scheduling.
 */
public class DVSASlotCheckerApp {
    private static final Logger logger = LoggerFactory.getLogger(DVSASlotCheckerApp.class);

    private WebDriver driver;
    private Scheduler scheduler;
    private final AppConfig config;

    public DVSASlotCheckerApp() {
        this.config = AppConfig.getInstance();
    }

    /**
     * Initializes Selenium WebDriver with anti-detection configurations.
     */
    private void initializeWebDriver() {
        logger.info("Initializing WebDriver");

        // WebDriverManager automatically downloads and configures ChromeDriver
        WebDriverManager.chromedriver().setup();

        ChromeOptions options = new ChromeOptions();

        // Headless mode configuration
        if (config.isHeadlessMode()) {
            options.addArguments("--headless=new");
            logger.info("Running in headless mode");
        }

        // Anti-detection arguments
        options.addArguments("--disable-blink-features=AutomationControlled");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-gpu");
        options.addArguments("--window-size=1920,1080");

        // Set realistic user agent
        options.addArguments("user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");

        // Disable automation flags
        options.setExperimentalOption("excludeSwitches", new String[]{"enable-automation"});
        options.setExperimentalOption("useAutomationExtension", false);

        driver = new ChromeDriver(options);

        // Configure timeouts
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(config.getPageLoadTimeoutSeconds()));
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(config.getImplicitWaitSeconds()));

        logger.info("WebDriver initialized successfully");
    }

    /**
     * Initializes Quartz scheduler with polling job configuration.
     */
    private void initializeScheduler() throws SchedulerException {
        logger.info("Initializing Quartz scheduler");

        // Create scheduler
        SchedulerFactory schedulerFactory = new StdSchedulerFactory();
        scheduler = schedulerFactory.getScheduler();

        // Initialize services
        SessionManager sessionManager = new SessionManager(driver);
        ScraperService scraperService = new ScraperService(driver);
        StateRepository stateRepository = new StateRepository();
        NotificationService notificationService = new NotificationService();

        // Create job with service dependencies
        JobDetail job = JobBuilder.newJob(SlotPollingJob.class)
                .withIdentity("slotPollingJob", "dvsaGroup")
                .build();

        // Add services to job data map
        job.getJobDataMap().put("driver", driver);
        job.getJobDataMap().put("sessionManager", sessionManager);
        job.getJobDataMap().put("scraperService", scraperService);
        job.getJobDataMap().put("stateRepository", stateRepository);
        job.getJobDataMap().put("notificationService", notificationService);

        // Create trigger with configured interval
        int pollingInterval = config.getPollingIntervalSeconds();

        Trigger trigger = TriggerBuilder.newTrigger()
                .withIdentity("slotPollingTrigger", "dvsaGroup")
                .startNow()
                .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                        .withIntervalInSeconds(pollingInterval)
                        .repeatForever())
                .build();

        scheduler.scheduleJob(job, trigger);

        logger.info("Scheduler configured with polling interval: {}s", pollingInterval);
    }

    /**
     * Starts the application and begins polling.
     */
    public void start() {
        try {
            logger.info("=== DVSA Slot Checker Starting ===");

            // Initialize components
            initializeWebDriver();
            initializeScheduler();

            // Perform initial authentication
            logger.info("Performing initial authentication");
            SessionManager sessionManager = new SessionManager(driver);
            sessionManager.authenticate();

            // Send test notification
            logger.info("Sending test notification");
            NotificationService notificationService = new NotificationService();
            notificationService.sendTestNotification();

            // Start scheduler
            scheduler.start();
            logger.info("=== Scheduler started - monitoring for slots ===");

            // Keep application running
            waitForShutdown();

        } catch (Exception e) {
            logger.error("Application startup failed", e);
            shutdown();
            System.exit(1);
        }
    }

    /**
     * Waits for user shutdown command or system interrupt.
     */
    private void waitForShutdown() {
        logger.info("Application running. Type 'quit' or 'exit' to stop, 'status' for info");

        Scanner scanner = new Scanner(System.in);
        while (true) {
            String input = scanner.nextLine().trim().toLowerCase();

            switch (input) {
                case "quit":
                case "exit":
                    logger.info("Shutdown command received");
                    shutdown();
                    return;

                case "status":
                    printStatus();
                    break;

                case "test":
                    logger.info("Sending test notification");
                    new NotificationService().sendTestNotification();
                    break;

                default:
                    logger.warn("Unknown command: {}. Available: quit, exit, status, test", input);
            }
        }
    }

    /**
     * Prints current application status.
     */
    private void printStatus() {
        try {
            StateRepository repo = new StateRepository();
            int slotCount = repo.getAllSlots().size();

            logger.info("=== Application Status ===");
            logger.info("Scheduler running: {}", scheduler.isStarted());
            logger.info("Total slots tracked: {}", slotCount);
            logger.info("Polling interval: {}s", config.getPollingIntervalSeconds());
            logger.info("Test centers: {}", config.getTargetTestCenters());
            logger.info("========================");

        } catch (Exception e) {
            logger.error("Failed to retrieve status", e);
        }
    }

    /**
     * Gracefully shuts down application resources.
     */
    public void shutdown() {
        logger.info("Shutting down application");

        try {
            if (scheduler != null && scheduler.isStarted()) {
                scheduler.shutdown(true); // Wait for running jobs
                logger.info("Scheduler stopped");
            }

            if (driver != null) {
                driver.quit();
                logger.info("WebDriver closed");
            }

        } catch (Exception e) {
            logger.error("Error during shutdown", e);
        }

        logger.info("=== DVSA Slot Checker Stopped ===");
    }

    /**
     * Main entry point.
     */
    public static void main(String[] args) {
        // Register shutdown hook for graceful termination
        DVSASlotCheckerApp app = new DVSASlotCheckerApp();
        Runtime.getRuntime().addShutdownHook(new Thread(app::shutdown));

        // Start application
        app.start();
    }
}