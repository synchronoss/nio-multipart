/*
 * Copyright 2002-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.synchronoss.cloud.nio.multipart.example.adapter;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import java.io.IOException;

/**
 * <p> Class taken from the spring framework project <a href="https://projects.spring.io/spring-framework/">Spring framework</a>
 */
public class RequestBodyPublisher extends AbstractRequestBodyPublisher {

    private final RequestBodyPublisher.RequestBodyReadListener readListener = new RequestBodyPublisher.RequestBodyReadListener();

    private final ServletInputStream inputStream;
    private final byte[] buffer;

    public RequestBodyPublisher(ServletInputStream inputStream, int bufferSize) {

        this.inputStream = inputStream;
        this.buffer = new byte[bufferSize];
    }

    public void registerListener() throws IOException {
        this.inputStream.setReadListener(this.readListener);
    }

    @Override
    protected void checkOnDataAvailable() {
        if (!this.inputStream.isFinished() && this.inputStream.isReady()) {
            onDataAvailable();
        }
    }

    @Override
    protected byte[] read() throws IOException {
        if (this.inputStream.isReady()) {
            int read = this.inputStream.read(buffer);
            if (logger.isTraceEnabled()) {
                logger.trace("read:" + read);
            }

            if (read > 0) {
                byte[] readData = new byte[read];
                System.arraycopy(readData, 0, readData, 0, read);
                return readData;
            }
        }
        return null;
    }


    private class RequestBodyReadListener implements ReadListener {

        @Override
        public void onDataAvailable() throws IOException {
            RequestBodyPublisher.this.onDataAvailable();
        }

        @Override
        public void onAllDataRead() throws IOException {
            RequestBodyPublisher.this.onAllDataRead();
        }

        @Override
        public void onError(Throwable throwable) {
            RequestBodyPublisher.this.onError(throwable);

        }
    }
}
