/**
 * Copyright 2021 SkillTree
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
package skills.notify.builders

import groovy.json.JsonSlurper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.thymeleaf.context.Context
import org.thymeleaf.spring6.SpringTemplateEngine
import skills.storage.model.Notification

@Component
class ContactUsersNotificationBuilder  implements NotificationEmailBuilder {

    JsonSlurper jsonSlurper = new JsonSlurper()

    @Autowired
    SpringTemplateEngine thymeleafTemplateEngine;

    @Override
    String getId() {
        Notification.Type.ContactUsers.toString()
    }

    @Override
    Res build(Notification notification, Formatting formatParams) {
        def parsed = jsonSlurper.parseText(notification.encodedParams)
        Context context = buildThymeleafContext(parsed, formatParams)
        try {
            String htmlBody = thymeleafTemplateEngine.process("contact_users.html", context)
            String plainText = buildPlainText(parsed, formatParams)
            return new Res(
                    subject: parsed.emailSubject,
                    html: htmlBody,
                    plainText: plainText,
                    replyToEmail: parsed.replyTo,
                    ccRecipients: parsed.cc
            )
        } catch(Exception e) {
            e.printStackTrace()
            throw e
        }
    }


    private Context buildThymeleafContext(Object parsed, Formatting formatting) {
        Context templateContext = new Context()
        templateContext.setVariable("htmlBody", parsed.htmlBody)
        templateContext.setVariable("htmlHeader", formatting.htmlHeader)
        templateContext.setVariable("htmlFooter", formatting.htmlFooter)

        return templateContext
    }

    private String buildPlainText(Object parsed, Formatting formatting) {
        String pt = parsed.rawBody

        if (formatting.plaintextHeader) {
            pt = "${formatting.plaintextHeader}\n${pt}"
        }
        if (formatting.plaintextFooter) {
            pt = "${pt}\n${formatting.plaintextFooter}"
        }
        return pt
    }
}
