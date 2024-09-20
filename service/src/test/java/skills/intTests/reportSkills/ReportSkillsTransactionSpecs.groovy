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

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.annotation.Rollback
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsFactory
import skills.storage.model.UserAchievement
import skills.storage.model.UserPerformedSkill
import skills.storage.model.UserPoints
import skills.storage.repos.UserAchievedLevelRepo
import skills.storage.repos.UserPerformedSkillRepo
import skills.storage.repos.UserPointsRepo
import spock.lang.IgnoreIf

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock

@Slf4j
class ReportSkillsTransactionSpecs extends DefaultIntSpec {

    @Autowired
    JdbcTemplate jdbcTemplate

    @Autowired
    UserAchievedLevelRepo userAchievedLevelRepo

    @Autowired
    UserPerformedSkillRepo userPerformedSkillRepo

    @Autowired
    UserPointsRepo userPointsRepo

    def "multi threaded insert of the same skill"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(1,)
        skills[0].numPerformToCompletion = 1000
        skills[0].pointIncrementInterval = 0 // disable
        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        AtomicInteger atomicInteger = new AtomicInteger()
        int numThreads = 8
        int expectedCount = numThreads * 200
        CountDownLatch countDownLatch = new CountDownLatch(expectedCount)
        when:
        numThreads.times {
            Thread.start {
                200.times {
                    skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId])
                    int addedSkills = atomicInteger.incrementAndGet()
                    countDownLatch.countDown()
                    if (addedSkills % 25 == 0 || addedSkills == expectedCount) {
                        log.info("Reported {}/{} skills", atomicInteger.get(), expectedCount)
                    }
                }
            }
        }
        countDownLatch.await(1, TimeUnit.MINUTES)

        then:
        atomicInteger.get() == expectedCount

        List<UserAchievement> achievements = userAchievedLevelRepo.findAllByUserIdAndProjectIdAndSkillId(skillsService.userName, proj.projectId, null)
        List<UserPerformedSkill> userPerformedSkills = userPerformedSkillRepo.findAll().findAll { it.projectId == proj.projectId && it.userId == skillsService.userName }
        List<UserPoints> userPoints = userPointsRepo.findAll().findAll { it.projectId == proj.projectId && it.userId == skillsService.userName }

        // make sure total assigned points do not exceed max
        userPoints.each {
            assert it.points == skills[0].numPerformToCompletion * skills[0].pointIncrement
        }

        List<String> userPointsAsStrs = userPoints.collect { "${it.projectId}-${it.userId}-${it.skillId}".toString() }
        userPointsAsStrs.sort() == userPointsAsStrs.unique().sort()

        // validate that duplicate skills events were not inserted
        userPerformedSkills.size() == skills[0].numPerformToCompletion
        // validate that duplicate achievements were not iserted
        achievements.collect({ it.level }).sort() == achievements.collect({ it.level }).unique().sort()
    }

    @IgnoreIf({env["SPRING_PROFILES_ACTIVE"] == "pki" })
    def "multi threaded insert skill for many different users"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(1,)
        skills[0].numPerformToCompletion = 1
        skills[0].pointIncrementInterval = 0 // disable
        skills[0].pointIncrement = 1000
        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        AtomicInteger atomicInteger = new AtomicInteger()
        int numThreads = 8
        int expectedCount = numThreads * 200
        CountDownLatch countDownLatch = new CountDownLatch(expectedCount)
        List<String> ussrAttrsBefore = userAttrsRepo.findAll()
        when:
        numThreads.times {
            Thread.start {
                200.times {
                    skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], "ConcSkillInsertTest${it}", new Date())
                    int addedSkills = atomicInteger.incrementAndGet()
                    countDownLatch.countDown()
                    if (addedSkills % 25 == 0 || addedSkills == expectedCount) {
                        log.info("Reported {}/{} skills", atomicInteger.get(), expectedCount)
                    }
                }
            }
        }
        countDownLatch.await(1, TimeUnit.MINUTES)

        then:
        atomicInteger.get() == expectedCount

        List<String> ussrAttrs = userAttrsRepo.findAll().findAll({ it.userId.toLowerCase().startsWith("ConcSkillInsertTest".toLowerCase()) }).collect({ it.userId })
        ussrAttrs.size() == 200
        ussrAttrs.sort() == ussrAttrs.unique().sort()
    }

    def "test transaction when reporting skill - no rollback"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(1,)
        skills[0].numPerformToCompletion = 1
        skills[0].pointIncrement = 500
        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        String user = "usera"

        when:
//        SkillEventResult skillEventResult = reportSkillsTransactionWrapper.reportSkill1(proj.projectId, skills[0].skillId, user, new Date())
        def skillEventResult = skillsService.addSkillAndOptionallyThrowExceptionAtTheEnd([projectId: proj.projectId, skillId: skills[0].skillId], user, new Date(), false)
        List res = getUserPointsFromDB(proj.projectId, skills)
        then:
        skillEventResult.body.skillApplied
        res.size() == 1
    }

    @Rollback(false)
    def "test transaction when reporting skill - rollback"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(1,)
        skills[0].numPerformToCompletion = 1
        skills[0].pointIncrement = 500
        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        String user = "usera"

        when:
//        SkillEventResult skillEventResult = reportSkillsTransactionWrapper.reportSkill1(proj.projectId, skills[0].skillId, user, new Date())
        skillsService.addSkillAndOptionallyThrowExceptionAtTheEnd([projectId: proj.projectId, skillId: skills[0].skillId], user, new Date(), true)
        then:
        SkillsClientException exception = thrown(SkillsClientException)
        getUserPointsFromDB(proj.projectId, skills).size() == 0
        exception.httpStatus == HttpStatus.INTERNAL_SERVER_ERROR
    }

    private List<Map<String, Object>> getUserPointsFromDB(String projectId, List<Map> skills) {
        jdbcTemplate.queryForList("select * from user_points where project_id='${projectId}' and skill_id='${skills[0].skillId}'")
    }

}
