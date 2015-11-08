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

import java.io.Closeable;

/**
 * <p> {@code ReadListenerDeferredResult} that is also a {@code Closeable}
 *
 * @author Silvano Riz.
 */
public abstract class CloseableReadListenerDeferredResult<T> extends ReadListenerDeferredResult<T> implements Closeable{

}
