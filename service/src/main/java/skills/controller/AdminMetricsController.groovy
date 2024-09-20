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
package skills.controller

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*
import skills.metrics.MetricsService
import skills.profile.EnableCallStackProf
import skills.services.AdminUsersService

@RestController
@RequestMapping("/admin")
@Slf4j
@EnableCallStackProf
class AdminMetricsController {

    @Autowired
    AdminUsersService adminUsersService

    @Autowired
    MetricsService metricsServiceNew

    @RequestMapping(value = "/projects/{projectId}/metrics/{metricsId}", method =  RequestMethod.GET, produces = "application/json")
    def getChartData(@PathVariable("projectId") String projectId,
                                                  @PathVariable("metricsId") String metricsId,
                                                  @RequestParam Map<String,String> metricsProps) {
        return metricsServiceNew.loadProjectMetrics(projectId, metricsId, metricsProps)
    }

}
