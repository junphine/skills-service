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
package skills.intTests.reportSkills

import org.springframework.beans.factory.annotation.Autowired
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsFactory
import skills.storage.model.UserAchievement
import skills.storage.repos.UserAchievedLevelRepo
import spock.lang.IgnoreRest

class ReportSkills_BadgeSkillsSpecs extends DefaultIntSpec {

    String projId = SkillsFactory.defaultProjId

    def setup(){
        skillsService.deleteProjectIfExist(projId)
    }

    def "give credit if all dependencies were fulfilled"(){
        String subj = "testSubj"

        Map skill1 = [projectId: projId, subjectId: subj, skillId: "skill1", name  : "Test Skill 1", type: "Skill",
                      pointIncrement: 25, numPerformToCompletion: 1, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1]
        Map skill2 = [projectId: projId, subjectId: subj, skillId: "skill2", name  : "Test Skill 2", type: "Skill",
                      pointIncrement: 25, numPerformToCompletion: 1, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1]
        Map skill3 = [projectId: projId, subjectId: subj, skillId: "skill3", name  : "Test Skill 3", type: "Skill",
                      pointIncrement: 25, numPerformToCompletion: 1, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1]
        Map skill4 = [projectId: projId, subjectId: subj, skillId: "skill4", name  : "Test Skill 4", type: "Skill",
                      pointIncrement: 25, numPerformToCompletion: 1, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1*60, numMaxOccurrencesIncrementInterval: 1, dependentSkillsIds: [skill1.skillId, skill2.skillId, skill3.skillId]]

        Map badge = [projectId: projId, badgeId: 'badge1', name: 'Test Badge 1']
        List<String> requiredSkillsIds = [skill1.skillId, skill2.skillId, skill3.skillId, skill4.skillId]


        when:
        skillsService.createProject([projectId: projId, name: "Test Project"])
        skillsService.createSubject([projectId: projId, subjectId: subj, name: "Test Subject"])
        skillsService.createSkill(skill1)
        skillsService.createSkill(skill2)
        skillsService.createSkill(skill3)
        skillsService.createSkill(skill4)
        skillsService.createBadge(badge)
        requiredSkillsIds.each { skillId ->
            skillsService.assignSkillToBadge(projectId: projId, badgeId: badge.badgeId, skillId: skillId)
        }
        badge.enabled  = 'true'
        skillsService.updateBadge(badge, badge.badgeId)

        def resSkill1 = skillsService.addSkill([projectId: projId, skillId: skill1.skillId]).body
        def resSkill3 = skillsService.addSkill([projectId: projId, skillId: skill3.skillId]).body
        def resSkill2 = skillsService.addSkill([projectId: projId, skillId: skill2.skillId]).body
        def resSkill4 = skillsService.addSkill([projectId: projId, skillId: skill4.skillId]).body

        then:
        resSkill1.skillApplied && !resSkill1.completed.find { it.id == 'badge1'}
        resSkill2.skillApplied && !resSkill2.completed.find { it.id == 'badge1'}
        resSkill3.skillApplied && !resSkill3.completed.find { it.id == 'badge1'}
        resSkill4.skillApplied && resSkill4.completed.find { it.id == 'badge1'}
    }

