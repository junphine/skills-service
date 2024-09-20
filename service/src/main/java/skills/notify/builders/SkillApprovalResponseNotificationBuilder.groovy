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
package skills.notify.builders

import groovy.json.JsonSlurper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.thymeleaf.context.Context
import org.thymeleaf.spring6.SpringTemplateEngine
import skills.storage.model.Notification

@Component
class SkillApprovalResponseNotificationBuilder implements NotificationEmailBuilder{

    JsonSlurper jsonSlurper = new JsonSlurper()

    @Autowired
    SpringTemplateEngine thymeleafTemplateEngine;

    @Override
    String getId() {
        return Notification.Type.SkillApprovalResponse.toString()
    }

    @Override
    Res build(Notification notification, Formatting formatting) {
        def parsed = jsonSlurper.parseText(notification.encodedParams)
        Context context = buildThymeleafContext(parsed, formatting)
        String htmlBody = thymeleafTemplateEngine.process("skill_approval_response.html", context)
        String plainText = buildPlainText(parsed, formatting)
        return new Res(
                subject: "SkillTree Points ${parsed.approved ? 'Approved' : 'Denied'}",
                html: htmlBody,
                plainText: plainText,
                replyToEmail: parsed.replyTo,
        )
    }

    private Context buildThymeleafContext(parsed, Formatting formatting) {
        Context templateContext = new Context()
        templateContext.setVariable("approver", parsed.approver)
        templateContext.setVariable("approved", parsed.approved)
        templateContext.setVariable("skillName", parsed.skillName)
        templateContext.setVariable("skillId", parsed.skillId)
        templateContext.setVariable("subjectId", parsed.subjectId)
        templateContext.setVariable("projectName", parsed.projectName)
        templateContext.setVariable("projectId", parsed.projectId)
        templateContext.setVariable("publicUrl", parsed.publicUrl)
        templateContext.setVariable("htmlHeader", formatting.htmlHeader)
        templateContext.setVariable("htmlFooter", formatting.htmlFooter)

        return templateContext
    }

    private String buildPlainText(parsed, Formatting formatting) {
        String message
        if (parsed.approved) {
            message = "Congratulations! Your request for the ${parsed.skillName} skill in the ${parsed.projectName} project has been approved."
        } else {
            message = "Your request for the ${parsed.skillName} skill in the ${parsed.projectName} project has been denied."
        }
        String pt = "${message}" +
                "\n   Project: ${parsed.projectName}" +
                "\n   Skill: ${parsed.skillName}" +
                "\n   Approver: ${parsed.approver}" +
                "\n\n\nAlways yours," +
                "\nSkillTree Bot"

        if (formatting.plaintextHeader) {
            pt = "${formatting.plaintextHeader}\n${pt}"
        }
        if (formatting.plaintextFooter) {
            pt = "${pt}\n${formatting.plaintextFooter}"
        }

        return pt
    }

}
