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
package skills.storage.accessors

import org.apache.commons.lang3.StringUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import skills.controller.exceptions.ErrorCode
import skills.controller.exceptions.SkillException
import skills.storage.model.ProjDef
import skills.storage.model.ProjSummaryResult
import skills.storage.repos.ProjDefRepo
import skills.storage.repos.ProjDefWithDescriptionRepo

@Service
class ProjDefAccessor {

    @Autowired
    ProjDefRepo projDefRepo

    @Autowired
    ProjDefWithDescriptionRepo projDefWithDescriptionRepo

    @Transactional()
    ProjDef getProjDef(String projectId) {
        if (StringUtils.isBlank(projectId)) {
            throw new SkillException("Bad project id parameter, [${projectId}] was provided", ErrorCode.BadParam)
        }
        ProjDef projDef = projDefRepo.findByProjectIdIgnoreCase(projectId)
        if (!projDef) {
            throw new SkillException("Failed to find project [$projectId]", projectId, null, ErrorCode.ProjectNotFound)
        }
        return projDef
    }

    @Transactional
    String getProjDescription(String projectId) {
        if (StringUtils.isBlank(projectId)) {
            throw new SkillException("Bad project id parameter, [${projectId}] was provided", ErrorCode.BadParam)
        }
        return projDefWithDescriptionRepo.getDescriptionByProjectId(projectId)
    }

    @Transactional()
    ProjSummaryResult getProjSummaryResult(String projectId) {
        ProjSummaryResult projDef = projDefRepo.getSummaryByProjectId(projectId)
        if (!projDef) {
            throw new SkillException("Failed to find project [$projectId]", projectId, null, ErrorCode.ProjectNotFound)
        }
        return projDef
    }
}
