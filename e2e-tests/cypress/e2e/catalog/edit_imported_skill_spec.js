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

describe('Edit Imported Skill Tests', () => {

    beforeEach(() => {
        cy.createProject(1);
        cy.createSubject(1, 1);
    });

    const tableSelector = '[data-cy="skillsTable"]';

    it('edit point increment of an imported skill', () => {
        cy.createSkill(1, 1, 1);
        cy.createSkill(1, 1, 2);
        cy.exportSkillToCatalog(1, 1, 1);
        cy.exportSkillToCatalog(1, 1, 2);

        cy.createProject(2);
        cy.createSubject(2, 1);

        cy.importSkillFromCatalog(2, 1, 1, 1);
        cy.wait(1000);
        cy.importSkillFromCatalog(2, 1, 1, 2);

        cy.visit('/administrator/projects/proj2/subjects/subj1');
        cy.get('[data-cy="editSkillButton_skill1"]')
            .click();
        cy.contains('You can change the Point Increment');
        cy.get('[data-cy="skillPointIncrement"]')
            .should('have.value', '100');
        cy.get('[data-cy="skillPointIncrement"]')
            .type('1');
        cy.get('[data-cy="saveSkillButton"]')
            .click();

        cy.get(`[data-cy="skillsTable-additionalColumns"]`)
            .contains('Points')
            .click();
        cy.validateTable(tableSelector, [
            [{
                colIndex: 0,
                value: 'Very Great Skill 2'
            }, {
                colIndex: 3,
                value: '200'
            }],
            [{
                colIndex: 0,
                value: 'Very Great Skill 1'
            }, {
                colIndex: 3,
                value: '2,002'
            }],
        ], 5, false, null, false);
        cy.get(`${tableSelector} tbody tr`)
            .should('have.length', 2);

        cy.get('[data-cy="editSkillButton_skill1"]')
            .click();
        cy.contains('You can change the Point Increment');
        cy.get('[data-cy="skillPointIncrement"]')
            .should('have.value', '1001');

        // refresh re-validate
        cy.visit('/administrator/projects/proj2/subjects/subj1');

        cy.get(`[data-cy="skillsTable-additionalColumns"]`)
            .contains('Points')
            .click();
        cy.validateTable(tableSelector, [
            [{
                colIndex: 0,
                value: 'Very Great Skill 2'
            }, {
                colIndex: 3,
                value: '200'
            }],
            [{
                colIndex: 0,
                value: 'Very Great Skill 1'
            }, {
                colIndex: 3,
                value: '2,002'
            }],
        ], 5, false, null, false);
        cy.get(`${tableSelector} tbody tr`)
            .should('have.length', 2);

        cy.get('[data-cy="editSkillButton_skill1"]')
            .click();
        cy.contains('You can change the Point Increment');
        cy.get('[data-cy="skillPointIncrement"]')
            .should('have.value', '1001');
    });

    it('point increment input validation', () => {
        cy.createSkill(1, 1, 1);
        cy.createSkill(1, 1, 2);
        cy.exportSkillToCatalog(1, 1, 1);
        cy.exportSkillToCatalog(1, 1, 2);

        cy.createProject(2);
        cy.createSubject(2, 1);

        cy.importSkillFromCatalog(2, 1, 1, 1);
        cy.importSkillFromCatalog(2, 1, 1, 2);

        cy.visit('/administrator/projects/proj2/subjects/subj1');
        cy.get('[data-cy="editSkillButton_skill1"]')
            .click();
        cy.get('[data-cy="skillPointIncrement"]')
            .should('have.value', '100');

        cy.get('[data-cy="skillPointIncrement"]')
            .type('a');
        cy.get('[data-cy="skillPointIncrementError"]')
            .contains('Point Increment may only contain numeric characters.');
        cy.get('[data-cy="saveSkillButton"]')
            .should('be.disabled');

        cy.get('[data-cy="skillPointIncrement"]')
            .clear();
        cy.get('[data-cy="skillPointIncrementError"]')
            .contains('Point Increment is required');
        cy.get('[data-cy="saveSkillButton"]')
            .should('be.disabled');

        cy.get('[data-cy="skillPointIncrement"]')
            .type('10001');
        cy.get('[data-cy="skillPointIncrementError"]')
            .contains('Point Increment cannot exceed 10000');
        cy.get('[data-cy="saveSkillButton"]')
            .should('be.disabled');

        cy.get('[data-cy="skillPointIncrement"]')
            .clear()
            .type('10000');
        cy.get('[data-cy="skillPointIncrementError"]')
            .should('have.value', '');
        cy.get('[data-cy="saveSkillButton"]')
            .should('be.enabled');
    });

    it('cancel edit', () => {
        cy.createSkill(1, 1, 1);
        cy.createSkill(1, 1, 2);
        cy.exportSkillToCatalog(1, 1, 1);
        cy.exportSkillToCatalog(1, 1, 2);

        cy.createProject(2);
        cy.createSubject(2, 1);

        cy.importSkillFromCatalog(2, 1, 1, 1);
        cy.wait(1000);
        cy.importSkillFromCatalog(2, 1, 1, 2);

        cy.visit('/administrator/projects/proj2/subjects/subj1');
        cy.get('[data-cy="editSkillButton_skill1"]')
            .click();
        cy.get('[data-cy="skillPointIncrement"]')
            .type('1');
        cy.get('[data-cy="closeSkillButton"]')
            .click();
        cy.get(`[data-cy="skillsTable-additionalColumns"]`)
            .contains('Points')
            .click();
        cy.validateTable(tableSelector, [
            [{
                colIndex: 0,
                value: 'Very Great Skill 2'
            }, {
                colIndex: 3,
                value: '200'
            }],
            [{
                colIndex: 0,
                value: 'Very Great Skill 1'
            }, {
                colIndex: 3,
                value: '200'
            }],
        ], 5, false, null, false);
        cy.get(`${tableSelector} tbody tr`)
            .should('have.length', 2);

        cy.get('[data-cy="editSkillButton_skill1"]')
            .click();
        cy.get('[data-cy="skillPointIncrement"]')
            .type('1');
        cy.get('[class="modal-content"] [aria-label="Close"]')
            .click();
        cy.validateTable(tableSelector, [
            [{
                colIndex: 0,
                value: 'Very Great Skill 2'
            }, {
                colIndex: 3,
                value: '200'
            }],
            [{
                colIndex: 0,
                value: 'Very Great Skill 1'
            }, {
                colIndex: 3,
                value: '200'
            }],
        ], 5, false, null, false);
        cy.get(`${tableSelector} tbody tr`)
            .should('have.length', 2);
    });

    it('edit after finalizing', () => {
        cy.createSkill(1, 1, 1);
        cy.createSkill(1, 1, 2);
        cy.exportSkillToCatalog(1, 1, 1);
        cy.exportSkillToCatalog(1, 1, 2);

        cy.createProject(2);
        cy.createSubject(2, 1);

        cy.importSkillFromCatalog(2, 1, 1, 1);
        cy.wait(1000);
        cy.importSkillFromCatalog(2, 1, 1, 2);

        cy.finalizeCatalogImport(2);

        cy.visit('/administrator/projects/proj2/subjects/subj1');
        cy.get('[data-cy="editSkillButton_skill1"]')
            .click();
        cy.contains('You can change the Point Increment');
        cy.get('[data-cy="skillPointIncrement"]')
            .should('have.value', '100');
        cy.get('[data-cy="skillPointIncrement"]')
            .type('1');
        cy.get('[data-cy="saveSkillButton"]')
            .click();

        cy.get(`[data-cy="skillsTable-additionalColumns"]`)
            .contains('Points')
            .click();
        cy.validateTable(tableSelector, [
            [{
                colIndex: 0,
                value: 'Very Great Skill 2'
            }, {
                colIndex: 3,
                value: '200'
            }],
            [{
                colIndex: 0,
                value: 'Very Great Skill 1'
            }, {
                colIndex: 3,
                value: '2,002'
            }],
        ], 5, false, null, false);
        cy.get(`${tableSelector} tbody tr`)
            .should('have.length', 2);
        cy.get('[data-cy="editSkillButton_skill1"]')
            .click();
        cy.contains('You can change the Point Increment');
        cy.get('[data-cy="skillPointIncrement"]')
            .should('have.value', '1001');

        // drill-down and validate the points
        cy.get('[data-cy="closeSkillButton"]')
            .click();
        cy.get('[data-cy="manageSkillBtn_skill1"]')
            .click();
        cy.get('[data-cy="skillOverviewTotalpoints"]')
            .contains('2,002');
    });

    it('changing pointIncrement updates point metrics if skill is finalized', () => {
        cy.createSkill(1, 1, 1);
        cy.createSkill(1, 1, 2);
        cy.exportSkillToCatalog(1, 1, 1);
        cy.exportSkillToCatalog(1, 1, 2);

        cy.createProject(2);
        cy.createSubject(2, 1);

        cy.importSkillFromCatalog(2, 1, 1, 1);
        cy.importSkillFromCatalog(2, 1, 1, 2);

        cy.finalizeCatalogImport(2);

        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get('[data-cy="pageHeaderStat_Points"] [data-cy="statValue"]')
            .should('have.text', '400');
        cy.get('[data-cy="editSkillButton_skill1"]')
            .click();
        cy.get('[data-cy="skillPointIncrement"]')
            .should('have.value', '100');
        cy.get('[data-cy="skillPointIncrement"]')
            .clear()
            .type('33');
        cy.get('[data-cy="saveSkillButton"]')
            .click();
        cy.get('[data-cy="pageHeaderStat_Points"] [data-cy="statValue"]')
            .should('have.text', '266');
    });

    it('changing pointIncrement does not update point metrics if skill is was not finalized', () => {
        cy.intercept('PATCH', '/admin/projects/proj2/import/skills/skill2')
            .as('updateImportedSkill');
        cy.createSkill(1, 1, 1);
        cy.createSkill(1, 1, 2);
        cy.exportSkillToCatalog(1, 1, 1);
        cy.exportSkillToCatalog(1, 1, 2);

        cy.createProject(2);
        cy.createSubject(2, 1);

        cy.importSkillFromCatalog(2, 1, 1, 1);
        cy.finalizeCatalogImport(2);

        cy.importSkillFromCatalog(2, 1, 1, 2);

        cy.visit('/administrator/projects/proj2/subjects/subj1');
        cy.get('[data-cy="pageHeaderStat_Points"] [data-cy="statValue"]')
            .should('have.text', '200');
        cy.get('[data-cy="editSkillButton_skill2"]')
            .click();
        cy.get('[data-cy="skillPointIncrement"]')
            .should('have.value', '100');
        cy.get('[data-cy="skillPointIncrement"]')
            .clear()
            .type('33');
        cy.get('[data-cy="saveSkillButton"]')
            .click();
        cy.wait('@updateImportedSkill');
        cy.get('[data-cy="pageHeaderStat_Points"] [data-cy="statValue"]')
            .should('have.text', '200');

    });

});



