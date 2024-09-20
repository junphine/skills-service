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
package skills.intTests.crossProject

import org.springframework.http.HttpStatus
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsFactory
import skills.intTests.utils.SkillsService
import skills.services.admin.skillReuse.SkillReuseIdUtil

class CrossProjectSkillsManagementSpec extends DefaultIntSpec {

    SkillsService skillsServiceAdmin2

    def setup() {
        skillsServiceAdmin2 = createService("userNumberTwo")
        skillsServiceAdmin2.deleteAllMyProjects()
    }

    def cleanup() {
        skillsServiceAdmin2.deleteAllMyProjects()
    }

    def "share skill with another project"() {
        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(2, 1, 1)

        def proj2 = SkillsFactory.createProject(2)
        def proj2_subj = SkillsFactory.createSubject(2, 2)
        List<Map> proj2_skills = SkillsFactory.createSkills(2, 2, 2)

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills)

        skillsService.createProject(proj2)
        skillsService.createSubject(proj2_subj)
        skillsService.createSkills(proj2_skills)

        when:
        skillsService.shareSkill(proj1.projectId, proj1_skills.get(0).skillId, proj2.projectId)
        def proj1SharedSkills = skillsService.getSharedSkills(proj1.projectId)
        def proj1SharedWithMeSkills = skillsService.getSharedWithMeSkills(proj1.projectId)

        def proj2SharedSkills = skillsService.getSharedSkills(proj2.projectId)
        def proj2SharedWithMeSkills = skillsService.getSharedWithMeSkills(proj2.projectId)

        then:
        proj1SharedSkills.size() == 1
        proj1SharedSkills.get(0).skillName == "Test Skill 1"
        proj1SharedSkills.get(0).skillId == "skill1"
        proj1SharedSkills.get(0).projectName == "Test Project#2"
        proj1SharedSkills.get(0).projectId == "TestProject2"
        !proj1SharedWithMeSkills

