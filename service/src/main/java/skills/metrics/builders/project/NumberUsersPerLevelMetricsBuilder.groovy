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
package skills.metrics.builders.project

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import skills.controller.result.model.LabelCountItem
import skills.metrics.builders.MetricsParams
import skills.metrics.builders.ProjectMetricsBuilder
import skills.services.AdminUsersService

@Component
class NumberUsersPerLevelMetricsBuilder implements ProjectMetricsBuilder {

    @Autowired
    AdminUsersService adminUsersService

    @Override
    String getId() {
        return "numUsersPerLevelChartBuilder"
    }

    @Override
    def build(String projectId, String chartId, Map<String, String> props) {
        String subjectId, tagKey, tagFilter
        if (props.containsKey(MetricsParams.P_SUBJECT_ID)) {
            subjectId = MetricsParams.getSubjectId(projectId, chartId, props);
        }
        if (props.containsKey(MetricsParams.P_TAG_KEY) && props.containsKey(MetricsParams.P_TAG_FILTER)) {
            tagKey = MetricsParams.getTagKey(projectId, chartId, props)
            tagFilter = MetricsParams.getTagFilter(projectId, chartId, props)
        }
        List<LabelCountItem> dataItems = adminUsersService.getUserCountsPerLevel(projectId, subjectId, tagKey, tagFilter)
        return dataItems
    }
}
