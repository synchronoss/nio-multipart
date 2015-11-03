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
package com.synchronoss.cloud.nio.multipart.example.config;

import com.synchronoss.cloud.nio.multipart.example.spring.ReadListenerDeferredResultProcessingInterceptor;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import org.springframework.web.servlet.view.InternalResourceViewResolver;
import org.springframework.web.servlet.view.JstlView;

/**
 * <p>
 *     Web config
 * </p>
 *
 * @author Silvano Riz.
 */
@Configuration
@EnableWebMvc
@ComponentScan(basePackages = {"com.synchronoss.cloud.nio.multipart.example.web"})
public class WebConfig extends WebMvcConfigurerAdapter {

    @Bean
    public MessageSource messageSource() {
        ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();
        messageSource.setBasename("classpath:messages");
        messageSource.setDefaultEncoding("UTF-8");
        messageSource.setCacheSeconds(0);
        return messageSource;
    }

    @Bean
    public ViewResolver viewResolver() {
        InternalResourceViewResolver resolver = new InternalResourceViewResolver();
        resolver.setPrefix("/WEB-INF/views/");
        resolver.setSuffix(".jsp");
        resolver.setCache(false);
        resolver.setViewClass(JstlView.class);
        return resolver;
    }

    @Override
    public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
//        <mvc:annotation-driven>
//        <mvc:async-support default-timeout="2500" task-executor="executor">
//        <mvc:callable-interceptors>
//        <bean class="org.springframework.web.servlet.config.MvcNamespaceTests.TestCallableProcessingInterceptor" />
//        </mvc:callable-interceptors>
//        <mvc:deferred-result-interceptors>
//        <bean class="org.springframework.web.servlet.config.MvcNamespaceTests.TestDeferredResultProcessingInterceptor" />
//        </mvc:deferred-result-interceptors>
//        </mvc:async-support>
//        </mvc:annotation-driven>
        configurer.registerDeferredResultInterceptors(new ReadListenerDeferredResultProcessingInterceptor());
    }

}