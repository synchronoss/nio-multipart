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

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * <p> AttachmentPart
 *
 * @author Silvano Riz.
 */
public class AttachmentPart implements Part{

    final Map<String, List<String>> headers;

    public AttachmentPart(final Map<String, List<String>> headers) {
        this.headers = headers;
    }

    public AttachmentPart() {
        this(Collections.<String, List<String>>emptyMap());
    }

    @Override
    public Type getType() {
        return Type.ATTACHMENT;
    }

    public Flux<byte[]> readBody(){
        // TODO
        return null;
    }

    public Mono<Void> write(final Publisher<byte[]> inputData){
        // TODO
        return null;
    }
}