    def "give credit if all dependencies were fulfilled, but the badge/gem is active"(){
        String subj = "testSubj"

        Map skill1 = [projectId: projId, subjectId: subj, skillId: "skill1", name  : "Test Skill 1", type: "Skill",
                      pointIncrement: 25, numPerformToCompletion: 1, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1]
        Map skill2 = [projectId: projId, subjectId: subj, skillId: "skill2", name  : "Test Skill 2", type: "Skill",
                      pointIncrement: 25, numPerformToCompletion: 1, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1]
        Map skill3 = [projectId: projId, subjectId: subj, skillId: "skill3", name  : "Test Skill 3", type: "Skill",
                      pointIncrement: 25, numPerformToCompletion: 1, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1]
        Map skill4 = [projectId: projId, subjectId: subj, skillId: "skill4", name  : "Test Skill 4", type: "Skill",
                      pointIncrement: 25, numPerformToCompletion: 1, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1, dependentSkillsIds: [skill1.skillId, skill2.skillId, skill3.skillId]]

        Date tomorrow = new Date()+1
        Date twoWeeksAgo = new Date()-14
        Map badge = [projectId: projId, badgeId: 'badge1', name: 'Test Badge 1', startDate: twoWeeksAgo, endDate: tomorrow]
        List<String> requiredSkillsIds = [skill1.skillId, skill2.skillId, skill3.skillId, skill4.skillId]

        when:
        skillsService.createProject([projectId: projId, name: "Test Project"])
        skillsService.createSubject([projectId: projId, subjectId: subj, name: "Test Subject"])
        skillsService.createSkill(skill1)
        skillsService.createSkill(skill2)
        skillsService.createSkill(skill3)
        skillsService.createSkill(skill4)
        skillsService.createBadge(badge)
        requiredSkillsIds.each { skillId ->
            skillsService.assignSkillToBadge(projectId: projId, badgeId: badge.badgeId, skillId: skillId)
        }
        badge.enabled  = 'true'
        skillsService.updateBadge(badge, badge.badgeId)

        def resSkill1 = skillsService.addSkill([projectId: projId, skillId: skill1.skillId]).body
        def resSkill3 = skillsService.addSkill([projectId: projId, skillId: skill3.skillId]).body
        def resSkill2 = skillsService.addSkill([projectId: projId, skillId: skill2.skillId]).body
        def resSkill4 = skillsService.addSkill([projectId: projId, skillId: skill4.skillId]).body

        then:
        resSkill1.skillApplied && !resSkill1.completed.find { it.id == 'badge1'}
        resSkill2.skillApplied && !resSkill2.completed.find { it.id == 'badge1'}
        resSkill3.skillApplied && !resSkill3.completed.find { it.id == 'badge1'}
        resSkill4.skillApplied && resSkill4.completed.find { it.id == 'badge1'}
    }

    def "do not give credit if all dependencies were fulfilled, but the badge/gem is not active"(){
        String subj = "testSubj"

        Map skill1 = [projectId: projId, subjectId: subj, skillId: "skill1", name  : "Test Skill 1", type: "Skill",
                      pointIncrement: 25, numPerformToCompletion: 1, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1]
        Map skill2 = [projectId: projId, subjectId: subj, skillId: "skill2", name  : "Test Skill 2", type: "Skill",
                      pointIncrement: 25, numPerformToCompletion: 1, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1]
        Map skill3 = [projectId: projId, subjectId: subj, skillId: "skill3", name  : "Test Skill 3", type: "Skill",
                      pointIncrement: 25, numPerformToCompletion: 1, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1]
        Map skill4 = [projectId: projId, subjectId: subj, skillId: "skill4", name  : "Test Skill 4", type: "Skill",
                      pointIncrement: 25, numPerformToCompletion: 1, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1, dependentSkillsIds: [skill1.skillId, skill2.skillId, skill3.skillId]]

        Date oneWeekAgo = new Date()-7
        Date twoWeeksAgo = new Date()-14
        Map badge = [projectId: projId, badgeId: 'badge1', name: 'Test Badge 1', startDate: twoWeeksAgo, endDate: oneWeekAgo]

        when:
        skillsService.createProject([projectId: projId, name: "Test Project"])
        skillsService.createSubject([projectId: projId, subjectId: subj, name: "Test Subject"])
        skillsService.createSkill(skill1)
        skillsService.createSkill(skill2)
        skillsService.createSkill(skill3)
        skillsService.createSkill(skill4)
        skillsService.createBadge(badge)

        def resSkill1 = skillsService.addSkill([projectId: projId, skillId: skill1.skillId]).body
        def resSkill3 = skillsService.addSkill([projectId: projId, skillId: skill3.skillId]).body
        def resSkill2 = skillsService.addSkill([projectId: projId, skillId: skill2.skillId]).body
        def resSkill4 = skillsService.addSkill([projectId: projId, skillId: skill4.skillId]).body

        List<String> requiredSkillsIds = [skill1.skillId, skill2.skillId, skill3.skillId, skill4.skillId]
        requiredSkillsIds.each { String skillId ->
            skillsService.assignSkillToBadge(projectId: projId, badgeId: badge.badgeId, skillId: skillId)
        }

        then:
        resSkill1.skillApplied && !resSkill1.completed.find { it.id == 'badge1'}
        resSkill2.skillApplied && !resSkill2.completed.find { it.id == 'badge1'}
        resSkill3.skillApplied && !resSkill3.completed.find { it.id == 'badge1'}
        resSkill4.skillApplied && !resSkill4.completed.find { it.id == 'badge1'}
    }

