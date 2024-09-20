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
var moment = require('moment-timezone');

describe('Metrics Tests - Skills', () => {

    const waitForSnap = 4000;

    before(() => {
        Cypress.Commands.add('addUserTag', (userId, tagKey, tags) => {
            cy.request('POST', `/root/users/${userId}/tags/${tagKey}`, { tags });
        });
    })

    beforeEach(() => {
        cy.request('POST', '/app/projects/proj1', {
            projectId: 'proj1',
            name: 'proj1'
        });

        cy.intercept('GET', '/public/config', (req) => {
            req.reply({
                body: {
                    projectMetricsTagCharts: '[{"key":"tagA","type":"table","title":"Tag A","tagLabel":"Tag A"}]'
                },
            });
        })
            .as('getConfig');
    });

    it('stat cards with zero activity', () => {
        cy.intercept('/admin/projects/proj1/metrics/singleSkillCountsChartBuilder?skillId=skill1')
            .as('singleSkillCountsChartBuilder');

        cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            name: 'Interesting Subject 1',
        });

        const numSkills = 2;
        for (let skillsCounter = 1; skillsCounter <= numSkills; skillsCounter += 1) {
            cy.request('POST', `/admin/projects/proj1/subjects/subj1/skills/skill${skillsCounter}`, {
                projectId: 'proj1',
                subjectId: 'subj1',
                skillId: `skill${skillsCounter}`,
                name: `Very Great Skill # ${skillsCounter}`,
                pointIncrement: '1000',
                numPerformToCompletion: '2',
            });
        }

        cy.visit('/administrator/projects/proj1/subjects/subj1/skills/skill1');
        cy.clickNav('Metrics');
        cy.wait('@singleSkillCountsChartBuilder');

        cy.get('[data-cy=numUserAchievedStatCard] [data-cy=statCardValue]')
            .contains('0');
        cy.get('[data-cy=inProgressStatCard] [data-cy=statCardValue]')
            .contains('0');
        cy.get('[data-cy=lastAchievedStatCard] [data-cy=statCardValue]')
            .contains('Never');
    });

    it('stat cards have data', () => {
        cy.intercept('/admin/projects/proj1/metrics/singleSkillCountsChartBuilder**')
            .as('singleSkillCountsChartBuilder');

        cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            name: 'Interesting Subject 1',
        });

        const numSkills = 2;
        for (let skillsCounter = 1; skillsCounter <= numSkills; skillsCounter += 1) {
            cy.request('POST', `/admin/projects/proj1/subjects/subj1/skills/skill${skillsCounter}`, {
                projectId: 'proj1',
                subjectId: 'subj1',
                skillId: `skill${skillsCounter}`,
                name: `Very Great Skill # ${skillsCounter}`,
                pointIncrement: '1000',
                numPerformToCompletion: '2',
            });
        }
        ;

        const m = moment.utc()
            .subtract(2, 'months');
        const numUsers = 5;
        for (let userCounter = 1; userCounter <= numUsers; userCounter += 1) {
            cy.request('POST', `/api/projects/proj1/skills/skill1`,
                {
                    userId: `user${userCounter}achieved@skills.org`,
                    timestamp: m.clone()
                        .subtract(0, 'day')
                        .format('x')
                });
            cy.request('POST', `/api/projects/proj1/skills/skill1`,
                {
                    userId: `user${userCounter}achieved@skills.org`,
                    timestamp: m.clone()
                        .subtract(1, 'day')
                        .format('x')
                });
        }

        const numUsersInProgress = 3;
        for (let userCounter = 1; userCounter <= numUsersInProgress; userCounter += 1) {
            cy.request('POST', `/api/projects/proj1/skills/skill1`,
                {
                    userId: `user${userCounter}progress@skills.org`,
                    timestamp: m.clone()
                        .subtract(0, 'day')
                        .format('x')
                });
        }

        cy.visit('/administrator/projects/proj1/subjects/subj1/skills/skill1');
        cy.clickNav('Metrics');
        cy.wait('@singleSkillCountsChartBuilder');

        cy.get('[data-cy=numUserAchievedStatCard] [data-cy=statCardValue]')
            .contains('5');
        cy.get('[data-cy=inProgressStatCard] [data-cy=statCardValue]')
            .contains('3');
        cy.get('[data-cy=lastAchievedStatCard] [data-cy=statCardValue]')
            .contains('2 months ago');
    });

    it('stat cards with large counts', () => {
        const m = moment.utc()
            .subtract(5, 'years');
        const timestamp = m.format('x');
        cy.log(timestamp);
        cy.intercept('/admin/projects/proj1/metrics/singleSkillCountsChartBuilder**',
            {
                statusCode: 200,
                body: {
                    'numUsersAchieved': 3828283,
                    'lastAchieved': parseInt(timestamp),
                    'numUsersInProgress': 5817714
                },
            })
            .as('singleSkillCountsChartBuilder');

        cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            name: 'Interesting Subject 1',
        });

        const numSkills = 1;
        for (let skillsCounter = 1; skillsCounter <= numSkills; skillsCounter += 1) {
            cy.request('POST', `/admin/projects/proj1/subjects/subj1/skills/skill${skillsCounter}`, {
                projectId: 'proj1',
                subjectId: 'subj1',
                skillId: `skill${skillsCounter}`,
                name: `Very Great Skill # ${skillsCounter}`,
                pointIncrement: '1000',
                numPerformToCompletion: '2',
            });
        }

        cy.visit('/administrator/projects/proj1/subjects/subj1/skills/skill1');
        cy.clickNav('Metrics');
        cy.wait('@singleSkillCountsChartBuilder');

        cy.get('[data-cy=numUserAchievedStatCard] [data-cy=statCardValue]')
            .contains('3,828,283');
        cy.get('[data-cy=numUserAchievedStatCard] [data-cy=statCardDescription]')
            .contains('Number of users that achieved this skill ');

        cy.get('[data-cy=inProgressStatCard] [data-cy=statCardValue]')
            .contains('5,817,714');
        cy.get('[data-cy=inProgressStatCard] [data-cy=statCardDescription]')
            .contains('Number of Users with some points earned toward the skill');

        cy.get('[data-cy=lastAchievedStatCard] [data-cy=statCardValue]')
            .contains('5 years ago');
        cy.get('[data-cy=lastAchievedStatCard] [data-cy=statCardDescription]')
            .contains(`This skill was last achieved on ${m.format('YYYY-MM-DD HH:mm')}`);
    });

    it('number of users over time', () => {
        cy
            .intercept('/admin/projects/proj1/metrics/numUserAchievedOverTimeChartBuilder**')
            .as('singleSkillCountsChartBuilder');

        cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            name: 'Interesting Subject 1',
        });

        const numSkills = 1;
        for (let skillsCounter = 1; skillsCounter <= numSkills; skillsCounter += 1) {
            cy.request('POST', `/admin/projects/proj1/subjects/subj1/skills/skill${skillsCounter}`, {
                projectId: 'proj1',
                subjectId: 'subj1',
                skillId: `skill${skillsCounter}`,
                name: `Very Great Skill # ${skillsCounter}`,
                pointIncrement: '1000',
                numPerformToCompletion: '1',
            });
        }
        ;

        const m = moment.utc('2020-09-02 11', 'YYYY-MM-DD HH');
        const numDays = 6;
        for (let dayCounter = 1; dayCounter <= numDays; dayCounter += 1) {
            for (let userCounter = 1; userCounter <= dayCounter; userCounter += 1) {
                cy.request('POST', `/api/projects/proj1/skills/skill1`,
                    {
                        userId: `user${dayCounter}-${userCounter}achieved@skills.org`,
                        timestamp: m.clone()
                            .add(dayCounter, 'day')
                            .format('x')
                    });
            }
        }

        cy.visit('/administrator/projects/proj1/subjects/subj1/skills/skill1');
        cy.clickNav('Metrics');
        cy.wait('@singleSkillCountsChartBuilder');

        cy.wait(waitForSnap);
        cy.matchSnapshotImageForElement('[data-cy=numUsersAchievedOverTimeMetric]');
    });

    it('skill metrics - empty', () => {
        cy
            .intercept('/admin/projects/proj1/metrics/numUserAchievedOverTimeChartBuilder?skillId=skill1')
            .as('singleSkillCountsChartBuilder');

        cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            name: 'Interesting Subject 1',
        });

        const numSkills = 1;
        for (let skillsCounter = 1; skillsCounter <= numSkills; skillsCounter += 1) {
            cy.request('POST', `/admin/projects/proj1/subjects/subj1/skills/skill${skillsCounter}`, {
                projectId: 'proj1',
                subjectId: 'subj1',
                skillId: `skill${skillsCounter}`,
                name: `Very Great Skill # ${skillsCounter}`,
                pointIncrement: '1000',
                numPerformToCompletion: '1',
            });
        }

        cy.visit('/administrator/projects/proj1/subjects/subj1/skills/skill1');
        cy.clickNav('Metrics');
        cy.wait('@singleSkillCountsChartBuilder');

        cy.get('[data-cy=numUsersAchievedOverTimeMetric]')
            .contains('No achievements yet for this skill');

        cy.get('[data-cy=appliedSkillEventsOverTimeMetric]')
            .contains('This chart needs at least 2 days of user activity');
    });

    it('number of users over time - 1 day', () => {
        cy
            .intercept('/admin/projects/proj1/metrics/numUserAchievedOverTimeChartBuilder?skillId=skill1',
                {
                    body: {
                        'achievementCounts': [{
                            'num': 1,
                            'timestamp': 1599130800000
                        }]
                    },
                })
            .as('singleSkillCountsChartBuilder');

        cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            name: 'Interesting Subject 1',
        });

        const numSkills = 1;
        for (let skillsCounter = 1; skillsCounter <= numSkills; skillsCounter += 1) {
            cy.request('POST', `/admin/projects/proj1/subjects/subj1/skills/skill${skillsCounter}`, {
                projectId: 'proj1',
                subjectId: 'subj1',
                skillId: `skill${skillsCounter}`,
                name: `Very Great Skill # ${skillsCounter}`,
                pointIncrement: '1000',
                numPerformToCompletion: '1',
            });
        }

        cy.visit('/administrator/projects/proj1/subjects/subj1/skills/skill1');
        cy.clickNav('Metrics');
        cy.wait('@singleSkillCountsChartBuilder');

        cy.wait(waitForSnap);
        cy.matchSnapshotImageForElement('[data-cy=numUsersAchievedOverTimeMetric]');
    });

    it('applied skill events over time', () => {
        cy.intercept('/admin/projects/proj1/metrics/numUserAchievedOverTimeChartBuilder**')
            .as('skillEventsOverTimeChartBuilder');

        cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            name: 'Interesting Subject 1',
        });

        cy.createSkill(1, 1, 1);
        cy.reportSkill(1, 1, 'user5Good@skills.org', '2020-09-13 11:00');
        cy.reportSkill(1, 1, 'user6Good@skills.org', '2020-09-13 11:00');
        cy.reportSkill(1, 1, 'user7Good@skills.org', '2020-09-13 11:00');

        const m = moment.utc('2020-09-02 11', 'YYYY-MM-DD HH');
        const numDays = 3;
        for (let dayCounter = 1; dayCounter <= numDays; dayCounter += 1) {
            const numUsers = (dayCounter % 2 == 0) ? 2 : 4;
            for (let userCounter = 1; userCounter <= numUsers; userCounter += 1) {
                cy.request('POST', `/api/projects/proj1/skills/skill1`,
                    {
                        userId: `user${dayCounter}-${userCounter}achieved@skills.org`,
                        timestamp: m.clone()
                            .add(dayCounter, 'day')
                            .format('x')
                    });
            }
        }

        cy.visit('/administrator/projects/proj1/subjects/subj1/skills/skill1');
        cy.clickNav('Metrics');
        cy.wait('@skillEventsOverTimeChartBuilder');

        cy.wait(waitForSnap);
        cy.matchSnapshotImageForElement('[data-cy=appliedSkillEventsOverTimeMetric]');
    });

    it('applied skill events over time - 1 skill', () => {
        cy
            .intercept('/admin/projects/proj1/metrics/skillEventsOverTimeChartBuilder**')
            .as('skillEventsOverTimeChartBuilder');

        cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            name: 'Interesting Subject 1',
        });

        const numSkills = 1;
        for (let skillsCounter = 1; skillsCounter <= numSkills; skillsCounter += 1) {
            cy.request('POST', `/admin/projects/proj1/subjects/subj1/skills/skill${skillsCounter}`, {
                projectId: 'proj1',
                subjectId: 'subj1',
                skillId: `skill${skillsCounter}`,
                name: `Very Great Skill # ${skillsCounter}`,
                pointIncrement: '1000',
                numPerformToCompletion: '1',
            });
        }

        const m = moment.utc('2020-09-02 11', 'YYYY-MM-DD HH');
        const numDays = 1;
        for (let dayCounter = 1; dayCounter <= numDays; dayCounter += 1) {
            const numUsers = (dayCounter % 2 == 0) ? 2 : 4;
            for (let userCounter = 1; userCounter <= numUsers; userCounter += 1) {
                cy.request('POST', `/api/projects/proj1/skills/skill1`,
                    {
                        userId: `user${dayCounter}-${userCounter}achieved@skills.org`,
                        timestamp: m.clone()
                            .add(dayCounter, 'day')
                            .format('x')
                    });
            }
        }

        cy.visit('/administrator/projects/proj1/subjects/subj1/skills/skill1');
        cy.clickNav('Metrics');
        cy.wait('@skillEventsOverTimeChartBuilder');

        cy.get('[data-cy=appliedSkillEventsOverTimeMetric]')
            .contains('This chart needs at least 2 days of user activity');
    });

    it('post achievement skill usage metrics', () => {
        cy
            .intercept('/admin/projects/proj1/metrics/binnedUsagePostAchievementMetricsBuilder**')
            .as('binnedUsagePostAchievementMetricsBuilder');

        cy
            .intercept('/admin/projects/proj1/metrics/usagePostAchievementMetricsBuilder**')
            .as('usagePostAchievementMetricsBuilder');

        cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            name: 'Interesting Subject 1',
        });

        const numSkills = 1;
        for (let skillsCounter = 1; skillsCounter <= numSkills; skillsCounter += 1) {
            cy.request('POST', `/admin/projects/proj1/subjects/subj1/skills/skill${skillsCounter}`, {
                projectId: 'proj1',
                subjectId: 'subj1',
                skillId: `skill${skillsCounter}`,
                name: `Very Great Skill # ${skillsCounter}`,
                pointIncrement: '1000',
                numPerformToCompletion: '1',
            });
        }

        const m = moment.utc('2020-09-02 11', 'YYYY-MM-DD HH');
        const numDays = 20;
        const users = ['user1', 'user2', 'user3', 'user4', 'user5'];
        for (let dayCounter = 1; dayCounter <= numDays; dayCounter += 1) {
            let numUsers = (numDays + 1 - dayCounter) / 4;
            numUsers = numUsers == 0 ? 1 : numUsers;
            for (let userCounter = 0; userCounter <= numUsers; userCounter += 1) {
                console.log(`adding skillEvent for user ${users[userCounter]} for day ${m.clone()
                    .add(dayCounter, 'day')}`);
                cy.request('POST', `/api/projects/proj1/skills/skill1`,
                    {
                        userId: `${users[userCounter]}_achieved@skills.org`,
                        timestamp: m.clone()
                            .add(dayCounter, 'day')
                            .format('x')
                    });
            }
        }

        cy.visit('/administrator/projects/proj1/subjects/subj1/skills/skill1');
        cy.clickNav('Metrics');
        cy.wait('@binnedUsagePostAchievementMetricsBuilder');
        cy.wait('@usagePostAchievementMetricsBuilder');

        cy.wait(8000);
        cy.matchSnapshotImageForElement('[data-cy=numUsersPostAchievement]', 'numUsersPostAchievement');
        cy.matchSnapshotImageForElement('[data-cy=binnedNumUsersPostAchievement]', 'binnedNumUsersPostAchievement');
    });

    it('post achievement user table has data', () => {
        cy
            .intercept('/admin/projects/proj1/metrics/usagePostAchievementUsersBuilder**')
            .as('usagePostAchievementUsersBuilder');

        cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            name: 'Interesting Subject 1',
        });

        const numSkills = 1;
        for (let skillsCounter = 1; skillsCounter <= numSkills; skillsCounter += 1) {
            cy.request('POST', `/admin/projects/proj1/subjects/subj1/skills/skill${skillsCounter}`, {
                projectId: 'proj1',
                subjectId: 'subj1',
                skillId: `skill${skillsCounter}`,
                name: `Very Great Skill # ${skillsCounter}`,
                pointIncrement: '1000',
                numPerformToCompletion: '1',
            });
        }

        const m = moment.utc('2020-09-02 11', 'YYYY-MM-DD HH');
        const numDays = 20;
        const users = ['user1', 'user2', 'user3', 'user4', 'user5', 'user6', 'user7', 'user8'];
        for (let dayCounter = 1; dayCounter <= numDays; dayCounter += 1) {
            let numUsers = users.length;
            for (let userCounter = 0; userCounter < numUsers; userCounter += 1) {
                console.log(`adding skillEvent for user ${users[userCounter]} for day ${m.clone()
                    .add(dayCounter, 'day')}`);
                cy.request('POST', `/api/projects/proj1/skills/skill1`,
                    {
                        userId: `${users[userCounter]}_achieved@skills.org`,
                        timestamp: m.clone()
                            .add(dayCounter, 'day')
                            .format('x')
                    });
            }
        }

        cy.visit('/administrator/projects/proj1/subjects/subj1/skills/skill1');
        cy.clickNav('Metrics');
        cy.wait('@usagePostAchievementUsersBuilder');

        cy.validateTable('[data-cy=postAchievementUserList]', [
            [
                {
                    colIndex: 0,
                    value: 'user8'
                },
            ],
            [
                {
                    colIndex: 0,
                    value: 'user7'
                },
            ],
            [
                {
                    colIndex: 0,
                    value: 'user6'
                },
            ],
            [
                {
                    colIndex: 0,
                    value: 'user5'
                },
            ],
            [
                {
                    colIndex: 0,
                    value: 'user4'
                },
            ],
            [
                {
                    colIndex: 0,
                    value: 'user3'
                },
            ],
            [
                {
                    colIndex: 0,
                    value: 'user2'
                },
            ],
            [
                {
                    colIndex: 0,
                    value: 'user1'
                },
            ],
        ]);

    });

    it('post achievement user table has no data', () => {
        cy.intercept('/admin/projects/proj1/metrics/usagePostAchievementUsersBuilder**')
            .as('usagePostAchievementUsersBuilder');

        cy.intercept('/admin/projects/proj1/metrics/noUsagePostAchievementUsersBuilder**')
            .as('noUsagePostAchievementUsersBuilder');

        cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            name: 'Interesting Subject 1',
        });

        const numSkills = 1;
        for (let skillsCounter = 1; skillsCounter <= numSkills; skillsCounter += 1) {
            cy.request('POST', `/admin/projects/proj1/subjects/subj1/skills/skill${skillsCounter}`, {
                projectId: 'proj1',
                subjectId: 'subj1',
                skillId: `skill${skillsCounter}`,
                name: `Very Great Skill # ${skillsCounter}`,
                pointIncrement: '1000',
                numPerformToCompletion: '1',
            });
        }

        const m = moment.utc('2020-09-02 11', 'YYYY-MM-DD HH');
        const numDays = 1;
        const users = ['user1', 'user2', 'user3', 'user4', 'user5', 'user6', 'user7', 'user8'];
        for (let dayCounter = 1; dayCounter <= numDays; dayCounter += 1) {
            let numUsers = users.length;
            for (let userCounter = 0; userCounter < numUsers; userCounter += 1) {
                console.log(`adding skillEvent for user ${users[userCounter]} for day ${m.clone()
                    .add(dayCounter, 'day')}`);
                cy.request('POST', `/api/projects/proj1/skills/skill1`,
                    {
                        userId: `${users[userCounter]}_achieved@skills.org`,
                        timestamp: m.clone()
                            .add(dayCounter, 'day')
                            .format('x')
                    });
            }
        }

        cy.visit('/administrator/projects/proj1/subjects/subj1/skills/skill1');
        cy.clickNav('Metrics');

        cy.get('[data-cy=modeSelector]').contains('Stopped').click()
        cy.wait('@noUsagePostAchievementUsersBuilder');

        cy.validateTable('[data-cy=postAchievementUserList]', [
            [
                {
                    colIndex: 0,
                    value: 'user8'
                },
            ],
            [
                {
                    colIndex: 0,
                    value: 'user7'
                },
            ],
            [
                {
                    colIndex: 0,
                    value: 'user6'
                },
            ],
            [
                {
                    colIndex: 0,
                    value: 'user5'
                },
            ],
            [
                {
                    colIndex: 0,
                    value: 'user4'
                },
            ],
            [
                {
                    colIndex: 0,
                    value: 'user3'
                },
            ],
            [
                {
                    colIndex: 0,
                    value: 'user2'
                },
            ],
            [
                {
                    colIndex: 0,
                    value: 'user1'
                },
            ],
        ]);

    });

    if (!Cypress.env('oauthMode')) {
        it('number of users by tag', () => {
            cy
              .intercept('/admin/projects/proj1/metrics/skillAchievementsByTagBuilder**')
              .as('skillAchievementsByTagBuilder');

            cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
                projectId: 'proj1',
                subjectId: 'subj1',
                name: 'Interesting Subject 1',
            });

            const numSkills = 1;
            for (let skillsCounter = 1; skillsCounter <= numSkills; skillsCounter += 1) {
                cy.request('POST', `/admin/projects/proj1/subjects/subj1/skills/skill${skillsCounter}`, {
                    projectId: 'proj1',
                    subjectId: 'subj1',
                    skillId: `skill${skillsCounter}`,
                    name: `Very Great Skill # ${skillsCounter}`,
                    pointIncrement: '1000',
                    numPerformToCompletion: '2',
                });
            }
            ;

            const m = moment.utc('2020-09-02 11', 'YYYY-MM-DD HH');
            const numDays = 6;

            cy.fixture('vars.json')
              .then((vars) => {
                  cy.login(vars.rootUser, vars.defaultPass);
              });

            // Add users who achieved the skill
            for (let dayCounter = 1; dayCounter <= numDays; dayCounter += 1) {
                for (let userCounter = 1; userCounter <= dayCounter; userCounter += 1) {
                    const userId = `user-${userCounter}achieved@skills.org`;

                    cy.request('POST', `/api/projects/proj1/skills/skill1`,
                      {
                          userId: userId,
                          timestamp: m.clone()
                            .add(dayCounter, 'day')
                            .format('x')
                      });

                    cy.addUserTag(userId, 'tagA', ['ABCDE', 'DEFGH']);
                }
            }

            // Add users in progress
            for (let userCounter = numDays; userCounter <= 10; userCounter += 1) {
                const userId = `user-${userCounter}achieved@skills.org`;

                cy.request('POST', `/api/projects/proj1/skills/skill1`,
                  {
                      userId: userId,
                      timestamp: m.clone()
                        .add(1, 'day')
                        .format('x')
                  });

                cy.addUserTag(userId, 'tagA', ['ABCDE', 'DEFGH']);
            }

            cy.logout();
            cy.fixture('vars.json')
              .then((vars) => {
                  cy.login(vars.defaultUser, vars.defaultPass);
              });

            cy.visit('/administrator/projects/proj1/subjects/subj1/skills/skill1');
            cy.wait('@getConfig');
            cy.clickNav('Metrics');
            cy.wait('@skillAchievementsByTagBuilder');

            cy.wait(waitForSnap);
            cy.matchSnapshotImageForElement('[data-cy=numUsersByTag]');
        });
    }
});
