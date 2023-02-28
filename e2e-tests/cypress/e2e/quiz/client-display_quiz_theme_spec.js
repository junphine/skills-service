/*
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
import dayjs from 'dayjs';
import relativeTimePlugin from 'dayjs/plugin/relativeTime';
import advancedFormatPlugin from 'dayjs/plugin/advancedFormat';

dayjs.extend(relativeTimePlugin);
dayjs.extend(advancedFormatPlugin);

describe('Client Display Quiz Theme Tests', () => {

    beforeEach(() => {

    });

    it('quiz theme', () => {
        cy.createQuizDef(1, { name: 'Trivia Knowledge' });
        cy.createQuizQuestionDef(1, 1);
        cy.createQuizQuestionDef(1, 2, { questionType: 'SingleChoice' });
        cy.createQuizQuestionDef(1, 3);

        cy.createProject(1)
        cy.createSubject(1,1)
        cy.createSkill(1, 1, 1, { selfReportingType: 'Quiz', quizId: 'quiz1',  pointIncrement: '150', numPerformToCompletion: 1 });

        cy.cdVisit('/subjects/subj1/skills/skill1/quizzes/quiz1?enableTheme=true');
        cy.get('[data-cy="quizSplashScreen"]').contains('You will earn 150 points for Very Great Skill 1');
        cy.matchSnapshotImage(`quiz-splash-screen`);
    });
});


