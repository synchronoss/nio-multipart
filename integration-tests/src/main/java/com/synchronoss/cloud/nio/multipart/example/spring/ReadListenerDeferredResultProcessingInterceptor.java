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
package com.synchronoss.cloud.nio.multipart.example.spring;

import com.synchronoss.cloud.nio.multipart.example.spring.ReadListenerDeferredResult;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.context.request.async.DeferredResultProcessingInterceptor;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;

import javax.servlet.ServletRequest;

/**
 * <p> {@code DeferredResultProcessingInterceptor} that attaches the {@link ReadListenerDeferredResult} to the
 *     {@link javax.servlet.ServletInputStream}.
 *     This interceptor needs to be configured in the spring context. See {@link AsyncSupportConfigurer}
 *
 * @author Silvano Riz.
 */
public class ReadListenerDeferredResultProcessingInterceptor implements DeferredResultProcessingInterceptor{

    @Override
    public <T> void beforeConcurrentHandling(NativeWebRequest request, DeferredResult<T> deferredResult) throws Exception {

    }

    @Override
    public <T> void preProcess(NativeWebRequest request, DeferredResult<T> deferredResult) throws Exception {
        if (deferredResult instanceof ReadListenerDeferredResult){
            // If the deferredResult is a ReadListenerDeferredResult (which is a ReadListener) attach it to the InputStream
            final ReadListenerDeferredResult readListenerDeferredResult = (ReadListenerDeferredResult)deferredResult;
            request.getNativeRequest(ServletRequest.class).getInputStream().setReadListener(readListenerDeferredResult);
        }
    }

    @Override
    public <T> void postProcess(NativeWebRequest request, DeferredResult<T> deferredResult, Object concurrentResult) throws Exception {

    }

    @Override
    public <T> boolean handleTimeout(NativeWebRequest request, DeferredResult<T> deferredResult) throws Exception {
        return false;
    }

    @Override
    public <T> void afterCompletion(NativeWebRequest request, DeferredResult<T> deferredResult) throws Exception {

    }
}
