package com.dvsachecker.service;

import com.dvsachecker.config.AppConfig;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

/**
 * Manages DVSA authentication flow.
 * Only requires: driving license number + disability question.
 */
public class SessionManager {
    private static final Logger logger = LoggerFactory.getLogger(SessionManager.class);

    private static final String DVSA_START_URL = "https://driverpracticaltest.dvsa.gov.uk/application";

    private final WebDriver driver;
    private final AppConfig config;
    private boolean isAuthenticated = false;

    public SessionManager(WebDriver driver) {
        this.driver = driver;
        this.config = AppConfig.getInstance();
    }

    /**
     * Authenticates through DVSA multi-step wizard.
     * Step 1: Select car test type
     * Step 2: Enter license number + answer questions
     */
    public void authenticate() throws SessionException {
        try {
            logger.info("Initiating DVSA authentication");
            driver.get(DVSA_START_URL);

            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));

            // STEP 1: Select "Car (manual and automatic)"
            logger.debug("Step 1: Selecting car test type");
            WebElement carOption = wait.until(
                    ExpectedConditions.elementToBeClickable(By.linkText("Car (manual and automatic)"))
            );
            carOption.click();
            Thread.sleep(1000);

            // STEP 2: Enter license details
            logger.debug("Step 2: Entering license details");

            WebElement licenseField = wait.until(
                    ExpectedConditions.presenceOfElementLocated(By.id("driving-licence"))
            );
            licenseField.clear();
            licenseField.sendKeys(config.getDvsaLicenseNumber());
            logger.debug("Entered license number");

            // Select "No" for extended test
            try {
                WebElement extendedTestNo = driver.findElement(
                        By.cssSelector("input[name='extendedTest'][value='false']")
                );
                if (!extendedTestNo.isSelected()) {
                    extendedTestNo.click();
                }
                logger.debug("Selected No for extended test");
            } catch (NoSuchElementException e) {
                logger.debug("Extended test radio not found");
            }

            // Select "No" for special needs
            try {
                WebElement specialNeedsNo = driver.findElement(By.id("special-needs-none"));
                if (!specialNeedsNo.isSelected()) {
                    specialNeedsNo.click();
                }
                logger.debug("Selected No for special needs");
            } catch (NoSuchElementException e) {
                logger.debug("Special needs radio not found");
            }

            // Click Continue
            WebElement continueButton = wait.until(
                    ExpectedConditions.elementToBeClickable(By.id("driving-licence-submit"))
            );
            continueButton.click();
            logger.debug("Submitted license details");

            Thread.sleep(2000);

            // Verify authentication
            String currentUrl = driver.getCurrentUrl();
            if (currentUrl.contains("application") || currentUrl.contains("booking") || currentUrl.contains("test-centre")) {
                isAuthenticated = true;
                logger.info("Authentication successful - reached: {}", currentUrl);
            } else {
                throw new SessionException("Unexpected URL after authentication: " + currentUrl);
            }

        } catch (TimeoutException e) {
            logger.error("Authentication timeout - page elements not found", e);
            throw new SessionException("Authentication failed: timeout", e);
        } catch (NoSuchElementException e) {
            logger.error("Authentication failed - element not found", e);
            throw new SessionException("Authentication failed: element not found", e);
        } catch (Exception e) {
            logger.error("Unexpected error during authentication", e);
            throw new SessionException("Authentication failed: unexpected error", e);
        }
    }

    /**
     * Validates current session by checking URL.
     */
    public boolean validateSession() {
        if (!isAuthenticated) {
            return false;
        }

        try {
            String currentUrl = driver.getCurrentUrl();

            // If we're back at the start page (execution=e1s1), session expired
            if (currentUrl.contains("/application?execution=") && currentUrl.contains("s1")) {
                logger.warn("Session expired - back at start page");
                isAuthenticated = false;
                return false;
            }

            return true;

        } catch (Exception e) {
            logger.warn("Session validation failed", e);
            isAuthenticated = false;
            return false;
        }
    }

    /**
     * Ensures valid session, re-authenticating if necessary.
     */
    public void ensureValidSession() throws SessionException {
        if (!validateSession()) {
            logger.info("Invalid session detected, re-authenticating");
            authenticate();
        }
    }

    public boolean isAuthenticated() {
        return isAuthenticated;
    }

    public static class SessionException extends Exception {
        public SessionException(String message) {
            super(message);
        }

        public SessionException(String message, Throwable cause) {
            super(message, cause);
}
}
}