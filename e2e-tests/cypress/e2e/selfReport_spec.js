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

describe('Self Report Skills Management Tests', () => {

    beforeEach(() => {
        cy.request('POST', '/app/projects/proj1', {
            projectId: 'proj1',
            name: 'proj1'
        });
        cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            name: 'Subject 1'
        });

        Cypress.Commands.add('rejectRequest', (requestNum = 0, rejectionMsg = 'Skill was rejected') => {
            cy.request('/admin/projects/proj1/approvals?limit=10&ascending=true&page=1&orderBy=userId')
                .then((response) => {
                    cy.request('POST', '/admin/projects/proj1/approvals/reject', {
                        skillApprovalIds: [response.body.data[requestNum].id],
                        rejectionMessage: rejectionMsg,
                    });
                });
        });
    });

    it('manage self reporting settings at project level', () => {
        cy.visit('/administrator/projects/proj1/settings');

        cy.get('[data-cy="selfReportSwitch"]')
            .should('not.be.checked');
        cy.get('[data-cy="saveSettingsBtn"]')
            .should('be.disabled');
        cy.get('[data-cy="selfReportSwitch"]')
            .check({ force: true });
        cy.get('[data-cy="selfReportSwitch"]')
            .should('be.checked');

        cy.get('[data-cy="saveSettingsBtn"]')
            .should('be.enabled');
        cy.get('[data-cy="unsavedChangesAlert"]')
            .contains('Unsaved Changes');

        cy.get('[data-cy="selfReportTypeSelector"] [value="Approval"]')
            .should('be.checked');
        cy.get('[data-cy="selfReportTypeSelector"] [value="HonorSystem"]')
            .should('not.be.checked');

        cy.get('[data-cy="selfReportTypeSelector"] [value="HonorSystem"]')
            .click({ force: true });
        cy.get('[data-cy="selfReportTypeSelector"] [value="Approval"]')
            .should('not.be.checked');
        cy.get('[data-cy="selfReportTypeSelector"] [value="HonorSystem"]')
            .should('be.checked');

        cy.get('[data-cy="saveSettingsBtn"]')
            .should('be.enabled');
        cy.get('[data-cy="unsavedChangesAlert"]')
            .contains('Unsaved Changes');

        cy.get('[data-cy="saveSettingsBtn"]')
            .click();
        cy.get('[data-cy="settingsSavedAlert"]')
            .contains('Settings Updated');
        cy.get('[data-cy="unsavedChangesAlert"]')
            .should('not.exist');
        cy.get('[data-cy="saveSettingsBtn"]')
            .should('be.disabled');

        cy.get('[data-cy="justificationRequiredCheckbox"]')
            .should('not.be.checked');

        cy.get('[data-cy="justificationRequiredCheckbox"]')
            .click({ force: true });
        cy.get('[data-cy="justificationRequiredCheckbox"]')
            .should('be.checked');

        cy.get('[data-cy="saveSettingsBtn"]')
            .should('be.enabled');
        cy.get('[data-cy="unsavedChangesAlert"]')
            .contains('Unsaved Changes');

        cy.get('[data-cy="saveSettingsBtn"]')
            .click();
        cy.get('[data-cy="settingsSavedAlert"]')
            .contains('Settings Updated');
        cy.get('[data-cy="unsavedChangesAlert"]')
            .should('not.exist');
        cy.get('[data-cy="saveSettingsBtn"]')
            .should('be.disabled');

        // refresh and check that the values persisted
        cy.visit('/administrator/projects/proj1/settings');
        cy.get('[data-cy="selfReportSwitch"]')
            .should('be.checked');
        cy.get('[data-cy="selfReportTypeSelector"] [value="Approval"]')
            .should('not.be.checked');
        cy.get('[data-cy="selfReportTypeSelector"] [value="HonorSystem"]')
            .should('be.checked');
        cy.get('[data-cy="unsavedChangesAlert"]')
            .should('not.exist');
        cy.get('[data-cy="settingsSavedAlert"]')
            .should('not.exist');

        // disable skill, refresh and validate
        cy.get('[data-cy="selfReportSwitch"]')
            .uncheck({ force: true });
        cy.get('[data-cy="unsavedChangesAlert"]')
            .contains('Unsaved Changes');
        cy.get('[data-cy="settingsSavedAlert"]')
            .should('not.exist');
        cy.get('[data-cy="saveSettingsBtn"]')
            .should('be.enabled');
        cy.get('[data-cy="selfReportTypeSelector"] [value="Approval"]')
            .should('be.disabled');
        cy.get('[data-cy="selfReportTypeSelector"] [value="HonorSystem"]')
            .should('be.disabled');
        cy.get('[data-cy="saveSettingsBtn"]')
            .click();

        cy.visit('/administrator/projects/proj1/settings');
        cy.get('[data-cy="selfReportSwitch"]')
            .should('not.be.checked');
        cy.get('[data-cy="selfReportTypeSelector"] [value="Approval"]')
            .should('be.disabled');
        cy.get('[data-cy="justificationRequiredCheckbox"]')
            .should('be.disabled');
        cy.get('[data-cy="selfReportTypeSelector"] [value="HonorSystem"]')
            .should('be.disabled');
        cy.get('[data-cy="selfReportTypeSelector"] [value="Approval"]')
            .should('be.checked');
        cy.get('[data-cy="selfReportTypeSelector"] [value="HonorSystem"]')
            .should('not.be.checked');
        cy.get('[data-cy="justificationRequiredCheckbox"]')
            .should('be.checked');
        cy.get('[data-cy="unsavedChangesAlert"]')
            .should('not.exist');
        cy.get('[data-cy="settingsSavedAlert"]')
            .should('not.exist');
        cy.get('[data-cy="saveSettingsBtn"]')
            .should('be.disabled');

        // enable then disable should disable save button
        cy.get('[data-cy="selfReportSwitch"]')
            .check({ force: true });
        cy.get('[data-cy="unsavedChangesAlert"]')
            .contains('Unsaved Changes');
        cy.get('[data-cy="saveSettingsBtn"]')
            .should('be.enabled');
        cy.get('[data-cy="selfReportSwitch"]')
            .uncheck({ force: true });
        cy.get('[data-cy="unsavedChangesAlert"]')
            .should('not.exist');
        cy.get('[data-cy="saveSettingsBtn"]')
            .should('be.disabled');

        // enabled and save
        cy.get('[data-cy="selfReportSwitch"]')
            .check({ force: true });
        cy.get('[data-cy="saveSettingsBtn"]')
            .click();
        cy.get('[data-cy="settingsSavedAlert"]')
            .contains('Settings Updated');

        cy.get('[data-cy="unsavedChangesAlert"]')
            .should('not.exist');
        cy.get('[data-cy="selfReportTypeSelector"] [value="HonorSystem"]')
            .click({ force: true });
        cy.get('[data-cy="unsavedChangesAlert"]')
            .contains('Unsaved Changes');
        cy.get('[data-cy="selfReportTypeSelector"] [value="Approval"]')
            .click({ force: true });
        cy.get('[data-cy="unsavedChangesAlert"]')
            .should('not.exist');

        cy.get('[data-cy="selfReportSwitch"]')
            .uncheck({ force: true });
        cy.get('[data-cy="unsavedChangesAlert"]')
            .contains('Unsaved Changes');
        cy.get('[data-cy="selfReportSwitch"]')
            .check({ force: true });
        cy.get('[data-cy="unsavedChangesAlert"]')
            .should('not.exist');
    });

    it('create skills - self reporting disabled - no project level default', () => {
        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get('[data-cy="newSkillButton"]')
            .click();
        cy.get('[data-cy="selfReportEnableCheckbox"]')
            .should('not.be.checked');
        cy.get('[data-cy="selfReportTypeSelector"] [value="Approval"]')
            .should('be.disabled');
        cy.get('[data-cy="selfReportTypeSelector"] [value="HonorSystem"]')
            .should('be.disabled');
        cy.get('[data-cy="selfReportTypeSelector"] [value="Approval"]')
            .should('be.checked');
        cy.get('[data-cy="selfReportTypeSelector"] [value="HonorSystem"]')
            .should('not.be.checked');

        cy.get('[data-cy=skillName]')
            .type('skill1');
        cy.clickSave();
        cy.get('[data-cy="editSkillButton_skill1Skill"]')
            .click();
        cy.get('[data-cy="selfReportEnableCheckbox"]')
            .should('not.be.checked');
        cy.get('[data-cy="selfReportTypeSelector"] [value="Approval"]')
            .should('be.disabled');
        cy.get('[data-cy="selfReportTypeSelector"] [value="HonorSystem"]')
            .should('be.disabled');
        cy.get('[data-cy="selfReportTypeSelector"] [value="Approval"]')
            .should('be.checked');
        cy.get('[data-cy="selfReportTypeSelector"] [value="HonorSystem"]')
            .should('not.be.checked');
    });

    it('create skills - self reporting with approval - no project level default', () => {
        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get('[data-cy="newSkillButton"]')
            .click();
        cy.get('[data-cy="selfReportEnableCheckbox"]')
            .should('not.be.checked');
        cy.get('[data-cy="selfReportTypeSelector"] [value="Approval"]')
            .should('be.disabled');
        cy.get('[data-cy="selfReportTypeSelector"] [value="HonorSystem"]')
            .should('be.disabled');
        cy.get('[data-cy="selfReportTypeSelector"] [value="Approval"]')
            .should('be.checked');
        cy.get('[data-cy="selfReportTypeSelector"] [value="HonorSystem"]')
            .should('not.be.checked');

        cy.get('[data-cy="selfReportEnableCheckbox"]')
            .check({ force: true });
        cy.get('[data-cy="selfReportTypeSelector"] [value="Approval"]')
            .should('be.enabled');
        cy.get('[data-cy="selfReportTypeSelector"] [value="HonorSystem"]')
            .should('be.enabled');
        cy.get('[data-cy="selfReportTypeSelector"] [value="Approval"]')
            .should('be.checked');
        cy.get('[data-cy="selfReportTypeSelector"] [value="HonorSystem"]')
            .should('not.be.checked');

        cy.get('[data-cy=skillName]')
            .type('skill1');
        cy.clickSave();
        cy.get('[data-cy="editSkillButton_skill1Skill"]')
            .click();
        cy.get('[data-cy="selfReportEnableCheckbox"]')
            .should('be.checked');
        cy.get('[data-cy="selfReportTypeSelector"] [value="Approval"]')
            .should('be.enabled');
        cy.get('[data-cy="selfReportTypeSelector"] [value="HonorSystem"]')
            .should('be.enabled');
        cy.get('[data-cy="selfReportTypeSelector"] [value="Approval"]')
            .should('be.checked');
        cy.get('[data-cy="selfReportTypeSelector"] [value="HonorSystem"]')
            .should('not.be.checked');
    });

    it('create skills - self reporting with Honor System - no project level default', () => {
        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get('[data-cy="newSkillButton"]')
            .click();
        cy.get('[data-cy="selfReportEnableCheckbox"]')
            .should('not.be.checked');
        cy.get('[data-cy="selfReportTypeSelector"] [value="Approval"]')
            .should('be.disabled');
        cy.get('[data-cy="selfReportTypeSelector"] [value="HonorSystem"]')
            .should('be.disabled');
        cy.get('[data-cy="selfReportTypeSelector"] [value="Approval"]')
            .should('be.checked');
        cy.get('[data-cy="selfReportTypeSelector"] [value="HonorSystem"]')
            .should('not.be.checked');

        cy.get('[data-cy="selfReportEnableCheckbox"]')
            .check({ force: true });
        cy.get('[data-cy="selfReportTypeSelector"] [value="Approval"]')
            .should('be.enabled');
        cy.get('[data-cy="selfReportTypeSelector"] [value="HonorSystem"]')
            .should('be.enabled');
        cy.get('[data-cy="selfReportTypeSelector"] [value="Approval"]')
            .should('be.checked');
        cy.get('[data-cy="selfReportTypeSelector"] [value="HonorSystem"]')
            .should('not.be.checked');

        cy.get('[data-cy="selfReportTypeSelector"] [value="HonorSystem"]')
            .click({ force: true });
        cy.get('[data-cy="selfReportTypeSelector"] [value="Approval"]')
            .should('not.be.checked');
        cy.get('[data-cy="selfReportTypeSelector"] [value="HonorSystem"]')
            .should('be.checked');

        cy.get('[data-cy=skillName]')
            .type('skill1');
        cy.clickSave();
        cy.get('[data-cy="editSkillButton_skill1Skill"]')
            .click();
        cy.get('[data-cy="selfReportEnableCheckbox"]')
            .should('be.checked');
        cy.get('[data-cy="selfReportTypeSelector"] [value="Approval"]')
            .should('be.enabled');
        cy.get('[data-cy="selfReportTypeSelector"] [value="HonorSystem"]')
            .should('be.enabled');
        cy.get('[data-cy="selfReportTypeSelector"] [value="Approval"]')
            .should('not.be.checked');
        cy.get('[data-cy="selfReportTypeSelector"] [value="HonorSystem"]')
            .should('be.checked');
    });

    it('create skill - project level default of Honor System', () => {
        cy.visit('/administrator/projects/proj1/settings');
        cy.get('[data-cy="selfReportSwitch"]')
            .check({ force: true });
        cy.get('[data-cy="selfReportTypeSelector"] [value="HonorSystem"]')
            .click({ force: true });
        cy.get('[data-cy="saveSettingsBtn"]')
            .click();
        cy.get('[data-cy="settingsSavedAlert"]')
            .contains('Settings Updated');

        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get('[data-cy="newSkillButton"]')
            .click();
        cy.get('[data-cy="selfReportEnableCheckbox"]')
            .should('be.checked');
        cy.get('[data-cy="selfReportTypeSelector"] [value="Approval"]')
            .should('be.enabled');
        cy.get('[data-cy="selfReportTypeSelector"] [value="HonorSystem"]')
            .should('be.enabled');
        cy.get('[data-cy="selfReportTypeSelector"] [value="Approval"]')
            .should('not.be.checked');
        cy.get('[data-cy="selfReportTypeSelector"] [value="HonorSystem"]')
            .should('be.checked');
    });

    it('create skill - project level default of Approval', () => {
        cy.visit('/administrator/projects/proj1/settings');
        cy.get('[data-cy="selfReportSwitch"]')
            .check({ force: true });
        cy.get('[data-cy="saveSettingsBtn"]')
            .click();
        cy.get('[data-cy="settingsSavedAlert"]')
            .contains('Settings Updated');

        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get('[data-cy="newSkillButton"]')
            .click();
        cy.get('[data-cy="selfReportEnableCheckbox"]')
            .should('be.checked');
        cy.get('[data-cy="selfReportTypeSelector"] [value="Approval"]')
            .should('be.enabled');
        cy.get('[data-cy="selfReportTypeSelector"] [value="HonorSystem"]')
            .should('be.enabled');
        cy.get('[data-cy="justificationRequiredCheckbox"]')
            .should('be.enabled');
        cy.get('[data-cy="selfReportTypeSelector"] [value="Approval"]')
            .should('be.checked');
        cy.get('[data-cy="selfReportTypeSelector"] [value="HonorSystem"]')
            .should('not.be.checked');
        cy.get('[data-cy="justificationRequiredCheckbox"]')
            .should('not.be.checked');
    });

    it('create skill - project level default of Approval and Require Justification', () => {
        cy.visit('/administrator/projects/proj1/settings');
        cy.get('[data-cy="selfReportSwitch"]')
            .check({ force: true });
        cy.get('[data-cy="justificationRequiredCheckbox"]')
            .click({ force: true });
        cy.get('[data-cy="saveSettingsBtn"]')
            .click();
        cy.get('[data-cy="settingsSavedAlert"]')
            .contains('Settings Updated');

        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get('[data-cy="newSkillButton"]')
            .click();
        cy.get('[data-cy="selfReportEnableCheckbox"]')
            .should('be.checked');
        cy.get('[data-cy="selfReportTypeSelector"] [value="Approval"]')
            .should('be.enabled');
        cy.get('[data-cy="selfReportTypeSelector"] [value="HonorSystem"]')
            .should('be.enabled');
        cy.get('[data-cy="justificationRequiredCheckbox"]')
            .should('be.enabled');
        cy.get('[data-cy="selfReportTypeSelector"] [value="Approval"]')
            .should('be.checked');
        cy.get('[data-cy="selfReportTypeSelector"] [value="HonorSystem"]')
            .should('not.be.checked');
        cy.get('[data-cy="justificationRequiredCheckbox"]')
            .should('be.checked');
    });

    it('edit skills - approval -> warnings', () => {
        cy.createSkill(1, 1, 1, {
            selfReportingType: 'Approval',
            name: 'Approval 1'
        });
        cy.createSkill(1, 1, 2, {
            selfReportingType: 'Approval',
            name: 'Approval 2'
        });
        cy.createSkill(1, 1, 3, {
            selfReportingType: 'Approval',
            name: 'Approval 3'
        });
        cy.createSkill(1, 1, 4, {
            selfReportingType: 'HonorSystem',
            name: 'Honor System 1'
        });
        cy.createSkill(1, 1, 5, { name: 'Disabled 1' });
        cy.reportSkill(1, 1, 'user6Good@skills.org', '2020-09-12 11:00');
        cy.reportSkill(1, 1, 'user5Good@skills.org', '2020-09-13 11:00');
        cy.reportSkill(1, 1, 'user4Good@skills.org', '2020-09-14 11:00');
        cy.reportSkill(1, 1, 'user3Good@skills.org', '2020-09-15 11:00');
        cy.rejectRequest(3);

        cy.visit('/administrator/projects/proj1/subjects/subj1');

        // approval -> honor
        cy.get('[data-cy="editSkillButton_skill1"]')
            .click();
        cy.get('[data-cy="selfReportingTypeWarning"]')
            .should('not.exist');
        cy.get('[data-cy="selfReportTypeSelector"] [value="HonorSystem"]')
            .click({ force: true });

        cy.get('[data-cy="selfReportingTypeWarning"]')
            .contains('Switching this skill to the Honor System will automatically:');
        cy.get('[data-cy="selfReportingTypeWarning"]')
            .contains('Approve 3 pending requests');
        cy.get('[data-cy="selfReportingTypeWarning"]')
            .contains('Remove 1 rejected request');

        // honor -> disabled
        cy.get('[data-cy="selfReportEnableCheckbox"]')
            .uncheck({ force: true });
        cy.get('[data-cy="selfReportingTypeWarning"]')
            .contains('Disabling Self Reporting will automatically:');
        cy.get('[data-cy="selfReportingTypeWarning"]')
            .contains('Remove 3 pending requests');
        cy.get('[data-cy="selfReportingTypeWarning"]')
            .contains('Remove 1 rejected request');

        // disabled -> honor
        cy.get('[data-cy="selfReportEnableCheckbox"]')
            .check({ force: true });
        cy.get('[data-cy="selfReportingTypeWarning"]')
            .contains('Switching this skill to the Honor System will automatically:');
        cy.get('[data-cy="selfReportingTypeWarning"]')
            .contains('Approve 3 pending requests');
        cy.get('[data-cy="selfReportingTypeWarning"]')
            .contains('Remove 1 rejected request');

        // honor -> approval
        cy.get('[data-cy="selfReportTypeSelector"] [value="Approval"]')
            .click({ force: true });
        cy.get('[data-cy="selfReportingTypeWarning"]')
            .should('not.exist');

        // approval -> disable
        cy.get('[data-cy="selfReportEnableCheckbox"]')
            .uncheck({ force: true });
        cy.get('[data-cy="selfReportingTypeWarning"]')
            .contains('Disabling Self Reporting will automatically:');
        cy.get('[data-cy="selfReportingTypeWarning"]')
            .contains('Remove 3 pending requests');
        cy.get('[data-cy="selfReportingTypeWarning"]')
            .contains('Remove 1 rejected request');

        // disable -> approval
        cy.get('[data-cy="selfReportEnableCheckbox"]')
            .check({ force: true });
        cy.get('[data-cy="selfReportingTypeWarning"]')
            .should('not.exist');
    });

    it('edit skills - approval -> no pending requests then no warning', () => {
        cy.createSkill(1, 1, 1, {
            selfReportingType: 'Approval',
            name: 'Approval 1'
        });
        cy.createSkill(1, 1, 2, {
            selfReportingType: 'Approval',
            name: 'Approval 2'
        });
        cy.createSkill(1, 1, 3, {
            selfReportingType: 'Approval',
            name: 'Approval 3'
        });
        cy.createSkill(1, 1, 4, {
            selfReportingType: 'HonorSystem',
            name: 'Honor System 1'
        });
        cy.createSkill(1, 1, 5, { name: 'Disabled 1' });
        cy.reportSkill(1, 1, 'user6Good@skills.org', '2020-09-12 11:00');
        cy.reportSkill(1, 1, 'user5Good@skills.org', '2020-09-13 11:00');
        cy.reportSkill(1, 1, 'user4Good@skills.org', '2020-09-14 11:00');
        cy.reportSkill(1, 1, 'user3Good@skills.org', '2020-09-15 11:00');
        cy.rejectRequest(3);

        cy.visit('/administrator/projects/proj1/subjects/subj1');

        // approval -> honor
        cy.get('[data-cy="editSkillButton_skill2"]')
            .click();
        cy.get('[data-cy="selfReportingTypeWarning"]')
            .should('not.exist');

        // honor -> disabled
        cy.get('[data-cy="selfReportEnableCheckbox"]')
            .uncheck({ force: true });
        cy.get('[data-cy="selfReportingTypeWarning"]')
            .should('not.exist');

        // disabled -> honor
        cy.get('[data-cy="selfReportEnableCheckbox"]')
            .check({ force: true });
        cy.get('[data-cy="selfReportingTypeWarning"]')
            .should('not.exist');

        // honor -> approval
        cy.get('[data-cy="selfReportTypeSelector"] [value="Approval"]')
            .click({ force: true });
        cy.get('[data-cy="selfReportingTypeWarning"]')
            .should('not.exist');

        // approval -> disable
        cy.get('[data-cy="selfReportEnableCheckbox"]')
            .uncheck({ force: true });
        cy.get('[data-cy="selfReportingTypeWarning"]')
            .should('not.exist');

        // disable -> approval
        cy.get('[data-cy="selfReportEnableCheckbox"]')
            .check({ force: true });
        cy.get('[data-cy="selfReportingTypeWarning"]')
            .should('not.exist');
    });

    it('edit skills - disable self reporting -> warnings -> save', () => {
        cy.createSkill(1, 1, 1, {
            selfReportingType: 'Approval',
            name: 'Approval 1'
        });
        cy.createSkill(1, 1, 2, {
            selfReportingType: 'Approval',
            name: 'Approval 2'
        });
        cy.createSkill(1, 1, 3, {
            selfReportingType: 'Approval',
            name: 'Approval 3'
        });
        cy.createSkill(1, 1, 4, {
            selfReportingType: 'HonorSystem',
            name: 'Honor System 1'
        });
        cy.createSkill(1, 1, 5, { name: 'Disabled 1' });
        cy.reportSkill(1, 1, 'user6Good@skills.org', '2020-09-12 11:00');
        cy.reportSkill(1, 1, 'user5Good@skills.org', '2020-09-13 11:00');
        cy.reportSkill(1, 1, 'user4Good@skills.org', '2020-09-14 11:00');
        cy.reportSkill(1, 1, 'user3Good@skills.org', '2020-09-15 11:00');
        cy.rejectRequest(3);

        cy.visit('/administrator/projects/proj1/subjects/subj1');

        // approval -> disabled
        cy.get('[data-cy="editSkillButton_skill1"]')
            .click();
        cy.get('[data-cy="selfReportingTypeWarning"]')
            .should('not.exist');

        // honor -> disabled
        cy.get('[data-cy="selfReportEnableCheckbox"]')
            .uncheck({ force: true });
        cy.get('[data-cy="selfReportingTypeWarning"]')
            .contains('Disabling Self Reporting will automatically:');
        cy.get('[data-cy="selfReportingTypeWarning"]')
            .contains('Remove 3 pending requests');
        cy.get('[data-cy="selfReportingTypeWarning"]')
            .contains('Remove 1 rejected request');

        cy.clickSave();
        cy.get('[data-cy="editSkillButton_skill1"]')
            .click();
        cy.get('[data-cy="selfReportingTypeWarning"]')
            .should('not.exist');
        cy.get('[data-cy="selfReportEnableCheckbox"]')
            .should('not.be.checked');
    });

    it('skill overview - display self reporting card', () => {
        cy.request('POST', `/admin/projects/proj1/subjects/subj1/skills/skill1`, {
            projectId: 'proj1',
            subjectId: 'subj1',
            skillId: `skill1`,
            name: `Very Great Skill # 1`,
            pointIncrement: '1500',
            numPerformToCompletion: '10',
            selfReportingType: 'Approval'
        });

        cy.request('POST', `/admin/projects/proj1/subjects/subj1/skills/skill2`, {
            projectId: 'proj1',
            subjectId: 'subj1',
            skillId: `skill2`,
            name: `Very Great Skill # 2`,
            pointIncrement: '1500',
            numPerformToCompletion: '10',
            selfReportingType: 'HonorSystem'
        });

        cy.request('POST', `/admin/projects/proj1/subjects/subj1/skills/skill3`, {
            projectId: 'proj1',
            subjectId: 'subj1',
            skillId: `skill3`,
            name: `Very Great Skill # 3`,
            pointIncrement: '1500',
            numPerformToCompletion: '10',
        });

        cy.visit('/administrator/projects/proj1/subjects/subj1/skills/skill1');
        cy.get('[data-cy="selfReportMediaCard"] [data-cy="mediaInfoCardTitle"]')
            .contains('Self Report: Approval');
        cy.get('[data-cy="selfReportMediaCard"] [data-cy="mediaInfoCardSubTitle"]')
            .contains('Users can self report this skill and will go into an approval queue');

        cy.visit('/administrator/projects/proj1/subjects/subj1/skills/skill2');
        cy.get('[data-cy="selfReportMediaCard"] [data-cy="mediaInfoCardTitle"]')
            .contains('Self Report: Honor System');
        cy.get('[data-cy="selfReportMediaCard"] [data-cy="mediaInfoCardSubTitle"]')
            .contains('Users can self report this skill and will apply immediately');

        cy.visit('/administrator/projects/proj1/subjects/subj1/skills/skill3');
        cy.get('[data-cy="selfReportMediaCard"] [data-cy="mediaInfoCardTitle"]')
            .contains('Self Report: Disabled');
        cy.get('[data-cy="selfReportMediaCard"] [data-cy="mediaInfoCardSubTitle"]')
            .contains('Self reporting is disabled for this skill');
    });

    it('show how many points are requested', () => {
        cy.createSkill(1, 1, 1, {
            selfReportingType: 'Approval',
            pointIncrement: '100'
        });
        cy.createSkill(1, 1, 2, {
            selfReportingType: 'Approval',
            pointIncrement: '220'
        });
        cy.createSkill(1, 1, 3, {
            selfReportingType: 'Approval',
            pointIncrement: '180'
        });
        cy.reportSkill(1, 1, 'user1Good@skills.org', '2020-09-12 11:00');
        cy.reportSkill(1, 2, 'user2Good@skills.org', '2020-09-13 11:00');
        cy.reportSkill(1, 3, 'user3Good@skills.org', '2020-09-14 11:00');

        cy.visit('/administrator/projects/proj1/self-report');

        const tableSelector = '[data-cy="skillsReportApprovalTable"]';
        const expected = [
            [{
                colIndex: 1,
                value: 'user3Good@skills.org '
            }, {
                colIndex: 0,
                value: '180'
            }],
            [{
                colIndex: 1,
                value: 'user2Good@skills.org '
            }, {
                colIndex: 0,
                value: '220'
            }],
            [{
                colIndex: 1,
                value: 'user1Good@skills.org '
            }, {
                colIndex: 0,
                value: '100'
            }],
        ];
        cy.validateTable(tableSelector, expected);
    });

    it('sorting and paging of the approval table', () => {
        cy.createSkill(1, 1, 1, { selfReportingType: 'Approval' });
        cy.createSkill(1, 1, 2, { selfReportingType: 'Approval' });
        cy.createSkill(1, 1, 3, { selfReportingType: 'Approval' });
        cy.reportSkill(1, 1, 'user6Good@skills.org', '2020-09-12 11:00');
        cy.reportSkill(1, 2, 'user5Good@skills.org', '2020-09-13 11:00');
        cy.reportSkill(1, 3, 'user4Good@skills.org', '2020-09-14 11:00');
        cy.reportSkill(1, 1, 'user3Good@skills.org', '2020-09-15 11:00');
        cy.reportSkill(1, 2, 'user2Good@skills.org', '2020-09-16 11:00');
        cy.reportSkill(1, 3, 'user1Good@skills.org', '2020-09-17 11:00');
        cy.reportSkill(1, 1, 'user0Good@skills.org', '2020-09-18 11:00');

        cy.visit('/administrator/projects/proj1/self-report');

        const tableSelector = '[data-cy="skillsReportApprovalTable"]';
        const expected = [
            [{
                colIndex: 1,
                value: 'user0Good@skills.org '
            }, {
                colIndex: 2,
                value: '2020-09-18 11:00'
            }],
            [{
                colIndex: 1,
                value: 'user1Good@skills.org '
            }, {
                colIndex: 2,
                value: '2020-09-17 11:00'
            }],
            [{
                colIndex: 1,
                value: 'user2Good@skills.org '
            }, {
                colIndex: 2,
                value: '2020-09-16 11:00'
            }],
            [{
                colIndex: 1,
                value: 'user3Good@skills.org '
            }, {
                colIndex: 2,
                value: '2020-09-15 11:00'
            }],
            [{
                colIndex: 1,
                value: 'user4Good@skills.org '
            }, {
                colIndex: 2,
                value: '2020-09-14 11:00'
            }],
            [{
                colIndex: 1,
                value: 'user5Good@skills.org '
            }, {
                colIndex: 2,
                value: '2020-09-13 11:00'
            }],
            [{
                colIndex: 1,
                value: 'user6Good@skills.org '
            }, {
                colIndex: 2,
                value: '2020-09-12 11:00'
            }],
        ];
        const expectedReversed = [...expected].reverse();

        cy.validateTable(tableSelector, expected);

        cy.get(`${tableSelector} th`)
            .contains('Requested On')
            .click();
        cy.validateTable(tableSelector, expectedReversed);

        cy.get(`${tableSelector} th`)
            .contains('For User')
            .click();
        cy.validateTable(tableSelector, expected);
        cy.get(`${tableSelector} th`)
            .contains('For User')
            .click();
        cy.validateTable(tableSelector, expectedReversed);
    });

    it('change page size of the approval table', () => {
        cy.createSkill(1, 1, 1, { selfReportingType: 'Approval' });
        cy.createSkill(1, 1, 2, { selfReportingType: 'Approval' });
        cy.createSkill(1, 1, 3, { selfReportingType: 'Approval' });
        cy.reportSkill(1, 1, 'user6Good@skills.org', '2020-09-12 11:00');
        cy.reportSkill(1, 2, 'user5Good@skills.org', '2020-09-13 11:00');
        cy.reportSkill(1, 3, 'user4Good@skills.org', '2020-09-14 11:00');
        cy.reportSkill(1, 1, 'user3Good@skills.org', '2020-09-15 11:00');
        cy.reportSkill(1, 2, 'user2Good@skills.org', '2020-09-16 11:00');
        cy.reportSkill(1, 3, 'user1Good@skills.org', '2020-09-17 11:00');
        cy.reportSkill(1, 1, 'user0Good@skills.org', '2020-09-18 11:00');

        cy.visit('/administrator/projects/proj1/self-report');
        const rowSelector = '[data-cy="skillsReportApprovalTable"] tbody tr';
        cy.get(rowSelector)
            .should('have.length', 5);

        const tableSelector = '[data-cy="skillsReportApprovalTable"]';
        cy.get(`${tableSelector} [data-cy="skillsBTablePageSize"]`)
            .select('10');
        cy.get(rowSelector)
            .should('have.length', 7);
    });

    it('approve one', () => {
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

        cy.get('[data-cy="approveBtn"]')
            .should('be.disabled');
        cy.get('[data-cy="rejectBtn"]')
            .should('be.disabled');
        cy.get('[data-cy="approvalSelect_user1-skill3"]')
            .click({ force: true });
        cy.get('[data-cy="approveBtn"]')
            .should('be.enabled');
        cy.get('[data-cy="rejectBtn"]')
            .should('be.enabled');

        cy.get('[data-cy="approveBtn"]')
            .click();
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

        cy.visit('/administrator/projects/proj1/users/user1/skillEvents');
        cy.validateTable('[data-cy="performedSkillsTable"]', [
            [{
                colIndex: 0,
                value: 'skill3'
            }],
        ]);
    });

    it('reject one', () => {
        cy.intercept('POST', '/admin/projects/proj1/approvals/reject', (req) => {
            expect(req.body.rejectionMessage)
                .to
                .include('Rejection message!');
        })
            .as('reject');

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

        cy.get('[data-cy="approveBtn"]')
            .should('be.disabled');
        cy.get('[data-cy="rejectBtn"]')
            .should('be.disabled');
        cy.get('[data-cy="approvalSelect_user1-skill3"]')
            .click({ force: true });
        cy.get('[data-cy="approveBtn"]')
            .should('be.enabled');
        cy.get('[data-cy="rejectBtn"]')
            .should('be.enabled');

        cy.get('[data-cy="rejectBtn"]')
            .click();
        cy.get('[data-cy="rejectionTitle"]')
            .contains('This will reject user\'s request(s) to get points');
        cy.get('[data-cy="rejectionInputMsg"]')
            .type('Rejection message!');
        cy.get('[data-cy="confirmRejectionBtn"]')
            .click();

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

        cy.visit('/administrator/projects/proj1/users/user1/skillEvents');
        cy.get('[data-cy="performedSkillsTable"] tbody tr')
            .should('have.length', 0);
    });

    it('custom validation for rejection message', () => {
        cy.intercept('POST', '/admin/projects/proj1/approvals/reject', (req) => {
            expect(req.body.rejectionMessage)
                .to
                .include('Rejection jabberwoc');
        })
            .as('reject');

        cy.createSkill(1, 1, 1, { selfReportingType: 'Approval' });
        cy.createSkill(1, 1, 2, { selfReportingType: 'Approval' });
        cy.createSkill(1, 1, 3, { selfReportingType: 'Approval' });
        cy.reportSkill(1, 2, 'user2', '2020-09-16 11:00');
        cy.reportSkill(1, 3, 'user1', '2020-09-17 11:00');
        cy.reportSkill(1, 1, 'user0', '2020-09-18 11:00');

        cy.visit('/administrator/projects/proj1/self-report');

        cy.get('[data-cy="approvalSelect_user1-skill3"]')
            .click({ force: true });
        cy.get('[data-cy="rejectBtn"]')
            .click();
        cy.get('[data-cy="rejectionTitle"]')
            .contains('This will reject user\'s request(s) to get points');

        cy.get('[data-cy="rejectionInputMsgError"]')
            .contains('Rejection Message - paragraphs may not contain jabberwocky.')
            .should('not.exist');
        cy.get('[data-cy="confirmRejectionBtn"]')
            .should('be.enabled');

        cy.get('[data-cy="rejectionInputMsg"]')
            .type('Rejection jabber');
        cy.get('[data-cy="rejectionInputMsgError"]')
            .contains('Rejection Message - paragraphs may not contain jabberwocky.')
            .should('not.exist');
        cy.get('[data-cy="confirmRejectionBtn"]')
            .should('be.enabled');

        cy.get('[data-cy="rejectionInputMsg"]')
            .type('wock');
        cy.get('[data-cy="rejectionInputMsgError"]')
            .contains('Rejection Message - paragraphs may not contain jabberwocky.')
            .should('not.exist');
        cy.get('[data-cy="confirmRejectionBtn"]')
            .should('be.enabled');

        cy.get('[data-cy="rejectionInputMsg"]')
            .type('y');
        cy.get('[data-cy="rejectionInputMsgError"]')
            .contains('Rejection Message - paragraphs may not contain jabberwocky.');
        cy.get('[data-cy="confirmRejectionBtn"]')
            .should('be.disabled');

        cy.get('[data-cy="rejectionInputMsg"]')
            .type(' ok');
        cy.get('[data-cy="rejectionInputMsgError"]')
            .contains('Rejection Message - paragraphs may not contain jabberwocky.');
        cy.get('[data-cy="confirmRejectionBtn"]')
            .should('be.disabled');

        cy.get('[data-cy="rejectionInputMsg"]')
            .type('{backspace}{backspace}{backspace}');
        cy.get('[data-cy="rejectionInputMsgError"]')
            .contains('Rejection Message - paragraphs may not contain jabberwocky.');
        cy.get('[data-cy="confirmRejectionBtn"]')
            .should('be.disabled');

        cy.get('[data-cy="rejectionInputMsg"]')
            .type('{backspace}{backspace}');
        cy.get('[data-cy="rejectionInputMsgError"]')
            .contains('Rejection Message - paragraphs may not contain jabberwocky.')
            .should('not.exist');
        cy.get('[data-cy="confirmRejectionBtn"]')
            .should('be.enabled');

        cy.get('[data-cy="confirmRejectionBtn"]')
            .click();
        cy.wait('@reject');
    });

    it('approve 1 page worth of records', () => {
        cy.createSkill(1, 1, 1, { selfReportingType: 'Approval' });
        cy.createSkill(1, 1, 2, { selfReportingType: 'Approval' });
        cy.createSkill(1, 1, 3, { selfReportingType: 'Approval' });
        cy.reportSkill(1, 2, 'user6', '2020-09-11 11:00');
        cy.reportSkill(1, 2, 'user5', '2020-09-12 11:00');
        cy.reportSkill(1, 2, 'user4', '2020-09-13 11:00');
        cy.reportSkill(1, 2, 'user3', '2020-09-14 11:00');
        cy.reportSkill(1, 2, 'user2', '2020-09-16 11:00');
        cy.reportSkill(1, 3, 'user1', '2020-09-17 11:00');
        cy.reportSkill(1, 1, 'user0', '2020-09-18 11:00');

        cy.intercept('GET', '/public/isFeatureSupported?feature=emailservice').as('featureSupported');
        cy.intercept('GET', '/admin/projects/proj1/approvals?*').as('loadApprovals');
        cy.intercept('GET', '/admin/projects/proj1/approvals/history?*').as('loadApprovalHistory');
        cy.visit('/administrator/projects/proj1/self-report');
        cy.wait('@featureSupported');
        cy.wait('@loadApprovalHistory');
        cy.wait('@loadApprovals');

        cy.get('[data-cy="selectPageOfApprovalsBtn"]')
            .click();
        cy.get('[data-cy="approveBtn"]')
            .click();

        const tableSelector = '[data-cy="skillsReportApprovalTable"]';

        cy.validateTable(tableSelector, [
            [{
                colIndex: 1,
                value: 'user5'
            }],
            [{
                colIndex: 1,
                value: 'user6'
            }],
        ]);

        cy.get('[data-cy="approveBtn"]')
            .should('be.disabled');
        cy.get('[data-cy="rejectBtn"]')
            .should('be.disabled');

        cy.visit('/administrator/projects/proj1/users');
        cy.validateTable('[data-cy="usersTable"]', [
            [{
                colIndex: 0,
                value: 'user0'
            }, {
                colIndex: 2,
                value: '100'
            }],
            [{
                colIndex: 0,
                value: 'user1'
            }, {
                colIndex: 2,
                value: '100'
            }],
            [{
                colIndex: 0,
                value: 'user2'
            }, {
                colIndex: 2,
                value: '100'
            }],
            [{
                colIndex: 0,
                value: 'user3'
            }, {
                colIndex: 2,
                value: '100'
            }],
            [{
                colIndex: 0,
                value: 'user4'
            }, {
                colIndex: 2,
                value: '100'
            }],
        ]);
    });

    it('reject 1 page worth of records', () => {
        cy.intercept({
            method: 'POST',
            url: '/admin/projects/proj1/approvals/reject',
        })
            .as('reject');

        cy.createSkill(1, 1, 1, { selfReportingType: 'Approval' });
        cy.createSkill(1, 1, 2, { selfReportingType: 'Approval' });
        cy.createSkill(1, 1, 3, { selfReportingType: 'Approval' });
        cy.reportSkill(1, 2, 'user6', '2020-09-11 11:00');
        cy.reportSkill(1, 2, 'user5', '2020-09-12 11:00');
        cy.reportSkill(1, 2, 'user4', '2020-09-13 11:00');
        cy.reportSkill(1, 2, 'user3', '2020-09-14 11:00');
        cy.reportSkill(1, 2, 'user2', '2020-09-16 11:00');
        cy.reportSkill(1, 3, 'user1', '2020-09-17 11:00');
        cy.reportSkill(1, 1, 'user0', '2020-09-18 11:00');

        cy.intercept('GET', '/public/isFeatureSupported?feature=emailservice').as('featureSupported');
        cy.intercept('GET', '/admin/projects/proj1/approvals?*').as('loadApprovals');
        cy.intercept('GET', '/admin/projects/proj1/approvals/history?*').as('loadApprovalHistory');
        cy.visit('/administrator/projects/proj1/self-report');
        cy.wait('@featureSupported');
        cy.wait('@loadApprovalHistory');
        cy.wait('@loadApprovals');

        cy.get('[data-cy="selectPageOfApprovalsBtn"]')
            .click();

        cy.get('[data-cy="rejectBtn"]')
            .click();
        cy.get('[data-cy="rejectionTitle"]')
            .contains('This will reject user\'s request(s) to get points');
        cy.get('[data-cy="rejectionInputMsg"]')
            .type('Rejection message!');
        cy.get('[data-cy="confirmRejectionBtn"]')
            .click();

        const tableSelector = '[data-cy="skillsReportApprovalTable"]';

        cy.validateTable(tableSelector, [
            [{
                colIndex: 1,
                value: 'user5'
            }],
            [{
                colIndex: 1,
                value: 'user6'
            }],
        ]);

        cy.get('[data-cy="approveBtn"]')
            .should('be.disabled');
        cy.get('[data-cy="rejectBtn"]')
            .should('be.disabled');

        cy.visit('/administrator/projects/proj1/users');
        cy.get('[data-cy="usersTable"] tbody tr')
            .should('have.length', 0);
    });

    it('select page and then clear', () => {
        cy.createSkill(1, 1, 1, { selfReportingType: 'Approval' });
        cy.createSkill(1, 1, 2, { selfReportingType: 'Approval' });
        cy.createSkill(1, 1, 3, { selfReportingType: 'Approval' });
        cy.reportSkill(1, 2, 'user6', '2020-09-11 11:00');
        cy.reportSkill(1, 2, 'user5', '2020-09-12 11:00');
        cy.reportSkill(1, 2, 'user4', '2020-09-13 11:00');

        cy.intercept('GET', '/public/isFeatureSupported?feature=emailservice').as('featureSupported');
        cy.intercept('GET', '/admin/projects/proj1/approvals**').as('loadApprovals');
        cy.visit('/administrator/projects/proj1/self-report');
        cy.wait('@featureSupported');
        cy.wait('@loadApprovals');

        cy.get('[data-cy="selectPageOfApprovalsBtn"]')
            .click();
        cy.get('[data-cy="approveBtn"]')
            .should('be.enabled');
        cy.get('[data-cy="rejectBtn"]')
            .should('be.enabled');

        cy.get('[data-cy="clearSelectedApprovalsBtn"]')
            .click();
        cy.get('[data-cy="approveBtn"]')
            .should('be.disabled');
        cy.get('[data-cy="rejectBtn"]')
            .should('be.disabled');
    });

    it('request message should be truncated by default', () => {
        cy.createSkill(1, 1, 1, {
            selfReportingType: 'Approval',
            description: 'This is skill 1'
        });
        cy.createSkill(1, 1, 2, {
            selfReportingType: 'Approval',
            description: 'very cool skill 2'
        });
        cy.createSkill(1, 1, 3, {
            selfReportingType: 'Approval',
            description: 'last but not least'
        });
        const msgNoExpandBtn = new Array(60).join('A');
        const msgExpandBtn1 = new Array(66).join('B');
        const msgExpandBtn2 = new Array(251).join('C');
        cy.doReportSkill({
            project: 1,
            skill: 2,
            userId: 'user6',
            date: '2020-09-11 11:00',
            approvalRequestedMsg: msgNoExpandBtn
        });
        cy.doReportSkill({
            project: 1,
            skill: 1,
            userId: 'user5',
            date: '2020-09-12 11:00',
            approvalRequestedMsg: msgExpandBtn1
        });
        cy.doReportSkill({
            project: 1,
            skill: 3,
            userId: 'user4',
            date: '2020-09-13 11:00',
            approvalRequestedMsg: msgExpandBtn2
        });

        cy.visit('/administrator/projects/proj1/self-report');

        cy.get('[data-cy="skillsReportApprovalTable"]')
          .contains('Requested points with the following justification:')
          .should('not.exist');
        cy.get('[data-cy="approvalMessage"]').should('not.exist');

        // open
        cy.get('[data-cy="expandDetailsBtn_skill1"]')
          .click();
        cy.get('[data-cy="skillsReportApprovalTable"]')
          .contains('Requested points with the following justification:')
          .should('exist');
        cy.get('[data-cy="approvalMessage"]')
          .should('have.length', 1)
          .eq(0)
          .should('contain.text', msgExpandBtn1);

        // open
        cy.get('[data-cy="expandDetailsBtn_skill3"]')
          .click();
        cy.get('[data-cy="skillsReportApprovalTable"]')
          .contains('Requested points with the following justification:')
          .should('exist');
        cy.get('[data-cy="approvalMessage"]')
          .should('have.length', 2)
          .eq(0)
          .should('contain.text', msgExpandBtn2);
        cy.get('[data-cy="approvalMessage"]')
          .should('have.length', 2)
          .eq(1)
          .should('contain.text', msgExpandBtn1);

        // close
        cy.get('[data-cy="expandDetailsBtn_skill1"]')
          .click();
        cy.get('[data-cy="skillsReportApprovalTable"]')
          .contains('Requested points with the following justification:')
          .should('exist');
        cy.get('[data-cy="approvalMessage"]')
          .should('have.length', 1)
          .eq(0)
          .should('contain.text', msgExpandBtn2);

        // close
        cy.get('[data-cy="expandDetailsBtn_skill3"]')
          .click();
        cy.get('[data-cy="skillsReportApprovalTable"]')
          .contains('Requested points with the following justification:')
          .should('not.exist');
        cy.get('[data-cy="approvalMessage"]').should('not.exist');
    });

    it('rejection message is limited to configure max size', () => {
        cy.createSkill(1, 1, 1, {
            selfReportingType: 'Approval',
            description: 'This is skill 1'
        });
        cy.createSkill(1, 1, 2, {
            selfReportingType: 'Approval',
            description: 'very cool skill 2'
        });
        cy.createSkill(1, 1, 3, {
            selfReportingType: 'Approval',
            description: 'last but not least'
        });
        const msgNoExpandBtn = new Array(60).join('A');
        cy.doReportSkill({
            project: 1,
            skill: 2,
            userId: 'user6',
            date: '2020-09-11 11:00',
            approvalRequestedMsg: msgNoExpandBtn
        });

        cy.intercept('/admin/projects/proj1/approvals*')
            .as('loadApprovals');
        cy.visit('/administrator/projects/proj1/self-report');
        cy.wait('@loadApprovals');
        cy.get('[data-cy="selectPageOfApprovalsBtn"]')
            .click();
        cy.get('[data-cy=rejectBtn]')
            .click();
        cy.contains('Reject Skills');
        cy.get('[data-cy=rejectionInputMsg]')
            .fill(new Array(500).join('A'));
        cy.get('[data-cy=rejectionInputMsgError]')
            .contains('Message cannot exceed 250 characters')
            .should('be.visible');
        cy.get('[data-cy=confirmRejectionBtn]')
            .should('be.disabled');
        cy.get('[data-cy=rejectionInputMsg]')
            .clear();
        cy.get('[data-cy=confirmRejectionBtn]')
            .should('be.enabled');
        cy.get('[data-cy=rejectionInputMsgError]')
            .should('not.be.visible');
        cy.get('[data-cy=rejectionInputMsg]')
            .type(new Array(50).join('B'));
        cy.get('[data-cy=confirmRejectionBtn]')
            .should('be.enabled');
        cy.get('[data-cy=rejectionInputMsgError]')
            .should('not.be.visible');
    });

    it('approval request skill should be a link to skill details', () => {
        cy.createSkill(1, 1, 1, {
            selfReportingType: 'Approval',
            description: 'This is skill 1'
        });
        cy.createSkill(1, 1, 2, {
            selfReportingType: 'Approval',
            description: 'very cool skill 2'
        });
        cy.createSkill(1, 1, 3, {
            selfReportingType: 'Approval',
            description: 'last but not least'
        });
        cy.reportSkill(1, 2, 'user6', '2020-09-11 11:00');
        cy.reportSkill(1, 1, 'user5', '2020-09-12 11:00');
        cy.reportSkill(1, 3, 'user4', '2020-09-13 11:00');

        cy.visit('/administrator/projects/proj1/self-report');

        cy.get('[data-cy="viewSkillLink_skill1"]')
            .should('have.attr', 'href', '/administrator/projects/proj1/subjects/subj1/skills/skill1/');
        cy.get('[data-cy="viewSkillLink_skill2"]')
            .should('have.attr', 'href', '/administrator/projects/proj1/subjects/subj1/skills/skill2/');
        cy.get('[data-cy="viewSkillLink_skill3"]')
            .should('have.attr', 'href', '/administrator/projects/proj1/subjects/subj1/skills/skill3/');
    });

    it('refresh button should pull from server', () => {
        cy.createSkill(1, 1, 1, { selfReportingType: 'Approval' });
        cy.createSkill(1, 1, 2, { selfReportingType: 'Approval' });
        cy.createSkill(1, 1, 3, { selfReportingType: 'Approval' });
        cy.reportSkill(1, 2, 'user1', '2020-09-12 11:00');

        cy.visit('/administrator/projects/proj1/self-report');

        const tableSelector = '[data-cy="skillsReportApprovalTable"]';
        cy.validateTable(tableSelector, [
            [{
                colIndex: 1,
                value: 'user1'
            }],
        ]);

        cy.reportSkill(1, 2, 'user2', '2020-09-11 11:00');

        cy.get('[data-cy="syncApprovalsBtn"]')
            .click();
        cy.validateTable(tableSelector, [
            [{
                colIndex: 1,
                value: 'user1'
            }],
            [{
                colIndex: 1,
                value: 'user2'
            }],
        ]);
    });

    it('self report stats', () => {
        cy.createSkill(1, 1, 1, { selfReportingType: 'Approval' });
        cy.createSkill(1, 1, 2, { selfReportingType: 'Approval' });
        cy.createSkill(1, 1, 3, { selfReportingType: 'Approval' });
        cy.createSkill(1, 1, 4, { selfReportingType: 'HonorSystem' });
        cy.createSkill(1, 1, 5, { selfReportingType: 'HonorSystem' });
        cy.createSkill(1, 1, 6);

        cy.visit('/administrator/projects/proj1/self-report');

        cy.get('[data-cy="selfReportInfoCardCount_Disabled"]')
            .contains('1');
        cy.get('[data-cy="selfReportInfoCardCount_Approval"]')
            .contains('3');
        cy.get('[data-cy="selfReportInfoCardCount_HonorSystem"]')
            .contains('2');
    });

    it('do not display approval table if no approval configured', () => {
        cy.createSkill(1, 1, 4, { selfReportingType: 'HonorSystem' });
        cy.createSkill(1, 1, 5, { selfReportingType: 'HonorSystem' });
        cy.createSkill(1, 1, 6);

        cy.visit('/administrator/projects/proj1/self-report');

        cy.get('[data-cy="selfReportInfoCardCount_Disabled"]')
            .contains('1');
        cy.get('[data-cy="selfReportInfoCardCount_Approval"]')
            .contains('0');
        cy.get('[data-cy="selfReportInfoCardCount_HonorSystem"]')
            .contains('2');

        cy.get('[data-cy="noApprovalTableMsg"]')
            .contains('No Skills Require Approval');
        cy.get('[data-cy="skillsReportApprovalTable"]')
            .should('not.exist');
    });

    it('warn if email service is not configured', () => {
        cy.intercept('/public/isFeatureSupported?feature=emailservice', 'false');

        cy.createSkill(1, 1, 4, { selfReportingType: 'Approval' });
        cy.createSkill(1, 1, 5, { selfReportingType: 'HonorSystem' });
        cy.createSkill(1, 1, 6);

        cy.visit('/administrator/projects/proj1/self-report');

        cy.get('[data-cy="selfReport_emailServiceWarning"]')
            .contains('Please note that email notifications are currently disabled');
    });

    it('email service warning should NOT be displayed if there 0 Approval required Self Reporting skills', () => {
        cy.createSkill(1, 1, 4, { selfReportingType: 'HonorSystem' });
        cy.createSkill(1, 1, 5, { selfReportingType: 'HonorSystem' });
        cy.createSkill(1, 1, 6);

        cy.visit('/administrator/projects/proj1/self-report');

        cy.get('[data-cy="selfReport_emailServiceWarning"]')
            .should('not.exist');
    });

    it('no email warning when email service is configured', () => {
        cy.createSkill(1, 1, 4, { selfReportingType: 'Approval' });
        cy.createSkill(1, 1, 5, { selfReportingType: 'HonorSystem' });
        cy.createSkill(1, 1, 6);

        cy.logout();
        cy.fixture('vars.json')
            .then((vars) => {
                cy.login(vars.rootUser, vars.defaultPass);
            });
        cy.request({
            method: 'POST',
            url: '/root/saveEmailSettings',
            body: {
                host: 'localhost',
                port: 1026,
                'protocol': 'smtp'
            },
        });

        cy.request({
            method: 'POST',
            url: '/root/saveSystemSettings',
            body: {
                publicUrl: 'http://localhost:8082/',
                resetTokenExpiration: 'PT2H',
                fromEmail: 'noreploy@skilltreeemail.org',
            }
        });
        cy.logout();
        cy.fixture('vars.json')
            .then((vars) => {
                cy.login(vars.defaultUser, vars.defaultPass);
            });

        cy.visit('/administrator/projects/proj1/self-report');

        cy.get('[data-cy="selfReport_emailServiceWarning"]')
            .should('not.exist');
    });

    it('should be able to unsubscribe from approval request emails', () => {
        cy.createSkill(1, 1, 1, {
            selfReportingType: 'Approval',
            pointIncrement: '100'
        });
        cy.createSkill(1, 1, 2, {
            selfReportingType: 'Approval',
            pointIncrement: '220'
        });
        cy.createSkill(1, 1, 3, {
            selfReportingType: 'Approval',
            pointIncrement: '180'
        });
        cy.reportSkill(1, 1, 'user1Good@skills.org', '2020-09-12 11:00');
        cy.reportSkill(1, 2, 'user2Good@skills.org', '2020-09-13 11:00');
        cy.reportSkill(1, 3, 'user3Good@skills.org', '2020-09-14 11:00');
        cy.createProject(2);
        cy.createSubject(2, 1);
        cy.createSkill(2, 1, 1, {
            selfReportingType: 'Approval',
            pointIncrement: '100'
        });
        cy.reportSkill(2, 1, 'user1Good@skills.org', '2020-09-12 11:00');

        cy.intercept('GET', '/admin/projects/**/approvalEmails/isSubscribed')
            .as('isSubscribed');
        cy.intercept('POST', '/admin/projects/proj1/approvalEmails/unsubscribe')
            .as('unsubscribe');
        cy.intercept('POST', '/admin/projects/proj1/approvalEmails/subscribe')
            .as('subscribe');
        cy.intercept('/public/isFeatureSupported?feature=emailservice', 'true');

        cy.visit('/administrator/projects/proj1/self-report');
        cy.wait('@isSubscribed');
        cy.contains('Subscribed')
            .should('be.visible');
        cy.get('[data-cy=unsubscribeSwitch]')
            .should('be.checked');
        cy.get('[data-cy=unsubscribeContainer] .custom-control-label')
            .click();
        cy.wait('@unsubscribe');
        cy.contains('Unsubscribed')
            .should('be.visible');
        cy.get('[data-cy=unsubscribeSwitch]')
            .should('not.be.checked');

        //setting should be per project
        cy.visit('/administrator/projects/proj2/self-report');
        cy.wait('@isSubscribed');
        cy.contains('Subscribed')
            .should('be.visible');
        cy.get('[data-cy=unsubscribeSwitch]')
            .should('be.checked');

        cy.visit('/administrator/projects/proj1/self-report');
        cy.wait('@isSubscribed');
        cy.contains('Unsubscribed')
            .should('be.visible');
        cy.get('[data-cy=unsubscribeSwitch]')
            .should('not.be.checked');
    });

    it('approval request email subscription toggle should not be visible if email is not configured', () => {
        cy.createSkill(1, 1, 1, {
            selfReportingType: 'Approval',
            pointIncrement: '100'
        });
        cy.createSkill(1, 1, 2, {
            selfReportingType: 'Approval',
            pointIncrement: '220'
        });
        cy.createSkill(1, 1, 3, {
            selfReportingType: 'Approval',
            pointIncrement: '180'
        });
        cy.reportSkill(1, 1, 'user1Good@skills.org', '2020-09-12 11:00');
        cy.reportSkill(1, 2, 'user2Good@skills.org', '2020-09-13 11:00');
        cy.reportSkill(1, 3, 'user3Good@skills.org', '2020-09-14 11:00');
        cy.intercept('/public/isFeatureSupported?feature=emailservice', 'false');

        cy.visit('/administrator/projects/proj1/self-report');
        cy.get('[data-cy=unsubscribeContainer]')
            .should('not.exist');
    });

});

