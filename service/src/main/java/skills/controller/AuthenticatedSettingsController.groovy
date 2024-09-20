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
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController
import skills.UIConfigProperties
import skills.controller.exceptions.SkillsValidator
import skills.controller.result.model.SettingsResult
import skills.profile.EnableCallStackProf
import skills.services.settings.SettingsService

@RestController
@RequestMapping("/app/public")
@Slf4j
@EnableCallStackProf
class AuthenticatedSettingsController {

    @Autowired
    SettingsService settingsService

    @RequestMapping(value = "/settings/group/{settingGroup}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    List<SettingsResult> getPublicSettings(@PathVariable("settingGroup") String settingGroup) {
        SkillsValidator.isNotBlank(settingGroup, "Setting Group")
        SkillsValidator.isTrue(settingGroup.startsWith("public_"), "Setting Group [$settingGroup] must be prefixed with 'public_'")
        return settingsService.getGlobalSettingsByGroup(settingGroup)
    }

    @RequestMapping(value = "/settings/{setting}/group/{settingGroup}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    SettingsResult getPublicSetting(@PathVariable("setting") String setting, @PathVariable("settingGroup") String settingGroup) {
        SkillsValidator.isNotBlank(setting, "Setting Id")
        SkillsValidator.isNotBlank(settingGroup, "Setting Group")
        SkillsValidator.isTrue(settingGroup.startsWith("public_"), "Setting Group [$settingGroup] must be prefixed with 'public_'")
        return settingsService.getGlobalSetting(setting, settingGroup)
    }
}
