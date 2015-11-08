/*
 * Copyright (C) 2008 The Guava Authors
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
package com.synchronoss.cloud.nio.multipart.util.collect;

import com.google.common.annotations.GwtCompatible;
import org.junit.Test;
import java.util.Iterator;
import java.util.NoSuchElementException;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * <p> Class taken from the google guava project <a href="https://code.google.com/p/guava-libraries/">guava</a>
 *
 * <p> Tests for {@link UnmodifiableIterator}.
 *
 * @author Jared Levy
 */
@GwtCompatible
public class UnmodifiableIteratorTest {

    @Test
    public void testRemove() {
        final String[] array = {"a", "b", "c"};

        Iterator<String> iterator = new UnmodifiableIterator<String>() {
            int i;
            @Override
            public boolean hasNext() {
                return i < array.length;
            }
            @Override
            public String next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                return array[i++];
            }
        };

        assertTrue(iterator.hasNext());
        assertEquals("a", iterator.next());
        try {
            iterator.remove();
            fail();
        } catch (UnsupportedOperationException expected) {}
    }
}
