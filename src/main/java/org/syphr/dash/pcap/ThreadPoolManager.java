/**
 * Copyright (c) 2014-2017 by the respective copyright holders.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.syphr.dash.pcap;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class ThreadPoolManager
{
    protected static final int DEFAULT_THREAD_POOL_SIZE = 5;
    protected static final long THREAD_TIMEOUT = 65L;

    /**
     * Returns an instance of a scheduled thread pool service. If it is the first
     * request for the given pool name, the instance is newly created.
     *
     * @param poolName a short name used to identify the pool, e.g. "discovery"
     *
     * @return an instance to use
     */
    static public ScheduledExecutorService getScheduledPool(String poolName)
    {
        ScheduledExecutorService pool = Executors.newScheduledThreadPool(DEFAULT_THREAD_POOL_SIZE,
                                                                         new NamedThreadFactory(poolName));
        ((ThreadPoolExecutor) pool).setKeepAliveTime(THREAD_TIMEOUT, TimeUnit.SECONDS);
        ((ThreadPoolExecutor) pool).allowCoreThreadTimeOut(true);
        return pool;
    }

    /**
     * This is a normal thread factory, which adds a named prefix to all created
     * threads.
     */
    protected static class NamedThreadFactory implements ThreadFactory
    {

        protected final ThreadGroup group;
        protected final AtomicInteger threadNumber = new AtomicInteger(1);
        protected final String namePrefix;
        protected final String name;

        public NamedThreadFactory(String threadPool)
        {
            this.name = threadPool;
            this.namePrefix = "DASH-" + threadPool + "-";
            group = Thread.currentThread().getThreadGroup();
        }

        @Override
        public Thread newThread(Runnable r)
        {
            Thread t = new Thread(group, r, namePrefix + threadNumber.getAndIncrement(), 0);
            if (!t.isDaemon()) {
                t.setDaemon(true);
            }
            if (t.getPriority() != Thread.NORM_PRIORITY) {
                t.setPriority(Thread.NORM_PRIORITY);
            }

            return t;
        }

        public String getName()
        {
            return name;
        }
    }
}
