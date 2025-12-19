package com.ronin.therapeuticdev.metrics;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.Disposable;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

///  setting up now the service that should collect coding metrics in real time
/// my listener components would feed raw data here, rule based system should be enough and the class
//accumulates them into snapshots.

// gonna have to use the service level app to tell intelliJ this is a singleton service scoped to the entire application not per project.
@Service(Service.Level.APP)
public final class MetricCollector implements Disposable

{
    private final String sessionId;
    private final Instant sessionStart;

    // THEREPEUTIC DEV : TYPING METRICS
    private final AtomicInteger keystrokeCount = new AtomicInteger(0);
    private final AtomicInteger backspcCount = new AtomicInteger(0);
    private final AtomicLong lastKeyStrokeTimems = new AtomicLong(0);

    //THEREPEUTIC DEV : CONTEXT SWITCHING /FILE FOCUS (could be useful for a ui idea)
    private final AtomicInteger fileChangeCount = new AtomicInteger(0);
    private final AtomicLong  currentFileStartms =new AtomicLong(0);
    private AtomicInteger focusLostCount = new AtomicInteger(0);

    //THEREPEUTIC DEV: ERROR METRICS

    private final AtomicInteger syntaxErrCount = new AtomicInteger(0);
    private final AtomicInteger compilationErrorCount = new AtomicInteger(0);
    private final AtomicLong LastErrorTimems = new AtomicLong(0);

    //THEREPEUTIC DEV: BUILD METRICS

    private volatile boolean lastBuildSuccess = true;
    //VOLATILE - i only use it because its ligher in weight and guarantees visibility across threads, especially with plugin management
    //when one thread writes, all other threads see the updated value IMMIDIENTLY, so basically im already optimising my programming with this
    //now where should i log this....
    private final AtomicInteger addedonFailedBuilds = new  AtomicInteger(0);
    private final AtomicLong LastBuildTimems = new AtomicLong(0);







    //establishing here a session with unique id and timestamp
    //construtor will be called by intellijs service system

    public MetricCollector()
    {
        this.sessionId = UUID.randomUUID().toString();
        this.sessionStart = Instant.now();
    }

    public void recKeystroke (long timestampMs)
    {
        keystrokeCount.incrementAndGet();
        lastKeyStrokeTimems.set(timestampMs);

        //EZZZZZ
    }

    public void recBackspc (long timestampMs)
    {
        backspcCount.incrementAndGet();
        lastKeyStrokeTimems.set(timestampMs);
    }


    public long getKeyboardIdlems()
    {
        long LastInteraction = lastKeyStrokeTimems.get();
        if (LastInteraction == 0)
        {
            return 0;
        }
        return System.currentTimeMillis() - LastInteraction;
    }

    // THEREPETUIC DEV CODE SECTION FOR ERROR RECORDING

    public void recSyntaxErr()
    {
        syntaxErrCount.incrementAndGet();
        LastErrorTimems.set(System.currentTimeMillis());
    }

    public void recCompilationError(int errorCount)
    {
        compilationErrorCount.addAndGet(errorCount);
        LastErrorTimems.set(System.currentTimeMillis());
    }

    public long  getTimeSinceLastError()
    {
        long lastError = LastErrorTimems.get();
        if(lastError == 0)
        {
            return Long.MAX_VALUE;
        }
        return System.currentTimeMillis() - lastError; //last error should be recorded in ms since its a var which is equal to the lasterrortimems
    }

    // THEREPEUTIC DEV CODE SECTION FOR FILE FOCUS RECORDING

    public void recFileChange()
    {
        fileChangeCount.incrementAndGet();
        currentFileStartms.set(System.currentTimeMillis());
    }

    public void recFocusLoss()
    {
        focusLostCount.incrementAndGet();
    }

    public long getTimeInCurrentFileMs()
    {
        long startingTime = currentFileStartms.get();
        if(startingTime == 0)
        {
            return 0;
        }
        return System.currentTimeMillis() - startingTime;
    }

    // THEREPEUTIC DEV CODE SECTION FOR BUILD METRICS
    public void recBuildResult(boolean success)
    {
        LastBuildTimems.set(System.currentTimeMillis());
        lastBuildSuccess = success;

        if(success)
        {
            addedonFailedBuilds.set(0); //bascially reset it back to 0 for the streak on any level of success with failing
        } else
        {
            addedonFailedBuilds.incrementAndGet(); //builds up the streak
            //could atttach the increment with ui desing too later down the line
        }
    }

    public long getTimeSinceLastBuildMs()
    {
        long lastBuild = LastBuildTimems.get();
        if(lastBuild == 0)
        {
            return Long.MAX_VALUE;
            //ill study more into the utilisation of max value longs being returned
        }
        return System.currentTimeMillis() - lastBuild;
    }
    public boolean wasLastBuildSuccessful()
    {
        return lastBuildSuccess; //adds on the streak (maybe more logic can be put here)
    }

    public int getAddedOnFailedBuilds()
    {
        return addedonFailedBuilds.get();
    }



    @Override
    public void dispose()
    {
        //should i add persistence or nah? ask supervisro maybe

    }





}
