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

import callStack.profiler.Profile
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import skills.controller.exceptions.ErrorCode
import skills.controller.exceptions.SkillException
import skills.storage.model.SkillDef
import skills.storage.model.SkillRelDef
import skills.storage.repos.ProjDefRepo
import skills.storage.repos.QuizDefRepo
import skills.storage.repos.SkillDefRepo

@Service
@Slf4j
class CreatedResourceLimitsValidator {
    @Autowired
    SkillDefRepo skillDefRepo

    @Autowired
    ProjDefRepo projDefRepo

    @Autowired
    QuizDefRepo quizDefRepo

    @Value('#{"${skills.config.ui.maxProjectsPerAdmin}"}')
    int maxProjectsPerUser

    @Value('#{"${skills.config.ui.maxQuizDefsPerAdmin}"}')
    int maxQuizDefsPerAdmin

    @Value('#{"${skills.config.ui.maxSubjectsPerProject}"}')
    int maxSubjectsPerProject

    @Value('#{"${skills.config.ui.maxBadgesPerProject}"}')
    int maxBadgesPerProject

    @Value('#{"${skills.config.ui.maxSkillsPerSubject}"}')
    int maxSkillsPerSubject

    void validateNumProjectsCreated(String userId) {
        Integer projectsByUserCount = projDefRepo.getProjectsByUserCount(userId)
        if(projectsByUserCount >= maxProjectsPerUser) {
            throw new SkillException("Each user is limited to [${maxProjectsPerUser}] Projects", ErrorCode.MaxProjectsThreshold)
        }
    }

    void validateNumQuizDefsCreated(String userId) {
        Integer numQuizDefs = quizDefRepo.getQuizCountByUserId(userId)
        if(numQuizDefs >= maxQuizDefsPerAdmin) {
            throw new SkillException("Each user is limited to [${maxQuizDefsPerAdmin}] quiz definitions", ErrorCode.MaxQuizDefThreshold)
        }
    }

    void validateNumSubjectsCreated(String projectId){
        long subjectCount = skillDefRepo.countByProjectIdAndType(projectId, SkillDef.ContainerType.Subject)
        if(subjectCount >= maxSubjectsPerProject){
            throw new SkillException("Each Project is limited to [${maxProjectsPerUser}] Subjects", ErrorCode.MaxSubjectsThreshold)
        }
    }

    void validateNumBadgesCreated(String projectId){
        long badgeCount = skillDefRepo.countByProjectIdAndType(projectId, SkillDef.ContainerType.Badge)
        if (badgeCount >= maxBadgesPerProject) {
            throw new SkillException("Each Project is limited to [${maxProjectsPerUser}] Badges", ErrorCode.MaxBadgesThreshold)
        }
    }

    @Profile
    void validateNumSkillsCreated(SkillDef subject){
        long skillCount = skillDefRepo.countChildSkillsByIdAndRelationshipTypeAndEnabled(subject.id, SkillRelDef.RelationshipType.RuleSetDefinition, "true")
        skillCount += skillDefRepo.countActiveGroupChildSkillsForSubject(subject.id)
        if(skillCount >= maxSkillsPerSubject){
            throw new SkillException("Each Subject is limited to [${maxSkillsPerSubject}] Skills", ErrorCode.MaxSkillsThreshold)
        }
    }

    /**
     * To be used when enabling a skill group - subject skill group validation does not take into account
     * disabled group's skills when enabling/publishing/"going live" for a Skill Group. When enabling a Skill Group
     * we need to ensure that the children of the Skill Group won't cause the maximum skill count to be exceeded.
     *
     * @param subject
     * @param toBeEnabledGroupSkills
     */
    void validateNumSkillsCreated(SkillDef subject, Integer toBeEnabledGroupSkills){
        long skillCount = skillDefRepo.countChildSkillsByIdAndRelationshipTypeAndEnabled(subject.id, SkillRelDef.RelationshipType.RuleSetDefinition, "true")
        skillCount += skillDefRepo.countActiveGroupChildSkillsForSubject(subject.id)
        skillCount += toBeEnabledGroupSkills

        if(skillCount > maxSkillsPerSubject){
            throw new SkillException("Each Subject is limited to [${maxSkillsPerSubject}] Skills, enabling the Skill Group would exceed that maximum", ErrorCode.MaxSkillsThreshold)
        }
    }
}
