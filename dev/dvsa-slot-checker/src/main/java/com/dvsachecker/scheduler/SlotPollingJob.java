package com.dvsachecker.scheduler;

import com.dvsachecker.model.Slot;
import com.dvsachecker.repository.StateRepository;
import com.dvsachecker.service.NotificationService;
import com.dvsachecker.service.ScraperService;
import com.dvsachecker.service.SessionManager;
import org.openqa.selenium.WebDriver;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Quartz job implementation for periodic slot polling.
 * Orchestrates session management, scraping, state diff, and notifications.
 */
public class SlotPollingJob implements Job {
    private static final Logger logger = LoggerFactory.getLogger(SlotPollingJob.class);

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        logger.info("=== Starting slot polling cycle ===");
        long startTime = System.currentTimeMillis();

        // Retrieve dependencies from JobDataMap
        JobDataMap dataMap = context.getJobDetail().getJobDataMap();
        WebDriver driver = (WebDriver) dataMap.get("driver");
        SessionManager sessionManager = (SessionManager) dataMap.get("sessionManager");
        ScraperService scraperService = (ScraperService) dataMap.get("scraperService");
        StateRepository stateRepository = (StateRepository) dataMap.get("stateRepository");
        NotificationService notificationService = (NotificationService) dataMap.get("notificationService");

        try {
            // Step 1: Ensure valid session
            logger.debug("Validating session");
            sessionManager.ensureValidSession();

            // Step 2: Perform anti-detection actions
            scraperService.performAntiDetectionActions();

            // Step 3: Scrape available slots
            logger.debug("Scraping available slots");
            List<Slot> currentSlots = scraperService.scrapeAvailableSlots();
            logger.info("Found {} total slots", currentSlots.size());

            // Step 4: Compute diff with existing state
            logger.debug("Computing diff with stored state");
            List<Slot> newSlots = stateRepository.computeNewSlots(currentSlots);

            if (!newSlots.isEmpty()) {
                logger.info("Discovered {} new slots!", newSlots.size());

                // Step 5: Persist new slots
                stateRepository.persistSlots(newSlots);

                // Step 6: Send notifications
                notificationService.dispatchNotifications(newSlots);

                // Step 7: Mark as notified
                for (Slot slot : newSlots) {
                    stateRepository.markNotified(slot);
                }
            } else {
                logger.info("No new slots found in this cycle");
            }

            long duration = System.currentTimeMillis() - startTime;
            logger.info("=== Polling cycle completed in {}ms ===", duration);

        } catch (SessionManager.SessionException e) {
            logger.error("Session management failed", e);
            throw new JobExecutionException("Session error", e);

        } catch (ScraperService.ScrapingException e) {
            logger.error("Scraping failed", e);
            // Don't throw - allow scheduler to retry next cycle

        } catch (Exception e) {
            logger.error("Unexpected error during polling cycle", e);
            throw new JobExecutionException("Polling error", e);
        }
    }
}