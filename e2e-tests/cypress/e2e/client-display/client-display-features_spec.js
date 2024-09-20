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
import moment from 'moment-timezone';

const dateFormatter = value => moment.utc(value)
    .format('YYYY-MM-DD[T]HH:mm:ss[Z]');

describe('Client Display Features Tests', () => {

    beforeEach(() => {
        Cypress.env('disabledUILoginProp', true);
        cy.request('POST', '/app/projects/proj1', {
            projectId: 'proj1',
            name: 'proj1'
        });
        cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            name: 'Subject 1',
            helpUrl: 'http://doHelpOnThisSubject.com',
            description: 'Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.'
        });

    });

    it('display new version banner when software is updated', () => {
        cy.intercept('/api/projects/proj1/subjects/subj1/summary*', (req) => {
            req.reply((res) => {
                res.send(200, {
                    'subject': 'Subject 1',
                    'subjectId': 'subj1',
                    'description': 'Description',
                    'skillsLevel': 0,
                    'totalLevels': 5,
                    'points': 0,
                    'totalPoints': 0,
                    'todaysPoints': 0,
                    'levelPoints': 0,
                    'levelTotalPoints': 0,
                    'skills': [],
                    'iconClass': 'fa fa-question-circle',
                    'helpUrl': 'http://doHelpOnThisSubject.com'
                }, { 'skills-client-lib-version': dateFormatter(new Date()) });
            });
        })
            .as('getSubjectSummary');
        cy.intercept('GET', '/api/projects/proj1/pointHistory')
            .as('pointHistoryChart');

        cy.cdVisit('/');
        cy.injectAxe();
        cy.contains('Overall Points');
        cy.contains('New Skills Software Version is Available')
            .should('not.exist');
        cy.cdClickSubj(0, 'Subject 1');

        cy.wait('@getSubjectSummary');
        cy.wait('@pointHistoryChart');

        cy.contains('New SkillTree Software Version is Available');

        cy.wait(500); //need to wait on the pointHistoryChart to complete rendering before running a11y
        cy.customA11y();

        cy.cdVisit('/');
        cy.contains('New Skills Software Version is Available')
            .should('not.exist');
    });

    it('do not display new version banner if lib version in headers is older than lib version in local storage', () => {
        const mockedLibVersion = dateFormatter(new Date() - 1000 * 60 * 60 * 24 * 5);
        cy.intercept('/api/projects/proj1/subjects/subj1/summary*', (req) => {
            req.reply((res) => {
                res.send(200, {
                    'subject': 'Subject 1',
                    'subjectId': 'subj1',
                    'description': 'Description',
                    'skillsLevel': 0,
                    'totalLevels': 5,
                    'points': 0,
                    'totalPoints': 0,
                    'todaysPoints': 0,
                    'levelPoints': 0,
                    'levelTotalPoints': 0,
                    'skills': [],
                    'iconClass': 'fa fa-question-circle',
                    'helpUrl': 'http://doHelpOnThisSubject.com'
                }, { 'skills-client-lib-version': mockedLibVersion });
            });
        }) .as('getSubjectSummary');

        cy.intercept({
            path: '/api/projects/proj1/subjects/subj1/rank',
        }, {
            statusCode: 200,
            body: {
                'numUsers': 1,
                'position': 1
            },
            headers: {
                'skills-client-lib-version': mockedLibVersion
            },
        })
            .as('getRank');

        cy.intercept({
            url: '/api/projects/proj1/subjects/subj1/pointHistory',
        }, {
            statusCode: 200,
            body: { 'pointsHistory': [] },
            headers: {
                'skills-client-lib-version': mockedLibVersion
            },
        })
            .as('getPointHistory');

        cy.cdVisit('/');
        cy.contains('Overall Points');
        cy.contains('New Skills Software Version is Available')
            .should('not.exist');

        cy.cdClickSubj(0, 'Subject 1');
        cy.wait('@getSubjectSummary');
        cy.wait('@getRank');
        cy.wait('@getPointHistory');

        cy.contains('New Skills Software Version is Available')
            .should('not.exist');
    });

    it('achieve level 5, then add new skill', () => {
        cy.request('POST', `/admin/projects/proj1/subjects/subj1/skills/skill1`, {
            projectId: 'proj1',
            subjectId: 'subj1',
            skillId: 'skill1',
            name: `This is 1`,
            type: 'Skill',
            pointIncrement: 50,
            numPerformToCompletion: 2,
            pointIncrementInterval: 0,
            numMaxOccurrencesIncrementInterval: -1,
            description: 'Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.',
            version: 0,
            helpUrl: 'http://doHelpOnThisSkill.com'
        });

        cy.request('POST', `/api/projects/proj1/skills/skill1`, {
            userId: Cypress.env('proxyUser'),
            timestamp: new Date().getTime()
        });
        cy.request('POST', `/api/projects/proj1/skills/skill1`, {
            userId: Cypress.env('proxyUser'),
            timestamp: new Date().getTime() - 1000 * 60 * 60 * 24
        });
        cy.intercept('GET', '/api/projects/proj1/pointHistory')
            .as('pointHistoryChart');

        cy.cdVisit('/');
        cy.injectAxe();

        cy.contains('Overall Points');

        cy.get('[data-cy=subjectTile]')
            .eq(0)
            .contains('Subject 1');
        cy.get('[data-cy=subjectTile]')
            .eq(0)
            .contains('Level 5');

        cy.request('POST', `/admin/projects/proj1/subjects/subj1/skills/skill2`, {
            projectId: 'proj1',
            subjectId: 'subj1',
            skillId: 'skill2',
            name: `This is 2`,
            type: 'Skill',
            pointIncrement: 50,
            numPerformToCompletion: 2,
            pointIncrementInterval: 0,
            numMaxOccurrencesIncrementInterval: -1,
            description: 'Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.',
            version: 0,
            helpUrl: 'http://doHelpOnThisSkill.com'
        });

        cy.wait('@pointHistoryChart');
        cy.customA11y();
        cy.cdVisit('/');

        cy.contains('Overall Points');

        cy.get('[data-cy=subjectTile]')
            .eq(0)
            .contains('Subject 1');
        cy.get('[data-cy=subjectTile]')
            .eq(0)
            .contains('Level 5');
    });

    it('custom icon for badge must display after a refresh', () => {
        cy.uploadCustomIcon('valid_icon.png', '/admin/projects/proj1/icons/upload');

        cy.createBadge(1, 1);
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1);
        cy.assignSkillToBadge(1, 1, 1);
        cy.enableBadge(1, 1, { iconClass: 'proj1-validiconpng' });

        cy.cdVisit('/');
        cy.cdClickBadges();
        cy.get('[data-cy="badge_badge1"] .proj1-validiconpng');

        cy.cdVisit('/badges');
        cy.get('[data-cy="badge_badge1"] .proj1-validiconpng');
    });

    it('ability to enable page visits reporting to the backend', () => {
        cy.intercept('GET', '/public/clientDisplay/config?projectId=proj1', (req) => {
            req.reply({
                body: {
                    enablePageVisitReporting: 'true',
                },
            });
        })
            .as('getConfig');
        cy.intercept('PUT', '/api/pageVisit', (req) => {
            expect(req.body.path)
                .to
                .include('/');
            req.reply({
                body: {
                    'success': true,
                    'explanation': null
                },
            });
        })
            .as('pageVisit');

        cy.cdVisit('/');
        cy.wait('@getConfig');
        cy.get('[data-cy="subjectTile"]')
            .contains('Subject 1');
        cy.wait('@pageVisit');
    });

    it('by default page visits reporting to the backend must not happen', () => {
        cy.intercept('GET', '/public/clientDisplay/config?projectId=proj1')
            .as('getConfig');
        cy.intercept('PUT', '/api/pageVisit', cy.spy()
            .as('pageVisit'));
        cy.cdVisit('/rank');
        cy.wait('@getConfig');
        cy.wait(5000);
        cy.get('@pageVisit')
            .should('not.have.been.called');
    });

});
