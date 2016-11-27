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

package org.synchronoss.cloud.reactive.multipart;

import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * <p> FormPart
 *
 * @author Silvano Riz.
 */
public class FormPart implements Part{

    final Map<String, List<String>> headers;
    final String parameterName;
    final String parameterValue;

    public FormPart(final String parameterName, final String parameterValue) {
        this(parameterName, parameterValue, Collections.<String, List<String>>emptyMap());
    }

    public FormPart(final String parameterName, final String parameterValue, final Map<String, List<String>> headers) {
        this.parameterName = parameterName;
        this.parameterValue = parameterValue;
        this.headers = headers;
    }

    @Override
    public Type getType() {
        return Type.ATTACHMENT;
    }

    public Mono<String> getParameterName() {
        return Mono.just(parameterName);
    }

    public Mono<String> getParameterValue() {
        return Mono.just(parameterValue);
    }

    public Mono<Map<String, List<String>>> getHeaders() {
        return Mono.just(headers);
    }
}