    def "gem not awarded if achieved after end date"(){
        String subj = "testSubj"

        Map skill1 = [projectId: projId, subjectId: subj, skillId: "skill1", name  : "Test Skill 1", type: "Skill",
                      pointIncrement: 25, numPerformToCompletion: 1, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1]
        Map skill2 = [projectId: projId, subjectId: subj, skillId: "skill2", name  : "Test Skill 2", type: "Skill",
                      pointIncrement: 25, numPerformToCompletion: 1, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1]
        Map skill3 = [projectId: projId, subjectId: subj, skillId: "skill3", name  : "Test Skill 3", type: "Skill",
                      pointIncrement: 25, numPerformToCompletion: 1, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1]
        Map skill4 = [projectId: projId, subjectId: subj, skillId: "skill4", name  : "Test Skill 4", type: "Skill",
                      pointIncrement: 25, numPerformToCompletion: 1, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1, dependentSkillsIds: [skill1.skillId, skill2.skillId, skill3.skillId]]

        Date oneWeekAgo = new Date()-7
        Date fiveWeeksAgo = new Date()-35

        Date threeWeeksAgo = new Date() -21

        Map badge = [projectId: projId, badgeId: 'badge1', name: 'Test Badge 1', startDate: fiveWeeksAgo, endDate: oneWeekAgo]

        when:
        skillsService.createProject([projectId: projId, name: "Test Project"])
        skillsService.createSubject([projectId: projId, subjectId: subj, name: "Test Subject"])
        skillsService.createSkill(skill1)
        skillsService.createSkill(skill2)
        skillsService.createSkill(skill3)
        skillsService.createSkill(skill4)
        skillsService.createBadge(badge)

        List<String> requiredSkillsIds = [skill1.skillId, skill2.skillId, skill3.skillId, skill4.skillId]
        requiredSkillsIds.each { String skillId ->
            skillsService.assignSkillToBadge(projectId: projId, badgeId: badge.badgeId, skillId: skillId)
        }
        badge.enabled = true
        skillsService.createBadge(badge)

        def resSkill1 = skillsService.addSkill([projectId: projId, skillId: skill1.skillId], "someuser", threeWeeksAgo).body
        def resSkill3 = skillsService.addSkill([projectId: projId, skillId: skill3.skillId], "someuser",threeWeeksAgo).body
        def resSkill2 = skillsService.addSkill([projectId: projId, skillId: skill2.skillId], "someuser",threeWeeksAgo).body
        def resSkill4 = skillsService.addSkill([projectId: projId, skillId: skill4.skillId], "someuser", new Date()).body

        def badgeSummary = skillsService.getBadgeSummary("someuser", projId, badge.badgeId)

        then:
        resSkill1.skillApplied && !resSkill1.completed.find { it.id == 'badge1'}
        resSkill2.skillApplied && !resSkill2.completed.find { it.id == 'badge1'}
        resSkill3.skillApplied && !resSkill3.completed.find { it.id == 'badge1'}
        resSkill4.skillApplied && !resSkill4.completed.find { it.id == 'badge1'}
        !badgeSummary.badgeAchieved
    }

