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
package skills.intTests.adminDisplayOrder

import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsFactory

class SkillsOrderSpecs extends DefaultIntSpec {
    def proj
    def subj
    List skills
    def setup(){
        proj = SkillsFactory.createProject()
        subj = SkillsFactory.createSubject()
        skillsService.createProject(proj)
        skillsService.createSubject(subj)

        int numSkills = 5
        skills = (1..numSkills).collect {
            def skill = SkillsFactory.createSkill(1, 1, it)
            skillsService.createSkill(skill)
            return skill
        }
    }

    def "move skill down"() {
        when:
        def beforeMove = skillsService.getSkillsForSubject(proj.projectId, subj.subjectId)
        skillsService.moveSkillDown(skills.first())
        def afterMove = skillsService.getSkillsForSubject(proj.projectId, subj.subjectId)
        then:
        beforeMove.collect({it.skillId}) == ["skill1", "skill2", "skill3", "skill4", "skill5"]
        beforeMove.collect({it.displayOrder}) == [1, 2, 3, 4, 5]
        afterMove.collect({it.skillId}) == ["skill2", "skill1", "skill3", "skill4", "skill5"]
        afterMove.collect({it.displayOrder}) == [1, 2, 3, 4, 5]
    }

    def "should not be able to move down the last skill"() {
        when:
        def beforeMove = skillsService.getSkillsForSubject(proj.projectId, subj.subjectId)
        skillsService.moveSkillDown(skills.last())
        then:
        thrown(SkillsClientException)
        beforeMove.collect({it.skillId}) == ["skill1", "skill2", "skill3", "skill4", "skill5"]
        skillsService.getSkillsForSubject(proj.projectId, subj.subjectId).collect({it.skillId}) == ["skill1", "skill2", "skill3", "skill4", "skill5"]
    }

    def "move skill up"() {
        when:
        def beforeMove = skillsService.getSkillsForSubject(proj.projectId, subj.subjectId)
        skillsService.moveSkillUp(skills.get(1))
        def afterMove = skillsService.getSkillsForSubject(proj.projectId, subj.subjectId)
        then:
        beforeMove.collect({it.skillId}) == ["skill1", "skill2", "skill3", "skill4", "skill5"]
        beforeMove.collect({it.displayOrder}) == [1, 2, 3, 4, 5]
        afterMove.collect({it.skillId}) == ["skill2", "skill1", "skill3", "skill4", "skill5"]
        afterMove.collect({it.displayOrder}) == [1, 2, 3, 4, 5]
    }

    def "should not be able to move the first skill up"() {
        when:
        def beforeMove = skillsService.getSkillsForSubject(proj.projectId, subj.subjectId)
        skillsService.moveSkillUp(skills.first())
        then:
        thrown(SkillsClientException)
        beforeMove.collect({it.skillId}) == ["skill1", "skill2", "skill3", "skill4", "skill5"]
        def afterMove = skillsService.getSkillsForSubject(proj.projectId, subj.subjectId)
        afterMove.collect({it.skillId}) == ["skill1", "skill2", "skill3", "skill4", "skill5"]
        afterMove.collect({it.displayOrder}) == [1, 2, 3, 4, 5]
    }

    def "attempt to move skill that doesn't exist"(){
        when:
        skillsService.moveSkillUp(projectId: skills.first().projectId, subjectId: skills.first().subjectId, skillId: "doesntexist")
        then:
        SkillsClientException e = thrown(SkillsClientException)
        e.message.contains("Failed to find skillId")
    }

    def "display order is correct when skill is deleted from the middle"() {
        when:
        skillsService.deleteSkill(skills.get(2))
        then:
        skillsService.getSkillsForSubject(proj.projectId, subj.subjectId).collect({it.displayOrder}) == [1, 2, 3, 4]
    }

    def "display order is correct when skill is deleted from the beginning"() {
        when:
        skillsService.deleteSkill(skills.get(0))
        then:
        skillsService.getSkillsForSubject(proj.projectId, subj.subjectId).collect({it.displayOrder}) == [1, 2, 3, 4]
    }

    def "display order is correct when skill is deleted from the end"() {
        when:
        skillsService.deleteSkill(skills.get(4))
        then:
        skillsService.getSkillsForSubject(proj.projectId, subj.subjectId).collect({it.displayOrder}) == [1, 2, 3, 4]
    }

    def "display order is correct when skill is added"() {
        when:
        def newSkill = SkillsFactory.createSkill(1, 1, 6)
        skillsService.createSkill(newSkill)
        then:
        skillsService.getSkillsForSubject(proj.projectId, subj.subjectId).collect({it.displayOrder}) == [1, 2, 3, 4, 5, 6]
    }

    def "display order is correct when skill is added and other skill is deleted"() {
        when:
        def newSkill = SkillsFactory.createSkill(1, 1, 6)
        skillsService.createSkill(newSkill)
        skillsService.deleteSkill(skills.get(0))
        skillsService.deleteSkill(skills.get(3))
        then:
        def modifiedSkills = skillsService.getSkillsForSubject(proj.projectId, subj.subjectId)
        modifiedSkills.collect({it.displayOrder}) == [1, 2, 3, 4]
        modifiedSkills.collect({it.skillId}) == ['skill2', 'skill3', 'skill5', 'skill6']
    }
}
