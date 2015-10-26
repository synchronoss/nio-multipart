package com.synchronoss.cloud.nio.multipart;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * <p>
 *     A {@link DefaultBodyStreamFactory} that uses a temporary file as input and output
 * </p>
 * Created by sriz0001 on 18/10/2015.
 */
public class DefaultBodyStreamFactory implements BodyStreamFactory {

    private static final Logger log = LoggerFactory.getLogger(DefaultBodyStreamFactory.class);

    final File tempFolder;

    public DefaultBodyStreamFactory(final String tempFolderPath) {
        tempFolder = new File(tempFolderPath);
        if (!tempFolder.exists()){
            if (!tempFolder.mkdirs()){
                throw new IllegalStateException("Unable to create the temporary folder for the nio temp files");
            }
        }
        if(log.isDebugEnabled())log.debug("Temporary folder: " + tempFolder.getAbsolutePath());
    }

    public DefaultBodyStreamFactory() {
        this(System.getProperty("java.io.tmpdir") + "/nio-file-upload");
    }

    @Override
    public NamedOutputStreamHolder getOutputStream(final Map<String, List<String>> headers, int partIndex) {
        try {
            // TODO - An improvement can be if we can detect th size and decide if go to disk or not.
            // TODO - is the name random enough?
            final File tempFile = new File(tempFolder, String.format("nio-body-%d-%s.tmp", partIndex, UUID.randomUUID().toString()));
            return new NamedOutputStreamHolder(tempFile.getAbsolutePath(), new FileOutputStream(tempFile));

        }catch (Exception e){
            throw new IllegalStateException("Unable to create the temporary file where to store the body", e);
        }
    }

    @Override
    public InputStream getInputStream(final String outputStreamName) {
        try {
            return new FileInputStream(outputStreamName);
        }catch (Exception e){
            throw new IllegalStateException("Unable to create input stream from temporary file where the body is stored: temporary file: " + outputStreamName, e);
        }
    }
}
