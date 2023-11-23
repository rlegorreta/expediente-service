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
 *  DocumentReception.kt
 *
 *  Developed 2023 by LegoSoftSoluciones, S.C. www.legosoft.com.mx
 */

package com.acme.expedienteservice.bpm.worker

import com.acme.expedienteservice.service.cmis.DocumentService
import com.acme.expedienteservice.service.event.EventService
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.ailegorreta.commons.event.EventType
import com.ailegorreta.commons.utils.HasLogger
import io.camunda.zeebe.spring.client.annotation.Variable
import io.camunda.zeebe.spring.client.annotation.VariablesAsType
import io.camunda.zeebe.spring.client.annotation.JobWorker
import io.camunda.zeebe.spring.client.exception.ZeebeBpmnError
import org.springframework.stereotype.Service

/**
 * This class handles all service task for the BPM process for document reception.
 * The BPM field is Reception.bpmn
 *
 * @project expediente-service
 * @author rlh
 * @date November 2023
 */
@Service
class DocumentReception(val eventService: EventService,
                        val documentService: DocumentService): HasLogger {

    /**
     *  In this case for ZeebeWorker
     * - The variables are stored in a data record with just the needed variables (as record)
     *   using @ZeebeVariablesAsType.
     * - Modify the variables
     * - The auto-completion for this task is done.
     */
    @JobWorker(type = "email", autoComplete = true)
    fun handleDocumentFirstRevision(@VariablesAsType variables: DocumentFirstRevisionVars): DocumentFirstRevisionVars {
        logger.info("Check document for any anti-virus: ${variables.message_content}")
        variables.message_content = variables.message_content + " Atte. La Dirección"

        return variables
    }

    /**
     * Task that simulates the file virus check. For future versions a real virsu validation will be executed
     */
    @JobWorker(type = "checkvirus", autoComplete = true)
    fun checkFileVirus(@Variable fileId: String): Map<String, String> {
        val document = DocumentService.Document(documentService, fileId)

        document.documentById(3, 10L)        // retry because maybe the document is not ready yet.
                                                                  // it is the first task before the document has been saved
        if (document.fileName != null) {
            if (document.fileName.contains("women") || document.fileName.contains("penguin"))
                throw ZeebeBpmnError("archivo-virus-error", "El archivo $fileId tiene virus")
            // approve it
            return mapOf("fileId" to fileId)
        }
        logger.error("No se encontró el archivo $fileId en el repositorio RE-READ")
        // could not read document throw an exception
        throw ZeebeBpmnError("archivo-virus-error", "El archivo $fileId NO se pudo leer correctamente")
    }

    @JobWorker(type = "notifyvirus", autoComplete = true)
    fun notifyVirus(@Variable fileId: String,
                    @Variable username: String) {
        logger.error("Se registró el archivo $fileId que tiene virus por lo que no se almacenó en el expediente y se borra de Alfresco")
        documentService.deleteDocument(fileId)
        eventService.sendEvent(eventType = EventType.DB_STORE, userName = username,
                               eventName = "NOTIFICACION",
                               value = "Archivo $fileId con virus no se almacenó en el expediente")
    }

    @JobWorker(type = "notifynotapprovedadmin", autoComplete = true)
    fun notifyNotApprovedAdmin(@Variable fileId: String,
                               @Variable persona: String?,
                               @Variable comments_admin: String?,
                               @Variable username: String): Map<String, String> {
        logger.error("El archivo  $fileId de la persona $persona y los comentarios $comments_admin NO fue aprobado por administración")

        val oldFileId = documentService.moveDocument(fileId, approved = false)
                        // ^ maybe the document exists in previous versions

        eventService.sendEvent(eventType = EventType.DB_STORE, userName = username,
                                eventName = "NOTIFICACION",
                                value = "Archivo $fileId NO aprobado por Administración:'$comments_admin'")

        return mapOf("fileId" to oldFileId)
    }

    @JobWorker(type = "notifyapproved", autoComplete = true)
    fun notifyApproved(@Variable fileId: String,
                       @Variable persona: String?,
                       @Variable comments_admin: String?,
                       @Variable comments_legal: String?,
                       @Variable previo_legal: String?,
                       @Variable username: String): Map<String, String>  {
        logger.debug("El archivo $fileId de la persona $persona y los comentarios $comments_admin y de legal $comments_legal fue aprobado")
        logger.error(">>>>> Previo legal: $previo_legal")
        val oldFileId = documentService.moveDocument(fileId, persona, approved = true, previo = (previo_legal == "true"))
                        // ^ maybe the document exists in previous versions

        if (comments_legal == null)
            eventService.sendEvent(eventType = EventType.DB_STORE, userName = username,
                                    eventName = "NOTIFICACION",
                                    value = "Archivo $fileId 'aprobado' comentarios:'$comments_admin'")
        else
            eventService.sendEvent(eventType = EventType.DB_STORE, userName = username,
                               eventName = "NOTIFICACION",
                               value = "Archivo $fileId 'aprobado' comentarios:'$comments_admin' y de legal:$comments_legal")

        return mapOf("fileId" to oldFileId)
    }

    @JobWorker(type = "notifynotapprovedlegal", autoComplete = true)
    fun notifyNotApprovedLegal(@Variable fileId: String,
                               @Variable persona: String?,
                               @Variable comments_legal: String?,
                               @Variable username: String) {
        logger.error("El archivo $fileId de la persona $persona y los comentarios de legal $comments_legal NO fue aprobado")
        documentService.moveDocument(fileId, approved = false)
        eventService.sendEvent(eventType = EventType.DB_STORE, userName = username,
                               eventName = "NOTIFICACION",
                               value = "Archivo $fileId 'NO aprobado' por legal, los comentarios son:$comments_legal")
    }

}

/**
 * Document variables
 */

@JsonIgnoreProperties(ignoreUnknown = true)
data class ReceiptDocumentVar constructor(val fileId: String,
                                        val persona: String? = null,
                                        val comments_admin: String? = null,
                                        val comments_legal: String? = null)


/**
 * Test email process
 */
data class DocumentFirstRevisionVars constructor(var message_content: String = "")