    def 'validate that if one gem date is provided both dates need to be provided - start provided'() {
        when:
        skillsService.createProject([projectId: projId, name: "Test Project"])
        Map badge = [projectId: projId, badgeId: 'badge1', name: 'Test Badge 1', startDate: new Date()]
        skillsService.createBadge(badge)

        then:
        SkillsClientException e = thrown()
        e.message.contains("explanation:If one date is provided then both start and end dates must be provided")
        e.message.contains("errorCode:BadParam")
    }

    def 'validate that if one gem date is provided both dates need to be provided - end provided'() {
        when:
        skillsService.createProject([projectId: projId, name: "Test Project"])
        Map badge = [projectId: projId, badgeId: 'badge1', name: 'Test Badge 1', startDate: new Date()]
        skillsService.createBadge(badge)

        then:
        SkillsClientException e = thrown()
        e.message.contains("explanation:If one date is provided then both start and end dates must be provided")
        e.message.contains("errorCode:BadParam")
    }

    def 'badge not awarded if inactive'() {
        String subj = "testSubj"

        Map skill1 = [projectId: projId, subjectId: subj, skillId: "skill1", name  : "Test Skill 1", type: "Skill",
                      pointIncrement: 100, numPerformToCompletion: 1, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1]

        Map badge = [projectId: projId, badgeId: 'badge1', name: 'Test Badge 1']
        badge.enabled = false
        List<String> requiredSkillsIds = [skill1.skillId]


        when:
        skillsService.createProject([projectId: projId, name: "Test Project"])
        skillsService.createSubject([projectId: projId, subjectId: subj, name: "Test Subject"])
        skillsService.createSkill(skill1)
        skillsService.createBadge(badge)
        requiredSkillsIds.each { skillId ->
            skillsService.assignSkillToBadge(projectId: projId, badgeId: badge.badgeId, skillId: skillId)
        }

        def resSkill1 = skillsService.addSkill([projectId: projId, skillId: skill1.skillId]).body

        then:
        resSkill1.skillApplied && !resSkill1.completed.find { it.id == 'badge1'}
    }

    def "badge awarded to users with requirements after enabling"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(20)
        def badge = SkillsFactory.createBadge()

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        badge.enabled = false
        skillsService.createBadge(badge)
        skillsService.assignSkillToBadge([projectId: proj.projectId, badgeId: badge.badgeId, skillId: skills.get(0).skillId])
        skillsService.assignSkillToBadge([projectId: proj.projectId, badgeId: badge.badgeId, skillId: skills.get(1).skillId])

        skillsService.addSkill([skillId: skills.get(0).skillId, projectId: proj.projectId], "user1", new Date())
        skillsService.addSkill([skillId: skills.get(0).skillId, projectId: proj.projectId], "user2", new Date())
        skillsService.addSkill([skillId: skills.get(1).skillId, projectId: proj.projectId], "user1", new Date())

        when:
        skillsService.updateBadge([projectId: proj.projectId, badgeId: badge.badgeId, enabled: true, name: badge.name], badge.badgeId)

        def user1Summary = skillsService.getBadgeSummary("user1", proj.projectId, badge.badgeId)
        def user2Summary = skillsService.getBadgeSummary("user2", proj.projectId, badge.badgeId)

