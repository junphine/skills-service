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
package skills.services

import groovy.util.logging.Slf4j
import skills.storage.model.LevelDef

@Slf4j
class LevelUtils {

    static final double SCALE_THRESHOLD = 1.3

    static int defaultTotalPointsGuess = 1000

    public void convertToPoints(List<LevelDef> levelDefs, int totalPoints){
        List<Integer> levelScores = levelDefs.sort({ it.level }).collect {
            return (int) (totalPoints * (it.percent / 100d))
        }
        levelDefs.eachWithIndex{ LevelDef entry, int i ->
            Integer fromPts = levelScores.get(i)
            Integer toPts = (i != levelScores.size() - 1) ? levelScores.get(i + 1) : null
            entry.pointsFrom = fromPts
            entry.pointsTo = toPts
        }
    }

    public void convertToPercentage(List<LevelDef> levelDefs, int totalPoints){
        levelDefs.sort({ it.level })

        Integer highestLevelPoints = levelDefs?.last()?.pointsFrom ? levelDefs.last().pointsFrom : 0

        double scaler = 1.0;

        if(highestLevelPoints > totalPoints){
            //this means that skills were deleted since the levels were converted to points/edited
            //if we convert as-is to percentages, we'll wind up with invalid percentage values
            log.warn("totalPoints [$totalPoints] are lower " +
                    "then the highest level's pointsFrom [${highestLevelPoints}], " +
                    "this would create invalid percentage values. Using [${highestLevelPoints}] as totalPoints for purposes of conversion")
            //since we don't know what the total was before deletion, let's model the percentages off the highest level points being 92%
            //since that's what we do for the default, otherwise the last level would be 100%
            totalPoints = highestLevelPoints*1.08
        } else if (SCALE_THRESHOLD*highestLevelPoints < totalPoints){
            //this will result in an approximation as we don't know for sure the user's original intent
            //but it will at least make more sense then leaving it untouched in this scenario.
            log.info("skills were added after defining levels as points, attempting to scale the current point posture to the total points")
            scaler = totalPoints/highestLevelPoints.toDouble()
        }

        LevelDef lastEntry = null

        int totalLevels = levelDefs.size()

        levelDefs.eachWithIndex { LevelDef entry, int i ->
            if (entry.pointsFrom != null) {
                double scaled = entry.pointsFrom * scaler
                double percentage = ((scaled / totalPoints) * 100d)
                if (percentage < 1.0) {
                    //this can happen if someone adds a skill with a very large range of points after
                    //a conversion to points happens. The first few level points could be such a small percentage
                    //of the total that they would be effectively zero. Lets prevent that and use some sort of sensible default
                    //in those cases
                    entry.percent = 10 + (lastEntry?.percent ? lastEntry.percent : 0)
                } else {
                    entry.percent = Math.round(percentage)
                }
            } else {
                //this could happen if there is an empty subject with no skills
                entry.percent = (((100/totalLevels)*(i+1)))-8
            }
            lastEntry = entry
        }

        levelDefs.each {
            it.pointsFrom = null
            it.pointsTo = null
        }
    }

    /**
     * Fixes any gaps left betwee toEdit and the levels before or after. NOTE that that this is
     * only relevant if levels have been configured to be points-based.
     * @param allLevels
     * @param toEdit
     * @return The level before and after which should be persisted to account for any changes in the pointsFrom or pointsTo
     */
    public List<LevelDef> fixGaps(List<LevelDef> allLevels, LevelDef toEdit){
        int index = allLevels.findIndexOf { it.level == toEdit.level }
        return fixGaps(allLevels, toEdit, index)
    }

    public List<LevelDef> fixGaps(List<LevelDef> allLevels, LevelDef toEdit, int toEditIndex){
        def res = []
        if(allLevels.size() <= 1) {
            return res
        }
        switch(toEditIndex){
            case 0:
                LevelDef fixGap = allLevels.get(toEditIndex+1)
                fixGap.pointsFrom = toEdit.pointsTo
                res.add(fixGap)
                break
            case allLevels.size()-1:
                LevelDef fixGap = allLevels.get(toEditIndex-1)
                fixGap.pointsTo = toEdit.pointsFrom
                res.add(fixGap)
                break
            default:
                LevelDef before = allLevels.get(toEditIndex-1)
                LevelDef after = allLevels.get(toEditIndex+1)
                before.pointsTo = toEdit.pointsFrom
                after.pointsFrom = toEdit.pointsTo
                res.add(before)
                res.add(after)
                break
        }

        return res
    }

}
