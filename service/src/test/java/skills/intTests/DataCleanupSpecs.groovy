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

import org.springframework.beans.factory.annotation.Autowired
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsFactory
import skills.storage.model.ProjDef
import skills.storage.model.SkillDefPartial
import skills.storage.model.SkillRelDef
import skills.storage.repos.LevelDefRepo
import skills.storage.repos.ProjDefRepo
import skills.storage.repos.SkillDefRepo
import skills.storage.repos.SkillRelDefRepo

class DataCleanupSpecs extends DefaultIntSpec {

    @Autowired
    LevelDefRepo levelDefRepo

    @Autowired
    ProjDefRepo projDefRepo

    @Autowired
    SkillRelDefRepo relDefRepo

    def "make sure there are no orphan levels in db when project is removed"() {
        Map proj = SkillsFactory.createProject()
        Map subject = SkillsFactory.createSubject()

        when:
        skillsService.createProject(proj)
        skillsService.createSubject(subject)

        ProjDef projDef = projDefRepo.findByProjectId(proj.projectId)
        long countBefore = levelDefRepo.findAllByProjectRefId(projDef.id).size()
        skillsService.deleteProject(proj.projectId)
        long countAfter = levelDefRepo.findAllByProjectRefId(projDef.id).size()

        then:
        countBefore > 0
        countAfter == 0
    }

    def "when project is removed performed events must be removed as well"() {
        String userId = "user1"

        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(5, 1, 1)
        proj1_skills.each{
            it.pointIncrement = 25
        }

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills)

        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(0).skillId], userId, new Date())
        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(1).skillId], userId, new Date())

        when:
        def skills = skillsService.getPerformedSkills(userId, proj1.projectId)
        skillsService.deleteProject(proj1.projectId)
        skillsService.createProject(proj1)
        def skillsAfter = skillsService.getPerformedSkills(userId, proj1.projectId)
        then:
        skills.data.size() == 2
        !skillsAfter.data
    }

    def "when skill is removed performed events must be removed as well"() {
        String userId = "user1"

        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(5, 1, 1)
        proj1_skills.each{
            it.pointIncrement = 20
        }

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills)

        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(0).skillId], userId, new Date())
        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(1).skillId], userId, new Date())

        when:
        def skills = skillsService.getPerformedSkills(userId, proj1.projectId)
        skillsService.deleteSkill([projectId: proj1.projectId, subjectId: proj1_subj.subjectId, skillId: proj1_skills.get(0).skillId])
        def skillsAfter = skillsService.getPerformedSkills(userId, proj1.projectId)
        then:
        skills.data.size() == 2
        skillsAfter.data.size() == 1
    }


    def 'when skill def is removed must be dis-associate its dependencies'(){
        def project = SkillsFactory.createProject()
        def subject = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(3)

        skillsService.createProject(project)
        skillsService.createSubject(subject)
        skillsService.createSkills(skills)

        skillsService.addLearningPathPrerequisite(project.projectId, skills.get(0).skillId, skills.get(1).skillId)
        List<SkillDefPartial> beforeDelete = relDefRepo.getChildrenPartial( project.projectId, skills.get(0).skillId, SkillRelDef.RelationshipType.Dependence)
        assert beforeDelete.size() == 1

        when:
        skillsService.deleteSkill([projectId: project.projectId, subjectId: subject.subjectId, skillId: skills.get(1).skillId,])
        then:
        List<SkillDefPartial> afterDelete = relDefRepo.getChildrenPartial( project.projectId, skills.get(0).skillId, SkillRelDef.RelationshipType.Dependence)
        assert afterDelete.size() == 0
    }
}
