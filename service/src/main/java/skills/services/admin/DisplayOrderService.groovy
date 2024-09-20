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
package skills.services.admin

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import skills.controller.exceptions.SkillException
import skills.controller.exceptions.SkillsValidator
import skills.controller.request.model.ActionPatchRequest
import skills.storage.model.SkillDef
import skills.storage.repos.SkillDefRepo

@Service
@Slf4j
class DisplayOrderService {

    @Autowired
    SkillDefRepo skillDefRepo

    void resetDisplayOrder(List<SkillDef> skillDefs) {
        if(skillDefs) {
            List <SkillDef> copy = new ArrayList<>(skillDefs)
            List<SkillDef> toSave = []
            copy = copy.sort({ it.displayOrder })
            copy.eachWithIndex { SkillDef entry, int i ->
                if (entry.displayOrder != i) {
                    toSave.add(entry)
                    entry.displayOrder = i+1
                }
            }
            if (toSave) {
                skillDefRepo.saveAll(toSave)
            }
        }
    }

    @Transactional
    void updateDisplayOrderByUsingNewIndex(String skillId, List<SkillDef> skills, ActionPatchRequest patchRequest) {
        assert patchRequest.action == ActionPatchRequest.ActionType.NewDisplayOrderIndex
        SkillsValidator.isTrue(patchRequest.newDisplayOrderIndex >= 0, "[newDisplayOrderIndex] param must be >=0 but received [${patchRequest.newDisplayOrderIndex}]", skills?.first()?.projectId, skillId)

        SkillDef theItem = skills.find({ it.skillId == skillId })
        List<SkillDef> result = skills.findAll({ it.skillId != skillId }).sort({ it.displayOrder })

        int newIndex = Math.min(patchRequest.newDisplayOrderIndex, skills.size() - 1)
        result.add(newIndex, theItem)
        result.eachWithIndex{ SkillDef entry, int i ->
            entry.displayOrder = i
        }

        skillDefRepo.saveAll(result)

        if (log.isDebugEnabled()) {
            log.debug("Updated display order {}", result.collect { "${it.skillId}=>${it.displayOrder}" })
        }
    }

}
