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

class ProjectNameSearchSpecs extends DefaultIntSpec {

    def setup(){
        skillsService.createProject([projectId: "proj0", name: "Name Search Proj 0"])
        skillsService.createProject([projectId: "proj3", name: "Name Search Proj 3 other"])
        skillsService.createProject([projectId: "proj2", name: "Name Search Proj 2 good"])
        skillsService.createProject([projectId: "proj1", name: "Name Search Proj 1"])
    }

    def "project search must not return itself"(){
        when:
        def res = skillsService.searchOtherProjectsByName("proj0", "Na")
        then:
        res.size() == 3
        res.collect {it.projectId} == ["proj1", "proj2", "proj3"]
    }

    def "project search must be case insensitive"(){
        when:
        def res = skillsService.searchOtherProjectsByName("proj0", "NaMe")
        then:
        res.size() == 3
        res.collect {it.projectId} == ["proj1", "proj2", "proj3"]
    }

    def "project search must search anywhere in the name"(){
        when:
        def res = skillsService.searchOtherProjectsByName("proj0", "Search Proj")
        then:
        res.size() == 3
        res.collect {it.projectId} == ["proj1", "proj2", "proj3"]
    }

    def "project search must work on the full name"(){
        when:
        def res = skillsService.searchOtherProjectsByName("proj0", "Name Search Proj 2 good")
        then:
        res.size() == 1
        res.collect {it.projectId} == ["proj2"]
    }

    def "return only first 5 projects in the result"(){
        skillsService.createProject([projectId: "proj4", name: "Name Search Proj 4"])
        skillsService.createProject([projectId: "proj5", name: "Name Search Proj 5"])
        skillsService.createProject([projectId: "proj6", name: "Name Search Proj 6"])
        when:
        def res = skillsService.searchOtherProjectsByName("proj0", "NaMe")

        then:
        res.size() == 5
        res.collect {it.projectId} == ["proj1", "proj2", "proj3", "proj4", "proj5"]
    }

    def "empty search should return first 5 projects"(){
        skillsService.createProject([projectId: "proj4", name: "A Name Search Proj 4"])
        skillsService.createProject([projectId: "proj5", name: "A Name Search Proj 5"])
        skillsService.createProject([projectId: "proj6", name: "A Name Search Proj 6"])
        skillsService.createProject([projectId: "proj7", name: "A Name Search Proj 7"])
        skillsService.createProject([projectId: "proj8", name: "A Name Search Proj 8"])
        when:
        def res = skillsService.searchOtherProjectsByName("proj0", "")

        then:
        res.size() == 5
        res.collect {it.projectId} == ["proj4", "proj5", "proj6", "proj7", "proj8"]
    }
}
