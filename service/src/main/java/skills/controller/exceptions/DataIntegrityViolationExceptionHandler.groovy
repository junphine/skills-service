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
package skills.controller.exceptions

import groovy.util.logging.Slf4j
import org.hibernate.exception.ConstraintViolationException
import org.springframework.dao.DataIntegrityViolationException

@Slf4j
class DataIntegrityViolationExceptionHandler {

    private final Map<String, String> constraintNameToMsgMapping

    DataIntegrityViolationExceptionHandler(Map<String, String> constraintNameToMsgMapping) {
        this.constraintNameToMsgMapping = constraintNameToMsgMapping
    }

    Object handle(String projectId, Closure closure) {
        return handle(projectId, null, closure)
    }

    Object handle(String projectId, String skillId, Closure closure) {
        return handle(projectId, null, null, closure)
    }

    Object handle(String projectId, String skillId, String quizId, Closure closure) {
        try {
            return closure.call()
        } catch (DataIntegrityViolationException violationException) {
            log.error("Violation Exception", violationException)
            String msg = "Data Integrity Violation"
            if (violationException.cause instanceof ConstraintViolationException) {
                ConstraintViolationException constraintViolationException = violationException.cause
                def entry = constraintNameToMsgMapping.find {
                    boolean generalCase = it.key.equalsIgnoreCase(constraintViolationException.constraintName)
                    boolean h2DBCase = constraintViolationException.constraintName.toUpperCase().startsWith("\"${it.key.toUpperCase()}")
                    generalCase || h2DBCase
                }
                if (entry) {
                    msg = entry.value
                } else {
                    log.warn("Failed to locate error explanation for the constraint name [{}], please consider adding one!", constraintViolationException.constraintName)
                }
            }

            if (projectId) {
                msg = "${msg}; ProjectId=[${projectId}]"
            }

            if (skillId) {
                msg = "${msg}; SkillId=[${skillId}]"
            }

            throw new SkillException(msg, projectId, skillId, ErrorCode.ConstraintViolation)
        }
    }
}
