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
 *  ProcessUtil.kt
 *
 *  Developed 2023 by LegoSoftSoluciones, S.C. www.legosoft.com.mx
 */
package com.acme.expedienteservice.bpm.utils

import com.ailegorreta.commons.utils.HasLogger
import io.camunda.zeebe.client.api.response.DeploymentEvent
import io.camunda.zeebe.client.api.response.ProcessInstanceEvent
import io.camunda.zeebe.spring.client.lifecycle.ZeebeClientLifecycle
import org.springframework.stereotype.Service

/**
 * Camnunda utils different from Work service
 *
 * @project expediente-service
 * @autho rlh
 * @date November 2023
 */
@Service
class ProcessUtil(private val client: ZeebeClientLifecycle): HasLogger {


    fun startProcess(processId: String, variables: Map<String, String>?): ProcessInstanceEvent {
        val event =  if (variables.isNullOrEmpty())
                        client.newCreateInstanceCommand()
                            .bpmnProcessId(processId)
                            .latestVersion()
                            .send()
                            .join()
                    else
                        client.newCreateInstanceCommand()
                                        .bpmnProcessId(processId)
                                        .latestVersion()
                                        .variables(variables)
                                        .send()
                                        .join()

        logger.debug("""
            Started instance for processDefinitionKey='${event.processDefinitionKey}', 
                    bpmnProcessId='${event.bpmnProcessId}', 
                    version='${event.version}' 
                    with processInstanceKey='${event.processInstanceKey}'
                    """
        )

        return event
    }

    fun deployProcess(name: String): DeploymentEvent {
        val event = client.newDeployResourceCommand()
                            .addResourceFromClasspath("name" + ".bpmn")
                            .send()
                            .join()

        logger.debug("""
                Deployed a new process: ${event.processes[0].processDefinitionKey}
                         from file: ${event.processes[0].resourceName}
                         bpmn process id: ${event.processes[0].bpmnProcessId}
                         version: ${event.processes[0].version}
                 """.trimIndent())

        return event
    }
}
