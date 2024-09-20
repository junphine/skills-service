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
package skills.intTests.video

import groovy.json.JsonOutput
import groovy.util.logging.Slf4j
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.Resource
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsService
import skills.storage.model.SkillDef

import java.nio.file.Files

import static skills.intTests.utils.SkillsFactory.*

@Slf4j
class SkillVideoClientDisplaySpecs extends DefaultIntSpec {

    def "get video attributes for a single skill" () {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(6, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        skillsService.saveSkillVideoAttributes(p1.projectId, p1Skills[0].skillId, [
                videoUrl: "http://some.url",
                transcript: "transcript",
                captions: "captions",
        ])
        p1Skills[0].selfReportingType = SkillDef.SelfReportingType.Video
        skillsService.createSkill(p1Skills[0])

        skillsService.saveSkillVideoAttributes(p1.projectId, p1Skills[1].skillId, [
                videoUrl: "http://some.url",
                transcript: "transcript",
        ])
        p1Skills[1].selfReportingType = SkillDef.SelfReportingType.Video
        skillsService.createSkill(p1Skills[1])

        skillsService.saveSkillVideoAttributes(p1.projectId, p1Skills[2].skillId, [
                videoUrl: "http://some.url",
        ])
        p1Skills[2].selfReportingType = SkillDef.SelfReportingType.Video
        skillsService.createSkill(p1Skills[2])

        skillsService.saveSkillVideoAttributes(p1.projectId, p1Skills[3].skillId, [
                videoUrl: "http://some.url",
        ])
        p1Skills[3].selfReportingType = SkillDef.SelfReportingType.Video
        skillsService.createSkill(p1Skills[3])

        skillsService.saveSkillVideoAttributes(p1.projectId, p1Skills[4].skillId, [
                videoUrl: "http://some.url",
        ])

        def user = getRandomUsers(1).first()
        when:
        def skill = skillsService.getSingleSkillSummary(user, p1.projectId, p1Skills[0].skillId)
        def skill1 = skillsService.getSingleSkillSummary(user, p1.projectId, p1Skills[1].skillId)
        def skill2 = skillsService.getSingleSkillSummary(user, p1.projectId, p1Skills[2].skillId)
        def skill3 = skillsService.getSingleSkillSummary(user, p1.projectId, p1Skills[3].skillId)
        def skill4 = skillsService.getSingleSkillSummary(user, p1.projectId, p1Skills[4].skillId)
        def skill5 = skillsService.getSingleSkillSummary(user, p1.projectId, p1Skills[5].skillId)
        println JsonOutput.prettyPrint(JsonOutput.toJson(skill1))
        then:
        skill.videoSummary.videoUrl == "http://some.url"
        skill.videoSummary.hasCaptions == true
        skill.videoSummary.hasTranscript == true
        skill.selfReporting.type == SkillDef.SelfReportingType.Video.toString()

        skill1.videoSummary.videoUrl == "http://some.url"
        skill1.videoSummary.hasCaptions == false
        skill1.videoSummary.hasTranscript == true
        skill1.selfReporting.type == SkillDef.SelfReportingType.Video.toString()

        skill2.videoSummary.videoUrl == "http://some.url"
        skill2.videoSummary.hasCaptions == false
        skill2.videoSummary.hasTranscript == false
        skill2.selfReporting.type == SkillDef.SelfReportingType.Video.toString()

        skill3.videoSummary.videoUrl == "http://some.url"
        skill3.videoSummary.hasCaptions == false
        skill3.videoSummary.hasTranscript == false
        skill3.selfReporting.type == SkillDef.SelfReportingType.Video.toString()

        skill4.videoSummary.videoUrl == "http://some.url"
        skill4.videoSummary.hasCaptions == false
        skill4.videoSummary.hasTranscript == false
        !skill4.selfReporting?.type

        !skill5.videoSummary
        !skill4.selfReporting?.type
    }

    def "get video attributes in descriptions endpoint" () {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(6, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        skillsService.saveSkillVideoAttributes(p1.projectId, p1Skills[0].skillId, [
                videoUrl  : "http://some.url",
                transcript: "transcript",
                captions  : "captions",
        ])
        p1Skills[0].selfReportingType = SkillDef.SelfReportingType.Video
        skillsService.createSkill(p1Skills[0])

        skillsService.saveSkillVideoAttributes(p1.projectId, p1Skills[1].skillId, [
                videoUrl  : "http://some.url",
                transcript: "transcript",
        ])
        p1Skills[1].selfReportingType = SkillDef.SelfReportingType.Video
        skillsService.createSkill(p1Skills[1])

        skillsService.saveSkillVideoAttributes(p1.projectId, p1Skills[2].skillId, [
                videoUrl : "http://some.url",
        ])
        p1Skills[2].selfReportingType = SkillDef.SelfReportingType.Video
        skillsService.createSkill(p1Skills[2])

        skillsService.saveSkillVideoAttributes(p1.projectId, p1Skills[3].skillId, [
                videoUrl: "http://some.url",
        ])
        p1Skills[3].selfReportingType = SkillDef.SelfReportingType.Video
        skillsService.createSkill(p1Skills[3])

        skillsService.saveSkillVideoAttributes(p1.projectId, p1Skills[4].skillId, [
                videoUrl: "http://some.url",
        ])

        def user = getRandomUsers(1).first()
        when:
        def descriptions = skillsService.getSubjectDescriptions(p1.projectId, p1subj1.subjectId, user)
        def skill1VidSummary = descriptions.find { it.skillId == p1Skills[0].skillId }.videoSummary
        def skill2VidSummary = descriptions.find { it.skillId == p1Skills[1].skillId }.videoSummary
        def skill3VidSummary = descriptions.find { it.skillId == p1Skills[2].skillId }.videoSummary
        def skill4VidSummary = descriptions.find { it.skillId == p1Skills[3].skillId }.videoSummary
        def skill5VidSummary = descriptions.find { it.skillId == p1Skills[4].skillId }.videoSummary
        def skill6VidSummary = descriptions.find { it.skillId == p1Skills[5].skillId }.videoSummary
        then:
        skill1VidSummary.videoUrl == "http://some.url"
        skill1VidSummary.hasCaptions == true
        skill1VidSummary.hasTranscript == true

        skill2VidSummary.videoUrl == "http://some.url"
        skill2VidSummary.hasCaptions == false
        skill2VidSummary.hasTranscript == true

        skill3VidSummary.videoUrl == "http://some.url"
        skill3VidSummary.hasCaptions == false
        skill3VidSummary.hasTranscript == false

        skill4VidSummary.videoUrl == "http://some.url"
        skill4VidSummary.hasCaptions == false
        skill4VidSummary.hasTranscript == false

        skill5VidSummary.videoUrl == "http://some.url"
        skill5VidSummary.hasCaptions == false
        skill5VidSummary.hasTranscript == false

        !skill6VidSummary
    }

    def "get video attributes in badge descriptions endpoint" () {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(6, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        skillsService.saveSkillVideoAttributes(p1.projectId, p1Skills[0].skillId, [
                videoUrl  : "http://some.url",
                transcript: "transcript",
                captions  : "captions",
        ])
        p1Skills[0].selfReportingType = SkillDef.SelfReportingType.Video
        skillsService.createSkill(p1Skills[0])

        skillsService.saveSkillVideoAttributes(p1.projectId, p1Skills[1].skillId, [
                videoUrl  : "http://some.url",
                transcript: "transcript",
        ])
        p1Skills[1].selfReportingType = SkillDef.SelfReportingType.Video
        skillsService.createSkill(p1Skills[1])

        skillsService.saveSkillVideoAttributes(p1.projectId, p1Skills[2].skillId, [
                videoUrl : "http://some.url",
        ])
        p1Skills[2].selfReportingType = SkillDef.SelfReportingType.Video
        skillsService.createSkill(p1Skills[2])

        skillsService.saveSkillVideoAttributes(p1.projectId, p1Skills[3].skillId, [
                videoUrl: "http://some.url",
        ])
        p1Skills[3].selfReportingType = SkillDef.SelfReportingType.Video
        skillsService.createSkill(p1Skills[3])

        skillsService.saveSkillVideoAttributes(p1.projectId, p1Skills[4].skillId, [
                videoUrl: "http://some.url",
        ])

        def badge = createBadge(1, 20)
        skillsService.createBadge(badge)
        p1Skills.each {
            skillsService.assignSkillToBadge(projectId: p1.projectId, badgeId: badge.badgeId, skillId: it.skillId)
        }
        badge.enabled = 'true'
        skillsService.createBadge(badge)

        def user = getRandomUsers(1).first()
        when:
        def descriptions = skillsService.getBadgeDescriptions(p1.projectId, badge.badgeId, false, user)
        def skill1VidSummary = descriptions.find { it.skillId == p1Skills[0].skillId }.videoSummary
        def skill2VidSummary = descriptions.find { it.skillId == p1Skills[1].skillId }.videoSummary
        def skill3VidSummary = descriptions.find { it.skillId == p1Skills[2].skillId }.videoSummary
        def skill4VidSummary = descriptions.find { it.skillId == p1Skills[3].skillId }.videoSummary
        def skill5VidSummary = descriptions.find { it.skillId == p1Skills[4].skillId }.videoSummary
        def skill6VidSummary = descriptions.find { it.skillId == p1Skills[5].skillId }.videoSummary
        then:
        skill1VidSummary.videoUrl == "http://some.url"
        skill1VidSummary.hasCaptions == true
        skill1VidSummary.hasTranscript == true

        skill2VidSummary.videoUrl == "http://some.url"
        skill2VidSummary.hasCaptions == false
        skill2VidSummary.hasTranscript == true

        skill3VidSummary.videoUrl == "http://some.url"
        skill3VidSummary.hasCaptions == false
        skill3VidSummary.hasTranscript == false

        skill4VidSummary.videoUrl == "http://some.url"
        skill4VidSummary.hasCaptions == false
        skill4VidSummary.hasTranscript == false

        skill5VidSummary.videoUrl == "http://some.url"
        skill5VidSummary.hasCaptions == false
        skill5VidSummary.hasTranscript == false

        !skill6VidSummary
    }

    def "get captions endpoint" () {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(2, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        skillsService.saveSkillVideoAttributes(p1.projectId, p1Skills[0].skillId, [
                videoUrl  : "http://some.url",
                transcript: "transcript",
                captions  : "captions",
        ])
        p1Skills[0].selfReportingType = SkillDef.SelfReportingType.Video
        skillsService.createSkill(p1Skills[0])

        skillsService.saveSkillVideoAttributes(p1.projectId, p1Skills[1].skillId, [
                videoUrl  : "http://some.url",
                transcript: "transcript",
        ])
        p1Skills[1].selfReportingType = SkillDef.SelfReportingType.Video
        skillsService.createSkill(p1Skills[1])

        when:
        def skill1Captions = skillsService.getVideoCaptions(p1.projectId, p1Skills[0].skillId)
        def skill2Captions = skillsService.getVideoCaptions(p1.projectId, p1Skills[1].skillId)

        then:
        skill1Captions == "captions"
        !skill2Captions
    }

    def "get transcript endpoint" () {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(2, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        skillsService.saveSkillVideoAttributes(p1.projectId, p1Skills[0].skillId, [
                videoUrl  : "http://some.url",
                transcript: "transcript",
                captions  : "captions",
        ])
        p1Skills[0].selfReportingType = SkillDef.SelfReportingType.Video
        skillsService.createSkill(p1Skills[0])

        skillsService.saveSkillVideoAttributes(p1.projectId, p1Skills[1].skillId, [
                videoUrl  : "http://some.url",
        ])
        p1Skills[1].selfReportingType = SkillDef.SelfReportingType.Video
        skillsService.createSkill(p1Skills[1])

        when:
        def skill1T = skillsService.getVideoTranscript(p1.projectId, p1Skills[0].skillId)
        def skill2T = skillsService.getVideoTranscript(p1.projectId, p1Skills[1].skillId)

        then:
        skill1T == "transcript"
        !skill2T
    }

    def "download video for watching" () {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(1, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        Resource video = new ClassPathResource("/testVideos/create-quiz.mp4")
        skillsService.saveSkillVideoAttributes(p1.projectId, p1Skills[0].skillId, [
                file: video,
                transcript: "transcript",
                captions: "captions",
        ])

        def user = getRandomUsers(1).first()
        def skill = skillsService.getSingleSkillSummary(user, p1.projectId, p1Skills[0].skillId)
        def videoUrl = skill.videoSummary.videoUrl

        File resource = video.getFile();
        byte[] expectedContentAsBytes = Files.readAllBytes(resource.toPath())

        when:
        SkillsService.FileAndHeaders fileAndHeaders = skillsService.downloadAttachment(videoUrl)
        then:
        fileAndHeaders.headers.get(HttpHeaders.CONTENT_TYPE)[0] == "video/mp4"
        fileAndHeaders.file.bytes == expectedContentAsBytes
    }

    def "UC protection applies for video download" () {
        SkillsService rootSkillsService = createRootSkillService()
        skillsService.getCurrentUser() // initialize skillsService user_attrs

        String userCommunityUserId =  skillsService.userName
        rootSkillsService.saveUserTag(userCommunityUserId, 'dragons', ['DivineDragon']);

        List<String> userNames = getRandomUsers(2)
        SkillsService nonUcUser = createService(userNames[0])

        SkillsService otherUcUser = createService(userNames[1])
        rootSkillsService.saveUserTag(otherUcUser.userName, 'dragons', ['DivineDragon']);

        def p1 = createProject(1)
        p1.enableProtectedUserCommunity = true
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(1, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        Resource video = new ClassPathResource("/testVideos/create-quiz.mp4")
        skillsService.saveSkillVideoAttributes(p1.projectId, p1Skills[0].skillId, [
                file: video,
                transcript: "transcript",
                captions: "captions",
        ])

        def user = getRandomUsers(1).first()
        def skill = skillsService.getSingleSkillSummary(user, p1.projectId, p1Skills[0].skillId)
        def videoUrl = skill.videoSummary.videoUrl

        File resource = video.getFile();
        byte[] expectedContentAsBytes = Files.readAllBytes(resource.toPath())

        when:
        SkillsService.FileAndHeaders fileAndHeaders = skillsService.downloadAttachment(videoUrl)
        SkillsService.FileAndHeaders fileAndHeaders1 = otherUcUser.downloadAttachment(videoUrl)

        nonUcUser.downloadAttachment(videoUrl)
        then:
        SkillsClientException exception = thrown(SkillsClientException)
        exception.httpStatus == HttpStatus.FORBIDDEN

        fileAndHeaders.headers.get(HttpHeaders.CONTENT_TYPE)[0] == "video/mp4"
        fileAndHeaders.file.bytes == expectedContentAsBytes

        fileAndHeaders1.headers.get(HttpHeaders.CONTENT_TYPE)[0] == "video/mp4"
        fileAndHeaders1.file.bytes == expectedContentAsBytes
    }
}