        !proj2SharedSkills
        proj2SharedWithMeSkills.size() == 1
        proj2SharedWithMeSkills.get(0).skillName == "Test Skill 1"
        proj2SharedWithMeSkills.get(0).skillId == "skill1"
        proj2SharedWithMeSkills.get(0).projectName == "Test Project#1"
        proj2SharedWithMeSkills.get(0).projectId == "TestProject1"
    }

    def "shared skills must use doc root from project from which they are shared"() {
        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(2, 1, 1)

        def proj2 = SkillsFactory.createProject(2)
        def proj2_subj = SkillsFactory.createSubject(2, 2)
        List<Map> proj2_skills = SkillsFactory.createSkills(2, 2, 2)
        proj1_skills.get(0).helpUrl = "/helpDocs"

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills)

        skillsService.createProject(proj2)
        skillsService.createSubject(proj2_subj)
        skillsService.createSkills(proj2_skills)

        skillsService.changeSetting(proj1.projectId, "help.url.root",
                [projectId: proj1.projectId, setting: 'help.url.root', value: 'http://testcrossprojectdocs.org'])

        when:
        skillsService.shareSkill(proj1.projectId, proj1_skills.get(0).skillId, proj2.projectId)
        def sharedSkillSummary = skillsService.getSharedSkillSummary(proj2.projectId, proj1.projectId,  proj1_skills.get(0).skillId)

        then:
        sharedSkillSummary
        sharedSkillSummary.description.href == "http://testcrossprojectdocs.org/helpDocs"
    }

    def "share skill with ALL other projects"() {
        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(2, 1, 1)

        def proj2 = SkillsFactory.createProject(2)

        def proj3 = SkillsFactory.createProject(3)

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills)

        skillsService.createProject(proj2)

        skillsService.createProject(proj3)

        when:
        skillsService.shareSkill(proj1.projectId, proj1_skills.get(0).skillId, 'ALL_SKILLS_PROJECTS')
        def proj1SharedSkills = skillsService.getSharedSkills(proj1.projectId)
        def proj1SharedWithMeSkills = skillsService.getSharedWithMeSkills(proj1.projectId)

        def proj2SharedSkills = skillsService.getSharedSkills(proj2.projectId)
        def proj2SharedWithMeSkills = skillsService.getSharedWithMeSkills(proj2.projectId)

        def proj3SharedSkills = skillsService.getSharedSkills(proj3.projectId)
        def proj3SharedWithMeSkills = skillsService.getSharedWithMeSkills(proj3.projectId)

        then:
        proj1SharedSkills.size() == 1
        proj1SharedSkills.get(0).skillName == "Test Skill 1"
        proj1SharedSkills.get(0).skillId == "skill1"
        proj1SharedSkills.get(0).projectName == null
        proj1SharedSkills.get(0).projectId == null
        proj1SharedSkills.get(0).sharedWithAllProjects
        !proj1SharedWithMeSkills

        !proj2SharedSkills
        proj2SharedWithMeSkills.size() == 1
        proj2SharedWithMeSkills.get(0).skillName == "Test Skill 1"
        proj2SharedWithMeSkills.get(0).skillId == "skill1"
        proj2SharedWithMeSkills.get(0).projectName == "Test Project#1"
        proj2SharedWithMeSkills.get(0).projectId == "TestProject1"

        !proj3SharedSkills
        proj3SharedWithMeSkills.size() == 1
        proj3SharedWithMeSkills.get(0).skillName == "Test Skill 1"
        proj3SharedWithMeSkills.get(0).skillId == "skill1"
        proj3SharedWithMeSkills.get(0).projectName == "Test Project#1"
        proj3SharedWithMeSkills.get(0).projectId == "TestProject1"
    }

    def "share skill with ALL other projects and then depend on that skill from another project"() {
        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(2, 1, 1)

        def proj2 = SkillsFactory.createProject(2)
        def proj2_subj = SkillsFactory.createSubject(2, 2)
        List<Map> proj2_skills = SkillsFactory.createSkills(2, 2, 2)

        def proj3 = SkillsFactory.createProject(3)

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills)

        skillsService.createProject(proj2)
        skillsService.createSubject(proj2_subj)
        skillsService.createSkills(proj2_skills)

        skillsService.createProject(proj3)

        when:
        skillsService.shareSkill(proj1.projectId, proj1_skills.get(0).skillId, 'ALL_SKILLS_PROJECTS')
        def proj1SharedSkills = skillsService.getSharedSkills(proj1.projectId)
        def proj1SharedWithMeSkills = skillsService.getSharedWithMeSkills(proj1.projectId)

        def proj2SharedSkills = skillsService.getSharedSkills(proj2.projectId)
        def proj2SharedWithMeSkills = skillsService.getSharedWithMeSkills(proj2.projectId)

        def proj3SharedSkills = skillsService.getSharedSkills(proj3.projectId)
        def proj3SharedWithMeSkills = skillsService.getSharedWithMeSkills(proj3.projectId)

        skillsService.addLearningPathPrerequisite(proj2.projectId, proj2_skills.get(0).skillId, proj1.projectId, proj1_skills.get(0).skillId)

        def sharedSkillInfo = skillsService.getSkillDependencyInfo("user1", proj2.projectId, proj2_skills.get(0).skillId)


        then:
        proj1SharedSkills.size() == 1
        proj1SharedSkills.get(0).skillName == "Test Skill 1"
        proj1SharedSkills.get(0).skillId == "skill1"
        proj1SharedSkills.get(0).projectName == null
        proj1SharedSkills.get(0).projectId == null
        proj1SharedSkills.get(0).sharedWithAllProjects
        !proj1SharedWithMeSkills

        !proj2SharedSkills
        proj2SharedWithMeSkills.size() == 1
        proj2SharedWithMeSkills.get(0).skillName == "Test Skill 1"
        proj2SharedWithMeSkills.get(0).skillId == "skill1"
        proj2SharedWithMeSkills.get(0).projectName == "Test Project#1"
        proj2SharedWithMeSkills.get(0).projectId == "TestProject1"

        !proj3SharedSkills
        proj3SharedWithMeSkills.size() == 1
        proj3SharedWithMeSkills.get(0).skillName == "Test Skill 1"
        proj3SharedWithMeSkills.get(0).skillId == "skill1"
        proj3SharedWithMeSkills.get(0).projectName == "Test Project#1"
        proj3SharedWithMeSkills.get(0).projectId == "TestProject1"

        sharedSkillInfo.dependencies.size() == 1
        sharedSkillInfo.dependencies.get(0).dependsOn.skillId == "skill1"
        sharedSkillInfo.dependencies.get(0).dependsOn.projectId == "TestProject1"
        sharedSkillInfo.dependencies.get(0).dependsOn.projectName == "Test Project#1"
    }

    def "share skill with another project managed by a different admin"() {
        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(2, 1, 1)

        def proj2 = SkillsFactory.createProject(2)
        def proj2_subj = SkillsFactory.createSubject(2, 2)
        List<Map> proj2_skills = SkillsFactory.createSkills(2, 2, 2)

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills)

        skillsServiceAdmin2.createProject(proj2)
        skillsServiceAdmin2.createSubject(proj2_subj)
        skillsServiceAdmin2.createSkills(proj2_skills)

        when:
        skillsService.shareSkill(proj1.projectId, proj1_skills.get(0).skillId, proj2.projectId)
        def proj1SharedSkills = skillsService.getSharedSkills(proj1.projectId)
        def proj1SharedWithMeSkills = skillsService.getSharedWithMeSkills(proj1.projectId)

        def proj2SharedSkills = skillsServiceAdmin2.getSharedSkills(proj2.projectId)
        def proj2SharedWithMeSkills = skillsServiceAdmin2.getSharedWithMeSkills(proj2.projectId)

        then:
        proj1SharedSkills.size() == 1
        proj1SharedSkills.get(0).skillName == "Test Skill 1"
        proj1SharedSkills.get(0).skillId == "skill1"
        proj1SharedSkills.get(0).projectName == "Test Project#2"
        proj1SharedSkills.get(0).projectId == "TestProject2"
        !proj1SharedWithMeSkills

        !proj2SharedSkills
        proj2SharedWithMeSkills.size() == 1
        proj2SharedWithMeSkills.get(0).skillName == "Test Skill 1"
        proj2SharedWithMeSkills.get(0).skillId == "skill1"
        proj2SharedWithMeSkills.get(0).projectName == "Test Project#1"
        proj2SharedWithMeSkills.get(0).projectId == "TestProject1"
    }

    def "only admins of a project should see shared skills"() {
        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(2, 1, 1)

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills)

        when:
        def proj1SharedSkills = skillsServiceAdmin2.getSharedSkills(proj1.projectId)

        then:
        SkillsClientException skillsClientException = thrown()
        skillsClientException.message.contains("code=403 FORBIDDEN")
    }

    def "remove shared skill"() {
        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(3, 1, 1)

        def proj2 = SkillsFactory.createProject(2)
        def proj2_subj = SkillsFactory.createSubject(2, 2)
        List<Map> proj2_skills = SkillsFactory.createSkills(2, 2, 2)

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills)

        skillsService.createProject(proj2)
        skillsService.createSubject(proj2_subj)
        skillsService.createSkills(proj2_skills)

        when:
        skillsService.shareSkill(proj1.projectId, proj1_skills.get(0).skillId, proj2.projectId)
        skillsService.shareSkill(proj1.projectId, proj1_skills.get(1).skillId, proj2.projectId)
        skillsService.shareSkill(proj1.projectId, proj1_skills.get(2).skillId, proj2.projectId)

        def proj1SharedSkills = skillsService.getSharedSkills(proj1.projectId)
        def proj1SharedWithMeSkills = skillsService.getSharedWithMeSkills(proj1.projectId)

        def proj2SharedSkills = skillsService.getSharedSkills(proj2.projectId)
        def proj2SharedWithMeSkills = skillsService.getSharedWithMeSkills(proj2.projectId)

        skillsService.deleteShared(proj1.projectId, proj1_skills.get(1).skillId, proj2.projectId)

        def proj1SharedSkills_afterDelete = skillsService.getSharedSkills(proj1.projectId)
        def proj2SharedWithMeSkills_afterDelete = skillsService.getSharedWithMeSkills(proj2.projectId)

        then:
        proj1SharedSkills.size() == 3
        proj2SharedWithMeSkills.size() == 3
        !proj1SharedWithMeSkills
        !proj2SharedSkills

        proj1SharedSkills_afterDelete.size() == 2
        proj1SharedSkills_afterDelete.find { it.skillId == "skill1" }.projectId == "TestProject2"
        proj1SharedSkills_afterDelete.find { it.skillId == "skill3" }.projectId == "TestProject2"

        proj2SharedWithMeSkills_afterDelete.size() == 2
        proj2SharedWithMeSkills_afterDelete.find { it.skillId == "skill1" }.projectId == "TestProject1"
        proj2SharedWithMeSkills_afterDelete.find { it.skillId == "skill3" }.projectId == "TestProject1"
    }

    def "remove shared skill with ALL projects"() {
        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(3, 1, 1)

        def proj2 = SkillsFactory.createProject(2)
        def proj2_subj = SkillsFactory.createSubject(2, 2)
        List<Map> proj2_skills = SkillsFactory.createSkills(2, 2, 2)

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills)

        skillsService.createProject(proj2)
        skillsService.createSubject(proj2_subj)
        skillsService.createSkills(proj2_skills)

        when:
        skillsService.shareSkill(proj1.projectId, proj1_skills.get(0).skillId, 'ALL_SKILLS_PROJECTS')
        skillsService.shareSkill(proj1.projectId, proj1_skills.get(1).skillId, 'ALL_SKILLS_PROJECTS')
        skillsService.shareSkill(proj1.projectId, proj1_skills.get(2).skillId, 'ALL_SKILLS_PROJECTS')

        def proj1SharedSkills = skillsService.getSharedSkills(proj1.projectId)
        def proj1SharedWithMeSkills = skillsService.getSharedWithMeSkills(proj1.projectId)

        def proj2SharedSkills = skillsService.getSharedSkills(proj2.projectId)
        def proj2SharedWithMeSkills = skillsService.getSharedWithMeSkills(proj2.projectId)

        skillsService.deleteShared(proj1.projectId, proj1_skills.get(1).skillId, 'ALL_SKILLS_PROJECTS')

        def proj1SharedSkills_afterDelete = skillsService.getSharedSkills(proj1.projectId)
        def proj2SharedWithMeSkills_afterDelete = skillsService.getSharedWithMeSkills(proj2.projectId)

        then:
        proj1SharedSkills.size() == 3
        proj2SharedWithMeSkills.size() == 3
        !proj1SharedWithMeSkills
        !proj2SharedSkills

        proj1SharedSkills_afterDelete.size() == 2
        proj1SharedSkills_afterDelete.find { it.skillId == "skill1" }.projectId == null
        proj1SharedSkills_afterDelete.find { it.skillId == "skill3" }.projectId == null

        proj2SharedWithMeSkills_afterDelete.size() == 2
        proj2SharedWithMeSkills_afterDelete.find { it.skillId == "skill1" }.projectId == "TestProject1"
        proj2SharedWithMeSkills_afterDelete.find { it.skillId == "skill3" }.projectId == "TestProject1"
    }

    def "delete cross-project dependency skill"() {
        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(10, 1, 1)

        def proj2 = SkillsFactory.createProject(2)
        def proj2_subj = SkillsFactory.createSubject(2, 2)
        List<Map> proj2_skills = SkillsFactory.createSkills(2, 2, 2)

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills)

        skillsService.createProject(proj2)
        skillsService.createSubject(proj2_subj)
        skillsService.createSkills(proj2_skills)

        when:
        skillsService.shareSkill(proj1.projectId, proj1_skills.get(1).skillId, proj2.projectId)
        skillsService.addLearningPathPrerequisite(proj2.projectId, proj2_skills.get(0).skillId, proj1.projectId, proj1_skills.get(1).skillId)

        def sharedSkillInfo = skillsService.getSkillDependencyInfo("user1", proj2.projectId, proj2_skills.get(0).skillId)

        skillsService.deleteSkill([projectId: proj1.projectId, subjectId: proj1_subj.subjectId, skillId: proj1_skills.get(1).skillId,])

        def sharedSkillInfoAfterDelete = skillsService.getSkillDependencyInfo("user1", proj2.projectId, proj2_skills.get(0).skillId)

        then:
        sharedSkillInfo.dependencies.size() == 1
        sharedSkillInfo.dependencies.get(0).dependsOn.skillId == "skill2"
        sharedSkillInfo.dependencies.get(0).dependsOn.projectId == "TestProject1"
        sharedSkillInfo.dependencies.get(0).dependsOn.projectName == "Test Project#1"

        !sharedSkillInfoAfterDelete.dependencies
    }
    def "delete cross-project dependency skill shared with ALL projects"() {
        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(10, 1, 1)

        def proj2 = SkillsFactory.createProject(2)
        def proj2_subj = SkillsFactory.createSubject(2, 2)
        List<Map> proj2_skills = SkillsFactory.createSkills(2, 2, 2)

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills)

        skillsService.createProject(proj2)
        skillsService.createSubject(proj2_subj)
        skillsService.createSkills(proj2_skills)

        when:
        skillsService.shareSkill(proj1.projectId, proj1_skills.get(1).skillId, 'ALL_SKILLS_PROJECTS')
        skillsService.addLearningPathPrerequisite(proj2.projectId, proj2_skills.get(0).skillId, proj1.projectId, proj1_skills.get(1).skillId)

        def sharedSkillInfo = skillsService.getSkillDependencyInfo("user1", proj2.projectId, proj2_skills.get(0).skillId)

        skillsService.deleteSkill([projectId: proj1.projectId, subjectId: proj1_subj.subjectId, skillId: proj1_skills.get(1).skillId,])

        def sharedSkillInfoAfterDelete = skillsService.getSkillDependencyInfo("user1", proj2.projectId, proj2_skills.get(0).skillId)

        then:
        sharedSkillInfo.dependencies.size() == 1
        sharedSkillInfo.dependencies.get(0).dependsOn.skillId == "skill2"
        sharedSkillInfo.dependencies.get(0).dependsOn.projectId == "TestProject1"
        sharedSkillInfo.dependencies.get(0).dependsOn.projectName == "Test Project#1"

        !sharedSkillInfoAfterDelete.dependencies
    }

    def "delete cross-project dependency transient skill"() {
        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(4, 1, 1)

        def proj2 = SkillsFactory.createProject(2)
        def proj2_subj = SkillsFactory.createSubject(2, 2)
        List<Map> proj2_skills = SkillsFactory.createSkills(2, 2, 2)

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills)

        skillsService.createProject(proj2)
        skillsService.createSubject(proj2_subj)
        skillsService.createSkills(proj2_skills)

        when:
        // project 1 internal dependencies
        skillsService.addLearningPathPrerequisite(proj1.projectId, proj1_skills.get(0).skillId, proj1_skills.get(1).skillId)
        skillsService.addLearningPathPrerequisite(proj1.projectId, proj1_skills.get(1).skillId, proj1_skills.get(2).skillId)
        skillsService.addLearningPathPrerequisite(proj1.projectId, proj1_skills.get(2).skillId, proj1_skills.get(3).skillId)

        // cross project dependency
        skillsService.shareSkill(proj1.projectId, proj1_skills.get(0).skillId, proj2.projectId)
        skillsService.addLearningPathPrerequisite(proj2.projectId, proj2_skills.get(0).skillId, proj1.projectId, proj1_skills.get(0).skillId)


        def crossProjectSharedInfo = skillsService.getSkillDependencyInfo("user1", proj2.projectId, proj2_skills.get(0).skillId)

        def proj1Skill1SharedInfo = skillsService.getSkillDependencyInfo("user1", proj1.projectId, proj1_skills.get(0).skillId)
        def proj2Skill1SharedInfo = skillsService.getSkillDependencyInfo("user1", proj1.projectId, proj1_skills.get(1).skillId)
        def proj3Skill1SharedInfo = skillsService.getSkillDependencyInfo("user1", proj1.projectId, proj1_skills.get(2).skillId)

        skillsService.deleteSkill([projectId: proj1.projectId, subjectId: proj1_subj.subjectId, skillId: proj1_skills.get(2).skillId,])

        def crossProjectSharedInfoAfterDelete = skillsService.getSkillDependencyInfo("user1", proj2.projectId, proj2_skills.get(0).skillId)
        def proj1Skill1SharedInfoAfterDelete = skillsService.getSkillDependencyInfo("user1", proj1.projectId, proj1_skills.get(0).skillId)
        def proj2Skill1SharedInfoAfterDelete = skillsService.getSkillDependencyInfo("user1", proj1.projectId, proj1_skills.get(1).skillId)

        then:
        crossProjectSharedInfo.dependencies.size() == 1
        crossProjectSharedInfo.dependencies.get(0).dependsOn.skillId == "skill1"
        crossProjectSharedInfo.dependencies.get(0).dependsOn.projectId == "TestProject1"

        proj1Skill1SharedInfo.dependencies.size() == 3
        proj1Skill1SharedInfo.dependencies.find { it.skill.skillId == "skill1" }.dependsOn.skillId == "skill2"
        proj1Skill1SharedInfo.dependencies.find { it.skill.skillId == "skill1" }.dependsOn.projectId == "TestProject1"
        proj1Skill1SharedInfo.dependencies.find { it.skill.skillId == "skill2" }.dependsOn.skillId == "skill3"
        proj1Skill1SharedInfo.dependencies.find { it.skill.skillId == "skill2" }.dependsOn.projectId == "TestProject1"
        proj1Skill1SharedInfo.dependencies.find { it.skill.skillId == "skill3" }.dependsOn.skillId == "skill4"
        proj1Skill1SharedInfo.dependencies.find { it.skill.skillId == "skill3" }.dependsOn.projectId == "TestProject1"

        proj2Skill1SharedInfo.dependencies.size() == 2
        proj1Skill1SharedInfo.dependencies.find { it.skill.skillId == "skill2" }.dependsOn.skillId == "skill3"
        proj1Skill1SharedInfo.dependencies.find { it.skill.skillId == "skill2" }.dependsOn.projectId == "TestProject1"
        proj1Skill1SharedInfo.dependencies.find { it.skill.skillId == "skill3" }.dependsOn.skillId == "skill4"
        proj1Skill1SharedInfo.dependencies.find { it.skill.skillId == "skill3" }.dependsOn.projectId == "TestProject1"

        proj3Skill1SharedInfo.dependencies.size() == 1
        proj1Skill1SharedInfo.dependencies.find { it.skill.skillId == "skill3" }.dependsOn.skillId == "skill4"
        proj1Skill1SharedInfo.dependencies.find { it.skill.skillId == "skill3" }.dependsOn.projectId == "TestProject1"

        crossProjectSharedInfoAfterDelete.dependencies.size() == 1
        crossProjectSharedInfoAfterDelete.dependencies.get(0).dependsOn.skillId == "skill1"
        crossProjectSharedInfoAfterDelete.dependencies.get(0).dependsOn.projectId == "TestProject1"

        proj1Skill1SharedInfoAfterDelete.dependencies.size() == 1
        proj1Skill1SharedInfoAfterDelete.dependencies.get(0).dependsOn.skillId == "skill2"
        proj1Skill1SharedInfoAfterDelete.dependencies.get(0).dependsOn.projectId == "TestProject1"

        !proj2Skill1SharedInfoAfterDelete.dependencies.size()
    }

    def "delete skill when it has cross-project dependency"() {
        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(3, 1, 1)

        def proj2 = SkillsFactory.createProject(2)
        def proj2_subj = SkillsFactory.createSubject(2, 2)
        List<Map> proj2_skills = SkillsFactory.createSkills(2, 2, 2)

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills)

        skillsService.createProject(proj2)
        skillsService.createSubject(proj2_subj)
        skillsService.createSkills(proj2_skills)

        when:
        skillsService.shareSkill(proj1.projectId, proj1_skills.get(1).skillId, proj2.projectId)
        skillsService.addLearningPathPrerequisite(proj2.projectId, proj2_skills.get(0).skillId, proj1.projectId, proj1_skills.get(1).skillId)

        def sharedSkillInfo = skillsService.getSkillDependencyInfo("user1", proj2.projectId, proj2_skills.get(0).skillId)

        skillsService.deleteSkill([projectId: proj2.projectId, subjectId: proj2_subj.subjectId, skillId: proj2_skills.get(0).skillId,])

        then:
        sharedSkillInfo.dependencies.size() == 1
        sharedSkillInfo.dependencies.get(0).dependsOn.skillId == "skill2"
        sharedSkillInfo.dependencies.get(0).dependsOn.projectId == "TestProject1"
        sharedSkillInfo.dependencies.get(0).dependsOn.projectName == "Test Project#1"
    }


    def "dependent skill from another project was achieved by another user - that shouldn't affect me"() {
        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(3, 1, 1)
        proj1_skills.each{
            it.pointIncrement = 40
        }

        def proj2 = SkillsFactory.createProject(2)
        def proj2_subj = SkillsFactory.createSubject(2, 2)
        List<Map> proj2_skills = SkillsFactory.createSkills(2, 2, 2)
        proj2_skills.each{
            it.pointIncrement = 50
        }

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills)

        skillsService.createProject(proj2)
        skillsService.createSubject(proj2_subj)
        skillsService.createSkills(proj2_skills)

        skillsService.shareSkill(proj1.projectId, proj1_skills.get(0).skillId, proj2.projectId)

        when:
        skillsService.addLearningPathPrerequisite(proj2.projectId, proj2_skills.get(0).skillId, proj1.projectId, proj1_skills.get(0).skillId)
        def res1 = skillsService.addSkill([projectId: proj2.projectId, skillId: proj2_skills.get(0).skillId])
        def res2 = skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(0).skillId], "phil", new Date())
        def res3 = skillsService.addSkill([projectId: proj2.projectId, skillId: proj2_skills.get(0).skillId])
        then:
        !res1.body.skillApplied
        res1.body.explanation == "Not all dependent skills have been achieved. Missing achievements for 1 out of 1. Waiting on completion of [TestProject1:skill1]."

        res2.body.skillApplied
        res2.body.explanation == "Skill event was applied"

        !res3.body.skillApplied
        res3.body.explanation == "Not all dependent skills have been achieved. Missing achievements for 1 out of 1. Waiting on completion of [TestProject1:skill1]."
    }

    def "attempt to assign dependency that already exist"() {
        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(3, 1, 1)

        def proj2 = SkillsFactory.createProject(2)
        def proj2_subj = SkillsFactory.createSubject(2, 2)
        List<Map> proj2_skills = SkillsFactory.createSkills(2, 2, 2)

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills)

        skillsService.createProject(proj2)
        skillsService.createSubject(proj2_subj)
        skillsService.createSkills(proj2_skills)

        skillsService.shareSkill(proj1.projectId, proj1_skills.get(0).skillId, proj2.projectId)

        skillsService.addLearningPathPrerequisite(proj2.projectId, proj2_skills.get(0).skillId, proj1.projectId, proj1_skills.get(0).skillId)
        when:
        skillsService.addLearningPathPrerequisite(proj2.projectId, proj2_skills.get(0).skillId, proj1.projectId, proj1_skills.get(0).skillId)

        then:
        SkillsClientException e = thrown()
        e.message.contains("Learning path from [Test Skill 1] to [Test Skill 1 Subject2] already exists")
    }


    def "cannot create project with reserved project id"() {

        def proj1 = SkillsFactory.createProject(1)
        proj1.projectId = 'ALL_SKILLS_PROJECTS'

        when:

        skillsService.createProject(proj1)

        then:
        SkillsClientException skillsClientException = thrown()
        skillsClientException.httpStatus == HttpStatus.BAD_REQUEST
    }

    def "cannot share with Inception project"() {
        def proj1 = SkillsFactory.createProject(999)
        proj1.projectId = "noInception"

        def proj2 = SkillsFactory.createProject(998)
        proj2.projectId = "noInception2"

        def proj3 = SkillsFactory.createProject(997)
        proj3.projectId = "Inception"
        proj3.name = "Inception"

        skillsService.createProject(proj1)
        skillsService.createProject(proj2)
        skillsService.createProject(proj3)

        when:
        def results = skillsService.searchOtherProjectsByName(proj1.projectId, "")

        then:
        !results.find() { it.projectId.toLowerCase() == "inception" }
        results.size() == 1
    }

    def "removing shared skill removes it from learning paths"() {
        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(3, 1, 1)

        def proj2 = SkillsFactory.createProject(2)
        def proj2_subj = SkillsFactory.createSubject(2, 2)
        List<Map> proj2_skills = SkillsFactory.createSkills(2, 2, 2)

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills)

        skillsService.createProject(proj2)
        skillsService.createSubject(proj2_subj)
        skillsService.createSkills(proj2_skills)

        when:
        skillsService.shareSkill(proj1.projectId, proj1_skills.get(0).skillId, proj2.projectId)
        skillsService.addLearningPathPrerequisite(proj2.projectId, proj2_skills.get(0).skillId, proj1.projectId, proj1_skills.get(0).skillId)

        def proj1SharedSkills = skillsService.getSharedSkills(proj1.projectId)
        def proj1SharedWithMeSkills = skillsService.getSharedWithMeSkills(proj1.projectId)

        def proj2SharedSkills = skillsService.getSharedSkills(proj2.projectId)
        def proj2SharedWithMeSkills = skillsService.getSharedWithMeSkills(proj2.projectId)

        def hasDependencyBeforeDelete = skillsService.checkIfSkillsHaveDependencies(proj2.projectId, [proj2_skills.get(0).skillId])

        skillsService.deleteShared(proj1.projectId, proj1_skills.get(0).skillId, proj2.projectId)

        def proj1SharedSkills_afterDelete = skillsService.getSharedSkills(proj1.projectId)
        def proj2SharedWithMeSkills_afterDelete = skillsService.getSharedWithMeSkills(proj2.projectId)
        def hasDependencyAfterDelete = skillsService.checkIfSkillsHaveDependencies(proj2.projectId, [proj2_skills.get(0).skillId])

        then:
        proj1SharedSkills.size() == 1
        proj2SharedWithMeSkills.size() == 1
        !proj1SharedWithMeSkills
        !proj2SharedSkills
        hasDependencyBeforeDelete[0].skillId == proj2_skills.get(0).skillId
        hasDependencyBeforeDelete[0].hasDependency == true

        proj1SharedSkills_afterDelete.size() == 0
        proj2SharedWithMeSkills_afterDelete.size() == 0
        hasDependencyAfterDelete[0].skillId == proj2_skills.get(0).skillId
        hasDependencyAfterDelete[0].hasDependency == false
    }

    def "removing shared skill with all projects removes it from learning paths"() {
        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(3, 1, 1)

        def proj2 = SkillsFactory.createProject(2)
        def proj2_subj = SkillsFactory.createSubject(2, 2)
        List<Map> proj2_skills = SkillsFactory.createSkills(2, 2, 2)

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills)

        skillsService.createProject(proj2)
        skillsService.createSubject(proj2_subj)
        skillsService.createSkills(proj2_skills)

        when:
        skillsService.shareSkill(proj1.projectId, proj1_skills.get(0).skillId, 'ALL_SKILLS_PROJECTS')
        skillsService.addLearningPathPrerequisite(proj2.projectId, proj2_skills.get(0).skillId, proj1.projectId, proj1_skills.get(0).skillId)

        def proj1SharedSkills = skillsService.getSharedSkills(proj1.projectId)
        def proj1SharedWithMeSkills = skillsService.getSharedWithMeSkills(proj1.projectId)

        def proj2SharedSkills = skillsService.getSharedSkills(proj2.projectId)
        def proj2SharedWithMeSkills = skillsService.getSharedWithMeSkills(proj2.projectId)

        def hasDependencyBeforeDelete = skillsService.checkIfSkillsHaveDependencies(proj2.projectId, [proj2_skills.get(0).skillId])

        skillsService.deleteShared(proj1.projectId, proj1_skills.get(0).skillId, 'ALL_SKILLS_PROJECTS')

        def proj1SharedSkills_afterDelete = skillsService.getSharedSkills(proj1.projectId)
        def proj2SharedWithMeSkills_afterDelete = skillsService.getSharedWithMeSkills(proj2.projectId)
        def hasDependencyAfterDelete = skillsService.checkIfSkillsHaveDependencies(proj2.projectId, [proj2_skills.get(0).skillId])

        then:
        proj1SharedSkills.size() == 1
        proj2SharedWithMeSkills.size() == 1
        !proj1SharedWithMeSkills
        !proj2SharedSkills
        hasDependencyBeforeDelete[0].skillId == proj2_skills.get(0).skillId
        hasDependencyBeforeDelete[0].hasDependency == true

        proj1SharedSkills_afterDelete.size() == 0
        proj2SharedWithMeSkills_afterDelete.size() == 0
        hasDependencyAfterDelete[0].skillId == proj2_skills.get(0).skillId
        hasDependencyAfterDelete[0].hasDependency == false
    }

    def "deleting shared skill removes it from learning paths"() {
        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(3, 1, 1)

        def proj2 = SkillsFactory.createProject(2)
        def proj2_subj = SkillsFactory.createSubject(2, 2)
        List<Map> proj2_skills = SkillsFactory.createSkills(2, 2, 2)

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills)

        skillsService.createProject(proj2)
        skillsService.createSubject(proj2_subj)
        skillsService.createSkills(proj2_skills)

        when:
        skillsService.shareSkill(proj1.projectId, proj1_skills.get(0).skillId, proj2.projectId)
        skillsService.addLearningPathPrerequisite(proj2.projectId, proj2_skills.get(0).skillId, proj1.projectId, proj1_skills.get(0).skillId)

        def proj1SharedSkills = skillsService.getSharedSkills(proj1.projectId)
        def proj1SharedWithMeSkills = skillsService.getSharedWithMeSkills(proj1.projectId)

        def proj2SharedSkills = skillsService.getSharedSkills(proj2.projectId)
        def proj2SharedWithMeSkills = skillsService.getSharedWithMeSkills(proj2.projectId)

        def hasDependencyBeforeDelete = skillsService.checkIfSkillsHaveDependencies(proj2.projectId, [proj2_skills.get(0).skillId])

        skillsService.deleteSkill([projectId: proj1.projectId, subjectId: proj1_subj.subjectId, skillId: proj1_skills.get(0).skillId,])

        def proj1SharedSkills_afterDelete = skillsService.getSharedSkills(proj1.projectId)
        def proj2SharedWithMeSkills_afterDelete = skillsService.getSharedWithMeSkills(proj2.projectId)
        def hasDependencyAfterDelete = skillsService.checkIfSkillsHaveDependencies(proj2.projectId, [proj2_skills.get(0).skillId])

        then:
        proj1SharedSkills.size() == 1
        proj2SharedWithMeSkills.size() == 1
        !proj1SharedWithMeSkills
        !proj2SharedSkills
        hasDependencyBeforeDelete[0].skillId == proj2_skills.get(0).skillId
        hasDependencyBeforeDelete[0].hasDependency == true

        proj1SharedSkills_afterDelete.size() == 0
        proj2SharedWithMeSkills_afterDelete.size() == 0
        hasDependencyAfterDelete[0].skillId == proj2_skills.get(0).skillId
        hasDependencyAfterDelete[0].hasDependency == false
    }

    def "deleting shared skill with all projects removes it from learning paths"() {
        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(3, 1, 1)

        def proj2 = SkillsFactory.createProject(2)
        def proj2_subj = SkillsFactory.createSubject(2, 2)
        List<Map> proj2_skills = SkillsFactory.createSkills(2, 2, 2)

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills)

        skillsService.createProject(proj2)
        skillsService.createSubject(proj2_subj)
        skillsService.createSkills(proj2_skills)

        when:
        skillsService.shareSkill(proj1.projectId, proj1_skills.get(0).skillId, 'ALL_SKILLS_PROJECTS')
        skillsService.addLearningPathPrerequisite(proj2.projectId, proj2_skills.get(0).skillId, proj1.projectId, proj1_skills.get(0).skillId)

        def proj1SharedSkills = skillsService.getSharedSkills(proj1.projectId)
        def proj1SharedWithMeSkills = skillsService.getSharedWithMeSkills(proj1.projectId)

        def proj2SharedSkills = skillsService.getSharedSkills(proj2.projectId)
        def proj2SharedWithMeSkills = skillsService.getSharedWithMeSkills(proj2.projectId)

        def hasDependencyBeforeDelete = skillsService.checkIfSkillsHaveDependencies(proj2.projectId, [proj2_skills.get(0).skillId])

        skillsService.deleteSkill([projectId: proj1.projectId, subjectId: proj1_subj.subjectId, skillId: proj1_skills.get(0).skillId,])

        def proj1SharedSkills_afterDelete = skillsService.getSharedSkills(proj1.projectId)
        def proj2SharedWithMeSkills_afterDelete = skillsService.getSharedWithMeSkills(proj2.projectId)
        def hasDependencyAfterDelete = skillsService.checkIfSkillsHaveDependencies(proj2.projectId, [proj2_skills.get(0).skillId])

        then:
        proj1SharedSkills.size() == 1
        proj2SharedWithMeSkills.size() == 1
        !proj1SharedWithMeSkills
        !proj2SharedSkills
        hasDependencyBeforeDelete[0].skillId == proj2_skills.get(0).skillId
        hasDependencyBeforeDelete[0].hasDependency == true

        proj1SharedSkills_afterDelete.size() == 0
        proj2SharedWithMeSkills_afterDelete.size() == 0
        hasDependencyAfterDelete[0].skillId == proj2_skills.get(0).skillId
        hasDependencyAfterDelete[0].hasDependency == false
    }
}
