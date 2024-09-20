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
package skills.services

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.thymeleaf.util.StringUtils
import skills.auth.AuthMode
import skills.controller.result.model.SettingsResult
import skills.services.settings.Settings
import skills.services.settings.SettingsService
import skills.settings.EmailSettingsService

@Slf4j
@Component
class FeatureService {

    @Autowired
    SettingsService settingsService

    @Autowired
    EmailSettingsService emailSettingsService

    @Value('${skills.authorization.authMode:#{T(skills.auth.AuthMode).DEFAULT_AUTH_MODE}}')
    AuthMode authMode

    @Value('#{"${skills.authorization.oAuthOnly:false}"}')
    Boolean oAuthOnly

    @Value('#{"${skills.authorization.verifyEmailAddresses:false}"}')
    Boolean verifyEmailAddresses

    boolean isPasswordResetFeatureEnabled() {
        return isEmailServiceFeatureEnabled('Password Reset')
    }

    boolean isEmailVerificationFeatureEnabled() {
        if (!verifyEmailAddresses) {
            return false;
        } else {
            if (authMode != AuthMode.FORM || oAuthOnly) {
                log.warn('Email Verification is only available in when username/password authentication is enabled')
                return false
            }
            return isEmailServiceFeatureEnabled('Email Verification')
        }
    }

    boolean isEmailServiceFeatureEnabled(String featureName = 'Email Service') {
        if (!emailSettingsService.fetchEmailSettings()?.host || !emailSettingsService.fetchEmailSettings()?.publicUrl || !emailSettingsService.fetchEmailSettings()?.fromEmail) {
            log.warn("Email Settings are not configured or are invalid, please configure through Dashboard for ${featureName} feature to function")
            return false;
        }

        return true
    }

    String getPublicUrl() {
        String publicUrl = emailSettingsService.fetchEmailSettings()?.publicUrl
        if (!publicUrl) {
            log.warn("Notifications are disabled since email setting [${publicUrl}] is NOT set")
            return null
        }

        if (!publicUrl.endsWith("/")){
            publicUrl += "/"
        }
        return publicUrl
    }
}
