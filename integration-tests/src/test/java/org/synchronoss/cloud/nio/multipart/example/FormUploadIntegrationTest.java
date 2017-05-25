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

package org.synchronoss.cloud.nio.multipart.example;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.synchronoss.cloud.nio.multipart.example.model.VerificationItem;
import org.synchronoss.cloud.nio.multipart.example.model.VerificationItems;
import org.synchronoss.cloud.nio.multipart.example.utils.FileUploadClient;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Silvano Riz
 */
@RunWith(Parameterized.class)
public class FormUploadIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(FormUploadIntegrationTest.class);

    @Parameterized.Parameters
    public static Collection data() {
        return getTestCases();
    }

    static class TestCase{
        private final Map<String, String> formParameters;
        private final String url;

        public TestCase(Map<String, String> formParameters, String url) {
            this.formParameters = formParameters;
            this.url = url;
        }

        @Override
        public String toString() {
            return "TestCase{" +
                    "formParameters=" + formParameters +
                    ", url='" + url + '\'' +
                    '}';
        }
    }

    private final FormUploadIntegrationTest.TestCase testCase;

    public FormUploadIntegrationTest(final FormUploadIntegrationTest.TestCase testCase){
        this.testCase = testCase;
    }

    @Test
    public void formUpload(){

        FileUploadClient fileUploadClient = new FileUploadClient();
        VerificationItems verificationItems = fileUploadClient.postForm(testCase.formParameters, testCase.url);
        List<VerificationItem> verificationItemList = verificationItems.getVerificationItems();
        for (VerificationItem verificationItem : verificationItemList){
            Assert.assertTrue(verificationItem.isFormField());
        }
    }

    static Collection<FormUploadIntegrationTest.TestCase> getTestCases(){
        try {

            final int applicationServerPort = Integer.parseInt(System.getProperty("application.server.port", "8080"));
            final String applicationServerHost = "localhost";

            List<FormUploadIntegrationTest.TestCase> testCases = new ArrayList<FormUploadIntegrationTest.TestCase>();
            for (FileUploadClient.Endpoint endpoint : FileUploadClient.Endpoint.values()){
                testCases.add(new TestCase(new HashMap<String, String>(){{
                    put("width", "1234");
                    put("height", "4321");
                }}, endpoint.getEndpoint(applicationServerHost, applicationServerPort)));
            }

            return testCases;

        }catch (Exception e){
            throw new IllegalStateException("Cannot find the test file", e);
        }
    }

}
