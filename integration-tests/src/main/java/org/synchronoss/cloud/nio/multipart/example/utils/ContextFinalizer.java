/*
 * Copyright (C) 2015 Synchronoss Technologies
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.synchronoss.cloud.nio.multipart.example.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.stereotype.Component;

import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.Set;

/**
 * <p> Context finalizer
 *
 * @author Silvano Riz.
 */
@Component
public class ContextFinalizer implements ApplicationListener<ContextClosedEvent> {

    private static final Logger log = LoggerFactory.getLogger(ContextFinalizer.class);

    @Override
    public void onApplicationEvent(ContextClosedEvent event) {

        if (log.isInfoEnabled()) log.info("onApplicationEvent: " + event);

        Enumeration<Driver> drivers = DriverManager.getDrivers();
        Driver driver = null;
        while (drivers.hasMoreElements()) {
            try {
                driver = drivers.nextElement();
                DriverManager.deregisterDriver(driver);
                if (log.isWarnEnabled()) log.warn(String.format("Driver %s unregistered", driver));
            } catch (SQLException e) {
                if (log.isWarnEnabled()) log.warn(String.format("Error unregistering driver %s", driver), e);
            }
        }
        Set<Thread> threadSet = Thread.getAllStackTraces().keySet();
        Thread[] threadArray = threadSet.toArray(new Thread[threadSet.size()]);
        for (Thread thread : threadArray) {
            if (thread.getName().contains("Abandoned connection cleanup thread")) {
                synchronized (thread) {
                    thread.stop(); //don't complain, it works
                }
            }
        }
        if (log.isInfoEnabled()) log.info("Finished processing onApplicationEvent");
    }
}
