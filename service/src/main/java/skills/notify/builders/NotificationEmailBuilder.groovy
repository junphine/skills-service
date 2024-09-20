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


import skills.storage.model.Notification

interface NotificationEmailBuilder {

    static class Res {
        String subject
        String plainText
        String html
        String replyToEmail
        /**
         * Flag that the builder can use to indicate that all recipients should be included in a single email
         * as opposed to sending one email per recipient
         */
        boolean singleEmailToAllRecipients
        /**
         * Flag that the builder can use to indicate that userIds are really email addresses and no
         * userId to email address should be performed
         */
        boolean userIdsAreEmailAdresses
        List<String> ccRecipients
    }

    String getId()

    Res build(Notification notification, Formatting formatParams)


}
