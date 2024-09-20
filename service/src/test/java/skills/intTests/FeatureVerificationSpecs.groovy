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
package skills.intTests

import com.icegreen.greenmail.util.GreenMail
import com.icegreen.greenmail.util.ServerSetupTest
import org.thymeleaf.context.Context
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsService
import skills.notify.Notifier
import skills.services.settings.Settings
import spock.lang.IgnoreRest

class FeatureVerificationSpecs extends DefaultIntSpec {

    GreenMail greenMail = new GreenMail(ServerSetupTest.SMTP)

    SkillsService rootSkillsService


    def setup() {
        rootSkillsService = createService("rootUser", 'aaaaaaaa')
        if (!rootSkillsService.isRoot()) {
            rootSkillsService.grantRoot()
        }
    }

    def cleanup(){
        greenMail?.stop()
    }

    def "password reset feature enabled if mail settings and public url are configured" () {
        greenMail.start()
        rootSkillsService.getWsHelper().rootPost("/saveEmailSettings", [
                "host"       : "localhost",
                "port"       : ServerSetupTest.SMTP.port,
                "protocol"   : "smtp",
                "authEnabled": false,
                "tlsEnabled" : false,
                "publicUrl"  : "http://localhost:${localPort}/".toString(),
                "fromEmail"  : "noreply@skilltreeemail.com"
        ])

        when:

        def enabled = skillsService.isFeatureEnabled("passwordReset")

        then:
        enabled == true
    }

    def "password reset disabled if email is not configured"(){
        rootSkillsService.getWsHelper().rootPost("/saveEmailSettings", [
                "host"       : "localhost",
                "port"       : ServerSetupTest.SMTP.port,
                "protocol"   : "smtp",
                "authEnabled": false,
                "tlsEnabled" : false
        ])


        when:

        def enabled = skillsService.isFeatureEnabled("passwordReset")

        then:
        enabled == false
    }

    def "password reset disabled if public url is not configured"() {
        greenMail.start()
        rootSkillsService.getWsHelper().rootPost("/saveEmailSettings", [
                "host"       : "localhost",
                "port"       : ServerSetupTest.SMTP.port,
                "protocol"   : "smtp",
                "authEnabled": false,
                "tlsEnabled" : false,
                "publicUrl"  : ""
        ])

        when:

        def enabled = skillsService.isFeatureEnabled("passwordReset")

        then:
        enabled == false
    }

    def "is email service enabled"() {
        SkillsService rootSkillsService = createRootSkillService()

        when:
        assert !skillsService.isFeatureEnabled("emailservice")

        rootSkillsService.getWsHelper().rootPost("/saveEmailSettings", [
                "host"       : "localhost",
                "port"       : 3923,
                "protocol"   : "smtp",
                "authEnabled": false,
                "tlsEnabled" : false,
                "publicUrl"  : "http://localhost:${localPort}/".toString(),
                "fromEmail"  : "resetspec@skilltreetests"
        ])

        then:
        skillsService.isFeatureEnabled("emailservice")
    }

    def "is email service enabled - from_email must have value"() {
        SkillsService rootSkillsService = createRootSkillService()

        when:
        rootSkillsService.getWsHelper().rootPost("/saveEmailSettings", [
                "host"       : "localhost",
                "port"       : 3923,
                "protocol"   : "smtp",
                "authEnabled": false,
                "tlsEnabled" : false,
                "fromEmail"  : ""
        ])

        then:
        !skillsService.isFeatureEnabled("emailservice")
    }
}
