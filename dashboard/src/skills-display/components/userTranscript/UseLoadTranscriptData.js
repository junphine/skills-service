/*
 * Copyright 2024 SkillTree
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
import { useSkillsDisplayService } from '@/skills-display/services/UseSkillsDisplayService.js'
import { ref } from 'vue'
import { useAuthState } from '@/stores/UseAuthState.js'
import { useSkillsDisplayAttributesState } from '@/skills-display/stores/UseSkillsDisplayAttributesState.js'

export const useLoadTranscriptData = () => {
  const skillsDisplayService = useSkillsDisplayService()
  const userAuthState = useAuthState()
  const skillsDisplayAttributesState = useSkillsDisplayAttributesState()
  const isLoading = ref(false)
  const loadTranscriptData = () => {
    isLoading.value = true

    return skillsDisplayService.loadUserProjectSummary()
      .then((projRes) => {
        const getSubjectPromises = projRes.subjects.map((subjRes) => skillsDisplayService.loadSubjectSummary(subjRes.subjectId, true))
        getSubjectPromises.push(skillsDisplayService.getBadgeSummaries())
        return Promise.all(getSubjectPromises).then((endpointsRes) => {
          const transcriptInfo = {}
          transcriptInfo.labelsConf = buildLabelConf()
          transcriptInfo.userName = `${userAuthState.userInfo.nickname} (${userAuthState.userInfo.userIdForDisplay})`
          transcriptInfo.projectName = projRes.projectName
          transcriptInfo.userLevel = projRes.skillsLevel
          transcriptInfo.totalLevels = projRes.totalLevels
          transcriptInfo.userPoints = projRes.points
          transcriptInfo.totalPoints = projRes.totalPoints

          const subjRes = endpointsRes.filter((r) => r && r.subjectId && r.subjectId.length > 0)

          transcriptInfo.subjects = subjRes.map((subjRes) => {
            return {
              name: subjRes.subject,
              userLevel: subjRes.skillsLevel,
              totalLevels: subjRes.totalLevels,
              userPoints: subjRes.points,
              totalPoints: subjRes.totalPoints,
              userSkillsCompleted: subjRes.skills ? subjRes.skills.filter((s) => s.points === s.totalPoints).length : 0,
              totalSkills: subjRes.skills ? subjRes.skills.length : 0,
              skills: subjRes.skills?.map((skillRes) => {
                return {
                  name: skillRes.skill,
                  userPoints: skillRes.points,
                  totalPoints: skillRes.totalPoints
                }
              })
            }
          })

          transcriptInfo.userSkillsCompleted = transcriptInfo.subjects.map((subj) => subj.userSkillsCompleted).reduce((a, b) => a + b, 0)
          transcriptInfo.totalSkills = transcriptInfo.subjects.map((subj) => subj.totalSkills).reduce((a, b) => a + b, 0)

          const badgeRes = endpointsRes.find((r) => r && (r instanceof Array) && r.length > 0 && r[0].badgeId && r[0].badgeId.length > 0)
          transcriptInfo.achievedBadges = badgeRes?.filter((b) => b.badgeAchieved)?.map((badgeRes) => {
            return {
              name: badgeRes.badge,
              dateAchieved: badgeRes.dateAchieved
            }
          })

          return transcriptInfo
        })



      })
      .finally(() => {
        isLoading.value = false
      })
  }

  const buildLabelConf = () => {
    return {
      subject: skillsDisplayAttributesState.subjectDisplayName,
      skill: skillsDisplayAttributesState.skillDisplayName,
      level: skillsDisplayAttributesState.levelDisplayName,
      badge: 'Badge'
    }
  }

  return {
    isLoading,
    loadTranscriptData
  }
}