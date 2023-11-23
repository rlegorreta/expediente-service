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
 *  EventService.kt
 *
 *  Developed 2023 by LegoSoftSoluciones, S.C. www.legosoft.com.mx
 */
package com.acme.expedienteservice.service.event

import com.acme.expedienteservice.config.ServiceConfig
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.ailegorreta.commons.event.EventDTO
import com.ailegorreta.commons.event.EventType
import com.ailegorreta.commons.utils.HasLogger
import org.springframework.cloud.stream.function.StreamBridge
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Service

/**
 * EventService that sends events to the kafka machine. These events are notifications
 * for the Auth microservice for all things that happened registering a new document in the
 * expediente
 *
 *  @author rlh
 *  @project : expediente-service
 *  @date November 2023
 */
@Service
class EventService(private val streamBridge: StreamBridge,
                   private val mapper: ObjectMapper,
                   private var serviceConfig: ServiceConfig
) : HasLogger {
    companion object {
        const val CORRELATION_ID = "lm-correlation-id"
        // ^ this constant is the header value defined in the gateway micro.service
    }

    private val coreName = "expediente"

    /**
     * Send the event directly to a Kafka microservice using the EventConfig class or the .yml file if it is a
     * producer only.
     *
     * note: These messages are for notification purpose to the Auth microservice
     */
    fun sendEvent(headers: HttpHeaders? = null,
                  userName: String,
                  eventName: String,
                  value: Any,
                  eventType: EventType = EventType.DB_STORE): EventDTO {
        val eventBody = mapper.readTree(mapper.writeValueAsString(value))
        val parentNode = mapper.createObjectNode()
        val correlationId = if ((headers == null) || (headers.get(CORRELATION_ID) == null))
                                "No gateway, so no correlation id found"
        else
            headers.get(CORRELATION_ID)?.firstOrNull { header -> header != null }

        // Add the permit where notification will be sent
        parentNode.put("notificaFacultad", serviceConfig.getNotificaFacultad())
        parentNode.set<JsonNode>("datos", eventBody!!)

        val event = EventDTO(correlationId = correlationId ?: "No gateway, so no correlation id found",
                                eventType = eventType,
                                username = userName,
                                eventName = eventName,
                                applicationName = serviceConfig.getAppName(),
                                coreName = coreName,
                                eventBody = parentNode)

        streamBridge.send("notify-out-0", event)

        return event
    }

}
