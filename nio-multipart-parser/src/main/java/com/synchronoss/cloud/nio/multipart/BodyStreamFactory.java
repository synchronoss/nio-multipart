package com.synchronoss.cloud.nio.multipart;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

/**
 * <p>
 *     Factory for the part body streams.
 * </p>
 * Created by sriz0001 on 18/10/2015.
 */
public interface BodyStreamFactory {

    /**
     * <p>
     *     Holds an {@link OutputStream} and it's name
     *     The name will be used to retrieve the correspondent {@link InputStream} by calling {@link BodyStreamFactory#getInputStream(String)}
     * </p>
     */
    class NamedOutputStreamHolder {

        final String name;
        final OutputStream outputStream;
        public NamedOutputStreamHolder(final String name, final OutputStream outputStream) {
            this.name = name;
            this.outputStream = outputStream;
        }

        /**
         * <p>
         *     Returns the name of the {@link OutputStream}
         * </p>
         * @return The name of the stream.
         */
        public String getName() {
            return name;
        }

        /**
         * <p>
         *     Returns the {@link OutputStream}
         * </p>
         * @return the {@link OutputStream}
         */
        public OutputStream getOutputStream() {
            return outputStream;
        }
    }

    /**
     * <p>
     *     Gets the {@link NamedOutputStreamHolder} where to store the part body.
     * </p>
     * @param headers The headers of the processed part
     * @param partIndex The index of the processed part.
     * @return The {@link NamedOutputStreamHolder} holding the {@link OutputStream} where to store the part body.
     */
    NamedOutputStreamHolder getOutputStream(final Map<String, List<String>> headers, final int partIndex);

    /**
     * <p>
     *     Returns the {@link InputStream} to read the part body.
     * </p>
     * @param outputStreamName The name of the of the {@link NamedOutputStreamHolder} created by the {@link #getOutputStream(Map, int)}
     * @return The {@link InputStream} from where to read the part body.
     */
    InputStream getInputStream(final String outputStreamName);

}
