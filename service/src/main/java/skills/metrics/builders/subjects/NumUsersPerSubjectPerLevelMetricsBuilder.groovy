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
package skills.metrics.builders.subjects

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import skills.metrics.builders.ProjectMetricsBuilder
import skills.storage.model.SkillDef
import skills.storage.repos.LevelDefRepo
import skills.storage.repos.UserAchievedLevelRepo
import skills.utils.InputSanitizer

@Component
@Slf4j
class NumUsersPerSubjectPerLevelMetricsBuilder implements ProjectMetricsBuilder {

    @Autowired
    UserAchievedLevelRepo userAchievedRepo

    @Autowired
    LevelDefRepo levelDefRepo

    @Override
    String getId() {
        return "numUsersPerSubjectPerLevelChartBuilder"
    }

    static class SubjectLevelCounts {
        String subject
        List<NumUsersPerLevel> numUsersPerLevels
    }

    static class NumUsersPerLevel {
        Integer level
        Long numberUsers
    }

    @Override
    def build(String projectId, String chartId, Map<String, String> props) {
        List<UserAchievedLevelRepo.SkillAndLevelUserCount> skillAndLevelUserCounts = userAchievedRepo.countNumUsersPerContainerTypeAndLevel(projectId, SkillDef.ContainerType.Subject)
        Map<String, List<UserAchievedLevelRepo.SkillAndLevelUserCount>> bySubjectId =  skillAndLevelUserCounts.groupBy {it.skillId}
        List<LevelDefRepo.SubjectLevelCount> subjectLevel = levelDefRepo.countNumLevelsPerSubject(projectId)
        return subjectLevel.collect{
            int numberLevels = it.getNumberLevels()
            List<UserAchievedLevelRepo.SkillAndLevelUserCount> userCountsForLevels = bySubjectId.get(it.getSkillId())
            List<NumUsersPerLevel> numUsersPerLevels = (1..numberLevels).collect {Integer level ->
                UserAchievedLevelRepo.SkillAndLevelUserCount found = userCountsForLevels.find({it.level == level})
                new NumUsersPerLevel(level: level, numberUsers: found?.getNumberUsers() ?: 0)
            }

            adjustCountsToOnlyCountLastLevel(numUsersPerLevels)

            return new SubjectLevelCounts(subject: InputSanitizer.unsanitizeName(it.subject), numUsersPerLevels: numUsersPerLevels)
        }
    }

    private void adjustCountsToOnlyCountLastLevel(List<NumUsersPerLevel> numUsersPerLevels) {
        List<NumUsersPerLevel> sorted = numUsersPerLevels.sort({ it.level }).reverse()
        for (int i = 0; i < sorted.size(); i++) {
            for (int j = i + 1; j < sorted.size(); j++) {
                sorted[j].numberUsers = sorted[j].numberUsers - sorted[i].numberUsers
            }
        }
    }
}
