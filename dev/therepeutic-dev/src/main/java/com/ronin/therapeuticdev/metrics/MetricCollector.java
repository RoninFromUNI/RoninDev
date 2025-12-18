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
    @Override
    public void dispose()
    {
        //should i add persistence or nah? ask supervisro maybe

    }





}
