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

import groovy.json.JsonOutput
import groovy.time.TimeCategory
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsFactory
import skills.storage.model.UserAchievement
import skills.storage.model.UserPerformedSkill
import skills.storage.repos.UserAchievedLevelRepo
import skills.storage.repos.UserPerformedSkillRepo
import spock.lang.IgnoreRest

import java.text.DateFormat

@Slf4j
class SkillOccurrencesSpecs extends DefaultIntSpec {

    @Autowired
    UserPerformedSkillRepo userPerformedSkillRepo

    @Autowired
    UserAchievedLevelRepo userAchievementRepo

    DateFormat df = new java.text.SimpleDateFormat("yyyy-MM-dd")
    Closure getPoints = { def ptsHistory, int daysAgo ->
        use(TimeCategory) {
            return ptsHistory.pointsHistory.find {
                df.parse(it.dayPerformed) == daysAgo.days.ago
            }.points
        }
    }

    Closure createProject = { int projNum, boolean twoSubjs = false ->
        def proj1 = SkillsFactory.createProject(projNum)
        def proj1_subj1 = SkillsFactory.createSubject(projNum, 1)


        List<Map> proj1_skills = SkillsFactory.createSkills(3, projNum, 1)
        proj1_skills.each {
            it.numPerformToCompletion = 5
            it.pointIncrementInterval = 0 // ability to achieve right away
        }

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj1)
        skillsService.createSkills(proj1_skills)

