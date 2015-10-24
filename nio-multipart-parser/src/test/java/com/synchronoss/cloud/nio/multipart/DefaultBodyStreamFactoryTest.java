package com.synchronoss.cloud.nio.multipart;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.*;

/**
 * <p>
 *     Unit tests for {@link DefaultBodyStreamFactory}
 * </p>
 * Created by sriz0001 on 21/10/2015.
 */
public class DefaultBodyStreamFactoryTest {

    private static final Logger log = LoggerFactory.getLogger(DefaultBodyStreamFactoryTest.class);

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void testBodyStreams() throws Exception {

        BodyStreamFactory.PartOutputStream outputStream = null;
        try{
            DefaultBodyStreamFactory defaultBodyStreamFactory = new DefaultBodyStreamFactory();

            // Get the output stream
            outputStream = defaultBodyStreamFactory.getOutputStream(new HashMap<String, List<String>>(), 2);
            assertNotNull(outputStream);

            log.info("Part body output stream created with name: " + outputStream.getName());

            // Write some data
            byte[] dataToWrite = {0x01, 0x02, 0x03};
            writeToPartOutputStreamAndClose(outputStream, dataToWrite);

            // Get the input stream related to the PartOutputStream
            InputStream inputStream = defaultBodyStreamFactory.getInputStream(outputStream.getName());
            assertNotNull(inputStream);

            // Read back the data and verify
            byte[] readData = readFromInputStreamAndClose(inputStream);
            assertArrayEquals(dataToWrite, readData);

        }finally {

            // Clean up the temporary file...
            if (outputStream != null) {

                File tmpFileToCleanup = new File(outputStream.getName());
                if (tmpFileToCleanup.exists()) {
                    assertTrue(tmpFileToCleanup.delete());
                    log.info("Temp file deleted");
                }
            }

        }

    }

    @Test
    public void testBodyStreams_customFolder() throws Exception {

        DefaultBodyStreamFactory defaultBodyStreamFactory = new DefaultBodyStreamFactory(tempFolder.newFolder("body-parts").getAbsolutePath());

        // Get the output stream
        BodyStreamFactory.PartOutputStream outputStream = defaultBodyStreamFactory.getOutputStream(new HashMap<String, List<String>>(), 2);
        assertNotNull(outputStream);

        log.info("Part body output stream created with name: " + outputStream.getName());

        // Write some data
        byte[] dataToWrite = {0x01, 0x02, 0x03};
        writeToPartOutputStreamAndClose(outputStream, dataToWrite);

        // Get the input stream related to the PartOutputStream
        InputStream inputStream = defaultBodyStreamFactory.getInputStream(outputStream.getName());
        assertNotNull(inputStream);

        // Read back the data and verify
        byte[] readData = readFromInputStreamAndClose(inputStream);
        assertArrayEquals(dataToWrite, readData);

    }

    @Test
    public void testGetOutputStream_error() throws Exception {

        File folder = tempFolder.newFolder();
        DefaultBodyStreamFactory defaultBodyStreamFactory = new DefaultBodyStreamFactory(folder.getAbsolutePath());

        // Delete the folder to have an error when creating the output stream
        assertTrue(folder.delete());

        Exception expected = null;
        try{
            defaultBodyStreamFactory.getOutputStream(new HashMap<String, List<String>>(), 2);
        }catch (Exception e){
            expected = e;
        }
        assertNotNull(expected);
        assertTrue(expected instanceof IllegalStateException);

    }

    @Test
    public void testCreation_error() throws Exception {

        File folder = tempFolder.newFolder();

        Exception expected = null;
        try{
            assertTrue(folder.setWritable(false));
            new DefaultBodyStreamFactory(folder.getAbsolutePath() + "/body-parts" );
        }catch (Exception e){
            expected = e;
        }finally {
            assertTrue(folder.setWritable(true));
        }
        assertNotNull(expected);
        assertTrue(expected instanceof IllegalStateException);

    }

    @Test
    public void testGetInputStream_error() throws Exception {

        File folder = tempFolder.newFolder();
        DefaultBodyStreamFactory defaultBodyStreamFactory = new DefaultBodyStreamFactory(folder.getAbsolutePath());

        // Get the output stream
        BodyStreamFactory.PartOutputStream outputStream = defaultBodyStreamFactory.getOutputStream(new HashMap<String, List<String>>(), 2);
        assertNotNull(outputStream);

        log.info("Part body output stream created with name: " + outputStream.getName());

        // Write some data
        byte[] dataToWrite = {0x01, 0x02, 0x03};
        writeToPartOutputStreamAndClose(outputStream, dataToWrite);

        // Delete the folder to have an error when creating the input stream
        FileUtils.deleteDirectory(folder);
        assertFalse(folder.exists());

        Exception expected = null;
        try{
            defaultBodyStreamFactory.getInputStream(outputStream.getName());
        }catch (Exception e){
            expected = e;
        }
        assertNotNull(expected);
        assertTrue(expected instanceof IllegalStateException);

    }

    void writeToPartOutputStreamAndClose(final BodyStreamFactory.PartOutputStream outputStream, byte[] data){
        try{
            IOUtils.write(data, outputStream);

        }catch (Exception e){
            throw new IllegalStateException("Unable to write to PartOutputStream", e);
        }finally {
            IOUtils.closeQuietly(outputStream);
        }
    }

    byte[] readFromInputStreamAndClose(final InputStream inputStream){
        try{
            return IOUtils.toByteArray(inputStream);
        }catch (Exception e){
            throw new IllegalStateException("Unable to read from InputStream", e);
        }finally {
            IOUtils.closeQuietly(inputStream);
        }
    }

}