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
 *  ServiceConfig.kt
 *
 *  Developed 2023 by LegoSoftSoluciones, S.C. www.legosoft.com.mx
 */
package com.acme.expedienteservice.config

import com.ailegorreta.commons.cmis.config.ServiceConfigAlfresco
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component

/**
 * Service configuration stored in the properties .yml file.
 *
 * @author rlh
 * @project : expediente-service
 * @date November 2023
 *
 */
@Component
@Configuration
class ServiceConfig: ServiceConfigAlfresco {

    @Value("\${spring.application.name}")
    private val appName: String = ""
    fun getAppName() = appName

    @Value("\${expediente.notifica.facultad}")
    private val notificaFacultad: String = ""
    fun getNotificaFacultad() = notificaFacultad

    @Value("\${alfresco.url}")
    private val alfrescoServer: String = "Client rest not defined"
    override fun getAlfrescoServer() = alfrescoServer

    @Value("\${alfresco.username}")
    private val alfrescoUsername: String = "Alfresco username dose not exists"
    override fun getAlfrescoUsername() = alfrescoUsername

    @Value("\${alfresco.password}")
    private val alfrescoPassword: String = "Alfresco password does not exists"
    override fun getAlfrescoPassword() = alfrescoPassword

    @Value("\${alfresco.company}")
    private val alfrescoCompany: String = "Alfresco root directory"
    override fun getAlfrescoCompany() = alfrescoCompany
}
