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
 *  ExpedienteControllerWebFluxTests.java
 *
 *  Developed 2023 by LegoSoftSoluciones, S.C. www.legosoft.com.mx
 */
package com.acme.expedienteservice.web;

import com.acme.expedienteservice.bpm.controller.ExpedienteController;
import com.acme.expedienteservice.bpm.controller.StartProcessRequestBody;
import com.acme.expedienteservice.config.*;
import com.ailegorreta.commons.utils.HasLogger;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockJwt;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.springSecurity;

/**
 * This class tests the REST calls received, mainly this test is to start a Camunda process BPM from the test
 * tool.
 *
 * @proyect: expediente-service
 * @author: rlh
 * @date: November 2023
 */
@WebFluxTest(ExpedienteController.class)
// @EnableTestContainers
@ExtendWith(MockitoExtension.class)
@Import({ServiceConfig.class, ResourceServerConfig.class, ExpedienteController.class})
@ActiveProfiles("integration-tests-webflux")            // This is to permit duplicate singleton beans
public class ExpedienteControllerWebFluxTests implements HasLogger {

    @Autowired
    ApplicationContext applicationContext;

    WebTestClient webTestClient;

    @MockBean
    private ReactiveJwtDecoder reactiveJwtDecoder;
    /* ^ Mocks the Reactive JwtDecoder so that the application does not try to call Spring Security Server and get the
     * public keys for decoding the Access Token  */
    @MockBean
    private StreamBridge streamBridge;

    @BeforeEach
    void setUp() {
        webTestClient = WebTestClient.bindToApplicationContext(applicationContext)
                // ^ add Spring Security test Support
                .apply(springSecurity())
                .configureClient()
                .build();
    }

    /**
     * This test is a utility to start Camunda BPM processes without the need to use the acme-ui micro-front
     *
     * note: This test work correctly but the initia
     */
    @Test
    void startCamundaBpmProcess() {
        var uri = UriComponentsBuilder.fromUriString("/expediente/startProcess");
        var variables = new HashMap<>(Map.ofEntries(
                                new AbstractMap.SimpleEntry<>("persona", "ACME Bodega SA de CV"),
                                new AbstractMap.SimpleEntry<>("fileId", "4b090521-f2b3-4083-a37c-1be01eb9036c"),
                                new AbstractMap.SimpleEntry<>("username", "adminACME"),
                                new AbstractMap.SimpleEntry<>("tipoDocumento", "VISA")
                            ));
        var processId = "recepcion-documento";
        var body = new StartProcessRequestBody(processId, variables);

        ProcessInstanceEvent res = webTestClient.mutateWith(mockJwt().authorities(Arrays.asList(new SimpleGrantedAuthority("SCOPE_acme.facultad"),
                                                                               new SimpleGrantedAuthority("ROLE_ADMINLEGO"))))
                                .post()
                                .uri(uri.build().toUri())
                                .accept(MediaType.APPLICATION_JSON)
                                .body(Mono.just(body), StartProcessRequestBody.class)
                                .exchange()
                                .expectStatus().isOk()
                                .expectBody(ProcessInstanceEvent.class)
                                .returnResult()
                                .getResponseBody();
        assertThat(res).isNotNull();
        assertThat(res.getBpmnProcessId()).isEqualTo(processId);
        getLogger().debug("The process has been initialized with the processID:" + res.getBpmnProcessId());
        getLogger().debug("    and with the instance number:" + res.getProcessInstanceKey());
        getLogger().debug("""
                In order to follow the process we need to go to the Camunda Operate portal (localhost:8081)
                and continue the process, i.e., ignore the stack trace from Spring that the bean Metrics was not found.
                """);
    }

    @NotNull
    @Override
    public Logger getLogger() { return HasLogger.DefaultImpls.getLogger(this); }

}
