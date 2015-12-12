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

package org.synchronoss.cloud.nio.multipart.example.config;

import org.synchronoss.cloud.nio.multipart.ChecksumPartBodyByteStoreFactory;
import org.synchronoss.cloud.nio.multipart.PartBodyByteStoreFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

/**
 * <p> Root application context
 *
 * @author Silvano Riz.
 */
@Configuration
@ComponentScan(basePackages = {"org.synchronoss.cloud.nio.multipart"})
@PropertySource("classpath:app.properties")
public class RootApplicationConfig {

    @Bean
    public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
        return new PropertySourcesPlaceholderConfigurer();
    }

    @Bean
    public static PartBodyByteStoreFactory partStreamsFactory(){
        return new ChecksumPartBodyByteStoreFactory("SHA-256");
    }
}
