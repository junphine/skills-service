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

    it('community projected project cannot export skills to the catalog', () => {
        cy.createProject(1, {enableProtectedUserCommunity: true})
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1)

        cy.visit('/administrator/projects/proj1/subjects/subj1')
        cy.get('[data-cy="skillSelect-skill1"]').click({ force: true });
        cy.get('[data-cy="skillActionsBtn"] button').should('be.enabled');
        cy.get('[data-cy="skillActionsNumSelected"]').should('have.text', '1');

        cy.get('[data-cy="skillActionsBtn"] button').click();
        cy.get('[data-cy="skillExportToCatalogBtn"]').click()

        cy.get('[data-cy="userCommunityRestrictedWarning"]').contains('restricted to Divine Dragon')
        cy.get('[data-cy="okButton"]').should('be.enabled')
        cy.get('[data-cy="exportToCatalogButton"]').should('not.exist')
    });

    it('community projected project cannot share skills for cross-project dependencies', () => {
        cy.createProject(1, {enableProtectedUserCommunity: true})

        cy.visit('/administrator/projects/proj1/dependencies')
        cy.get('[data-cy="restrictedUserCommunityWarning"]').contains('is restricted to Divine Dragon')
        cy.get('[data-cy="shareButton"]').should('not.exist')
    });

});
