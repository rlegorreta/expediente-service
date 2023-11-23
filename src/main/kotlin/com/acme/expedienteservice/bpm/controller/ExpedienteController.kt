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
 *  ExpedienteController.kt
 *
 *  Developed 2023 by LegoSoftSoluciones, S.C. www.legosoft.com.mx
 */
package com.acme.expedienteservice.bpm.controller

import com.acme.expedienteservice.bpm.utils.ProcessUtil
import com.ailegorreta.commons.utils.HasLogger
import io.camunda.zeebe.client.api.response.DeploymentEvent
import io.camunda.zeebe.client.api.response.ProcessInstanceEvent
import org.springframework.web.bind.annotation.*

/**
 * This is the API REST that can receive the Expediente controller.
 *
 * @project: expediente-service
 * @author: rlh
 * @date: November 2023
 */
@RestController
@RequestMapping("/expediente")
class ExpedienteController(val processUtil: ProcessUtil): HasLogger {

    @PostMapping("/startProcess", produces = ["application/json"])
    fun startProcess(@RequestBody(required=true) body: StartProcessRequestBody): ProcessInstanceEvent {
        logger.debug("Se va a comenzar una nueva instancia del proceso: ${body.processId} ")
        return processUtil.startProcess(body.processId, body.variables)
    }

    @GetMapping("/deployProcess", produces = ["application/json"])
    fun deployProcess(@RequestParam(required=true) name: String): DeploymentEvent {
        logger.debug("Se va a deployar un nuevo: $name")
        return processUtil.deployProcess(name)
    }
}

data class StartProcessRequestBody(val processId: String,
                                   val variables: Map<String, String>? = null)