        def proj1_subj2
        List<Map> proj1_subj2_skills
        if (twoSubjs) {
            proj1_subj2 = SkillsFactory.createSubject(projNum, 2)
            proj1_subj2_skills = SkillsFactory.createSkills(3, projNum, 2)
            proj1_subj2_skills.each {
                it.numPerformToCompletion = 5
                it.pointIncrementInterval = 0 // ability to achieve right away
            }
            skillsService.createSubject(proj1_subj2)
            skillsService.createSkills(proj1_subj2_skills)
        }
        return [proj1, proj1_subj1, proj1_skills, proj1_subj2, proj1_subj2_skills]
    }

    Closure fullyAchieveSkill = { String userIdToAdd, String projectId, String skillId ->
        assert skillsService.addSkill([projectId: projectId, skillId: skillId], userIdToAdd, new Date()).body.skillApplied
        assert skillsService.addSkill([projectId: projectId, skillId: skillId], userIdToAdd, new Date()).body.skillApplied
        assert skillsService.addSkill([projectId: projectId, skillId: skillId], userIdToAdd, new Date()).body.skillApplied
        assert skillsService.addSkill([projectId: projectId, skillId: skillId], userIdToAdd, new Date() - 1).body.skillApplied
        def res = skillsService.addSkill([projectId: projectId, skillId: skillId], userIdToAdd, new Date() - 2).body
        assert res.skillApplied

        return res
    }

    private List<String> getSubjectSkillsPtsSlashTotalPts(String userId, String projId, String subjId) {
        return skillsService.getSkillSummary(userId, projId, subjId).skills.sort { it.skillId }.collect { "${it.points}/${it.totalPoints}" }
    }

    private List<Integer> getPointHistory(String userId, String projectId, String subjId = null) {
        return skillsService.getPointHistory(userId, projectId, subjId).pointsHistory.sort { df.parse(it.dayPerformed) }.collect { it.points }
    }

    def "reduce skill occurrences from 2 to 1"() {
        String userId = "user1"
        def proj1 = SkillsFactory.createProject(5)
        def proj1_subj1 = SkillsFactory.createSubject(5, 1)

        List<Map> proj1_skills = SkillsFactory.createSkills(3, 5, 1)
        proj1_skills.each {
            it.pointIncrement = 100
            it.numPerformToCompletion = 2
            it.pointIncrementInterval = 0 // ability to achieve right away
        }
        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj1)
        skillsService.createSkills(proj1_skills)


        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(0).skillId], userId, new Date()).body
        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(0).skillId], userId, new Date()).body

        def eventsResBefore = skillsService.getPerformedSkills(userId, proj1.projectId)

        when:
        proj1_skills.get(0).numPerformToCompletion = 1
        skillsService.createSkill(proj1_skills.get(0))

        then:
        def eventsResAfter = skillsService.getPerformedSkills(userId, proj1.projectId)
        eventsResAfter.totalCount == eventsResBefore.totalCount -1
    }

    def "reduce skill occurrences after user completed the skill - multiple users, projects and skills"() {
        String userId = "user1"
        String userId2 = "user2"

        def proj1, proj1_subj, proj1_skills
        def proj2, proj2_subj, proj2_skills
        (proj1, proj1_subj, proj1_skills) = createProject.call(1)
        (proj2, proj2_subj, proj2_skills) = createProject.call(2)

        // user 1
        def lastAddProj1Skill1ResUser1 = fullyAchieveSkill.call(userId, proj1.projectId, proj1_skills.get(0).skillId)
        def lastAddProj1Skill3ResUser1 = fullyAchieveSkill.call(userId, proj1.projectId, proj1_skills.get(2).skillId)
        def lastAddProj2Skill1ResUser1 = fullyAchieveSkill.call(userId, proj2.projectId, proj2_skills.get(0).skillId)
        def lastAddProj2Skill3ResUser1 = fullyAchieveSkill.call(userId, proj2.projectId, proj2_skills.get(2).skillId)

        // user 2
        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(0).skillId], userId2, new Date() - 1).body
        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(0).skillId], userId2, new Date()).body

        skillsService.addSkill([projectId: proj2.projectId, skillId: proj2_skills.get(0).skillId], userId2, new Date() - 1).body
        skillsService.addSkill([projectId: proj2.projectId, skillId: proj2_skills.get(0).skillId], userId2, new Date()).body

        def beforeChangeUser1Proj1Summary = skillsService.getSkillSummary(userId, proj1.projectId)
        def beforeChangeUser1Proj1SummarySubj1 = skillsService.getSkillSummary(userId, proj1.projectId, proj1_subj.subjectId)

        def beforeChangeUser2Summary = skillsService.getSkillSummary(userId2, proj1.projectId)
        def beforeChangeUser2SummarySubj1 = skillsService.getSkillSummary(userId2, proj1.projectId, proj1_subj.subjectId)

        Closure<List<UserPerformedSkill>> getEvents = { String projId, String userIdToGet, String skillId ->
            return userPerformedSkillRepo.findAll().findAll({ UserPerformedSkill event -> event.projectId == projId && event.userId == userIdToGet && event.skillId == skillId })
        }
        List<UserPerformedSkill> eventsBeforeUser1Skill1 = getEvents.call(proj1.projectId, userId, proj1_skills.get(0).skillId)
        List<UserPerformedSkill> eventsBeforeUser1Skill3 = getEvents.call(proj1.projectId, userId, proj1_skills.get(2).skillId)
        List<UserPerformedSkill> eventsBeforeUser2Skill1 = getEvents.call(proj1.projectId, userId2, proj1_skills.get(0).skillId)
        List<UserAchievement> beforeAchievements = userAchievementRepo.findAllByUserAndProjectIds(userId, [proj1.projectId])

        def beforePointHistoryUser1Proj1 = skillsService.getPointHistory(userId, proj1.projectId)
        def beforePointHistoryUser1Proj1Subj1 = skillsService.getPointHistory(userId, proj1.projectId, proj1_subj.subjectId)

        when:
        proj1_skills.get(0).numPerformToCompletion = 3
        skillsService.createSkill(proj1_skills.get(0))

        def afterChangeUser1Proj1Summary = skillsService.getSkillSummary(userId, proj1.projectId)
        def afterChangeUser1Proj1SummarySubj1 = skillsService.getSkillSummary(userId, proj1.projectId, proj1_subj.subjectId)
        def afterChangeUser1Proj2Summary = skillsService.getSkillSummary(userId, proj2.projectId)
        def afterChangeUser1Proj2SummarySubj1 = skillsService.getSkillSummary(userId, proj2.projectId, proj2_subj.subjectId)

        def afterChangeUser2Proj1Summary = skillsService.getSkillSummary(userId2, proj1.projectId)
        def afterChangeUser2Proj1SummarySubj1 = skillsService.getSkillSummary(userId2, proj1.projectId, proj1_subj.subjectId)
        def afterChangeUser2Proj2Summary = skillsService.getSkillSummary(userId2, proj2.projectId)
        def afterChangeUser2Proj2SummarySubj1 = skillsService.getSkillSummary(userId2, proj2.projectId, proj2_subj.subjectId)

        List<UserPerformedSkill> eventsAfterUser1Skill1 = getEvents.call(proj1.projectId, userId, proj1_skills.get(0).skillId)
        List<UserPerformedSkill> eventsAfterUser1Skill3 = getEvents.call(proj1.projectId, userId, proj1_skills.get(2).skillId)
        List<UserPerformedSkill> eventsAfterUser2Skill1 = getEvents.call(proj1.projectId, userId2, proj1_skills.get(0).skillId)
        List<UserAchievement> afterAchievements = userAchievementRepo.findAllByUserAndProjectIds(userId, [proj1.projectId])

        def afterPointHistoryUser1Proj1 = skillsService.getPointHistory(userId, proj1.projectId)
        def afterPointHistoryUser1Proj1Subj1 = skillsService.getPointHistory(userId, proj1.projectId, proj1_subj.subjectId)
        def afterPointHistoryUser2Proj1 = skillsService.getPointHistory(userId2, proj1.projectId)
        def afterPointHistoryUser2Proj1Subj1 = skillsService.getPointHistory(userId2, proj1.projectId, proj1_subj.subjectId)

        then:
        lastAddProj1Skill1ResUser1.completed.size() == 1
        lastAddProj1Skill1ResUser1.completed.get(0).id == proj1_skills.get(0).skillId
        lastAddProj1Skill3ResUser1.completed.find { it.type == "Skill" }.id == proj1_skills.get(2).skillId

        lastAddProj2Skill1ResUser1.completed.size() == 1
        lastAddProj2Skill1ResUser1.completed.get(0).id == proj2_skills.get(0).skillId
        lastAddProj2Skill3ResUser1.completed.find { it.type == "Skill" }.id == proj2_skills.get(2).skillId

        // validate that events were properly removed, the latest events must be removed
        eventsBeforeUser1Skill1.size() == 5
        eventsAfterUser1Skill1.size() == 3
        eventsBeforeUser1Skill1.sort({ it.performedOn }).subList(0, 3).collect({ it.id }) == eventsAfterUser1Skill1.sort({ it.performedOn }).collect({ it.id })

        // validate that events were kept for user 1 - skill 3
        eventsBeforeUser1Skill3.size() == 5
        eventsAfterUser1Skill3.size() == 5
        eventsBeforeUser1Skill3.sort({ it.performedOn }).collect({ it.id }) == eventsAfterUser1Skill3.sort({ it.performedOn }).collect({ it.id })

        // validate that events were kept for user2
        eventsBeforeUser2Skill1.size() == 2
        eventsAfterUser2Skill1.size() == 2
        eventsBeforeUser2Skill1.sort({ it.performedOn }).collect({ it.id }) == eventsAfterUser2Skill1.sort({ it.performedOn }).collect({ it.id })

        // validate the achievement is persistent
        beforeAchievements.find { it.skillId == proj1_skills.get(0).skillId }
        afterAchievements.find { it.skillId == proj1_skills.get(0).skillId }

        // user 1
        beforeChangeUser1Proj1Summary.points == 100
        beforeChangeUser1Proj1Summary.todaysPoints == 60
        beforeChangeUser1Proj1Summary.subjects.get(0).points == 100
        beforeChangeUser1Proj1Summary.subjects.get(0).todaysPoints == 60

        beforeChangeUser1Proj1SummarySubj1
        beforeChangeUser1Proj1SummarySubj1.points == 100
        List skillsBeforeChange = beforeChangeUser1Proj1SummarySubj1.skills.sort { it.skillId }
        skillsBeforeChange.get(0).points == 50
        skillsBeforeChange.get(0).todaysPoints == 30
        skillsBeforeChange.get(1).points == 0
        skillsBeforeChange.get(1).todaysPoints == 0
        skillsBeforeChange.get(2).points == 50
        skillsBeforeChange.get(2).todaysPoints == 30

        afterChangeUser1Proj1Summary.points == 80
        afterChangeUser1Proj1Summary.todaysPoints == 40
        afterChangeUser1Proj1Summary.subjects.get(0).points == 80
        afterChangeUser1Proj1Summary.subjects.get(0).todaysPoints == 40

        afterChangeUser1Proj1SummarySubj1.points == 80
        List skillsAfterChange = afterChangeUser1Proj1SummarySubj1.skills.sort { it.skillId }
        skillsAfterChange.get(0).points == 30
        skillsAfterChange.get(0).todaysPoints == 10
        skillsAfterChange.get(1).points == 0
        skillsAfterChange.get(1).todaysPoints == 0
        skillsAfterChange.get(2).points == 50
        skillsAfterChange.get(2).todaysPoints == 30

        // project 2 should not be changed
        afterChangeUser1Proj2Summary.points == 100
        afterChangeUser1Proj2Summary.todaysPoints == 60
        afterChangeUser1Proj2Summary.subjects.get(0).points == 100
        afterChangeUser1Proj2Summary.subjects.get(0).todaysPoints == 60
        afterChangeUser1Proj2SummarySubj1.points == 100
        List skillsAfterChangeProj2 = afterChangeUser1Proj2SummarySubj1.skills.sort { it.skillId }
        skillsAfterChangeProj2.get(0).points == 50
        skillsAfterChangeProj2.get(0).todaysPoints == 30
        skillsAfterChangeProj2.get(1).points == 0
        skillsAfterChangeProj2.get(1).todaysPoints == 0
        skillsAfterChangeProj2.get(2).points == 50
        skillsAfterChangeProj2.get(2).todaysPoints == 30

        // user 2
        beforeChangeUser2Summary.points == 20
        beforeChangeUser2Summary.todaysPoints == 10
        beforeChangeUser2Summary.subjects.get(0).points == 20
        beforeChangeUser2Summary.subjects.get(0).todaysPoints == 10

        beforeChangeUser2SummarySubj1.points == 20
        beforeChangeUser2SummarySubj1.todaysPoints == 10
        List skillsBeforeUser2Change = beforeChangeUser2SummarySubj1.skills.sort { it.skillId }
        skillsBeforeUser2Change.get(0).points == 20
        skillsBeforeUser2Change.get(0).todaysPoints == 10
        skillsBeforeUser2Change.get(1).points == 0
        skillsBeforeUser2Change.get(1).todaysPoints == 0
        skillsBeforeUser2Change.get(2).points == 0
        skillsBeforeUser2Change.get(1).todaysPoints == 0

        beforeChangeUser2Summary.points == 20
        beforeChangeUser2Summary.todaysPoints == 10
        beforeChangeUser2Summary.subjects.get(0).points == 20
        beforeChangeUser2Summary.subjects.get(0).todaysPoints == 10

        afterChangeUser2Proj1Summary.points == 20
        afterChangeUser2Proj1Summary.todaysPoints == 10
        List skillsAfterUser2Change = afterChangeUser2Proj1SummarySubj1.skills.sort { it.skillId }
        skillsAfterUser2Change.get(0).points == 20
        skillsAfterUser2Change.get(0).todaysPoints == 10
        skillsAfterUser2Change.get(1).points == 0
        skillsAfterUser2Change.get(1).todaysPoints == 0
        skillsAfterUser2Change.get(2).points == 0
        skillsAfterUser2Change.get(1).todaysPoints == 0

        afterChangeUser2Proj2Summary.points == 20
        afterChangeUser2Proj2Summary.todaysPoints == 10
        List skillsAfterUser2ChangeProj2 = afterChangeUser2Proj2SummarySubj1.skills.sort { it.skillId }
        skillsAfterUser2ChangeProj2.get(0).points == 20
        skillsAfterUser2ChangeProj2.get(0).todaysPoints == 10
        skillsAfterUser2ChangeProj2.get(1).points == 0
        skillsAfterUser2ChangeProj2.get(1).todaysPoints == 0
        skillsAfterUser2ChangeProj2.get(2).points == 0
        skillsAfterUser2ChangeProj2.get(1).todaysPoints == 0

        /////////////////////////
        // Point History Validation
        beforePointHistoryUser1Proj1.pointsHistory.size() == 3
        getPoints(beforePointHistoryUser1Proj1, 2) == 20
        getPoints(beforePointHistoryUser1Proj1, 1) == 40
        getPoints(beforePointHistoryUser1Proj1, 0) == 100

        afterPointHistoryUser1Proj1.pointsHistory.size() == 3
        getPoints(afterPointHistoryUser1Proj1, 2) == 20
        getPoints(afterPointHistoryUser1Proj1, 1) == 40
        getPoints(afterPointHistoryUser1Proj1, 0) == 80

        beforePointHistoryUser1Proj1Subj1
        getPoints(beforePointHistoryUser1Proj1Subj1, 2) == 20
        getPoints(beforePointHistoryUser1Proj1Subj1, 1) == 40
        getPoints(beforePointHistoryUser1Proj1Subj1, 0) == 100

        afterPointHistoryUser1Proj1Subj1
        getPoints(afterPointHistoryUser1Proj1Subj1, 2) == 20
        getPoints(afterPointHistoryUser1Proj1Subj1, 1) == 40
        getPoints(afterPointHistoryUser1Proj1Subj1, 0) == 80

        afterPointHistoryUser2Proj1
        getPoints(afterPointHistoryUser2Proj1, 1) == 10
        getPoints(afterPointHistoryUser2Proj1, 0) == 20

        afterPointHistoryUser2Proj1Subj1
        getPoints(afterPointHistoryUser2Proj1Subj1, 1) == 10
        getPoints(afterPointHistoryUser2Proj1Subj1, 0) == 20
    }


    def "reduce skill occurrences - multiple users and multiple subjects - check pt history"() {

        String userId = "user1"
        String userId2 = "user2"

        def proj1, proj1_subj, proj1_skills, proj1_subj2, proj1_skills_subj2
        def proj2, proj2_subj, proj2_skills, proj2_subj2, proj2_skills_subj2
        (proj1, proj1_subj, proj1_skills, proj1_subj2, proj1_skills_subj2) = createProject.call(1, true)
        (proj2, proj2_subj, proj2_skills, proj2_subj2, proj2_skills_subj2) = createProject.call(2, true)

        // proj 1 - user 1
        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(0).skillId], userId, new Date() - 2).body
        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(0).skillId], userId, new Date() - 1).body
        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(1).skillId], userId, new Date() - 1).body
        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(1).skillId], userId, new Date() - 0).body
        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(2).skillId], userId, new Date() - 0).body

        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills_subj2.get(0).skillId], userId, new Date() - 0).body
        fullyAchieveSkill.call(userId, proj1.projectId, proj1_skills_subj2.get(1).skillId)
        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills_subj2.get(2).skillId], userId, new Date() - 2).body

        // proj 1 - user 2
        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(0).skillId], userId2, new Date() - 0).body
        fullyAchieveSkill.call(userId2, proj1.projectId, proj1_skills.get(1).skillId)
        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(2).skillId], userId2, new Date() - 2).body

        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills_subj2.get(0).skillId], userId2, new Date() - 2).body
        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills_subj2.get(0).skillId], userId2, new Date() - 1).body
        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills_subj2.get(1).skillId], userId2, new Date() - 1).body
        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills_subj2.get(1).skillId], userId2, new Date() - 0).body
        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills_subj2.get(2).skillId], userId2, new Date() - 0).body

        // proj2 - user 1 - fully achieve it all
        fullyAchieveSkill.call(userId, proj2.projectId, proj2_skills.get(0).skillId)
        fullyAchieveSkill.call(userId, proj2.projectId, proj2_skills.get(1).skillId)
        fullyAchieveSkill.call(userId, proj2.projectId, proj2_skills.get(2).skillId)
        fullyAchieveSkill.call(userId, proj2.projectId, proj2_skills_subj2.get(0).skillId)
        fullyAchieveSkill.call(userId, proj2.projectId, proj2_skills_subj2.get(1).skillId)
        fullyAchieveSkill.call(userId, proj2.projectId, proj2_skills_subj2.get(2).skillId)

        def beforePointHistoryUser1Proj1 = skillsService.getPointHistory(userId, proj1.projectId)
        def beforePointHistoryUser1Proj1Subj1 = skillsService.getPointHistory(userId, proj1.projectId, proj1_subj.subjectId)
        def beforePointHistoryUser1Proj1Subj2 = skillsService.getPointHistory(userId, proj1.projectId, proj1_subj2.subjectId)

        def beforePointHistoryUser2Proj1 = skillsService.getPointHistory(userId2, proj1.projectId)
        def beforePointHistoryUser2Proj1Subj1 = skillsService.getPointHistory(userId2, proj1.projectId, proj1_subj.subjectId)
        def beforePointHistoryUser2Proj1Subj2 = skillsService.getPointHistory(userId2, proj1.projectId, proj1_subj2.subjectId)

        def beforePointHistoryUser1Proj2 = skillsService.getPointHistory(userId, proj2.projectId)
        def beforePointHistoryUser1Proj2Subj1 = skillsService.getPointHistory(userId, proj2.projectId, proj2_subj.subjectId)
        def beforePointHistoryUser1Proj2Subj2 = skillsService.getPointHistory(userId, proj2.projectId, proj2_subj2.subjectId)

        when:
        proj1_skills.get(0).numPerformToCompletion = 1
        skillsService.createSkill(proj1_skills.get(0))

        def afterPointHistoryUser1Proj1 = skillsService.getPointHistory(userId, proj1.projectId)
        def afterPointHistoryUser1Proj1Subj1 = skillsService.getPointHistory(userId, proj1.projectId, proj1_subj.subjectId)
        def afterPointHistoryUser1Proj1Subj2 = skillsService.getPointHistory(userId, proj1.projectId, proj1_subj2.subjectId)

        def afterPointHistoryUser2Proj1 = skillsService.getPointHistory(userId2, proj1.projectId)
        def afterPointHistoryUser2Proj1Subj1 = skillsService.getPointHistory(userId2, proj1.projectId, proj1_subj.subjectId)
        def afterPointHistoryUser2Proj1Subj2 = skillsService.getPointHistory(userId2, proj1.projectId, proj1_subj2.subjectId)

        def afterPointHistoryUser1Proj2 = skillsService.getPointHistory(userId, proj2.projectId)
        def afterPointHistoryUser1Proj2Subj1 = skillsService.getPointHistory(userId, proj2.projectId, proj2_subj.subjectId)
        def afterPointHistoryUser1Proj2Subj2 = skillsService.getPointHistory(userId, proj2.projectId, proj2_subj2.subjectId)

        then:
        /// BEFORE
        // user 1 - proj1
        beforePointHistoryUser1Proj1.pointsHistory.size() == 3
        getPoints(beforePointHistoryUser1Proj1, 2) == 30
        getPoints(beforePointHistoryUser1Proj1, 1) == 60
        getPoints(beforePointHistoryUser1Proj1, 0) == 120

        beforePointHistoryUser1Proj1Subj1.pointsHistory.size() == 3
        getPoints(beforePointHistoryUser1Proj1Subj1, 2) == 10
        getPoints(beforePointHistoryUser1Proj1Subj1, 1) == 30
        getPoints(beforePointHistoryUser1Proj1Subj1, 0) == 50

        beforePointHistoryUser1Proj1Subj2.pointsHistory.size() == 3
        getPoints(beforePointHistoryUser1Proj1Subj2, 2) == 20
        getPoints(beforePointHistoryUser1Proj1Subj2, 1) == 30
        getPoints(beforePointHistoryUser1Proj1Subj2, 0) == 70

        // user 2 - proj1
        beforePointHistoryUser2Proj1.pointsHistory.size() == 3
        getPoints(beforePointHistoryUser2Proj1, 2) == 30
        getPoints(beforePointHistoryUser2Proj1, 1) == 60
        getPoints(beforePointHistoryUser2Proj1, 0) == 120

        beforePointHistoryUser2Proj1Subj2.pointsHistory.size() == 3
        getPoints(beforePointHistoryUser2Proj1Subj1, 2) == 20
        getPoints(beforePointHistoryUser2Proj1Subj1, 1) == 30
        getPoints(beforePointHistoryUser2Proj1Subj1, 0) == 70

        beforePointHistoryUser2Proj1Subj1.pointsHistory.size() == 3
        getPoints(beforePointHistoryUser2Proj1Subj2, 2) == 10
        getPoints(beforePointHistoryUser2Proj1Subj2, 1) == 30
        getPoints(beforePointHistoryUser2Proj1Subj2, 0) == 50

        // user 1 - proj 2
        beforePointHistoryUser1Proj2.pointsHistory.size() == 3
        getPoints(beforePointHistoryUser1Proj2, 2) == 60
        getPoints(beforePointHistoryUser1Proj2, 1) == 120
        getPoints(beforePointHistoryUser1Proj2, 0) == 120 + 180

        beforePointHistoryUser1Proj2Subj1.pointsHistory.size() == 3
        getPoints(beforePointHistoryUser1Proj2Subj1, 2) == 30
        getPoints(beforePointHistoryUser1Proj2Subj1, 1) == 60
        getPoints(beforePointHistoryUser1Proj2Subj1, 0) == 60 + 90

        beforePointHistoryUser1Proj2Subj2.pointsHistory.size() == 3
        getPoints(beforePointHistoryUser1Proj2Subj2, 2) == 30
        getPoints(beforePointHistoryUser1Proj2Subj2, 1) == 60
        getPoints(beforePointHistoryUser1Proj2Subj2, 0) == 60 + 90

        /// AFTER
        // user 1 - proj1
        afterPointHistoryUser1Proj1.pointsHistory.size() == 3
        getPoints(afterPointHistoryUser1Proj1, 2) == 30
        getPoints(afterPointHistoryUser1Proj1, 1) == 50 // LOST 10 POINTS
        getPoints(afterPointHistoryUser1Proj1, 0) == 110 // LOST 10 POINTS

        afterPointHistoryUser1Proj1Subj1.pointsHistory.size() == 3
        getPoints(afterPointHistoryUser1Proj1Subj1, 2) == 10
        getPoints(afterPointHistoryUser1Proj1Subj1, 1) == 20 // LOST 10 POINTS
        getPoints(afterPointHistoryUser1Proj1Subj1, 0) == 40 // LOST 10 POINTS

        afterPointHistoryUser1Proj1Subj2.pointsHistory.size() == 3
        getPoints(afterPointHistoryUser1Proj1Subj2, 2) == 20
        getPoints(afterPointHistoryUser1Proj1Subj2, 1) == 30
        getPoints(afterPointHistoryUser1Proj1Subj2, 0) == 70

        // user 2 - proj1
        afterPointHistoryUser2Proj1.pointsHistory.size() == 3
        getPoints(afterPointHistoryUser2Proj1, 2) == 30
        getPoints(afterPointHistoryUser2Proj1, 1) == 60
        getPoints(afterPointHistoryUser2Proj1, 0) == 120

        afterPointHistoryUser2Proj1Subj2.pointsHistory.size() == 3
        getPoints(afterPointHistoryUser2Proj1Subj1, 2) == 20
        getPoints(afterPointHistoryUser2Proj1Subj1, 1) == 30
        getPoints(afterPointHistoryUser2Proj1Subj1, 0) == 70

        afterPointHistoryUser2Proj1Subj1.pointsHistory.size() == 3
        getPoints(afterPointHistoryUser2Proj1Subj2, 2) == 10
        getPoints(afterPointHistoryUser2Proj1Subj2, 1) == 30
        getPoints(afterPointHistoryUser2Proj1Subj2, 0) == 50

        // user 1 - proj 2
        afterPointHistoryUser1Proj2.pointsHistory.size() == 3
        getPoints(afterPointHistoryUser1Proj2, 2) == 60
        getPoints(afterPointHistoryUser1Proj2, 1) == 120
        getPoints(afterPointHistoryUser1Proj2, 0) == 120 + 180

        afterPointHistoryUser1Proj2Subj1.pointsHistory.size() == 3
        getPoints(afterPointHistoryUser1Proj2Subj1, 2) == 30
        getPoints(afterPointHistoryUser1Proj2Subj1, 1) == 60
        getPoints(afterPointHistoryUser1Proj2Subj1, 0) == 60 + 90

        afterPointHistoryUser1Proj2Subj2.pointsHistory.size() == 3
        getPoints(afterPointHistoryUser1Proj2Subj2, 2) == 30
        getPoints(afterPointHistoryUser1Proj2Subj2, 1) == 60
        getPoints(afterPointHistoryUser1Proj2Subj2, 0) == 60 + 90
    }

    def "do not change points if user have not fu-filled removed occurrences"() {
        String userId = "user1"

        def proj1, proj1_subj, proj1_skills
        (proj1, proj1_subj, proj1_skills) = createProject.call(1)

        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(0).skillId], userId, new Date() - 1).body
        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(0).skillId], userId, new Date() - 0).body
        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(0).skillId], userId, new Date() - 0).body

        when:
        proj1_skills.get(0).numPerformToCompletion = 3
        skillsService.createSkill(proj1_skills.get(0))

        def afterChangeUser1Proj1Summary = skillsService.getSkillSummary(userId, proj1.projectId)
        def afterChangeUser1Proj1SummarySubj1 = skillsService.getSkillSummary(userId, proj1.projectId, proj1_subj.subjectId)

        def afterPointHistoryUser1Proj1 = skillsService.getPointHistory(userId, proj1.projectId)
        def afterPointHistoryUser1Proj1Subj1 = skillsService.getPointHistory(userId, proj1.projectId, proj1_subj.subjectId)

        then:
        afterChangeUser1Proj1Summary.points == 30
        afterChangeUser1Proj1Summary.todaysPoints == 20
        afterChangeUser1Proj1Summary.subjects.get(0).points == 30
        afterChangeUser1Proj1Summary.subjects.get(0).todaysPoints == 20

        afterChangeUser1Proj1SummarySubj1.points == 30
        List skillsAfterChange = afterChangeUser1Proj1SummarySubj1.skills.sort { it.skillId }
        skillsAfterChange.get(0).points == 30
        skillsAfterChange.get(0).todaysPoints == 20
        skillsAfterChange.get(1).points == 0
        skillsAfterChange.get(1).todaysPoints == 0
        skillsAfterChange.get(2).points == 0
        skillsAfterChange.get(2).todaysPoints == 0

        afterPointHistoryUser1Proj1.pointsHistory.size() == 2
        getPoints(afterPointHistoryUser1Proj1, 1) == 10
        getPoints(afterPointHistoryUser1Proj1, 0) == 30

        afterPointHistoryUser1Proj1Subj1.pointsHistory.size() == 2
        getPoints(afterPointHistoryUser1Proj1Subj1, 1) == 10
        getPoints(afterPointHistoryUser1Proj1Subj1, 0) == 30
    }

    def "decreasing occurrences puts user(s) into completion of the skill - multiple users"() {
        String userId1 = "user1" // will get an achievement
        String userId2 = "user2" // will be 1 event short
        String userId3 = "user3" // already achieved skill 1 so will keep its achievement
        String userId4 = "user4" // achieved another skill, should not be changed

        def proj1, proj1_subj, proj1_skills, proj2, proj2_subj, proj2_skills
        (proj1, proj1_subj, proj1_skills) = createProject.call(1)
        (proj2, proj2_subj, proj2_skills) = createProject.call(2)

        Closure initSkillsForProject = { String projId, def projSkills ->
            skillsService.addSkill([projectId: projId, skillId: projSkills.get(0).skillId], userId1, new Date() - 1).body
            skillsService.addSkill([projectId: projId, skillId: projSkills.get(0).skillId], userId1, new Date() - 0).body
            skillsService.addSkill([projectId: projId, skillId: projSkills.get(0).skillId], userId1, new Date() - 0).body

            skillsService.addSkill([projectId: projId, skillId: projSkills.get(1).skillId], userId1, new Date() - 3).body
            skillsService.addSkill([projectId: projId, skillId: projSkills.get(1).skillId], userId1, new Date() - 2).body
            skillsService.addSkill([projectId: projId, skillId: projSkills.get(1).skillId], userId1, new Date() - 1).body
            skillsService.addSkill([projectId: projId, skillId: projSkills.get(1).skillId], userId1, new Date() - 0).body

            skillsService.addSkill([projectId: projId, skillId: projSkills.get(0).skillId], userId2, new Date() - 1).body

            fullyAchieveSkill.call(userId3, projId, projSkills.get(0).skillId)
            fullyAchieveSkill.call(userId4, projId, projSkills.get(1).skillId)
        }

        initSkillsForProject.call(proj1.projectId, proj1_skills)
        initSkillsForProject.call(proj2.projectId, proj2_skills)

        List<UserAchievement> beforeAchievements = userAchievementRepo.findAll()
        when:
        proj1_skills.get(0).numPerformToCompletion = 2
        skillsService.createSkill(proj1_skills.get(0))

        List<UserAchievement> afterAchievements = userAchievementRepo.findAll()

        then:
        // validate that 1 achievement was added for user 1 for skill 1
        beforeAchievements.size() == afterAchievements.size() - 1
        !beforeAchievements.find { it.userId == userId1 && it.projectId == proj1.projectId && it.skillId == proj1_skills.get(0).skillId }
        afterAchievements.find { it.userId == userId1 && it.projectId == proj1.projectId && it.skillId == proj1_skills.get(0).skillId }

        getSubjectSkillsPtsSlashTotalPts(userId1, proj1.projectId, proj1_subj.subjectId) == ["20/20", "40/50", "0/50"]
        getSubjectSkillsPtsSlashTotalPts(userId2, proj1.projectId, proj1_subj.subjectId) == ["10/20", "0/50", "0/50"]
        getSubjectSkillsPtsSlashTotalPts(userId3, proj1.projectId, proj1_subj.subjectId) == ["20/20", "0/50", "0/50"]
        getSubjectSkillsPtsSlashTotalPts(userId4, proj1.projectId, proj1_subj.subjectId) == ["0/20", "50/50", "0/50"]

        getSubjectSkillsPtsSlashTotalPts(userId1, proj2.projectId, proj2_subj.subjectId) == ["30/50", "40/50", "0/50"]
        getSubjectSkillsPtsSlashTotalPts(userId2, proj2.projectId, proj2_subj.subjectId) == ["10/50", "0/50", "0/50"]
        getSubjectSkillsPtsSlashTotalPts(userId3, proj2.projectId, proj2_subj.subjectId) == ["50/50", "0/50", "0/50"]
        getSubjectSkillsPtsSlashTotalPts(userId4, proj2.projectId, proj2_subj.subjectId) == ["0/50", "50/50", "0/50"]
    }

    def "if occurrences are added after skill is completed then skill achievement will be removed"() {
        String userId1 = "user1" // will have an achievement needs to be removed
        String userId2 = "user2" //
        String userId3 = "user3" // will have an achievement needs to be removed
        String userId4 = "user4" //

        def proj1, proj1_subj, proj1_skills, proj2, proj2_subj, proj2_skills
        (proj1, proj1_subj, proj1_skills) = createProject.call(1)
        (proj2, proj2_subj, proj2_skills) = createProject.call(2)

        Closure initSkillsForProject = { String projId, def projSkills ->
            fullyAchieveSkill.call(userId1, projId, projSkills.get(0).skillId)

            skillsService.addSkill([projectId: projId, skillId: projSkills.get(1).skillId], userId1, new Date() - 1).body
            skillsService.addSkill([projectId: projId, skillId: projSkills.get(1).skillId], userId1, new Date() - 0).body
            skillsService.addSkill([projectId: projId, skillId: projSkills.get(1).skillId], userId1, new Date() - 0).body

            fullyAchieveSkill.call(userId1, projId, projSkills.get(2).skillId)

            skillsService.addSkill([projectId: projId, skillId: projSkills.get(0).skillId], userId2, new Date() - 1).body

            fullyAchieveSkill.call(userId3, projId, projSkills.get(0).skillId)
            fullyAchieveSkill.call(userId4, projId, projSkills.get(1).skillId)
        }

        initSkillsForProject.call(proj1.projectId, proj1_skills)
        initSkillsForProject.call(proj2.projectId, proj2_skills)

        List<UserAchievement> beforeAchievements = userAchievementRepo.findAll()
        when:
        proj1_skills.get(0).numPerformToCompletion = 10
        skillsService.createSkill(proj1_skills.get(0))

        List<UserAchievement> afterAchievements = userAchievementRepo.findAll()

        then:
        beforeAchievements.size() == afterAchievements.size() + 2 // 2 achievement should be removed
        beforeAchievements.findAll { it.projectId == proj1.projectId && it.skillId == proj1_skills.get(0).skillId }.collect { it.userId }.sort() == [userId1, userId3]
        !afterAchievements.find { it.projectId == proj1.projectId && it.skillId == proj1_skills.get(0).skillId }

        getSubjectSkillsPtsSlashTotalPts(userId1, proj1.projectId, proj1_subj.subjectId) == ["50/100", "30/50", "50/50"]
        getSubjectSkillsPtsSlashTotalPts(userId2, proj1.projectId, proj1_subj.subjectId) == ["10/100", "0/50", "0/50"]
        getSubjectSkillsPtsSlashTotalPts(userId3, proj1.projectId, proj1_subj.subjectId) == ["50/100", "0/50", "0/50"]
        getSubjectSkillsPtsSlashTotalPts(userId4, proj1.projectId, proj1_subj.subjectId) == ["0/100", "50/50", "0/50"]

        getSubjectSkillsPtsSlashTotalPts(userId1, proj2.projectId, proj2_subj.subjectId) == ["50/50", "30/50", "50/50"]
        getSubjectSkillsPtsSlashTotalPts(userId2, proj2.projectId, proj2_subj.subjectId) == ["10/50", "0/50", "0/50"]
        getSubjectSkillsPtsSlashTotalPts(userId3, proj2.projectId, proj2_subj.subjectId) == ["50/50", "0/50", "0/50"]
        getSubjectSkillsPtsSlashTotalPts(userId4, proj2.projectId, proj2_subj.subjectId) == ["0/50", "50/50", "0/50"]
    }

    def "point increment increased and occurrences increased"() {
        String userId1 = "user1" // will have an achievement needs to be removed
        String userId2 = "user2" //
        String userId3 = "user3" // will have an achievement needs to be removed
        String userId4 = "user4" //

        def proj1, proj1_subj, proj1_skills, proj2, proj2_subj, proj2_skills
        (proj1, proj1_subj, proj1_skills) = createProject.call(1)
        (proj2, proj2_subj, proj2_skills) = createProject.call(2)

        Closure initSkillsForProject = { String projId, def projSkills ->
            fullyAchieveSkill.call(userId1, projId, projSkills.get(0).skillId)

            skillsService.addSkill([projectId: projId, skillId: projSkills.get(1).skillId], userId1, new Date() - 2).body
            skillsService.addSkill([projectId: projId, skillId: projSkills.get(1).skillId], userId1, new Date() - 1).body
            skillsService.addSkill([projectId: projId, skillId: projSkills.get(1).skillId], userId1, new Date() - 0).body

            fullyAchieveSkill.call(userId1, projId, projSkills.get(2).skillId)

            skillsService.addSkill([projectId: projId, skillId: projSkills.get(0).skillId], userId2, new Date() - 1).body

            fullyAchieveSkill.call(userId3, projId, projSkills.get(0).skillId)
            fullyAchieveSkill.call(userId4, projId, projSkills.get(1).skillId)
        }

        initSkillsForProject.call(proj1.projectId, proj1_skills)
        initSkillsForProject.call(proj2.projectId, proj2_skills)

        List<UserAchievement> beforeAchievements = userAchievementRepo.findAll()
        when:
        assert getPointHistory(userId1, proj1.projectId) == [30, 60, 60 + 70]
        assert getPointHistory(userId1, proj1.projectId, proj1_subj.subjectId) == [30, 60, 60 + 70]
        assert getPointHistory(userId1, proj2.projectId) == [30, 60, 60 + 70]
        assert getPointHistory(userId1, proj2.projectId, proj2_subj.subjectId) == [30, 60, 60 + 70]

        // user only has 1 day so history is not returned
        assert getPointHistory(userId2, proj1.projectId) == []
        assert getPointHistory(userId2, proj1.projectId, proj1_subj.subjectId) == []
        assert getPointHistory(userId2, proj2.projectId) == []
        assert getPointHistory(userId2, proj2.projectId, proj2_subj.subjectId) == []

        assert getPointHistory(userId3, proj1.projectId) == [10, 20, 50]
        assert getPointHistory(userId3, proj1.projectId, proj1_subj.subjectId) == [10, 20, 50]
        assert getPointHistory(userId3, proj2.projectId) == [10, 20, 50]
        assert getPointHistory(userId3, proj2.projectId, proj2_subj.subjectId) == [10, 20, 50]

        assert getPointHistory(userId4, proj1.projectId) == [10, 20, 50]
        assert getPointHistory(userId4, proj1.projectId, proj1_subj.subjectId) == [10, 20, 50]
        assert getPointHistory(userId4, proj2.projectId) == [10, 20, 50]
        assert getPointHistory(userId4, proj2.projectId, proj2_subj.subjectId) == [10, 20, 50]

        proj1_skills.get(0).numPerformToCompletion = 10
        proj1_skills.get(0).pointIncrement = 15
        skillsService.createSkill(proj1_skills.get(0))

        List<UserAchievement> afterAchievements = userAchievementRepo.findAll()

        then:
        beforeAchievements.size() == afterAchievements.size() + 2 // 2 achievement should be removed
        beforeAchievements.findAll { it.projectId == proj1.projectId && it.skillId == proj1_skills.get(0).skillId }.collect { it.userId }.sort() == [userId1, userId3]
        !afterAchievements.find { it.projectId == proj1.projectId && it.skillId == proj1_skills.get(0).skillId }

        getSubjectSkillsPtsSlashTotalPts(userId1, proj1.projectId, proj1_subj.subjectId) == ["75/150", "30/50", "50/50"]
        getSubjectSkillsPtsSlashTotalPts(userId2, proj1.projectId, proj1_subj.subjectId) == ["15/150", "0/50", "0/50"]
        getSubjectSkillsPtsSlashTotalPts(userId3, proj1.projectId, proj1_subj.subjectId) == ["75/150", "0/50", "0/50"]
        getSubjectSkillsPtsSlashTotalPts(userId4, proj1.projectId, proj1_subj.subjectId) == ["0/150", "50/50", "0/50"]

        getSubjectSkillsPtsSlashTotalPts(userId1, proj2.projectId, proj2_subj.subjectId) == ["50/50", "30/50", "50/50"]
        getSubjectSkillsPtsSlashTotalPts(userId2, proj2.projectId, proj2_subj.subjectId) == ["10/50", "0/50", "0/50"]
        getSubjectSkillsPtsSlashTotalPts(userId3, proj2.projectId, proj2_subj.subjectId) == ["50/50", "0/50", "0/50"]
        getSubjectSkillsPtsSlashTotalPts(userId4, proj2.projectId, proj2_subj.subjectId) == ["0/50", "50/50", "0/50"]

        getPointHistory(userId1, proj1.projectId) == [35, 70, 70 + 85]
        getPointHistory(userId1, proj1.projectId, proj1_subj.subjectId) == [35, 70, 70 + 85]
        getPointHistory(userId1, proj2.projectId) == [30, 60, 60 + 70]
        getPointHistory(userId1, proj2.projectId, proj2_subj.subjectId) == [30, 60, 60 + 70]

        // user only has 1 day so history is not returned
        getPointHistory(userId2, proj1.projectId) == []
        getPointHistory(userId2, proj1.projectId, proj1_subj.subjectId) == []
        getPointHistory(userId2, proj2.projectId) == []
        getPointHistory(userId2, proj2.projectId, proj2_subj.subjectId) == []

        getPointHistory(userId3, proj1.projectId) == [15, 30, 75]
        getPointHistory(userId3, proj1.projectId, proj1_subj.subjectId) == [15, 30, 75]
        getPointHistory(userId3, proj2.projectId) == [10, 20, 50]
        getPointHistory(userId3, proj2.projectId, proj2_subj.subjectId) == [10, 20, 50]

        getPointHistory(userId4, proj1.projectId) == [10, 20, 50]
        getPointHistory(userId4, proj1.projectId, proj1_subj.subjectId) == [10, 20, 50]
        getPointHistory(userId4, proj2.projectId) == [10, 20, 50]
        getPointHistory(userId4, proj2.projectId, proj2_subj.subjectId) == [10, 20, 50]
    }


    def "point increment decreased and occurrences increased"() {
        String userId1 = "user1" // will have an achievement needs to be removed
        String userId2 = "user2" //
        String userId3 = "user3" // will have an achievement needs to be removed
        String userId4 = "user4" //

        def proj1, proj1_subj, proj1_skills, proj2, proj2_subj, proj2_skills
        (proj1, proj1_subj, proj1_skills) = createProject.call(1)
        (proj2, proj2_subj, proj2_skills) = createProject.call(2)

        Closure initSkillsForProject = { String projId, def projSkills ->
            fullyAchieveSkill.call(userId1, projId, projSkills.get(0).skillId)

            skillsService.addSkill([projectId: projId, skillId: projSkills.get(1).skillId], userId1, new Date() - 2).body
            skillsService.addSkill([projectId: projId, skillId: projSkills.get(1).skillId], userId1, new Date() - 1).body
            skillsService.addSkill([projectId: projId, skillId: projSkills.get(1).skillId], userId1, new Date() - 0).body

            fullyAchieveSkill.call(userId1, projId, projSkills.get(2).skillId)

            skillsService.addSkill([projectId: projId, skillId: projSkills.get(0).skillId], userId2, new Date() - 1).body

            fullyAchieveSkill.call(userId3, projId, projSkills.get(0).skillId)
            fullyAchieveSkill.call(userId4, projId, projSkills.get(1).skillId)
        }

        initSkillsForProject.call(proj1.projectId, proj1_skills)
        initSkillsForProject.call(proj2.projectId, proj2_skills)

        List<UserAchievement> beforeAchievements = userAchievementRepo.findAll()
        when:
        assert getPointHistory(userId1, proj1.projectId) == [30, 60, 60 + 70]
        assert getPointHistory(userId1, proj1.projectId, proj1_subj.subjectId) == [30, 60, 60 + 70]
        assert getPointHistory(userId1, proj2.projectId) == [30, 60, 60 + 70]
        assert getPointHistory(userId1, proj2.projectId, proj2_subj.subjectId) == [30, 60, 60 + 70]

        // user only has 1 day so history is not returned
        assert getPointHistory(userId2, proj1.projectId) == []
        assert getPointHistory(userId2, proj1.projectId, proj1_subj.subjectId) == []
        assert getPointHistory(userId2, proj2.projectId) == []
        assert getPointHistory(userId2, proj2.projectId, proj2_subj.subjectId) == []

        assert getPointHistory(userId3, proj1.projectId) == [10, 20, 50]
        assert getPointHistory(userId3, proj1.projectId, proj1_subj.subjectId) == [10, 20, 50]
        assert getPointHistory(userId3, proj2.projectId) == [10, 20, 50]
        assert getPointHistory(userId3, proj2.projectId, proj2_subj.subjectId) == [10, 20, 50]

        assert getPointHistory(userId4, proj1.projectId) == [10, 20, 50]
        assert getPointHistory(userId4, proj1.projectId, proj1_subj.subjectId) == [10, 20, 50]
        assert getPointHistory(userId4, proj2.projectId) == [10, 20, 50]
        assert getPointHistory(userId4, proj2.projectId, proj2_subj.subjectId) == [10, 20, 50]

        proj1_skills.get(0).numPerformToCompletion = 10
        proj1_skills.get(0).pointIncrement = 3
        skillsService.createSkill(proj1_skills.get(0))

        List<UserAchievement> afterAchievements = userAchievementRepo.findAll()

        then:
        beforeAchievements.size() == afterAchievements.size() + 2 // 2 achievement should be removed
        beforeAchievements.findAll { it.projectId == proj1.projectId && it.skillId == proj1_skills.get(0).skillId }.collect { it.userId }.sort() == [userId1, userId3]
        !afterAchievements.find { it.projectId == proj1.projectId && it.skillId == proj1_skills.get(0).skillId }

        getSubjectSkillsPtsSlashTotalPts(userId1, proj1.projectId, proj1_subj.subjectId) == ["15/30", "30/50", "50/50"]
        getSubjectSkillsPtsSlashTotalPts(userId2, proj1.projectId, proj1_subj.subjectId) == ["3/30", "0/50", "0/50"]
        getSubjectSkillsPtsSlashTotalPts(userId3, proj1.projectId, proj1_subj.subjectId) == ["15/30", "0/50", "0/50"]
        getSubjectSkillsPtsSlashTotalPts(userId4, proj1.projectId, proj1_subj.subjectId) == ["0/30", "50/50", "0/50"]

        getSubjectSkillsPtsSlashTotalPts(userId1, proj2.projectId, proj2_subj.subjectId) == ["50/50", "30/50", "50/50"]
        getSubjectSkillsPtsSlashTotalPts(userId2, proj2.projectId, proj2_subj.subjectId) == ["10/50", "0/50", "0/50"]
        getSubjectSkillsPtsSlashTotalPts(userId3, proj2.projectId, proj2_subj.subjectId) == ["50/50", "0/50", "0/50"]
        getSubjectSkillsPtsSlashTotalPts(userId4, proj2.projectId, proj2_subj.subjectId) == ["0/50", "50/50", "0/50"]

        getPointHistory(userId1, proj1.projectId) == [23, 46, 46 + 3 * 3 + 10 * 3 + 10]
        getPointHistory(userId1, proj1.projectId, proj1_subj.subjectId) == [23, 46, 46 + 3 * 3 + 10 * 3 + 10]
        getPointHistory(userId1, proj2.projectId) == [30, 60, 60 + 70]
        getPointHistory(userId1, proj2.projectId, proj2_subj.subjectId) == [30, 60, 60 + 70]

        // user only has 1 day so history is not returned
        getPointHistory(userId2, proj1.projectId) == []
        getPointHistory(userId2, proj1.projectId, proj1_subj.subjectId) == []
        getPointHistory(userId2, proj2.projectId) == []
        getPointHistory(userId2, proj2.projectId, proj2_subj.subjectId) == []

        getPointHistory(userId3, proj1.projectId) == [3, 6, 6 + 3 * 3]
        getPointHistory(userId3, proj1.projectId, proj1_subj.subjectId) == [3, 6, 6 + 3 * 3]
        getPointHistory(userId3, proj2.projectId) == [10, 20, 50]
        getPointHistory(userId3, proj2.projectId, proj2_subj.subjectId) == [10, 20, 50]

        getPointHistory(userId4, proj1.projectId) == [10, 20, 50]
        getPointHistory(userId4, proj1.projectId, proj1_subj.subjectId) == [10, 20, 50]
        getPointHistory(userId4, proj2.projectId) == [10, 20, 50]
        getPointHistory(userId4, proj2.projectId, proj2_subj.subjectId) == [10, 20, 50]
    }

    def "point increment increased and occurrences decreased"() {
        String userId1 = "user1"
        String userId2 = "user2"
        String userId3 = "user3"
        String userId4 = "user4"

        def proj1, proj1_subj, proj1_skills, proj2, proj2_subj, proj2_skills
        (proj1, proj1_subj, proj1_skills) = createProject.call(1)
        (proj2, proj2_subj, proj2_skills) = createProject.call(2)

        Closure initSkillsForProject = { String projId, def projSkills ->
            fullyAchieveSkill.call(userId1, projId, projSkills.get(0).skillId)

            skillsService.addSkill([projectId: projId, skillId: projSkills.get(1).skillId], userId1, new Date() - 2).body
            skillsService.addSkill([projectId: projId, skillId: projSkills.get(1).skillId], userId1, new Date() - 1).body
            skillsService.addSkill([projectId: projId, skillId: projSkills.get(1).skillId], userId1, new Date() - 0).body

            fullyAchieveSkill.call(userId1, projId, projSkills.get(2).skillId)

            skillsService.addSkill([projectId: projId, skillId: projSkills.get(0).skillId], userId2, new Date() - 1).body

            fullyAchieveSkill.call(userId3, projId, projSkills.get(0).skillId)
            fullyAchieveSkill.call(userId4, projId, projSkills.get(1).skillId)
        }

        initSkillsForProject.call(proj1.projectId, proj1_skills)
        initSkillsForProject.call(proj2.projectId, proj2_skills)

        List<UserAchievement> beforeAchievements = userAchievementRepo.findAll()
        when:
        assert getPointHistory(userId1, proj1.projectId) == [30, 60, 60 + 70]
        assert getPointHistory(userId1, proj1.projectId, proj1_subj.subjectId) == [30, 60, 60 + 70]
        assert getPointHistory(userId1, proj2.projectId) == [30, 60, 60 + 70]
        assert getPointHistory(userId1, proj2.projectId, proj2_subj.subjectId) == [30, 60, 60 + 70]

        // user only has 1 day so history is not returned
        assert getPointHistory(userId2, proj1.projectId) == []
        assert getPointHistory(userId2, proj1.projectId, proj1_subj.subjectId) == []
        assert getPointHistory(userId2, proj2.projectId) == []
        assert getPointHistory(userId2, proj2.projectId, proj2_subj.subjectId) == []

        assert getPointHistory(userId3, proj1.projectId) == [10, 20, 50]
        assert getPointHistory(userId3, proj1.projectId, proj1_subj.subjectId) == [10, 20, 50]
        assert getPointHistory(userId3, proj2.projectId) == [10, 20, 50]
        assert getPointHistory(userId3, proj2.projectId, proj2_subj.subjectId) == [10, 20, 50]

        assert getPointHistory(userId4, proj1.projectId) == [10, 20, 50]
        assert getPointHistory(userId4, proj1.projectId, proj1_subj.subjectId) == [10, 20, 50]
        assert getPointHistory(userId4, proj2.projectId) == [10, 20, 50]
        assert getPointHistory(userId4, proj2.projectId, proj2_subj.subjectId) == [10, 20, 50]

        proj1_skills.get(1).numPerformToCompletion = 3
        proj1_skills.get(1).pointIncrement = 15
        skillsService.createSkill(proj1_skills.get(1))

        List<UserAchievement> afterAchievements = userAchievementRepo.findAll()

        then:
        //1 skill achievement should be added, 1 subject level achievement should be added, one project level achievement should be added
        beforeAchievements.size() == afterAchievements.size() - 3
        afterAchievements.findAll { it.notified == 'false' }.size() == 3
        !beforeAchievements.findAll { it.projectId == proj1.projectId && it.skillId == proj1_skills.get(1).skillId && it.userId == userId1 }
        afterAchievements.find { it.projectId == proj1.projectId && it.skillId == proj1_skills.get(1).skillId && it.userId == userId1 }

        getSubjectSkillsPtsSlashTotalPts(userId1, proj1.projectId, proj1_subj.subjectId) == ["50/50", "45/45", "50/50"]
        getSubjectSkillsPtsSlashTotalPts(userId2, proj1.projectId, proj1_subj.subjectId) == ["10/50", "0/45", "0/50"]
        getSubjectSkillsPtsSlashTotalPts(userId3, proj1.projectId, proj1_subj.subjectId) == ["50/50", "0/45", "0/50"]
        getSubjectSkillsPtsSlashTotalPts(userId4, proj1.projectId, proj1_subj.subjectId) == ["0/50", "45/45", "0/50"]

        getSubjectSkillsPtsSlashTotalPts(userId1, proj2.projectId, proj2_subj.subjectId) == ["50/50", "30/50", "50/50"]
        getSubjectSkillsPtsSlashTotalPts(userId2, proj2.projectId, proj2_subj.subjectId) == ["10/50", "0/50", "0/50"]
        getSubjectSkillsPtsSlashTotalPts(userId3, proj2.projectId, proj2_subj.subjectId) == ["50/50", "0/50", "0/50"]
        getSubjectSkillsPtsSlashTotalPts(userId4, proj2.projectId, proj2_subj.subjectId) == ["0/50", "50/50", "0/50"]

        getPointHistory(userId1, proj1.projectId) == [35, 70, 70 + 75]
        getPointHistory(userId1, proj1.projectId, proj1_subj.subjectId) == [35, 70, 70 + 75]
        getPointHistory(userId1, proj2.projectId) == [30, 60, 60 + 70]
        getPointHistory(userId1, proj2.projectId, proj2_subj.subjectId) == [30, 60, 60 + 70]

        // user only has 1 day so history is not returned
        getPointHistory(userId2, proj1.projectId) == []
        getPointHistory(userId2, proj1.projectId, proj1_subj.subjectId) == []
        getPointHistory(userId2, proj2.projectId) == []
        getPointHistory(userId2, proj2.projectId, proj2_subj.subjectId) == []

        getPointHistory(userId3, proj1.projectId) == [10, 20, 50]
        getPointHistory(userId3, proj1.projectId, proj1_subj.subjectId) == [10, 20, 50]
        getPointHistory(userId3, proj2.projectId) == [10, 20, 50]
        getPointHistory(userId3, proj2.projectId, proj2_subj.subjectId) == [10, 20, 50]

        getPointHistory(userId4, proj1.projectId) == [15, 30, 45]
        getPointHistory(userId4, proj1.projectId, proj1_subj.subjectId) == [15, 30, 45]
        getPointHistory(userId4, proj2.projectId) == [10, 20, 50]
        getPointHistory(userId4, proj2.projectId, proj2_subj.subjectId) == [10, 20, 50]
    }

    def "point increment decreased and occurrences decreased"() {
        String userId1 = "user1"
        String userId2 = "user2"
        String userId3 = "user3"
        String userId4 = "user4"

        def proj1, proj1_subj, proj1_skills, proj2, proj2_subj, proj2_skills
        (proj1, proj1_subj, proj1_skills) = createProject.call(1)
        (proj2, proj2_subj, proj2_skills) = createProject.call(2)

        Closure initSkillsForProject = { String projId, def projSkills ->
            fullyAchieveSkill.call(userId1, projId, projSkills.get(0).skillId)

            skillsService.addSkill([projectId: projId, skillId: projSkills.get(1).skillId], userId1, new Date() - 2).body
            skillsService.addSkill([projectId: projId, skillId: projSkills.get(1).skillId], userId1, new Date() - 1).body
            skillsService.addSkill([projectId: projId, skillId: projSkills.get(1).skillId], userId1, new Date() - 0).body

            fullyAchieveSkill.call(userId1, projId, projSkills.get(2).skillId)

            skillsService.addSkill([projectId: projId, skillId: projSkills.get(0).skillId], userId2, new Date() - 1).body

            fullyAchieveSkill.call(userId3, projId, projSkills.get(0).skillId)
            fullyAchieveSkill.call(userId4, projId, projSkills.get(1).skillId)
        }

        initSkillsForProject.call(proj1.projectId, proj1_skills)
        initSkillsForProject.call(proj2.projectId, proj2_skills)

        List<UserAchievement> beforeAchievements = userAchievementRepo.findAll()
        when:
        assert getPointHistory(userId1, proj1.projectId) == [30, 60, 60 + 70]
        assert getPointHistory(userId1, proj1.projectId, proj1_subj.subjectId) == [30, 60, 60 + 70]
        assert getPointHistory(userId1, proj2.projectId) == [30, 60, 60 + 70]
        assert getPointHistory(userId1, proj2.projectId, proj2_subj.subjectId) == [30, 60, 60 + 70]

        // user only has 1 day so history is not returned
        assert getPointHistory(userId2, proj1.projectId) == []
        assert getPointHistory(userId2, proj1.projectId, proj1_subj.subjectId) == []
        assert getPointHistory(userId2, proj2.projectId) == []
        assert getPointHistory(userId2, proj2.projectId, proj2_subj.subjectId) == []

        assert getPointHistory(userId3, proj1.projectId) == [10, 20, 50]
        assert getPointHistory(userId3, proj1.projectId, proj1_subj.subjectId) == [10, 20, 50]
        assert getPointHistory(userId3, proj2.projectId) == [10, 20, 50]
        assert getPointHistory(userId3, proj2.projectId, proj2_subj.subjectId) == [10, 20, 50]

        assert getPointHistory(userId4, proj1.projectId) == [10, 20, 50]
        assert getPointHistory(userId4, proj1.projectId, proj1_subj.subjectId) == [10, 20, 50]
        assert getPointHistory(userId4, proj2.projectId) == [10, 20, 50]
        assert getPointHistory(userId4, proj2.projectId, proj2_subj.subjectId) == [10, 20, 50]

        printLevels(proj1_skills.get(1).projectId, "Before")
        proj1_skills.get(1).numPerformToCompletion = 3
        proj1_skills.get(1).pointIncrement = 3
        skillsService.createSkill(proj1_skills.get(1))
        printLevels(proj1_skills.get(1).projectId, "After")

        List<UserAchievement> afterAchievements = userAchievementRepo.findAll()

        List newAchievements = afterAchievements.findAll({ after -> !beforeAchievements.find({ it.id == after.id})})
        List<String> newAchievementsAsStrings = newAchievements.collect {
            "user=[${it.userId}], skillId=[${it.skillId ?: "Overall"}], achievement=[${it.level ? "Level ${it.level}" : "Skill Completed" }]"
        }.sort()

        then:
        newAchievementsAsStrings == [
            "user=[user1], skillId=[Overall], achievement=[Level 5]",
            "user=[user1], skillId=[TestSubject1], achievement=[Level 5]",
            "user=[user1], skillId=[skill2], achievement=[Skill Completed]",
            "user=[user2], skillId=[Overall], achievement=[Level 1]",
            "user=[user2], skillId=[TestSubject1], achievement=[Level 1]",
            "user=[user3], skillId=[Overall], achievement=[Level 3]",
            "user=[user3], skillId=[TestSubject1], achievement=[Level 3]",
        ]

        afterAchievements.findAll { it.notified == 'false' }.size() == newAchievementsAsStrings.size()
        !beforeAchievements.findAll { it.projectId == proj1.projectId && it.skillId == proj1_skills.get(1).skillId && it.userId == userId1 }
        afterAchievements.find { it.projectId == proj1.projectId && it.skillId == proj1_skills.get(1).skillId && it.userId == userId1 }

        getSubjectSkillsPtsSlashTotalPts(userId1, proj1.projectId, proj1_subj.subjectId) == ["50/50", "9/9", "50/50"]
        getSubjectSkillsPtsSlashTotalPts(userId2, proj1.projectId, proj1_subj.subjectId) == ["10/50", "0/9", "0/50"]
        getSubjectSkillsPtsSlashTotalPts(userId3, proj1.projectId, proj1_subj.subjectId) == ["50/50", "0/9", "0/50"]
        getSubjectSkillsPtsSlashTotalPts(userId4, proj1.projectId, proj1_subj.subjectId) == ["0/50", "9/9", "0/50"]

        getSubjectSkillsPtsSlashTotalPts(userId1, proj2.projectId, proj2_subj.subjectId) == ["50/50", "30/50", "50/50"]
        getSubjectSkillsPtsSlashTotalPts(userId2, proj2.projectId, proj2_subj.subjectId) == ["10/50", "0/50", "0/50"]
        getSubjectSkillsPtsSlashTotalPts(userId3, proj2.projectId, proj2_subj.subjectId) == ["50/50", "0/50", "0/50"]
        getSubjectSkillsPtsSlashTotalPts(userId4, proj2.projectId, proj2_subj.subjectId) == ["0/50", "50/50", "0/50"]

        getPointHistory(userId1, proj1.projectId) == [23, 46, 46 + 63]
        getPointHistory(userId1, proj1.projectId, proj1_subj.subjectId) == [23, 46, 46 + 63]
        getPointHistory(userId1, proj2.projectId) == [30, 60, 60 + 70]
        getPointHistory(userId1, proj2.projectId, proj2_subj.subjectId) == [30, 60, 60 + 70]

        // user only has 1 day so history is not returned
        getPointHistory(userId2, proj1.projectId) == []
        getPointHistory(userId2, proj1.projectId, proj1_subj.subjectId) == []
        getPointHistory(userId2, proj2.projectId) == []
        getPointHistory(userId2, proj2.projectId, proj2_subj.subjectId) == []

        getPointHistory(userId3, proj1.projectId) == [10, 20, 50]
        getPointHistory(userId3, proj1.projectId, proj1_subj.subjectId) == [10, 20, 50]
        getPointHistory(userId3, proj2.projectId) == [10, 20, 50]
        getPointHistory(userId3, proj2.projectId, proj2_subj.subjectId) == [10, 20, 50]

        getPointHistory(userId4, proj1.projectId) == [3, 6, 9]
        getPointHistory(userId4, proj1.projectId, proj1_subj.subjectId) == [3, 6, 9]
        getPointHistory(userId4, proj2.projectId) == [10, 20, 50]
        getPointHistory(userId4, proj2.projectId, proj2_subj.subjectId) == [10, 20, 50]
    }

    def "point increment decreased causes users to achieve levels"() {
        String userId1 = "user1"
        String userId2 = "user2"

        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(6,)
        skills.each {
            it.pointIncrement = 100
            it.numPerformToCompletion = 2
        }
        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        /**
         Level 1 => 120
         Level 2 => 300
         Level 3 => 540
         Level 4 => 804
         Level 5 => 1104
         */

        skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], userId1, new Date())
        def user1_summary_before = skillsService.getSkillSummary(userId1, proj.projectId)
        def user1_subj_summary_before = skillsService.getSkillSummary(userId1, proj.projectId, subj.subjectId)
        def user1_level_before = skillsService.getUserLevel(proj.projectId, userId1)

        skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], userId2, new Date())
        skillsService.addSkill([projectId: proj.projectId, skillId: skills[2].skillId], userId2, new Date())
        skillsService.addSkill([projectId: proj.projectId, skillId: skills[3].skillId], userId2, new Date())
        skillsService.addSkill([projectId: proj.projectId, skillId: skills[4].skillId], userId2, new Date())
        skillsService.addSkill([projectId: proj.projectId, skillId: skills[5].skillId], userId2, new Date())

        def user2_summary_before = skillsService.getSkillSummary(userId2, proj.projectId)
        def user2_subj_summary_before = skillsService.getSkillSummary(userId2, proj.projectId, subj.subjectId)
        def user2_level_before = skillsService.getUserLevel(proj.projectId, userId2)

        List<UserAchievement> beforeAllAchievements = userAchievementRepo.findAll()
        List<UserAchievement> user1BeforeAchievements = beforeAllAchievements.findAll( { it.userId == userId1 })
        List<UserAchievement> user2BeforeAchievements = beforeAllAchievements.findAll( { it.userId == userId2 })
        when:
        skills[1].pointIncrement = 2
        skillsService.createSkills([skills[1]])

        /**
         Level 1 => 100
         Level 2 => 251
         Level 3 => 451
         Level 4 => 672
         Level 5 => 923
         */

        def user1_summary_after = skillsService.getSkillSummary(userId1, proj.projectId)
        def user1_subj_summary_after = skillsService.getSkillSummary(userId1, proj.projectId, subj.subjectId)
        def user1_level_after = skillsService.getUserLevel(proj.projectId, userId1)

        def user2_summary_after = skillsService.getSkillSummary(userId2, proj.projectId)
        def user2_subj_summary_after = skillsService.getSkillSummary(userId2, proj.projectId, subj.subjectId)
        def user2_level_after = skillsService.getUserLevel(proj.projectId, userId2)

        List<UserAchievement> afterAllAchievements = userAchievementRepo.findAll()
        List<UserAchievement> user1AfterAchievements = afterAllAchievements.findAll( { it.userId == userId1 })
        List<UserAchievement> user2AfterAchievements = afterAllAchievements.findAll( { it.userId == userId2 })
                .findAll({ after -> !user2BeforeAchievements.find({ it.id == after.id})})


        then:
        user1_summary_before.points == 100
        user1_summary_before.skillsLevel == 0
        user1_summary_before.totalLevels == 5
        user1_summary_before.totalPoints == 1200
        user1_subj_summary_before.points == 100
        user1_subj_summary_before.skillsLevel == 0
        user1_subj_summary_before.totalLevels == 5
        user1_subj_summary_before.totalPoints == 1200
        user1_level_before == 0
        !user1BeforeAchievements

        user2_summary_before.points == 500
        user2_summary_before.skillsLevel == 2
        user2_summary_before.totalPoints == 1200
        user2_subj_summary_before.points == 500
        user2_subj_summary_before.skillsLevel == 2
        user2_subj_summary_before.totalPoints == 1200
        user2_level_before == 2
        user2BeforeAchievements

        user1_summary_after.points == 100
        user1_summary_after.skillsLevel == 1
        user1_summary_after.totalLevels == 5
        user1_summary_after.totalPoints == 1004
        user1_subj_summary_after.points == 100
        user1_subj_summary_after.skillsLevel == 1
        user1_subj_summary_after.totalLevels == 5
        user1_subj_summary_after.totalPoints == 1004
        user1AfterAchievements.size() == 2
        user1AfterAchievements.find { it.projectId == proj.projectId && !it.skillId && it.level == 1}
        user1AfterAchievements.find { it.projectId == proj.projectId && it.skillId == subj.subjectId && it.level == 1}
        user1_level_after == 1

        user2_summary_after.points == 500
        user2_summary_after.skillsLevel == 3
        user2_summary_after.totalPoints == 1004
        user2_subj_summary_after.points == 500
        user2_subj_summary_after.skillsLevel == 3
        user2_subj_summary_after.totalPoints == 1004
        user2_level_after == 3
        user2AfterAchievements.size() == 2
        user2AfterAchievements.find { it.projectId == proj.projectId && !it.skillId && it.level == 3}
        user2AfterAchievements.find { it.projectId == proj.projectId && it.skillId == subj.subjectId && it.level == 3}
    }

    def "deleting of a skill (without points) causes users to achieve levels"() {
        String userId1 = "user1"
        String userId2 = "user2"

        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(6,)
        skills.each {
            it.pointIncrement = 100
            it.numPerformToCompletion = 2
        }
        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        /**
         Level 1 => 120
         Level 2 => 300
         Level 3 => 540
         Level 4 => 804
         Level 5 => 1104
         */

        skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], userId1, new Date())
        def user1_summary_before = skillsService.getSkillSummary(userId1, proj.projectId)
        def user1_subj_summary_before = skillsService.getSkillSummary(userId1, proj.projectId, subj.subjectId)
        def user1_level_before = skillsService.getUserLevel(proj.projectId, userId1)

        skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], userId2, new Date())
        skillsService.addSkill([projectId: proj.projectId, skillId: skills[2].skillId], userId2, new Date())
        skillsService.addSkill([projectId: proj.projectId, skillId: skills[3].skillId], userId2, new Date())
        skillsService.addSkill([projectId: proj.projectId, skillId: skills[4].skillId], userId2, new Date())
        skillsService.addSkill([projectId: proj.projectId, skillId: skills[5].skillId], userId2, new Date())

        def user2_summary_before = skillsService.getSkillSummary(userId2, proj.projectId)
        def user2_subj_summary_before = skillsService.getSkillSummary(userId2, proj.projectId, subj.subjectId)
        def user2_level_before = skillsService.getUserLevel(proj.projectId, userId2)

        List<UserAchievement> beforeAllAchievements = userAchievementRepo.findAll()
        List<UserAchievement> user1BeforeAchievements = beforeAllAchievements.findAll( { it.userId == userId1 })
        List<UserAchievement> user2BeforeAchievements = beforeAllAchievements.findAll( { it.userId == userId2 })
        when:
        skillsService.deleteSkill([skills[1]])

        /**
         Level 1 => 100
         Level 2 => 250
         Level 3 => 450
         Level 4 => 670
         Level 5 => 920
         */

        def user1_summary_after = skillsService.getSkillSummary(userId1, proj.projectId)
        def user1_subj_summary_after = skillsService.getSkillSummary(userId1, proj.projectId, subj.subjectId)
        def user1_level_after = skillsService.getUserLevel(proj.projectId, userId1)

        def user2_summary_after = skillsService.getSkillSummary(userId2, proj.projectId)
        def user2_subj_summary_after = skillsService.getSkillSummary(userId2, proj.projectId, subj.subjectId)
        def user2_level_after = skillsService.getUserLevel(proj.projectId, userId2)

        List<UserAchievement> afterAllAchievements = userAchievementRepo.findAll()
        List<UserAchievement> user1AfterAchievements = afterAllAchievements.findAll( { it.userId == userId1 })
        println JsonOutput.toJson(user1AfterAchievements)
        List<UserAchievement> user2AfterAchievements = afterAllAchievements.findAll( { it.userId == userId2 })
                .findAll({ after -> !user2BeforeAchievements.find({ it.id == after.id})})


        then:
        user1_summary_before.points == 100
        user1_summary_before.skillsLevel == 0
        user1_summary_before.totalLevels == 5
        user1_summary_before.totalPoints == 1200
        user1_subj_summary_before.points == 100
        user1_subj_summary_before.skillsLevel == 0
        user1_subj_summary_before.totalLevels == 5
        user1_subj_summary_before.totalPoints == 1200
        user1_level_before == 0
        !user1BeforeAchievements

        user2_summary_before.points == 500
        user2_summary_before.skillsLevel == 2
        user2_summary_before.totalPoints == 1200
        user2_subj_summary_before.points == 500
        user2_subj_summary_before.skillsLevel == 2
        user2_subj_summary_before.totalPoints == 1200
        user2_level_before == 2
        user2BeforeAchievements

        user1_summary_after.points == 100
        user1_summary_after.skillsLevel == 1
        user1_summary_after.totalLevels == 5
        user1_summary_after.totalPoints == 1000
        user1_subj_summary_after.points == 100
        user1_subj_summary_after.skillsLevel == 1
        user1_subj_summary_after.totalLevels == 5
        user1_subj_summary_after.totalPoints == 1000
        user1AfterAchievements.size() == 2
        user1AfterAchievements.find { it.projectId == proj.projectId && !it.skillId && it.level == 1}
        user1AfterAchievements.find { it.projectId == proj.projectId && it.skillId == subj.subjectId && it.skillRefId && it.level == 1}
        user1_level_after == 1

        user2_summary_after.points == 500
        user2_summary_after.skillsLevel == 3
        user2_summary_after.totalPoints == 1000
        user2_subj_summary_after.points == 500
        user2_subj_summary_after.skillsLevel == 3
        user2_subj_summary_after.totalPoints == 1000
        user2_level_after == 3
        user2AfterAchievements.size() == 2
        user2AfterAchievements.find { it.projectId == proj.projectId && !it.skillId && it.level == 3}
        user2AfterAchievements.find { it.projectId == proj.projectId && it.skillId == subj.subjectId && it.level == 3}
    }

    def "deleting of a skill (with points) causes users to achieve levels"() {
        String userId1 = "user1"
        String userId2 = "user2"

        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(6,)
        skills.each {
            it.pointIncrement = 100
            it.numPerformToCompletion = 2
        }
        skills[1].pointIncrement = 1
        skills[1].numPerformToCompletion = 200

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        /**
         Level 1 => 120
         Level 2 => 300
         Level 3 => 540
         Level 4 => 804
         Level 5 => 1104
         */

        skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], userId1, new Date())
        skillsService.addSkill([projectId: proj.projectId, skillId: skills[1].skillId], userId1, new Date())
        def user1_summary_before = skillsService.getSkillSummary(userId1, proj.projectId)
        def user1_subj_summary_before = skillsService.getSkillSummary(userId1, proj.projectId, subj.subjectId)
        def user1_level_before = skillsService.getUserLevel(proj.projectId, userId1)

        skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], userId2, new Date())
        skillsService.addSkill([projectId: proj.projectId, skillId: skills[1].skillId], userId2, new Date())
        skillsService.addSkill([projectId: proj.projectId, skillId: skills[2].skillId], userId2, new Date())
        skillsService.addSkill([projectId: proj.projectId, skillId: skills[3].skillId], userId2, new Date())
        skillsService.addSkill([projectId: proj.projectId, skillId: skills[4].skillId], userId2, new Date())
        skillsService.addSkill([projectId: proj.projectId, skillId: skills[5].skillId], userId2, new Date())

        def user2_summary_before = skillsService.getSkillSummary(userId2, proj.projectId)
        def user2_subj_summary_before = skillsService.getSkillSummary(userId2, proj.projectId, subj.subjectId)
        def user2_level_before = skillsService.getUserLevel(proj.projectId, userId2)

        List<UserAchievement> beforeAllAchievements = userAchievementRepo.findAll()
        List<UserAchievement> user1BeforeAchievements = beforeAllAchievements.findAll( { it.userId == userId1 })
        List<UserAchievement> user2BeforeAchievements = beforeAllAchievements.findAll( { it.userId == userId2 })
        when:
        skillsService.deleteSkill([skills[1]])

        /**
         Level 1 => 100
         Level 2 => 250
         Level 3 => 450
         Level 4 => 670
         Level 5 => 920
         */

        def user1_summary_after = skillsService.getSkillSummary(userId1, proj.projectId)
        def user1_subj_summary_after = skillsService.getSkillSummary(userId1, proj.projectId, subj.subjectId)
        def user1_level_after = skillsService.getUserLevel(proj.projectId, userId1)

        def user2_summary_after = skillsService.getSkillSummary(userId2, proj.projectId)
        def user2_subj_summary_after = skillsService.getSkillSummary(userId2, proj.projectId, subj.subjectId)
        def user2_level_after = skillsService.getUserLevel(proj.projectId, userId2)

        List<UserAchievement> afterAllAchievements = userAchievementRepo.findAll()
        List<UserAchievement> user1AfterAchievements = afterAllAchievements.findAll( { it.userId == userId1 })
        println JsonOutput.toJson(user1AfterAchievements)
        List<UserAchievement> user2AfterAchievements = afterAllAchievements.findAll( { it.userId == userId2 })
                .findAll({ after -> !user2BeforeAchievements.find({ it.id == after.id})})


        then:
        user1_summary_before.points == 101
        user1_summary_before.skillsLevel == 0
        user1_summary_before.totalLevels == 5
        user1_summary_before.totalPoints == 1200
        user1_subj_summary_before.points == 101
        user1_subj_summary_before.skillsLevel == 0
        user1_subj_summary_before.totalLevels == 5
        user1_subj_summary_before.totalPoints == 1200
        user1_level_before == 0
        !user1BeforeAchievements

        user2_summary_before.points == 501
        user2_summary_before.skillsLevel == 2
        user2_summary_before.totalPoints == 1200
        user2_subj_summary_before.points == 501
        user2_subj_summary_before.skillsLevel == 2
        user2_subj_summary_before.totalPoints == 1200
        user2_level_before == 2
        user2BeforeAchievements

        user1_summary_after.points == 100
        user1_summary_after.skillsLevel == 1
        user1_summary_after.totalLevels == 5
        user1_summary_after.totalPoints == 1000
        user1_subj_summary_after.points == 100
        user1_subj_summary_after.skillsLevel == 1
        user1_subj_summary_after.totalLevels == 5
        user1_subj_summary_after.totalPoints == 1000
        user1AfterAchievements.size() == 2
        user1AfterAchievements.find { it.projectId == proj.projectId && !it.skillId && it.level == 1}
        user1AfterAchievements.find { it.projectId == proj.projectId && it.skillId == subj.subjectId && it.skillRefId && it.level == 1}
        user1_level_after == 1

        user2_summary_after.points == 500
        user2_summary_after.skillsLevel == 3
        user2_summary_after.totalPoints == 1000
        user2_subj_summary_after.points == 500
        user2_subj_summary_after.skillsLevel == 3
        user2_subj_summary_after.totalPoints == 1000
        user2_level_after == 3
        user2AfterAchievements.size() == 2
        user2AfterAchievements.find { it.projectId == proj.projectId && !it.skillId && it.level == 3}
        user2AfterAchievements.find { it.projectId == proj.projectId && it.skillId == subj.subjectId && it.level == 3}
    }

    def "deleting of a skill causes users to achieve subject levels but not project"() {
        String userId1 = "user1"
        String userId2 = "user2"

        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(6,)
        skills.each {
            it.pointIncrement = 100
            it.numPerformToCompletion = 2
        }
        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        def subj2 = SkillsFactory.createSubject(1, 2)
        def skills1 = SkillsFactory.createSkills(2, 1, 2)
        skills1.each {
            it.pointIncrement = 100
            it.numPerformToCompletion = 2
        }
        skillsService.createSubject(subj2)
        skillsService.createSkills(skills1)

        /**
         * subject 1:
         Level 1 => 120
         Level 2 => 300
         Level 3 => 540
         Level 4 => 804
         Level 5 => 1104
         */

        skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], userId1, new Date())
        def user1_summary_before = skillsService.getSkillSummary(userId1, proj.projectId)
        def user1_subj_summary_before = skillsService.getSkillSummary(userId1, proj.projectId, subj.subjectId)
        def user1_level_before = skillsService.getUserLevel(proj.projectId, userId1)

        skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], userId2, new Date())
        skillsService.addSkill([projectId: proj.projectId, skillId: skills[2].skillId], userId2, new Date())
        skillsService.addSkill([projectId: proj.projectId, skillId: skills[3].skillId], userId2, new Date())
        skillsService.addSkill([projectId: proj.projectId, skillId: skills[4].skillId], userId2, new Date())
        skillsService.addSkill([projectId: proj.projectId, skillId: skills[5].skillId], userId2, new Date())

        def user2_summary_before = skillsService.getSkillSummary(userId2, proj.projectId)
        def user2_subj_summary_before = skillsService.getSkillSummary(userId2, proj.projectId, subj.subjectId)
        def user2_level_before = skillsService.getUserLevel(proj.projectId, userId2)

        List<UserAchievement> beforeAllAchievements = userAchievementRepo.findAll()
        List<UserAchievement> user1BeforeAchievements = beforeAllAchievements.findAll( { it.userId == userId1 })
        List<UserAchievement> user2BeforeAchievements = beforeAllAchievements.findAll( { it.userId == userId2 })
        when:
        skillsService.deleteSkill([skills[1]])

        /**
         *  subject1:
         Level 1 => 100
         Level 2 => 250
         Level 3 => 450
         Level 4 => 670
         Level 5 => 920
         */

        def user1_summary_after = skillsService.getSkillSummary(userId1, proj.projectId)
        def user1_subj_summary_after = skillsService.getSkillSummary(userId1, proj.projectId, subj.subjectId)
        def user1_level_after = skillsService.getUserLevel(proj.projectId, userId1)

        def user2_summary_after = skillsService.getSkillSummary(userId2, proj.projectId)
        def user2_subj_summary_after = skillsService.getSkillSummary(userId2, proj.projectId, subj.subjectId)
        def user2_level_after = skillsService.getUserLevel(proj.projectId, userId2)

        List<UserAchievement> afterAllAchievements = userAchievementRepo.findAll()
        List<UserAchievement> user1AfterAchievements = afterAllAchievements.findAll( { it.userId == userId1 })
        println JsonOutput.toJson(user1AfterAchievements)
        List<UserAchievement> user2AfterAchievements = afterAllAchievements.findAll( { it.userId == userId2 })
                .findAll({ after -> !user2BeforeAchievements.find({ it.id == after.id})})


        then:
        user1_summary_before.points == 100
        user1_summary_before.skillsLevel == 0
        user1_summary_before.totalLevels == 5
        user1_summary_before.totalPoints == 1600
        user1_subj_summary_before.points == 100
        user1_subj_summary_before.skillsLevel == 0
        user1_subj_summary_before.totalLevels == 5
        user1_subj_summary_before.totalPoints == 1200
        user1_level_before == 0
        !user1BeforeAchievements

        user2_summary_before.points == 500
        user2_summary_before.skillsLevel == 2
        user2_summary_before.totalPoints == 1600
        user2_subj_summary_before.points == 500
        user2_subj_summary_before.skillsLevel == 2
        user2_subj_summary_before.totalPoints == 1200
        user2_level_before == 2
        user2BeforeAchievements

        user1_summary_after.points == 100
        user1_summary_after.skillsLevel == 0
        user1_summary_after.totalLevels == 5
        user1_summary_after.totalPoints == 1400
        user1_subj_summary_after.points == 100
        user1_subj_summary_after.skillsLevel == 1
        user1_subj_summary_after.totalLevels == 5
        user1_subj_summary_after.totalPoints == 1000
        user1AfterAchievements.size() == 1
        user1AfterAchievements.find { it.projectId == proj.projectId && it.skillId == subj.subjectId && it.skillRefId && it.level == 1}
        user1_level_after == 0

        user2_summary_after.points == 500
        user2_summary_after.skillsLevel == 2
        user2_summary_after.totalPoints == 1400
        user2_subj_summary_after.points == 500
        user2_subj_summary_after.skillsLevel == 3
        user2_subj_summary_after.totalPoints == 1000
        user2_level_after == 2
        user2AfterAchievements.size() == 1
        user2AfterAchievements.find { it.projectId == proj.projectId && it.skillId == subj.subjectId && it.level == 3}
    }

    def "decrease in occurrences causes project and subject level achievements"() {
        def project = SkillsFactory.createProject()
        skillsService.createProject(project)
        def subject = SkillsFactory.createSubject(1)
        skillsService.createSubject(subject)
        def subject2 = SkillsFactory.createSubject(1, 2)
        skillsService.createSubject(subject2)


        def skill1_1 = SkillsFactory.createSkill(1, 1, 1, 0, 10, 0, 100)
        skillsService.createSkill(skill1_1)
        def skill1_2 = SkillsFactory.createSkill(1, 1, 2, 0, 10, 0, 10)
        skillsService.createSkill(skill1_2)
        def skill2_1 = SkillsFactory.createSkill(1, 2, 1, 0, 1, 0, 100)
        skillsService.createSkill(skill2_1)

        String dateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSS"
        List<Date> dates = [new Date() - 2, new Date() - 3]
        when:
        skillsService.addSkill([projectId: project.projectId, skillId: skill1_1.skillId], "user1", dates[0])
        skillsService.addSkill([projectId: project.projectId, skillId: skill1_2.skillId], "user2", dates[1])
        def beforeEditAchievements = userAchievementRepo.findAllByUserAndProjectIds("user1", [project.projectId])
        def beforeEditAchievementsU2 = userAchievementRepo.findAllByUserAndProjectIds("user2", [project.projectId])
        printLevels(project.projectId, "before")
        skill1_1.numPerformToCompletion = 1
        skillsService.updateSkill(skill1_1, skill1_1.skillId)
        def afterEditAchievements = userAchievementRepo.findAllByUserAndProjectIds("user1", [project.projectId])
        printLevels(project.projectId, "after")
        println JsonOutput.prettyPrint(JsonOutput.toJson(afterEditAchievements))
        def afterEditAchievementsU2 = userAchievementRepo.findAllByUserAndProjectIds("user2", [project.projectId])
        //1300 total, levels:
        //1: 130
        //2: 325
        //3: 585
        //4: 871
        //5: 1196

        //300 total, levels:
        //1: 30
        //2: 75
        //3: 135
        //4: 201
        //5: 276

        then:
        beforeEditAchievements.size() == 0
        afterEditAchievements.size() == 6
        afterEditAchievements.findAll { it.notified == 'false' }.size() == 6
        afterEditAchievements.find { it.projectId == project.projectId && it.skillId == 'skill1' }
        afterEditAchievements.find { it.projectId == project.projectId && !it.skillId  && it.level == 1 }.achievedOn.format(dateFormat) == dates[0].format(dateFormat)
        afterEditAchievements.find { it.projectId == project.projectId && !it.skillId  && it.level == 2 }.achievedOn.format(dateFormat) == dates[0].format(dateFormat)
        afterEditAchievements.find { it.projectId == project.projectId && it.skillId == subject.subjectId && it.level == 1 }
        afterEditAchievements.find { it.projectId == project.projectId && it.skillId == subject.subjectId && it.level == 2 }
        afterEditAchievements.find { it.projectId == project.projectId && it.skillId == subject.subjectId && it.level == 3 }
        beforeEditAchievementsU2.size() == 0
        afterEditAchievementsU2.size() == 0
    }

    def "decrease in occurrences does not cause any achievements"() {
        def project = SkillsFactory.createProject()
        skillsService.createProject(project)
        def subject = SkillsFactory.createSubject(1)
        skillsService.createSubject(subject)
        def subject2 = SkillsFactory.createSubject(1, 2)
        skillsService.createSubject(subject2)


        def skill1_1 = SkillsFactory.createSkill(1, 1, 1, 0, 10, 0, 100)
        skillsService.createSkill(skill1_1)
        def skill1_2 = SkillsFactory.createSkill(1, 1, 2, 0, 10, 0, 10)
        skillsService.createSkill(skill1_2)
        def skill2_1 = SkillsFactory.createSkill(1, 2, 1, 0, 1, 0, 100)
        skillsService.createSkill(skill2_1)

        when:
        def beforeEditAchievements = userAchievementRepo.findAllByUserAndProjectIds("user1", [project.projectId])
        skill1_1.numPerformToCompletion = 1
        skillsService.updateSkill(skill1_1, skill1_1.skillId)
        def afterEditAchievements = userAchievementRepo.findAllByUserAndProjectIds("user1", [project.projectId])

        then:
        beforeEditAchievements.size() == 0
        afterEditAchievements.size() == 0
    }

    def "decrease in occurrences causes project and subject level achievements for points based levels"() {
        def project = SkillsFactory.createProject()
        skillsService.createProject(project)

        def subject2 = SkillsFactory.createSubject(1, 2)
        skillsService.createSubject(subject2)
        def skill2_1 = SkillsFactory.createSkill(1, 2, 1, 0, 1, 0, 100)
        skillsService.createSkill(skill2_1)

        def subject = SkillsFactory.createSubject(1)
        skillsService.createSubject(subject)
        def skill1_1 = SkillsFactory.createSkill(1, 1, 1, 0, 10, 0, 100)
        skillsService.createSkill(skill1_1)
        def skill1_2 = SkillsFactory.createSkill(1, 1, 2, 0, 10, 0, 10)
        skillsService.createSkill(skill1_2)

        String projectPointsSetting = "level.points.enabled"
        skillsService.changeSetting(project.projectId, projectPointsSetting, [projectId: project.projectId, setting: projectPointsSetting, value: "true"])


        when:
        skillsService.addSkill([projectId: project.projectId, skillId: skill1_1.skillId], "user1", new Date())
        skillsService.addSkill([projectId: project.projectId, skillId: skill1_2.skillId], "user2", new Date())
        def beforeEditAchievements = userAchievementRepo.findAllByUserAndProjectIds("user1", [project.projectId])
        def beforeEditAchievementsU2 = userAchievementRepo.findAllByUserAndProjectIds("user2", [project.projectId])
        skill1_1.numPerformToCompletion = 1
        skillsService.updateSkill(skill1_1, skill1_1.skillId)

        def afterEditAchievements = userAchievementRepo.findAllByUserAndProjectIds("user1", [project.projectId])
        def afterEditAchievementsU2 = userAchievementRepo.findAllByUserAndProjectIds("user2", [project.projectId])

        then:
        beforeEditAchievements.size() == 0
        afterEditAchievements.size() == 1
        afterEditAchievements.findAll { it.notified == 'false' }.size() == 1
        afterEditAchievements.find { it.projectId == project.projectId && it.skillId == 'skill1' }
        //with change to points based levels, once the occurrences are deleted, the pointsFrom for levels still exceed the user's
        //points
        !afterEditAchievements.find { it.projectId == project.projectId && !it.skillId  && it.level == 1 }
        !afterEditAchievements.find { it.projectId == project.projectId && !it.skillId  && it.level == 2 }
        !afterEditAchievements.find { it.projectId == project.projectId && !it.skillId  && it.level == 1 }
        !afterEditAchievements.find { it.projectId == project.projectId && it.skillId == subject.subjectId && it.level == 1 }
        !afterEditAchievements.find { it.projectId == project.projectId && it.skillId == subject.subjectId && it.level == 2 }
        !afterEditAchievements.find { it.projectId == project.projectId && it.skillId == subject.subjectId && it.level == 3 }
        beforeEditAchievementsU2.size() == 0
        afterEditAchievementsU2.size() == 0
    }

    private void printLevels(String projectId, String label, String subjectId = null) {
        StringBuilder stringBuilder = new StringBuilder()
        stringBuilder.append("------------\n${projectId}${subjectId ? ":${subjectId}" : ""} - ${label}:\n")
        levelDefinitionStorageService.getLevels(projectId, subjectId).each{
            stringBuilder.append "  Level ${it.level} : [${it.pointsFrom}]=>[${it.pointsTo}]\n"
        }
        stringBuilder.append("-----------\n")
        log.info(stringBuilder.toString())
    }
}
