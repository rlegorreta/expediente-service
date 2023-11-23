/* Copyright (c) 2023, LegoSoft Soluciones, S.C.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are not permitted.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 *  ExpedienteServiceApplication.kt
 *
 *  Developed 2023 by LegoSoftSoluciones, S.C. www.legosoft.com.mx
 */
package com.acme.expedienteservice

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.camunda.zeebe.spring.client.EnableZeebeClient
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.client.discovery.EnableDiscoveryClient
import org.springframework.cloud.context.config.annotation.RefreshScope
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder

/**
 * Expediente service.
 *
 * This server repo answers to all back end document administration operations. Its principal operations are:
 *
 * - Handles all service task from the document-flow bpms.
 * - Stores or updates the documents in Alfresco microservice.
 * - Do all necessary notification when something happened in the document.
 *
 * @author rlh
 * @project : expediente-service
 * @date November 2023
 *
 */
@SpringBootApplication
@ComponentScan(basePackages = ["com.acme.expedienteservice",
                               "com.ailegorreta.resourceserver.utils",
                               "io.camunda.zeebe.spring.client"])
@EnableZeebeClient
// @Deployment(resources = "classpath*:/bpmn/**/*.bpmn")
// ^ If we want to deploy process in Zeebe
class ExpedienteServiceApplication {
    companion object {

        @Bean
        fun kotlinPropertyConfigurer(): PropertySourcesPlaceholderConfigurer {
            val propertyConfigurer = PropertySourcesPlaceholderConfigurer()

            propertyConfigurer.setPlaceholderPrefix("@{")
            propertyConfigurer.setPlaceholderSuffix("}")
            propertyConfigurer.setIgnoreUnresolvablePlaceholders(true)

            return propertyConfigurer
        }

        @Bean
        fun defaultPropertyConfigurer() = PropertySourcesPlaceholderConfigurer()
    }

    @Bean
    fun mapperConfigurer() = Jackson2ObjectMapperBuilder().apply {
        serializationInclusion(JsonInclude.Include.NON_NULL)
        failOnUnknownProperties(true)
        featuresToDisable(*arrayOf(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS))
        indentOutput(true)
        modules(listOf(KotlinModule.Builder().build(), JavaTimeModule(), Jdk8Module()))
    }
}

fun main(args: Array<String>) {
    runApplication<ExpedienteServiceApplication>(*args)
}