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

import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsFactory

class CustomValidationSpecs extends DefaultIntSpec {

    def "project name custom validation"(){

        when:
        skillsService.createProject([projectId: "Proj42", name: "Jabberwocky project"])

        then:
        def exception = thrown(SkillsClientException)
        exception.message.contains("names may not contain jabberwocky")
    }

    def "subject name custom validation"(){
        def proj = SkillsFactory.createProject()
        skillsService.createProject(proj)

        when:
        def subj = SkillsFactory.createSubject()
        subj.name = "sneaky_Jabberwocky_name"
        skillsService.createSubject(subj)

        then:
        def exception = thrown(SkillsClientException)
        exception.message.contains("names may not contain jabberwocky")
    }

    def "subject paragraph custom validation"(){
        def proj = SkillsFactory.createProject()
        skillsService.createProject(proj)

        when:
        def subj = SkillsFactory.createSubject()
        subj.name = "acceptable name"
        subj.description = """paragaraph

paragraph

jabberwocky

paragraph"""

        skillsService.createSubject(subj)

        then:
        def exception = thrown(SkillsClientException)
        exception.message.contains("paragraphs may not contain jabberwocky")
    }

    def "skill name custom validation"(){
        def proj = SkillsFactory.createProject()
        skillsService.createProject(proj)
        def subj = SkillsFactory.createSubject()
        skillsService.createSubject(subj)

        when:
        def skill = SkillsFactory.createSkill()
        skill.name = "has a jabberwocky in it"
        skillsService.createSkill(skill)

        then:
        def exception = thrown(SkillsClientException)
        exception.message.contains("names may not contain jabberwocky")
    }

    def "skill paragraph custom validation"(){
        def proj = SkillsFactory.createProject()
        skillsService.createProject(proj)
        def subj = SkillsFactory.createSubject()
        skillsService.createSubject(subj)

        when:
        def skill = SkillsFactory.createSkill()
        skill.name = "acceptable name"
        skill.description = """paragaraph

paragraph

jabberwocky

paragraph"""

        skillsService.createSkill(skill)

        then:
        def exception = thrown(SkillsClientException)
        exception.message.contains("paragraphs may not contain jabberwocky")
    }

    def "badge name custom validation"(){
        def proj = SkillsFactory.createProject()
        skillsService.createProject(proj)

        when:
        def badge = SkillsFactory.createBadge()
        badge.name = "has a jabberwocky in it"
        skillsService.createBadge(badge)

        then:
        def exception = thrown(SkillsClientException)
        exception.message.contains("names may not contain jabberwocky")
    }

    def "badge paragraph custom validation"(){
        def proj = SkillsFactory.createProject()
        skillsService.createProject(proj)

        when:
        def badge = SkillsFactory.createBadge()
        badge.name = "acceptable name"
        badge.description = """paragaraph

paragraph

jabberwocky

paragraph"""

        skillsService.createBadge(badge)

        then:
        def exception = thrown(SkillsClientException)
        exception.message.contains("paragraphs may not contain jabberwocky")
    }

    def "check against description validation endpoint"() {
        when:
        def res = skillsService.checkCustomDescriptionValidation("should not jabberwocky have")
        def resGood = skillsService.checkCustomDescriptionValidation("this one is fine")
        then:
        !res.body.valid
        res.body.msg == "paragraphs may not contain jabberwocky"

        resGood.body.valid
    }

    def "check codeblock against description validation endpoint"() {
        String descWithCodeBlock = """(A)
```
<template>
</template>
```

"""
        when:
        def res = skillsService.checkCustomDescriptionValidation("should not jabberwocky have")
        def resGood = skillsService.checkCustomDescriptionValidation(descWithCodeBlock)
        then:
        !res.body.valid
        res.body.msg == "paragraphs may not contain jabberwocky"

        resGood.body.valid
    }

    def "check against name validation endpoint"() {
        when:
        def res = skillsService.checkCustomNameValidation("should not jabberwocky have")
        def resGood = skillsService.checkCustomNameValidation("this one is fine")
        then:
        !res.body.valid
        res.body.msg == "names may not contain jabberwocky"

        resGood.body.valid
    }

    def "check against url validation endpoint"() {
        when:
        def res1 = skillsService.checkCustomUrlValidation("http://thisShouldBeFine.com/veryGood/ok/blah")
        def res2WithSpaces = skillsService.checkCustomUrlValidation("http://thisShouldBeFine.com/this one has spaces")
        def res3WithSpaces = skillsService.checkCustomUrlValidation("/this one has spaces")

        def res4BadFormat = skillsService.checkCustomUrlValidation("htt://thisShouldBeFine.com")
        then:
        res1.body.valid
        res2WithSpaces.body.valid
        res3WithSpaces.body.valid

        !res4BadFormat.body.valid
        res4BadFormat.body.msg == "only local urls or http/https protocols are allowed"
    }

    def "create badge with empty description"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        subj.description = ""
        def badge = SkillsFactory.createBadge()
        badge.description = ""

        skillsService.createProject(proj)
        when:
        skillsService.createSubject(subj)
        skillsService.createBadge(badge)

        then:
        true

    }
}
