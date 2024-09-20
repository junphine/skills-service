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

describe('Community Project Creation Tests', () => {

    const allDragonsUser = 'allDragons@email.org'

    beforeEach(() => {
        cy.fixture('vars.json').then((vars) => {
            cy.logout();
            cy.login(vars.rootUser, vars.defaultPass, true);
            cy.request('POST', `/root/users/${vars.rootUser}/tags/dragons`, { tags: ['DivineDragon'] });
            cy.request('POST', `/root/users/${vars.defaultUser}/tags/dragons`, { tags: ['DivineDragon'] });
            cy.logout();

            cy.register(allDragonsUser, vars.defaultPass);
            cy.logout();

            cy.login(vars.defaultUser, vars.defaultPass);
        });
    });

    it('project description is validated against custom validators', () => {
        cy.createProject(1, {enableProtectedUserCommunity: true})
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1)

        cy.intercept('GET', '/admin/projects/proj1/subjects').as('loadSubjects');
        cy.intercept('GET', '/app/projects/proj1/description').as('loadDescription');
        cy.intercept('POST', '/api/validation/description*').as('validateDescription');

        cy.visit('/administrator/projects/proj1');
        cy.wait('@loadSubjects');

        cy.get('[data-cy="btn_edit-project"]').click();
        cy.wait('@loadDescription');

        cy.get('[data-cy="markdownEditorInput"]').type('ldkj aljdl aj\n\njabberwocky');
        cy.wait('@validateDescription');
        cy.get('[data-cy="projectDescriptionError"]').should('not.be.visible')
        cy.get('[data-cy="saveProjectButton"]').should('be.enabled')

        cy.get('[data-cy="markdownEditorInput"]').type('{selectall}{backspace}')
        cy.get('[data-cy="markdownEditorInput"]').type('ldkj aljdl aj\n\ndivinedragon');
        cy.wait('@validateDescription');
        cy.get('[data-cy="projectDescriptionError"]').contains('Project Description - May not contain divinedragon word.');
        cy.get('[data-cy="saveProjectButton"]').should('be.disabled');

        cy.get('[data-cy="markdownEditorInput"]').type('{backspace}');
        cy.wait('@validateDescription');
        cy.get('[data-cy="saveProjectButton"]').should('be.enabled');
    });

    it('subject description is validated against custom validators', () => {
        cy.createProject(1, {enableProtectedUserCommunity: true})
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1)

        cy.intercept('GET', '/admin/projects/proj1/subjects').as('loadSubjects');
        cy.intercept('POST', '/api/validation/description*').as('validateDescription');

        cy.visit('/administrator/projects/proj1');
        cy.wait('@loadSubjects');

        cy.clickButton('Subject');

        cy.get('[data-cy="subjectNameInput"]').type('Great Name');
        cy.get('[data-cy="saveSubjectButton"]').should('be.enabled');

        cy.get('[data-cy="markdownEditorInput"]').type('ldkj aljdl aj\n\njabberwocky');
        cy.get('[data-cy="subjectDescError"]').should('not.be.visible')
        cy.get('[data-cy="saveSubjectButton"]').should('be.enabled')

        cy.get('[data-cy="markdownEditorInput"]').type('{selectall}{backspace}')
        cy.get('[data-cy="markdownEditorInput"]').type('ldkj aljdl aj\n\ndivinedragon');
        cy.get('[data-cy="subjectDescError"]').contains('Subject Description - May not contain divinedragon word.');
        cy.get('[data-cy="saveSubjectButton"]').should('be.disabled');

        cy.get('[data-cy="markdownEditorInput"]').type('{backspace}');
        cy.get('[data-cy="saveSubjectButton"]').should('be.enabled');
    });

    it('skill description is validated against custom validators', () => {
        cy.createProject(1, {enableProtectedUserCommunity: true})
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1)

        cy.intercept('GET', '/admin/projects/proj1/subjects/subj1').as('loadSubject');
        cy.intercept('POST', '/api/validation/description*').as('validateDescription');

        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.wait('@loadSubject');
        cy.get('[data-cy="newSkillButton"]').click();

        cy.get('[data-cy="skillName"]').type('Great Name');
        cy.get('[data-cy="saveSkillButton"]').should('be.enabled');

        cy.get('[data-cy="markdownEditorInput"]').type('ldkj aljdl aj\n\njabberwocky');
        cy.get('[data-cy="skillDescriptionError"]').should('not.be.visible')
        cy.get('[data-cy="saveSkillButton"]').should('be.enabled')

        cy.get('[data-cy="markdownEditorInput"]').type('{selectall}{backspace}')
        cy.get('[data-cy="markdownEditorInput"]').type('ldkj aljdl aj\n\ndivinedragon');
        cy.get('[data-cy="skillDescriptionError"]').contains('Skill Description - May not contain divinedragon word.');
        cy.get('[data-cy="saveSkillButton"]').should('be.disabled');

        cy.get('[data-cy="markdownEditorInput"]').type('{backspace}');
        cy.get('[data-cy="saveSkillButton"]').should('be.enabled');
    });

    it('badge description is validated against custom validators', () => {
        cy.createProject(1, {enableProtectedUserCommunity: true})
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1)

        cy.intercept('GET', '/admin/projects/proj1/badges').as('loadBadges');
        cy.intercept('GET', '/admin/projects/proj1/subjects/subj1').as('loadSubject');
        cy.intercept('POST', '/api/validation/description*').as('validateDescription');

        cy.visit('/administrator/projects/proj1/badges');
        cy.wait('@loadBadges');
        cy.clickButton('Badge');

        cy.get('[data-cy=badgeName]').type('Great Name');
        cy.get('[data-cy="saveBadgeButton"]').should('be.enabled');

        cy.get('[data-cy="markdownEditorInput"]').type('ldkj aljdl aj\n\njabberwocky');
        cy.get('[data-cy="badgeDescriptionError"]').should('not.be.visible')
        cy.get('[data-cy="saveBadgeButton"]').should('be.enabled')

        cy.get('[data-cy="markdownEditorInput"]').type('{selectall}{backspace}')
        cy.get('[data-cy="markdownEditorInput"]').type('ldkj aljdl aj\n\ndivinedragon');
        cy.get('[data-cy="badgeDescriptionError"]').contains('Badge Description - May not contain divinedragon word.');
        cy.get('[data-cy="saveBadgeButton"]').should('be.disabled');

        cy.get('[data-cy="markdownEditorInput"]').type('{backspace}');
        cy.get('[data-cy="saveBadgeButton"]').should('be.enabled');
    });

    it('self report reject messages are validated against custom validators', () => {
        cy.intercept('POST', '/admin/projects/proj1/approvals/reject').as('reject');
        cy.createProject(1, {enableProtectedUserCommunity: true})
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1, { selfReportingType: 'Approval' });
        cy.createSkill(1, 1, 2, { selfReportingType: 'Approval' });
        cy.createSkill(1, 1, 3, { selfReportingType: 'Approval' });
        cy.reportSkill(1, 2, 'user2', '2020-09-16 11:00');
        cy.reportSkill(1, 3, 'user1', '2020-09-17 11:00');
        cy.reportSkill(1, 1, 'user0', '2020-09-18 11:00');

        cy.visit('/administrator/projects/proj1/self-report');

        const tableSelector = '[data-cy="skillsReportApprovalTable"]';
        cy.validateTable(tableSelector, [
            [{
                colIndex: 1,
                value: 'user0'
            }],
            [{
                colIndex: 1,
                value: 'user1'
            }],
            [{
                colIndex: 1,
                value: 'user2'
            }],
        ]);

        cy.get('[data-cy="approveBtn"]').should('be.disabled');
        cy.get('[data-cy="rejectBtn"]').should('be.disabled');
        cy.get('[data-cy="approvalSelect_user1-skill3"]').click({ force: true });
        cy.get('[data-cy="approveBtn"]').should('be.enabled');
        cy.get('[data-cy="rejectBtn"]').should('be.enabled');

        cy.get('[data-cy="rejectBtn"]').click();
        cy.get('[data-cy="rejectionTitle"]').contains('This will reject user\'s request(s) to get points');
        cy.get('[data-cy="rejectionInputMsg"]').type('ldkj aljdl aj\n\njabberwocky');
        cy.get('[data-cy="rejectionInputMsgError"]').should('not.be.visible')
        cy.get('[data-cy="confirmRejectionBtn"]').should('be.enabled')

        cy.get('[data-cy="rejectionInputMsg"]').clear().type('ldkj aljdl aj\n\ndivinedragon');
        cy.get('[data-cy="rejectionInputMsgError"]').contains('Rejection Message - May not contain divinedragon word.');
        cy.get('[data-cy="confirmRejectionBtn"]').should('be.disabled');

        cy.get('[data-cy="rejectionInputMsg"]').type('{backspace}');
        cy.get('[data-cy="confirmRejectionBtn"]').should('be.enabled');
        cy.get('[data-cy="confirmRejectionBtn"]').click();

        cy.wait('@reject');

        cy.validateTable(tableSelector, [
            [{
                colIndex: 1,
                value: 'user0'
            }],
            [{
                colIndex: 1,
                value: 'user2'
            }],
        ]);
    });
});
