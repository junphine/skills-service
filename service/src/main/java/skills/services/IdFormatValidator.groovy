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

import groovy.transform.CompileStatic
import org.apache.commons.lang3.StringUtils
import skills.controller.exceptions.ErrorCode

@CompileStatic
class IdFormatValidator {

    static void validate(String id, Boolean allowPercent=false) {
        if (id?.equalsIgnoreCase("undefined")) {
            throw new skills.controller.exceptions.SkillException("Bad Id [$id] - this is not an allowed value.", null, null, ErrorCode.BadParam)
        }
        if (!isAlphanumericOrUnderscoreOrPercent(id, allowPercent)) {
            throw new skills.controller.exceptions.SkillException("Bad Id [$id] - must be alpha numeric - no spaces or special characters.")
        }
    }

    private static boolean isAlphanumericOrUnderscoreOrPercent(final CharSequence cs, final Boolean allowPercent) {
        if (StringUtils.isEmpty(cs)) {
            return false;
        }
        final int sz = cs.length();
        for (int i = 0; i < sz; i++) {
            char checkMe = cs.charAt(i)
            if (!Character.isLetterOrDigit(checkMe) && checkMe != '_' && (!allowPercent || checkMe != '%')) {
                return false;
            }
        }
        return true;
    }
}
