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
 *  DocumentService.kt
 *
 *  Developed 2023 by LegoSoftSoluciones, S.C. www.legosoft.com.mx
 */
package com.acme.expedienteservice.service.cmis

import com.ailegorreta.commons.cmis.CMISService
import com.ailegorreta.commons.cmis.config.ServiceConfigAlfresco
import com.ailegorreta.commons.cmis.data.AbstractCmisStreamObject
import com.ailegorreta.commons.cmis.exception.AlfrescoNotFoundException
import com.ailegorreta.commons.cmis.util.HasLogger
import org.apache.chemistry.opencmis.client.api.Folder
import org.apache.chemistry.opencmis.commons.PropertyIds
import org.apache.chemistry.opencmis.commons.exceptions.CmisRuntimeException
import org.springframework.stereotype.Service

/**
 * Cmis service that keeps the communication with alfresco.
 *
 * note: Maybe is good practice not keep the session open all times, but for now we believe this does not
 *       affect any performance penalty.
 *
 *  @author rlh
 *  @project : expediente-service
 *  @date November 2023
 */
@Service
class DocumentService(override val serviceConfig: ServiceConfigAlfresco): CMISService(serviceConfig), HasLogger {

    companion object {
        const val EN_REVISION_FOLDER = "En revision"
        const val NOT_ACCEPTED = "Rechazados"
        const val EXPEDIENTES = "Expedientes"
    }
    class Document: AbstractCmisStreamObject {
        constructor(documentService: DocumentService, objectId: String): super(documentService, objectId)
    }

    /**
     * Moves a document from an exiting Folder to an existing or not folder. This method is called when a document
     * is approved or not approved
     */
    fun moveDocument( fileId: String, persona: String? = null, approved: Boolean, previo: Boolean = false): String {
        val fromFolder = getFolder(EN_REVISION_FOLDER);

        if (fromFolder != null) {
            val toParentFolder = if (approved) EXPEDIENTES else NOT_ACCEPTED
            var parentFolder = getFolder(toParentFolder)

            if (parentFolder == null) // then created from root folder
                parentFolder = createFolder(rootFolder(), toParentFolder)

            var toFolder: Folder?

            if (approved) {     // stored if in the Person folder
                toFolder = getFolder(persona!!)

                if (toFolder == null)       // then create a new expediente folder
                    toFolder = createFolder(parentFolder, persona)
            } else
                toFolder = parentFolder

            try {
                var newDocument = moveDocument(fileId, fromFolder, toFolder)
                var title = newDocument.getPropertyValue<String>("cm:title")

                if (title == null) title = ""
                if (previo && (!title.contains("PREVIO"))) { // it is CONTRATO_PREVIO append the title 'PREVIO'
                    val properties = mapOf("cm:title" to "${title}_PREVIO")

                    newDocument.updateProperties(properties, true)
                }

                return fileId
            } catch (e: CmisRuntimeException) {
                if (e.message!!.startsWith("Duplicate child name not allowed")) {
                    // create a new version of the existing document
                    try {
                        val newVersion = Document(this, fileId)
                        val newVersionAlfresco = newVersion.documentById(2, 5)       // read content of the new version
                        val oldVersion = getDocumentByFolder(toFolder, newVersion.fileName)

                        if (previo && (!newVersion.title.contains("PREVIO"))) { // it is CONTRATO_PREVIO append the tite 'PREVIO'
                            newVersion.title = "${newVersion.title}_PREVIO"
                        }
                        newVersion.newVersion(oldVersion)                         // set new content in oldVersion document
                        newVersionAlfresco.delete()                               // now erase the newVersion since it a move operation and not a copy

                        val prop: String = oldVersion!!.getPropertyValue<String>(PropertyIds.OBJECT_ID)

                        return  prop.substring(0, prop.indexOf(';'))
                                    // store the ObjectId for future references or store it in the bpm
                                    // ^ take out the version
                    } catch (e: CmisRuntimeException) {
                        logger.error(e.message)
                        e.printStackTrace()
                        throw AlfrescoNotFoundException("No se pudo crear una nueva version del documento existente $fileId al folder ${toFolder.name}")
                    }
                } else {    // it is another error
                    e.printStackTrace()
                    throw (e)
                }
            }
        } else
            throw AlfrescoNotFoundException("El folder de 'En revisión' no existe en el repositorio")
    }

    private fun rootFolder(): Folder {
        var rootFolder = getFolder(serviceConfig.getAlfrescoCompany())

        if (rootFolder == null) {
            // create it inside 'User Homes' folder
            val userHomesFolder = getFolder("User Homes")

            if (userHomesFolder != null)
                rootFolder = createFolder(userHomesFolder,serviceConfig.getAlfrescoCompany() )
            else
                throw AlfrescoNotFoundException("No se encontró el folder 'User Homes'")
        }

        if (rootFolder != null)
            return rootFolder
        else
            throw AlfrescoNotFoundException("Error al creat el folder ${serviceConfig.getAlfrescoCompany()}")
    }


}
