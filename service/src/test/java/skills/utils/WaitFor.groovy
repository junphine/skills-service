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
package skills.utils

import com.icegreen.greenmail.util.GreenMail
import groovy.util.logging.Slf4j
import skills.intTests.utils.EmailUtils

@Slf4j
class WaitFor {

    static boolean wait(Closure closure) {
        wait(60, closure)
    }

    static boolean wait(int secsToWait, Closure closure) {
        long start = System.currentTimeMillis()
        while(!closure.call() && (System.currentTimeMillis() - start) < (secsToWait * 1000) ) {
            Thread.sleep(250)
        }

        return closure.call()
    }

    static List<EmailUtils.EmailRes> waitAndCollectEmails(GreenMail greenMail, int expectedNumEmails) {
        WaitFor.wait { greenMail.getReceivedMessages().size() == expectedNumEmails }
        if( greenMail.getReceivedMessages().size() != expectedNumEmails) {
            String emails = greenMail.getReceivedMessages().collect {"${it.from}: ${it.subject}" }.join("\n")
            log.error("Number of emails were different. Actual emails:\n {}", emails)

            assert greenMail.getReceivedMessages().size() != expectedNumEmails
        }
        // wait an additional 500ms in case additional and rogue emails arrive
        Thread.sleep(500)
        List<EmailUtils.EmailRes> emails = EmailUtils.getEmails(greenMail)
        return emails
    }
}
