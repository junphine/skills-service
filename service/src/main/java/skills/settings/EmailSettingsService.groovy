/**
 * Copyright 2020 SkillTree
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package skills.settings


import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.JavaMailSenderImpl
import org.springframework.stereotype.Service
import skills.controller.request.model.GlobalSettingsRequest
import skills.controller.request.model.SettingsRequest
import skills.controller.result.model.SettingsResult
import skills.services.SystemSettingsService
import skills.services.settings.SettingsService

import jakarta.mail.MessagingException
import skills.services.userActions.DashboardAction
import skills.services.userActions.DashboardItem
import skills.services.userActions.UserActionInfo
import skills.services.userActions.UserActionsHistoryService

import java.util.concurrent.TimeUnit

@Service
@Slf4j
@ConditionalOnProperty(
        name = "skills.db.startup",
        havingValue = "true",
        matchIfMissing = true)
class EmailSettingsService {

    private static final String SMTP_CONNECTION_TIMEOUT = Long.toString(TimeUnit.SECONDS.toMillis(10))

    static final String settingsGroup = 'GLOBAL.EMAIL'
    static final String hostSetting = 'email.host'
    static final String portSetting = 'email.port'
    static final String protocolSetting = 'email.protocol'
    static final String usernameSetting = 'email.username'
    static final String passwordSetting = 'email.password'
    static final String authSetting = 'email.auth'
    static final String tlsEnableSetting = 'email.tls.enable'
    static final String fromEmail = 'email.fromEmail'
    static final String publicUrl = 'email.publicUrl'

    static final String htmlHeader = 'email.htmlHeader'
    static final String htmlFooter = 'email.htmlFooter'
    static final String plaintextHeader = 'email.plaintextHeader'
    static final String plaintextFooter = 'email.plaintextFooter'

    @Autowired
    SettingsService settingsService

    @Autowired
    SystemSettingsService systemSettingsService

    @Autowired
    UserActionsHistoryService userActionsHistoryService

    EmailConfigurationResult updateConnectionInfo(EmailConnectionInfo emailConnectionInfo) {
        EmailConfigurationResult configurationSuccessful = configureMailSender(emailConnectionInfo)
        storeSettings(emailConnectionInfo)
        Map actionAttributes = emailConnectionInfo.properties
                .findAll { key, val -> key != "password" && val instanceof String || val instanceof Number }
                .collectEntries { key, val -> [(key): val] }
        userActionsHistoryService.saveUserAction(new UserActionInfo(
                action: DashboardAction.Create,
                item: DashboardItem.Settings,
                actionAttributes: actionAttributes,
                itemId: "EmailSettings",
        ))
        return configurationSuccessful;
    }

    EmailConfigurationResult configureMailSender(EmailConnectionInfo emailConnectionInfo) {
        log.info('Configuring the email sender with properties [{}]', emailConnectionInfo)
        JavaMailSenderImpl tmpMailSender = createJavaMailSender(emailConnectionInfo)
        try {
            tmpMailSender.testConnection()

            log.info('Refreshing the email sender')
            return new EmailConfigurationResult(configurationSuccessful: true)
        } catch (MessagingException e) {
            log.warn('Email connection failed!', e)

            String msg = e.message
            Exception next = e.nextException
            if (next instanceof SocketTimeoutException) {
                msg = next.message
            }
            return new EmailConfigurationResult(configurationSuccessful: false, explanation: msg)
        }
    }

    private JavaMailSenderImpl createJavaMailSender(EmailConnectionInfo emailConnectionInfo) {
        JavaMailSenderImpl tmpMailSender = new JavaMailSenderImpl()
        tmpMailSender.setHost(emailConnectionInfo.host)
        tmpMailSender.setPort(emailConnectionInfo.port)
        tmpMailSender.setUsername(emailConnectionInfo.username)
        tmpMailSender.setPassword(emailConnectionInfo.password)

        Properties props = tmpMailSender.getJavaMailProperties()
        if (emailConnectionInfo.protocol) {
            props.put('mail.transport.protocol', emailConnectionInfo.protocol)
        }
        props.put('mail.smtp.auth', emailConnectionInfo.authEnabled)
        props.put('mail.smtp.starttls.enable', emailConnectionInfo.tlsEnabled)
        props.put('mail.debug', 'false')

        //connectiontimeout doesn't work if connecting to an open socket that is not an smtp server
        props.put("mail.smtp.timeout", SMTP_CONNECTION_TIMEOUT)

        return tmpMailSender
    }

    boolean testConnection(EmailConnectionInfo emailConnectionInfo) {
        log.info('Testing email connection with properties [{}]', emailConnectionInfo)
        JavaMailSenderImpl mailSender = createJavaMailSender(emailConnectionInfo)

        try {
            mailSender.testConnection()
            log.info('Test connection successful!')
            return true
        } catch (MessagingException e) {
            log.info('Test connection failed!')
            return false
        }
    }

    private void storeSettings(EmailConnectionInfo emailConnectionInfo) {
        saveOrUpdateGlobalGroup(settingsGroup, [
                (hostSetting)     : emailConnectionInfo.host,
                (portSetting)     : emailConnectionInfo.port?.toString(),
                (protocolSetting) : emailConnectionInfo.protocol,
                (usernameSetting) : emailConnectionInfo.username,
                (passwordSetting) : emailConnectionInfo.password,
                (authSetting)     : emailConnectionInfo.authEnabled?.toString(),
                (tlsEnableSetting): emailConnectionInfo.tlsEnabled?.toString(),
                (fromEmail) : emailConnectionInfo.fromEmail,
                (publicUrl) : emailConnectionInfo.publicUrl,
        ])
    }

    EmailConnectionInfo fetchEmailSettings() {
        List<SettingsResult> emailSettings = settingsService.getGlobalSettingsByGroup(settingsGroup)
        return convert(emailSettings)
    }

    EmailConnectionInfo convert(List<SettingsResult> emailGroupSettings) {
        EmailConnectionInfo info = new EmailConnectionInfo()
        if (emailGroupSettings) {

            def mappedSettings = emailGroupSettings.collectEntries() {
                [it.setting, it.value]
            }
            if (mappedSettings[(hostSetting)]) {
                info.host = mappedSettings[(hostSetting)]
            }
            if (mappedSettings[(portSetting)]) {
                info.port = Integer.valueOf(mappedSettings[(portSetting)])
            }
            if (mappedSettings[(protocolSetting)]) {
                info.protocol = mappedSettings[(protocolSetting)]
            }
            if (mappedSettings[(usernameSetting)]) {
                info.username = mappedSettings[(usernameSetting)]
            }
            if (mappedSettings[(passwordSetting)]) {
                info.password = mappedSettings[(passwordSetting)]
            }
            if (mappedSettings[(authSetting)]) {
                info.authEnabled = Boolean.valueOf(mappedSettings[(authSetting)])
            }
            if (mappedSettings[(tlsEnableSetting)]) {
                info.tlsEnabled = Boolean.valueOf(mappedSettings[(tlsEnableSetting)])
            }
            if (mappedSettings[(fromEmail)]) {
                info.fromEmail = mappedSettings[(fromEmail)]
            }
            if (mappedSettings[(publicUrl)]) {
                info.publicUrl = mappedSettings[(publicUrl)]
            }
        }
        return info
    }

    private void saveOrUpdateGlobalGroup(String settingsGroup, Map<String, String> settingsMap) {
        List<SettingsRequest> settingsRequests = []
        List<SettingsRequest> deleteIfExist = []
        settingsMap.each { String setting, String value ->
            GlobalSettingsRequest gsr = new GlobalSettingsRequest(settingGroup: settingsGroup, setting: setting, value: value)
            if (value) {
                settingsRequests << gsr
            } else {
                //if there's not value specified, we need to check if that setting previously had a value and delete it
                deleteIfExist << gsr
            }
        }
        settingsService.saveSettings(settingsRequests)
        deleteIfExist.each {
            settingsService.deleteGlobalSetting(it.setting)
        }
    }


    JavaMailSender getMailSender() {
        EmailConnectionInfo emailConnectionInfo = fetchEmailSettings()
        return getMailSender(emailConnectionInfo)
    }

    JavaMailSender getMailSender(EmailConnectionInfo emailConnectionInfo) {
        return emailConnectionInfo ? createJavaMailSender(emailConnectionInfo) : null
    }
}
