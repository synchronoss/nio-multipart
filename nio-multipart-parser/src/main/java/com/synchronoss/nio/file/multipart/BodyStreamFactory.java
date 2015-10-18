package com.synchronoss.nio.file.multipart;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

/**
 * <p>
 *     Factory for the part body stream.
 * </p>
 * Created by sriz0001 on 18/10/2015.
 */
public interface BodyStreamFactory {

    /**
     * <p>
     *     A part {@link OutputStream}. Essentially a named {@link OutputStream}
     * </p>
     */
    abstract class PartOutputStream extends OutputStream {

        final String name;
        public PartOutputStream(String name) {
            this.name = name;
        }

        /**
         * <p>
         *     Returns the name of the stream
         * </p>
         * @return The name of the stream.
         */
        public String getName() {
            return name;
        }
    }

    /**
     * <p>
     *     Gets the {@link PartOutputStream} where to store the part body.
     * </p>
     * @param headers The headers of the processed part
     * @param partIndex The index of the processed part.
     * @return The {@link PartOutputStream} where to store the part body.
     */
    PartOutputStream getOutputStream(final Map<String, List<String>> headers, final int partIndex);

    /**
     * <p>
     *     Returns the {@link InputStream} to read the part body.
     * </p>
     * @param outputStreamName The name of the of the {@link PartOutputStream} created by the {@link #getOutputStream(Map, int)}
     * @return The {@link InputStream} from where to read the part body.
     */
    InputStream getInputStream(final String outputStreamName);

}
