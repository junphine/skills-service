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
import skills.metrics.builders.MetricsParams
import skills.metrics.builders.ProjectMetricsBuilder
import skills.storage.model.SkillDef
import skills.storage.repos.UserAchievedLevelRepo

@Component
@Slf4j
class AchievementsByTagPerLevelMetricsBuilder implements ProjectMetricsBuilder {

    @Autowired
    UserAchievedLevelRepo userAchievedRepo

    @Override
    String getId() {
        return "achievementsByTagPerLevelMetricsBuilder"
    }

    @Override
    def build(String projectId, String chartId, Map<String, String> props) {
        def userCount = userAchievedRepo.countNumUsersPerSubjectTagAndLevel(projectId, props.subjectId, props.userTagKey);

        Map users = new HashMap<String, Map>();
        def totalLevels = 0;
        userCount.sort({it.userTag}).forEach( it -> {
            Map userInfo = new HashMap<String, Integer>();
            totalLevels = it.level > totalLevels ? it.level : totalLevels
            userInfo.put(it.level, it.numberUsers);
            if(users[it.userTag]) {
                def tag = users[it.userTag];
                tag[it.level] = it.numberUsers
                users[it.userTag] = tag;
            } else {
                users[it.userTag] = userInfo;
            }
        })

        adjustCountsToOnlyCountLastLevel(users)

        def userList = []
        users.keySet().forEach{ key ->
            def totalUsers = users[key].totalUsers
            users[key].remove('totalUsers')
            def item = [ tag: key, value: users[key], totalUsers: totalUsers]
            userList.push(item)
        }
        def topUsers = userList.sort{ it -> it.totalUsers }.reverse().take(20)

        return [ totalLevels: totalLevels, data: topUsers ];
    }

    private void adjustCountsToOnlyCountLastLevel(users) {
        def tags = users.keySet();
        tags.forEach( tag -> {
            def levels = users[tag].size()
            users[tag]['totalUsers'] = users[tag][1]
            for (int i = levels; i > 1; i--) {
                for (int j = i - 1; j >= 1; j--) {
                    users[tag][j] = users[tag][j] - users[tag][i]
                }
            }
        })
    }
}
