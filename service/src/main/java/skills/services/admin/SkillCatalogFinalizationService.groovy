/**
 * Copyright 2021 SkillTree
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
package skills.services.admin

import callStack.profiler.Profile
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import skills.controller.exceptions.SkillException
import skills.controller.request.model.ProjectSettingsRequest
import skills.controller.result.model.SettingsResult
import skills.services.RuleSetDefGraphService
import skills.services.attributes.SkillAttributeService
import skills.services.events.SkillDate
import skills.services.events.pointsAndAchievements.ImportedSkillsAchievementsHandler
import skills.services.settings.SettingsService
import skills.storage.accessors.ProjDefAccessor
import skills.storage.model.SkillDef
import skills.storage.model.SkillDefMin
import skills.storage.model.UserAchievement
import skills.storage.model.UserPerformedSkill
import skills.storage.repos.*
import skills.storage.repos.nativeSql.PostgresQlNativeRepo
import skills.tasks.TaskSchedulerService
import skills.tasks.config.TaskConfig

@Service
@Slf4j
class SkillCatalogFinalizationService {

    @Autowired
    ProjDefAccessor projDefAccessor

    @Autowired
    SkillDefRepo skillDefRepo

    @Autowired
    SkillRelDefRepo skillRelDefRepo

    @Autowired
    RuleSetDefGraphService ruleSetDefGraphService

    @Autowired
    UserPointsRepo userPointsRepo

    @Autowired
    PostgresQlNativeRepo PostgresQlNativeRepo

    @Autowired
    UserAchievedLevelRepo userAchievedLevelRepo

    @Autowired
    UserPerformedSkillRepo userPerformedSkillRepo

    @Autowired
    TaskSchedulerService taskSchedulerService

    @Autowired
    ImportedSkillsAchievementsHandler importedSkillsAchievementsHandler

    @Autowired
    SettingsService settingsService

    @Autowired
    BatchOperationsTransactionalAccessor batchOperationsTransactionalAccessor

    @Autowired
    SkillAttributeService skillAttributeService

    final static String PROJ_FINALIZE_STATE_PROP = "catalog.finalize.state"
    static enum FinalizeState {
        NOT_RUNNING, RUNNING, COMPLETED, FAILED
    }

    @Transactional
    @Profile
    void requestFinalizationOfImportedSkills(String projectId) {
        if (getCurrentState(projectId) == FinalizeState.RUNNING) {
            throw new SkillException("Catalog import finalize is already running for [${projectId}]", projectId)
        }
        updateState(projectId, FinalizeState.RUNNING)

        taskSchedulerService.scheduleCatalogImportFinalization(projectId)
    }

    static class FinalizeCatalogSkillsImportResult {
        long start
        long end
        List<Integer> skillRefIds
    }

    // NOTE: it's critical NOT put @Transactional; each underlying statement should run in its transaction
    // wrapping the whole finalization process in a Transactions causes the method to take hours for a project with lots of users
    @Profile
    FinalizeCatalogSkillsImportResult finalizeCatalogSkillsImport(String projectId) {
        long start = System.currentTimeMillis()
        log.info("Finalizing imported skills for [{}]", projectId)
        List<Integer> finalizedSkillIds = []
        try {
            projDefAccessor.getProjDef(projectId) // validate
            List<SkillDef> disabledImportedSkills = skillDefRepo.findAllByProjectIdAndTypeAndEnabledAndCopiedFromIsNotNull(projectId, SkillDef.ContainerType.Skill, Boolean.FALSE.toString())

            if (disabledImportedSkills) {
                // update groups points
                List<SkillDef> groups = disabledImportedSkills
                        .findAll({ it.groupId != null })
                        .collect { ruleSetDefGraphService.getParentSkill(it) }
                        .unique(false) { SkillDef a, SkillDef b -> a.skillId <=> b.skillId }
                groups.each {
                    batchOperationsTransactionalAccessor.updateGroupTotalPoints(projectId, it.skillId)
                }

                // important: must update subject's total points first then project
                List<SkillDef> subjects = disabledImportedSkills.collect { ruleSetDefGraphService.getMySubjectParent(it.id) }
                        .unique(false) { SkillDef a, SkillDef b -> a.skillId <=> b.skillId }
                subjects.each {
                    batchOperationsTransactionalAccessor.updateSubjectTotalPoints(projectId, it.skillId)
                }
                batchOperationsTransactionalAccessor.updateProjectsTotalPoints(projectId)
                log.info("Updated totalPoints attribute for [{}] project", projectId)

                List<Integer> skillRefIds = disabledImportedSkills.collect { it.copiedFrom }
                start = System.currentTimeMillis()

                // 1. copy skill points and achievements
                batchOperationsTransactionalAccessor.copySkillUserPointsToTheImportedProjects(projectId, skillRefIds)
                batchOperationsTransactionalAccessor.copySkillAchievementsToTheImportedProjects(projectId, skillRefIds)

                log.info("Identifying group achievements for [{}] groups in project [{}]", groups.size(), projectId)
                batchOperationsTransactionalAccessor.identifyAndAddGroupAchievements(groups)
                log.info("Completed import of group achievements for [{}] groups in [{}] project", groups.size(), projectId)

                // 2. for each subject (1) create user points for new users (2) update existing (3) calculate achievements
                subjects.each { SkillDef subject ->
                    batchOperationsTransactionalAccessor.handlePointsAndAchievementsForSubject(subject)
                }

                // 3. for the project (1) create user points for new users (2) update existing (3) calculate achievements
                batchOperationsTransactionalAccessor.handlePointsAndAchievementsForProject(projectId)

                log.info("Completed import of points and achievements for [{}] skills for project [{}]", skillRefIds.size(), projectId)
                finalizedSkillIds = disabledImportedSkills.collect {it.id }
                batchOperationsTransactionalAccessor.enableSkills(disabledImportedSkills)
            } else {
                log.warn("Finalize was called for [{}] projectId but there were no disabled skills", projectId)
            }

            updateState(projectId, FinalizeState.COMPLETED)
        } catch (Throwable t) {
            updateState(projectId, FinalizeState.FAILED)
            throw new TaskConfig.DoNotRetryAsyncTaskException("Failed to finalize [${projectId}] project", t)
        }

        long end = System.currentTimeMillis()
        log.info("Completed finalizing imported skills for [{}]", projectId)
        return new FinalizeCatalogSkillsImportResult(start: start, end:end, skillRefIds: finalizedSkillIds)
    }

    @Transactional
    @Profile
    void applyEventsThatWereReportedDuringTheFinalizationRun(List<Integer> finalizedSkillIds, long startOfFinalization, long endOfFinalization) {
        if (finalizedSkillIds) {
            String dateFormat = "yyyy-MM-dd HH:mm:ss,SSS"
            Date start = new Date(startOfFinalization)
            Date end = new Date(endOfFinalization)
            log.info("Handling Events that were reporting during the finalization runs for [{}] skills between [{}] => [{}]", finalizedSkillIds.size(), start.format(dateFormat), end.format(dateFormat))
            finalizedSkillIds.each { Integer importedSkillRefId ->
                Integer originalSkillRefId = skillDefRepo.getCopiedFromById(importedSkillRefId)
                List<UserPerformedSkill> foundEvents = userPerformedSkillRepo.findAllBySkillRefIdWithinTimeRange(originalSkillRefId, start, end)
                if (foundEvents) {
                    SkillDefMin skill = skillDefRepo.findSkillDefMinById(originalSkillRefId)
                    Boolean isMotivationalSkill = skillAttributeService.isMotivationalSkill(skill.projectId, skill.skillId)
                    log.info("Processing [{}] missed events for skill [{}] between [{}] and [{}]", foundEvents.size(), skill.skillId, start, end)
                    foundEvents.each {
                        log.info("Processing missed event skill=[{}], created=[{}], userId=[{}]", it.skillId, it.created.format(dateFormat), it.userId)
                        List<UserAchievement> userAchievements = userAchievedLevelRepo.findAllByUserIdAndProjectIdAndSkillId(it.userId, it.projectId, it.skillId)
                        boolean thisRequestCompletedOriginalSkill = false
                        if (userAchievements) {
                            UserAchievement userAchievement = userAchievements.first()
                            thisRequestCompletedOriginalSkill = userAchievement.achievedOn.time == it.performedOn.time
                        }
                        SkillDate skillDate = new SkillDate(date: new Date(it.performedOn.time), isProvided: true)
                        log.info("scheduleImportedSkillAchievement userId=[{}], importedSkillRefId=[{}], skillDate=[{}], thisRequestCompletedOriginalSkill=[{}], isMotivationalSkill=[{}]",
                                it.userId, importedSkillRefId, skillDate, thisRequestCompletedOriginalSkill, isMotivationalSkill)
                        taskSchedulerService.scheduleImportedSkillAchievement(it.userId, importedSkillRefId, skillDate, thisRequestCompletedOriginalSkill, isMotivationalSkill)
                    }
                } else {
                    log.info("Handling Events that were reporting during the finalization: Found 0 events for originalSkillRefId=[{}], importedSkillRefId=[{}]. Nothing to do", originalSkillRefId, importedSkillRefId)
                }
            }
        }
    }


    @Profile
    void validateNotInFinalizationState(String projectId, String msg) {
        if (getCurrentState(projectId) == SkillCatalogFinalizationService.FinalizeState.RUNNING) {
            throw new SkillException(msg, projectId)
        }
    }

    @Profile
    void validateFinalizationIsNotPending(String projectId, String msg) {
        List<SkillDef> pendingSkills = skillDefRepo.findAllByProjectIdAndTypeAndEnabledAndCopiedFromIsNotNull(projectId, SkillDef.ContainerType.Skill, Boolean.FALSE.toString())
        if (pendingSkills) {
            throw new SkillException(msg, projectId)
        }
    }

    FinalizeState getCurrentState(String projectId) {
        SettingsResult res = settingsService.getProjectSetting(projectId, PROJ_FINALIZE_STATE_PROP)
        return res?.value ? FinalizeState.valueOf(res?.value) : FinalizeState.NOT_RUNNING
    }

    private void updateState(String projectId, FinalizeState state) {
        ProjectSettingsRequest startedState = new ProjectSettingsRequest(
                projectId: projectId,
                setting: PROJ_FINALIZE_STATE_PROP,
                value: state.toString()
        )
        settingsService.saveSetting(startedState)
    }
}