        then:
        user1Summary.badgeAchieved
        !user2Summary.badgeAchieved
    }

    def "badge awarded to users with requirements after enabling when dependencies include skills imported from catalog"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(10)
        def badge = SkillsFactory.createBadge()

        def proj2 = SkillsFactory.createProject(2)
        def subj2 = SkillsFactory.createSubject(2, 2)
        def toImport = SkillsFactory.createSkillsStartingAt(10, 11, 2, 2)

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        skillsService.createProject(proj2)
        skillsService.createSubject(subj2)
        skillsService.createSkills(toImport)

        toImport.each {
            skillsService.exportSkillToCatalog(it.projectId, it.skillId)
            skillsService.importSkillFromCatalog(proj.projectId, subj.subjectId, it.projectId, it.skillId)
        }
        skillsService.finalizeSkillsImportFromCatalog(proj.projectId, true)

        badge.enabled = false
        skillsService.createBadge(badge)
        skillsService.assignSkillToBadge([projectId: proj.projectId, badgeId: badge.badgeId, skillId: skills.get(0).skillId])
        skillsService.assignSkillToBadge([projectId: proj.projectId, badgeId: badge.badgeId, skillId: skills.get(1).skillId])
        skillsService.assignSkillToBadge([projectId: proj.projectId, badgeId: badge.badgeId, skillId: toImport[0].skillId])
        skillsService.assignSkillToBadge([projectId: proj.projectId, badgeId: badge.badgeId, skillId: toImport[1].skillId])

        skillsService.addSkill([skillId: skills.get(0).skillId, projectId: proj.projectId], "user1", new Date())
        skillsService.addSkill([skillId: skills.get(0).skillId, projectId: proj.projectId], "user2", new Date())
        skillsService.addSkill([skillId: skills.get(1).skillId, projectId: proj.projectId], "user1", new Date())
        skillsService.addSkill([skillId: toImport[0].skillId, projectId: proj2.projectId], "user1", new Date())
        skillsService.addSkill([skillId: toImport[1].skillId, projectId: proj2.projectId], "user1", new Date())

        skillsService.addSkill([skillId: skills.get(0).skillId, projectId: proj.projectId], "user3", new Date())
        skillsService.addSkill([skillId: skills.get(1).skillId, projectId: proj.projectId], "user3", new Date())
        skillsService.addSkill([skillId: toImport[0].skillId, projectId: proj2.projectId], "user3", new Date())

        waitForAsyncTasksCompletion.waitForAllScheduleTasks()

        when:
        skillsService.updateBadge([projectId: proj.projectId, badgeId: badge.badgeId, enabled: true, name: badge.name], badge.badgeId)

        def user1Summary = skillsService.getBadgeSummary("user1", proj.projectId, badge.badgeId)
        def user2Summary = skillsService.getBadgeSummary("user2", proj.projectId, badge.badgeId)
        def user3Summary = skillsService.getBadgeSummary("user3", proj.projectId, badge.badgeId)

        then:
        user1Summary.badgeAchieved
        !user2Summary.badgeAchieved
        !user3Summary.badgeAchieved
    }

    @Autowired
    UserAchievedLevelRepo userAchievedLevelRepo

    def "badge awarded to users with requirements after enabling, does not impact other badges"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(20)
        def badge = SkillsFactory.createBadge()

        def badge2 = SkillsFactory.createBadge(1, 2)

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        badge.enabled = false
        skillsService.createBadge(badge)
        skillsService.createBadge(badge2)
        skillsService.assignSkillToBadge([projectId: proj.projectId, badgeId: badge.badgeId, skillId: skills.get(0).skillId])
        skillsService.assignSkillToBadge([projectId: proj.projectId, badgeId: badge.badgeId, skillId: skills.get(1).skillId])

        skillsService.assignSkillToBadge([projectId: proj.projectId, badgeId: badge2.badgeId, skillId: skills[0].skillId])
        skillsService.assignSkillToBadge([projectId: proj.projectId, badgeId: badge2.badgeId, skillId: skills[5].skillId])
        badge2.enabled = true
        skillsService.createBadge(badge2)

        skillsService.addSkill([skillId: skills.get(0).skillId, projectId: proj.projectId], "user1", new Date())
        skillsService.addSkill([skillId: skills.get(0).skillId, projectId: proj.projectId], "user2", new Date())
        skillsService.addSkill([skillId: skills.get(1).skillId, projectId: proj.projectId], "user1", new Date())

        skillsService.addSkill([skillId: skills[0].skillId, projectId: proj.projectId], "user3", new Date())

        when:
        List<UserAchievement> achievementsBefore = userAchievedLevelRepo.findAllByUserAndProjectIds("user1", [proj.projectId])
        skillsService.updateBadge([projectId: proj.projectId, badgeId: badge.badgeId, enabled: true, name: badge.name], badge.badgeId)
        List<UserAchievement> achievementsAfter = userAchievedLevelRepo.findAllByUserAndProjectIds("user1", [proj.projectId])

        def user1Summary = skillsService.getBadgeSummary("user1", proj.projectId, badge.badgeId)
        def user1SummaryBadge2 = skillsService.getBadgeSummary("user1", proj.projectId, badge2.badgeId)
        def user2Summary = skillsService.getBadgeSummary("user2", proj.projectId, badge.badgeId)
        def user3SummaryBadge1 = skillsService.getBadgeSummary("user3", proj.projectId, badge.badgeId)
        def user3SummaryBadge2 = skillsService.getBadgeSummary("user3", proj.projectId, badge2.badgeId)

        then:
        !achievementsBefore.find( { it.skillId == badge.badgeId})
        achievementsAfter.find( { it.skillId == badge.badgeId})

        user1Summary.badgeAchieved
        !user1SummaryBadge2.badgeAchieved
        !user2Summary.badgeAchieved
        !user3SummaryBadge1.badgeAchieved
        !user3SummaryBadge2.badgeAchieved
    }

    def "gem awarded to users with requirements after enabling"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(20)
        def badge = SkillsFactory.createBadge()

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        Date twoWeeksAgo = new Date() - 14
        Date nextWeek = new Date() + 7

        badge.enabled = false
        badge.startDate = twoWeeksAgo
        badge.endDate = nextWeek

        //add start/end dates
        skillsService.createBadge(badge)
        skillsService.assignSkillToBadge([projectId: proj.projectId, badgeId: badge.badgeId, skillId: skills.get(0).skillId])
        skillsService.assignSkillToBadge([projectId: proj.projectId, badgeId: badge.badgeId, skillId: skills.get(1).skillId])

        skillsService.addSkill([skillId: skills.get(0).skillId, projectId: proj.projectId], "user1", new Date())
        skillsService.addSkill([skillId: skills.get(0).skillId, projectId: proj.projectId], "user2", new Date()-60)
        skillsService.addSkill([skillId: skills.get(1).skillId, projectId: proj.projectId], "user1", new Date()-7)
        skillsService.addSkill([skillId: skills.get(1).skillId, projectId: proj.projectId], "user2", new Date()-35)

        when:
        List<UserAchievement> achievementsBefore = userAchievedLevelRepo.findAllByUserAndProjectIds("user1", [proj.projectId])
        skillsService.updateBadge([projectId: proj.projectId,
                                   badgeId: badge.badgeId,
                                   enabled: true,
                                   name: badge.name,
                                   startDate: twoWeeksAgo,
                                   endDate: nextWeek], badge.badgeId)
        List<UserAchievement> achievementsAfter = userAchievedLevelRepo.findAllByUserAndProjectIds("user1", [proj.projectId])

        def user1Summary = skillsService.getBadgeSummary("user1", proj.projectId, badge.badgeId)
        def user2Summary = skillsService.getBadgeSummary("user2", proj.projectId, badge.badgeId)

        then:
        !achievementsBefore.find( { it.skillId == badge.badgeId})
        achievementsAfter.find( { it.skillId == badge.badgeId})
        user1Summary.badgeAchieved
        !user2Summary.badgeAchieved
    }

    def "changes to skill occurrence causes badge to be awarded"() {
        def proj1 = SkillsFactory.createProject(1)
        skillsService.createProject(proj1)
        def subj = SkillsFactory.createSubject(1)
        skillsService.createSubject(subj)

        def skill1 = SkillsFactory.createSkill(1, 1, 1, 0, 3, 90, 100)
        def skill2 = SkillsFactory.createSkill(1, 1, 2, 0, 1, 0, 100)

        skillsService.createSkills([skill1, skill2])

        def badge = SkillsFactory.createBadge()
        skillsService.createBadge(badge)
        skillsService.assignSkillToBadge([projectId: proj1.projectId, badgeId: badge.badgeId, skillId: skill1.skillId])
        skillsService.assignSkillToBadge([projectId: proj1.projectId, badgeId: badge.badgeId, skillId: skill2.skillId])
        badge.enabled = true
        skillsService.createBadge(badge)


        skillsService.addSkill([projectId: proj1.projectId, skillId: skill1.skillId], "u123", new Date())
        skillsService.addSkill([projectId: proj1.projectId, skillId: skill2.skillId], "u123", new Date())

        skillsService.addSkill([projectId: proj1.projectId, skillId: skill1.skillId], "u124", new Date())

        when:
        //get history for user123 and assert that badge is not awarded
        def u123SummaryBeforeEdit = skillsService.getBadgeSummary("u123", proj1.projectId, badge.badgeId)
        def u124SummaryBeforeEdit = skillsService.getBadgeSummary("u124", proj1.projectId, badge.badgeId)


        skillsService.updateSkill([projectId: proj1.projectId,
                                   subjectId: subj.subjectId,
                                   skillId: skill1.skillId,
                                   numPerformToCompletion: 1,
                                   pointIncrement: skill1.pointIncrement,
                                   pointIncrementInterval: skill1.pointIncrementInterval,
                                   numMaxOccurrencesIncrementInterval: skill1.numMaxOccurrencesIncrementInterval,
                                   version: skill1.version,
                                   name: skill1.name], skill1.skillId)

        def u123SummaryAfterEditOccurrences = skillsService.getBadgeSummary("u123", proj1.projectId, badge.badgeId)
        def u124SummaryAfterEditOccurrences = skillsService.getBadgeSummary("u124", proj1.projectId, badge.badgeId)

        then:
        !u123SummaryBeforeEdit.badgeAchieved
        u123SummaryAfterEditOccurrences.badgeAchieved
        !u124SummaryBeforeEdit.badgeAchieved
        !u124SummaryAfterEditOccurrences.badgeAchieved
    }

    def "deletion of a skill causes badge to be awarded"() {
        def proj1 = SkillsFactory.createProject(1)
        skillsService.createProject(proj1)
        def subj = SkillsFactory.createSubject(1)
        skillsService.createSubject(subj)

        def skill1 = SkillsFactory.createSkill(1, 1, 1, 0, 1, 90, 100)
        def skill2 = SkillsFactory.createSkill(1, 1, 2, 0, 1, 0, 100)

        skillsService.createSkills([skill1, skill2])

        def badge = SkillsFactory.createBadge()
        skillsService.createBadge(badge)
        skillsService.assignSkillToBadge([projectId: proj1.projectId, badgeId: badge.badgeId, skillId: skill1.skillId])
        skillsService.assignSkillToBadge([projectId: proj1.projectId, badgeId: badge.badgeId, skillId: skill2.skillId])
        badge.enabled = true
        skillsService.createBadge(badge)

        skillsService.addSkill([projectId: proj1.projectId, skillId: skill1.skillId], "u123", new Date())
        skillsService.addSkill([projectId: proj1.projectId, skillId: skill2.skillId], "u124", new Date())

        when:
        //get history for user123 and assert that badge is not awarded
        def u123SummaryBeforeEdit = skillsService.getBadgeSummary("u123", proj1.projectId, badge.badgeId)
        def u124SummaryBeforeEdit = skillsService.getBadgeSummary("u124", proj1.projectId, badge.badgeId)

        skillsService.deleteSkill([projectId: proj1.projectId, subjectId: subj.subjectId, skillId: skill2.skillId])

        def u123SummaryAfterSkillDeletion = skillsService.getBadgeSummary("u123", proj1.projectId, badge.badgeId)
        def u124SummaryAfterEdit = skillsService.getBadgeSummary("u124", proj1.projectId, badge.badgeId)

        then:
        !u123SummaryBeforeEdit.badgeAchieved
        u123SummaryAfterSkillDeletion.badgeAchieved
        !u124SummaryBeforeEdit.badgeAchieved
        !u124SummaryAfterEdit.badgeAchieved
    }
}
