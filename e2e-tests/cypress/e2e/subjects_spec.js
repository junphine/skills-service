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
// const attachFiles = require('cypress-form-data-with-file-upload');
describe('Subjects Tests', () => {

    beforeEach(() => {
        cy.request('POST', '/app/projects/proj1', {
            projectId: 'proj1',
            name: "proj1"
        })
    });

    it('Close level dialog', () => {
        cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            name: "Subject 1"
        });
        cy.intercept({
            method: 'GET',
            url: '/admin/projects/proj1/subjects/subj1'
        }).as('loadSubject');

        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.wait('@loadSubject');

        cy.contains('Levels').click();
        cy.contains('Add Next').click();
        cy.get('[data-cy=cancelLevel]').click();
        cy.get('[data-cy=cancelLevel]').should('not.exist');
    });

    it('create subject with special chars', () => {
        const expectedId = 'LotsofspecialPcharsSubject';
        const providedName = "!L@o#t$s of %s^p&e*c(i)/?#a_l++_|}{P c'ha'rs";
        cy.intercept('POST', `/admin/projects/proj1/subjects/${expectedId}`).as('postNewSubject');
        cy.intercept('POST', '/admin/projects/proj1/subjectNameExists').as('nameExists');
        cy.intercept('GET', '/admin/projects/proj1/subjects').as('loadSubjects');

        cy.visit('/administrator/projects/proj1');
        cy.wait('@loadSubjects');
        cy.clickButton('Subject');

        cy.get('#subjName').type(providedName);
        cy.wait('@nameExists');
        cy.getIdField().should('have.value', expectedId);

        cy.clickSave();
        cy.wait('@postNewSubject');

        cy.contains('ID: Lotsofspecial')
    });

  it('create subject with ampersand', () => {
    const providedName = "I am & a subject";
    cy.intercept('POST', `/admin/projects/proj1/subjects/**`).as('postNewSubject');
    cy.intercept('POST', '/admin/projects/proj1/subjectNameExists').as('nameExists');
    cy.intercept('GET', '/admin/projects/proj1/subjects').as('loadSubjects');

    cy.visit('/administrator/projects/proj1');
    cy.wait('@loadSubjects');
    cy.clickButton('Subject');

    cy.get('#subjName').type(providedName);
    cy.wait('@nameExists');

    cy.clickSave();
    cy.wait('@postNewSubject');

    cy.contains('I am & a subject')
  });

    it('create subject using enter key', () => {
        const expectedId = 'LotsofspecialPcharsSubject';
        const providedName = "!L@o#t$s of %s^p&e*c(i)/?#a_l++_|}{P c'ha'rs";
        cy.intercept('POST', `/admin/projects/proj1/subjects/${expectedId}`).as('postNewSubject');
        cy.intercept('POST', '/admin/projects/proj1/subjectNameExists').as('nameExists');
        cy.intercept('GET', '/admin/projects/proj1/subjects').as('loadSubjects');

        cy.visit('/administrator/projects/proj1');
        cy.wait('@loadSubjects');
        cy.clickButton('Subject');

        cy.get('#subjName').type(providedName);
        cy.wait('@nameExists');
        cy.getIdField().should('have.value', expectedId);

        cy.get('#subjName').type('{enter}');
        cy.wait('@postNewSubject');

        cy.contains('ID: Lotsofspecial')
    });

    it('Open new subject dialog with enter key', () => {
        cy.intercept('GET', '/admin/projects/proj1/subjects').as('loadSubjects');

        cy.visit('/administrator/projects/proj1');
        cy.wait('@loadSubjects');

        cy.get('[data-cy=btn_Subjects]').focus().realPress("Enter");
        cy.get('[data-cy="subjectNameInput"]').should('have.value', '');
        cy.get('#subjectNameError').should('have.value', '');
        cy.get('[data-cy=closeSubjectButton]').click();
        cy.get('[data-cy="titleLink"]').should('not.exist');
    });

    it('Open edit subject dialog using enter key', function () {
        const expectedId = 'testSubject';
        const providedName = "test";
        cy.intercept('POST', `/admin/projects/proj1/subjects/${expectedId}`).as('postNewSubject');
        cy.intercept('POST', '/admin/projects/proj1/subjectNameExists').as('nameExists');
        cy.intercept('GET', '/admin/projects/proj1/subjects').as('loadSubjects');

        cy.visit('/administrator/projects/proj1');
        cy.wait('@loadSubjects');
        cy.clickButton('Subject');

        cy.get('#subjName').type(providedName);
        cy.wait('@nameExists');
        cy.getIdField().should('have.value', expectedId);

        cy.get('#subjName').type('{enter}');
        cy.wait('@postNewSubject');

        cy.contains('ID: test')

        cy.get('[data-cy=editBtn]').focus();
        cy.realPress("Enter");

        cy.get('[data-cy="subjectNameInput"]').should('have.value', 'test');
        cy.get('#subjectNameError').should('have.value', '');
        cy.get('[data-cy=closeSubjectButton]').click();
        cy.contains('test');
        cy.contains('ID: test');
    });

    it('close subject dialog', () => {
        cy.intercept('GET', '/admin/projects/proj1/subjects').as('loadSubjects');

        cy.visit('/administrator/projects/proj1');
        cy.wait('@loadSubjects');
        cy.clickButton('Subject');
        cy.get('[data-cy=closeSubjectButton]').click();
        cy.get('[data-cy=closeSubjectButton]').should('not.exist');
    });

    it('name causes id to fail validation', () => {
        cy.intercept('GET', '/public/config', (req) => {
            req.reply((res) => {
                const conf = res.body;
                conf.maxIdLength = 50;
                res.send(conf);
            });
        }).as('loadConfig')
        cy.intercept('GET', '/admin/projects/proj1/subjects').as('loadSubjects');
        cy.intercept('POST', '/admin/projects/proj1/subjectNameExists').as('nameExists');

        cy.visit('/administrator/projects/proj1');
        cy.wait('@loadSubjects');
        cy.clickButton('Subject');

        // name causes id to be too long
        const msg = 'Subject ID cannot exceed 50 characters.';
        const validNameButInvalidId = Array(44).fill('a').join('');
        cy.get('#subjName').click();
        cy.get('#subjName').invoke('val', validNameButInvalidId).trigger('input');
        cy.get('[data-cy=idError]').contains(msg).should('be.visible');
        cy.get('[data-cy=saveSubjectButton]').should('be.disabled');
        cy.get('#subjName').type('{backspace}');
        cy.get('[data-cy=idError]').should('be.empty');
        cy.get('[data-cy=saveSubjectButton]').should('be.enabled');
    });

    it('helpUrl must be valid', () => {
        cy.intercept('GET', '/admin/projects/proj1/subjects').as('loadSubjects');
        cy.intercept('POST', '/admin/projects/proj1/subjectNameExists').as('nameExists');
        cy.intercept('POST', '/api/validation/url').as('customUrlValidation');

        cy.visit('/administrator/projects/proj1');
        cy.wait('@loadSubjects');
        cy.clickButton('Subject');
        //helpUrl
        cy.get('[data-cy=subjectNameInput]').type('A Subject');
        cy.get('[data-cy=skillHelpUrl]').clear().type('javascript:alert("uh oh");');
        cy.get('[data-cy=skillHelpUrlError]').should('be.visible');
        cy.get('[data-cy=skillHelpUrlError]').should('have.text', 'Help URL/Path must start with "/" or "http(s)"');
        cy.get('[data-cy=saveSubjectButton]').should('be.disabled');
        cy.get('[data-cy=skillHelpUrl]').clear().type('/foo?p1=v1&p2=v2');
        cy.get('[data-cy=skillHelpUrlError]').should('not.be.visible');
        cy.get('[data-cy=skillHelpUrl]').clear().type('http://foo.bar?p1=v1&p2=v2');
        cy.get('[data-cy=skillHelpUrlError]').should('not.be.visible');
        cy.get('[data-cy=skillHelpUrl]').clear().type('https://foo.bar?p1=v1&p2=v2');
        cy.get('[data-cy=skillHelpUrlError]').should('not.be.visible');

        cy.get('[data-cy=skillHelpUrl]').clear().type('https://');
        cy.wait('@customUrlValidation');
        cy.get('[data-cy=skillHelpUrlError]').should('be.visible');
        cy.get('[data-cy=saveSubjectButton]').should('be.disabled');

        cy.get('[data-cy=skillHelpUrl]').clear().type('https://---??..??##');
        cy.wait('@customUrlValidation');
        cy.get('[data-cy=skillHelpUrlError]').should('be.visible');
        cy.get('[data-cy=saveSubjectButton]').should('be.disabled');
        // trailing space should work now
        cy.get('[data-cy=skillHelpUrl]').clear().type('https://foo.bar?p1=v1&p2=v2 ');
        cy.wait('@customUrlValidation');
        cy.get('[data-cy=skillHelpUrlError]').should('not.be.visible');
        cy.get('[data-cy=saveSubjectButton]').should('be.enabled');
    });

    it('select font awesome icon', () => {
        cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            name: "Subject 1"
        });

        cy.visit('/administrator/projects/proj1/');
        cy.get('[data-cy="subjectCard-subj1"] [data-cy="editBtn"]').click();

        cy.get('div.modal-content .text-primary i.fa-question-circle').click();
        cy.get('a.nav-link').contains('Font Awesome Free').click();
        cy.wait(1500);
        cy.get('[role=tabpanel][aria-hidden=false]').should('be.visible');
        cy.get('[data-cy=fontAwesomeVirtualList]').scrollTo(0,540);
        cy.get('div[role=group] .icon-item>a:visible', {timeout:10000}).should('be.visible').last().then(($el)=> {
            const clazz = $el.attr('data-cy');
            cy.get(`[data-cy="${clazz}"]`).should('have.length', '1').click({force:true});
            cy.get('[data-cy=saveSubjectButton]').scrollIntoView().should('be.visible').click();
            cy.get('.preview-card-title').contains('Subject 1').should('be.visible');
            const classes = clazz.split(' ');
            let iconClass = classes[classes.length-1];
            iconClass = iconClass.replace(/-link$/, '')
            cy.get(`i.${iconClass}`).should('be.visible');
        })
    });

    it('select material icon', () => {
        cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            name: "Subject 1"
        });

        cy.visit('/administrator/projects/proj1/');
        cy.get('[data-cy="subjectCard-subj1"] [data-cy="editBtn"]').click();
        cy.get('div.modal-content .text-primary i.fa-question-circle').click();
        cy.get('a.nav-link').contains('Material').click();
        cy.wait(2500);
        cy.get('[role=tabpanel][aria-hidden=false]').should('be.visible');
        cy.get('[data-cy=materialVirtualList]').scrollTo(0,540);
        cy.get('div[role=group] .icon-item>a:visible',{timeout:1000}).last().then(($el)=> {
            const clazz = $el.attr('data-cy');
            cy.get(`[data-cy="${clazz}"]`).should('have.length', '1').click({ force: true });
            cy.get('[data-cy=saveSubjectButton]').scrollIntoView().should('be.visible').click();
            cy.get('.preview-card-title').contains('Subject 1').should('be.visible');
            const classes = clazz.split(' ');
            let iconClass = classes[classes.length - 1];
            iconClass = iconClass.replace(/-link$/, '')
            cy.get(`i.${iconClass}`).should('be.visible');
        });
    });

    it('search icons', () => {
        cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            name: "Subject 1"
        });

        cy.visit('/administrator/projects/proj1/');
        cy.get('[data-cy="subjectCard-subj1"] [data-cy="editBtn"]').click();
        cy.get('div.modal-content .text-primary i.fa-question-circle').click();
        cy.get('[data-cy=icon-search]').type('run');
        cy.get('.fas.fa-running').should('be.visible');
        //filter should persist between tab changes
        cy.get('a.nav-link').contains('Material').click();
        cy.get('.mi.mi-directions-run').should('be.visible');

        cy.get('a.nav-link').contains('Font Awesome Free').click();
        cy.get('.fas.fa-running').should('be.visible');

        //filter should not persist when icon manager is re-opened
        cy.contains('Cancel Icon Selection').click();
        cy.get('div.modal-content .text-primary i.fa-question-circle').click();
        cy.get('[data-cy=icon-search]').should('have.value', '');
        cy.get('i.fas.fa-ad').should('be.visible');
        cy.get('a.nav-link').contains('Material').click();
        cy.get('i.mi.mi-3d-rotation').should('be.visible');
    });

    it('upload custom icon', () => {
        cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            name: "Subject 1"
        });


        // TODO - waiting on https://github.com/cypress-io/cypress/issues/1647
        // cy.intercept('/admin/projects/proj1/icons/upload').as('uploadIcon');

        cy.intercept({
            method: 'POST',
            url: '/admin/projects/proj1/icons/upload',
        }).as('uploadIcon');

        cy.visit('/administrator/projects/proj1/');

        cy.get('[data-cy="subjectCard-subj1"] [data-cy="editBtn"]').click();

        cy.get('div.modal-content .text-primary i.fa-question-circle').click();

        cy.get('a.nav-link').contains('Custom').click();

        const filename = 'valid_icon.png';
        cy.get('input[type=file]').attachFile(filename);
        cy.wait('@uploadIcon')

        cy.get('#subj1___BV_modal_body_ .proj1-validiconpng');
    });


    it('upload custom icon - invalid mime type client validation', () => {
        cy.intercept('/app/projects/proj1/customIcons').as('getCustomIcons')
        cy.intercept('/api/projects/proj1/customIconCss').as('getCustomIconsCss')

        cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            name: "Subject 1"
        });

        cy.visit('/administrator/projects/proj1/');
        cy.wait('@getCustomIconsCss')

        cy.get('[data-cy="subjectCard-subj1"] [data-cy="editBtn"]').click();

        cy.get('div.modal-content .text-primary i.fa-question-circle').click();
        cy.wait('@getCustomIcons')

        cy.get('a.nav-link').contains('Custom').click();
        cy.get('[data-cy="customIconUpload"]').contains('Drag your file here to upload')
        cy.wait(2000)

        const filename = 'invalid_file.txt';
        cy.get('input[type=file]').attachFile(filename);

        cy.get('[data-cy="customIconErr"]').contains('File is not an image format');
    });

    it('upload custom icon - server side error', () => {
        cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            name: "Subject 1"
        });

        cy.intercept('POST', '/admin/projects/proj1/icons/upload', {
            statusCode: 400,
            response: {explanation: 'Something bad'}
        }).as('addAdmin');

        cy.visit('/administrator/projects/proj1/');

        cy.get('[data-cy="subjectCard-subj1"] [data-cy="editBtn"]').click();

        cy.get('div.modal-content .text-primary i.fa-question-circle').click();

        cy.get('a.nav-link').contains('Custom').click();

        const filename = 'valid_icon.png';
        cy.get('input[type=file]').attachFile(filename);

        cy.get('.toast-body').contains('Encountered error when uploading');
    });

    it('new subject button should retain focus after dialog closes', () => {
        cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            name: "Subject 1"
        });

        cy.visit('/administrator/projects/proj1');
        cy.get('[data-cy="manageBtn_subj1"')
        cy.get('[data-cy="btn_Subjects"]').click();
        cy.get('[data-cy=closeSubjectButton]').click();
        cy.get('[data-cy="btn_Subjects"]').should('have.focus');

        cy.get('[data-cy="btn_Subjects"]').click();
        cy.get('[data-cy="closeSubjectButton"]').click()
        cy.get('[data-cy="btn_Subjects"]').should('have.focus');

        cy.get('[data-cy="btn_Subjects"]').click();
        cy.get('[aria-label=Close]').click();
        cy.get('[data-cy="btn_Subjects"]').should('have.focus');

        cy.get('[data-cy="btn_Subjects"]').click();
        cy.get('[data-cy=subjectNameInput]').type('foobarbaz');
        cy.get('[data-cy=saveSubjectButton]').click();
        cy.get('[data-cy="btn_Subjects"]').should('have.focus');
    });

    it('focus should be returned to subject edit button', () => {
        cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            name: "Subject 1"
        });

        cy.request('POST', '/admin/projects/proj1/subjects/subj2', {
            projectId: 'proj1',
            subjectId: 'subj2',
            name: "Subject 2"
        });
        cy.intercept({
            method: 'POST',
            url: '/admin/projects/proj1/subjects/subj1'
        }).as('saveSubject');
        cy.intercept({
            method: 'POST',
            url: '/admin/projects/proj1/subjects/subj2'
        }).as('saveSubject2');

        cy.intercept({
            method: 'GET',
            url: '/admin/projects/proj1/subjects/subj1'
        }).as('loadSubject');
        cy.intercept({
            method: 'GET',
            url: '/admin/projects/proj1/subjects/subj2'
        }).as('loadSubject2');

        cy.visit('/administrator/projects/proj1');

        cy.get('[data-cy="subjectCard-subj1"] [data-cy="editBtn"]').click();
        cy.get('[data-cy=subjectNameInput]').should('be.visible');
        cy.get('[data-cy="closeSubjectButton"]').click()
        cy.get('[data-cy="subjectCard-subj1"] [data-cy="editBtn"]').should('be.enabled')
        cy.get('[data-cy="subjectCard-subj1"] [data-cy="editBtn"]').should('have.focus');

        cy.get('[data-cy="subjectCard-subj1"] [data-cy="editBtn"]').click();
        cy.get('[data-cy=closeSubjectButton]').click();
        cy.get('[data-cy="subjectCard-subj1"] [data-cy="editBtn"]').should('be.enabled')
        cy.get('[data-cy="subjectCard-subj1"] [data-cy="editBtn"]').should('have.focus');

        cy.get('[data-cy="subjectCard-subj1"] [data-cy="editBtn"]').click();
        cy.get('[data-cy=subjectNameInput]').type('test 123');
        cy.get('[data-cy=saveSubjectButton]').click();
        cy.wait('@saveSubject');
        cy.wait('@loadSubject');
        cy.get('[data-cy="subjectCard-subj1"] [data-cy="editBtn"]').should('be.enabled')
        cy.get('[data-cy="subjectCard-subj1"] [data-cy="editBtn"]').should('have.focus');

        cy.get('[data-cy="subjectCard-subj1"] [data-cy="editBtn"]').click();
        cy.get('[data-cy="markdownEditorInput"]').should('be.visible')
        cy.get('[aria-label=Close]').click();
        cy.get('[data-cy="subjectCard-subj1"] [data-cy="editBtn"]').should('be.enabled')
        cy.get('[data-cy="subjectCard-subj1"] [data-cy="editBtn"]').should('have.focus');

        //subject 2
        cy.get('[data-cy="subjectCard-subj2"] [data-cy="editBtn"]').click();
        cy.get('[data-cy=closeSubjectButton]').click();
        cy.get('[data-cy="subjectCard-subj2"] [data-cy="editBtn"]').should('be.enabled')
        cy.get('[data-cy="subjectCard-subj2"] [data-cy="editBtn"]').should('have.focus');

        cy.get('[data-cy="subjectCard-subj2"] [data-cy="editBtn"]').click();
        cy.get('[data-cy=subjectNameInput]').type('test 123');
        cy.get('[data-cy=saveSubjectButton]').click();
        cy.wait('@saveSubject2');
        cy.wait('@loadSubject2');
        cy.get('[data-cy="subjectCard-subj2"] [data-cy="editBtn"]').should('be.enabled')
        cy.get('[data-cy="subjectCard-subj2"] [data-cy="editBtn"]').should('have.focus');

        cy.get('[data-cy="subjectCard-subj2"] [data-cy="editBtn"]').click();
        cy.get('[aria-label=Close]').click();
        cy.get('[data-cy="subjectCard-subj2"] [data-cy="editBtn"]').should('be.enabled')
        cy.get('[data-cy="subjectCard-subj2"] [data-cy="editBtn"]').should('have.focus');
    });

    it('new level dialog should return focus to new level button', () => {
        cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            name: "Subject 1"
        });
        cy.intercept('GET', '/admin/projects/proj1/subjects/subj1').as('loadSubject');
        cy.intercept('PUT', '/admin/projects/proj1/subjects/subj1/levels/edit/*').as('saveLevel');

        cy.intercept('GET', '/admin/projects/proj1/subjects/subj1/levels').as('loadLevels');

        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.wait('@loadSubject');

        cy.contains('Levels').click();
        cy.get('[data-cy=addLevel]').click();
        cy.get('[data-cy=cancelLevel]').click();
        cy.get('[data-cy=addLevel]').should('have.focus');

        // cy.get('[data-cy=addLevel]').click();
        // cy.get('[data-cy=levelName]').type('{esc}');
        // cy.get('[data-cy=addLevel]').should('have.focus');

        cy.get('[data-cy=addLevel]').click();
        cy.get('[aria-label=Close]').filter('.text-light').click();
        cy.get('[data-cy=addLevel]').should('have.focus');

        cy.get('[data-cy=editLevelButton]').eq(0).click();
        cy.get('[data-cy=cancelLevel]').click();
        cy.get('[data-cy=editLevelButton]').eq(0).should('have.focus');

        // cy.get('[data-cy=editLevelButton]').eq(0).click();
        // cy.get('[data-cy=levelName]').type('{esc}');
        // cy.get('[data-cy=editLevelButton]').eq(0).should('have.focus');

        cy.get('[data-cy=editLevelButton]').eq(0).click();
        cy.get('[aria-label=Close]').filter('.text-light').click();
        cy.get('[data-cy=editLevelButton]').eq(0).should('have.focus');

        // cy.get('[data-cy=editLevelButton]').eq(0).click();
        // cy.get('[data-cy=levelName]').type('{selectall}Fooooooo');
        // cy.get('[data-cy=saveLevelButton]').click();
        // cy.wait('@saveLevel');
        // cy.wait('@loadLevels');
        // cy.get('[data-cy=editLevelButton]').eq(0).should('have.focus');

        cy.get('[data-cy=editLevelButton]').eq(3).click();
        cy.get('[data-cy=cancelLevel]').click();
        cy.get('[data-cy=editLevelButton]').eq(3).should('have.focus');

        // cy.get('[data-cy=editLevelButton]').eq(3).click();
        // cy.get('[data-cy=levelName]').type('{esc}');
        // cy.get('[data-cy=editLevelButton]').eq(3).should('have.focus');

        cy.get('[data-cy=editLevelButton]').eq(3).click();
        cy.get('[aria-label=Close]').filter('.text-light').click();
        cy.get('[data-cy=editLevelButton]').eq(3).should('have.focus');

        // cy.get('[data-cy=editLevelButton]').eq(3).click();
        // cy.get('[data-cy=levelName]').type('{selectall}Baaaaar');
        // cy.get('[data-cy=saveLevelButton]').click();
        // cy.wait('@saveLevel');
        // cy.wait('@loadLevels');
        // cy.get('[data-cy=editLevelButton]').eq(3).should('have.focus');
    });

    it('viewing subject user details does not break breadcrumb navigation', () => {
        cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            name: "Subject 1"
        });

        cy.request('POST', '/admin/projects/proj1/subjects/subj1/skills/skill1', {
            projectId: 'proj1',
            subjectId: "subj1",
            skillId: "skill1",
            name: "Skill 1",
            pointIncrement: '50',
            numPerformToCompletion: '5'
        });

        cy.request('POST', `/api/projects/proj1/skills/skill1`, {userId: 'someuser', timestamp: new Date().getTime()})
        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.clickNav('Users').click();
        cy.get('[data-cy="usersTable"]').contains('someuser').click();
        cy.get('[data-cy=breadcrumb-subj1]').should('be.visible');
        cy.get('[data-cy=breadcrumb-Users]').should('be.visible');
    })

    it('description is validated against custom validators', () => {
        cy.intercept('GET', '/admin/projects/proj1/subjects').as('loadSubjects');

        cy.visit('/administrator/projects/proj1');
        cy.wait('@loadSubjects');
        cy.clickButton('Subject');

        cy.get('[data-cy="subjectNameInput"]').type('Great Name');
        cy.get('[data-cy="saveSubjectButton"]').should('be.enabled');
        //
        cy.get('[data-cy="markdownEditorInput"]').type('ldkj aljdl aj\n\njabberwocky');
        cy.get('[data-cy="subjectDescError"]').contains('Subject Description - paragraphs may not contain jabberwocky');
        cy.get('[data-cy="saveSubjectButton"]').should('be.disabled');

        cy.get('[data-cy="markdownEditorInput"]').type('{backspace}');
        cy.get('[data-cy="saveSubjectButton"]').should('be.enabled');
    });

    it('name is validated against custom validators', () => {
        cy.intercept('GET', '/admin/projects/proj1/subjects').as('loadSubjects');

        cy.visit('/administrator/projects/proj1');
        cy.wait('@loadSubjects');
        cy.clickButton('Subject');

        cy.get('[data-cy="subjectNameInput"]').type('Great Name');

        cy.get('[data-cy="subjectNameError"]')
          .should('not.be.visible');
        cy.get('[data-cy="saveSubjectButton"]')
          .should('be.enabled');

        cy.get('input[data-cy=subjectNameInput]')
          .type('{selectall}(A) Updated Subject Name');
        cy.get('[data-cy="subjectNameError"]')
          .contains('Subject Name - names may not contain (A)');
        cy.get('[data-cy="saveSubjectButton"]')
          .should('be.disabled');

        cy.get('input[data-cy=subjectNameInput]')
          .type('{selectall}(B) A Updated Subject Name');
        cy.get('[data-cy="subjectNameError"]')
          .should('not.be.visible');
        cy.get('[data-cy="saveSubjectButton"]')
          .should('be.enabled');
    });

    it('edit in place', () => {
        cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            name: "Subject 1"
        });

        cy.request('POST', '/admin/projects/proj1/subjects/subj1/skills/skill1', {
            projectId: 'proj1',
            subjectId: "subj1",
            skillId: "skill1",
            name: "Skill 1",
            pointIncrement: '50',
            numPerformToCompletion: '5'
        });

        cy.intercept('GET', '/admin/projects/proj1/subjects/subj1').as('loadSubject1');
        cy.intercept('POST', '/admin/projects/proj1/subjects/subj1').as('saveSubject1');
        cy.intercept('POST', '/admin/projects/proj1/subjects/entirelyNewId/skills/skill1').as('saveSkill');
        cy.intercept('GET', '/admin/projects/proj1/subjects/entirelyNewId').as('loadSubject2');
        cy.intercept('POST', '/admin/projects/proj1/subjects/entirelyNewId/skills/copy_of_skill1').as('saveSkill2');
        cy.intercept('DELETE', '/admin/projects/proj1/subjects/entirelyNewId/skills/skill1').as('deleteSkill');

        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.wait('@loadSubject1');
        cy.contains('SUBJECT: Subject 1').should('be.visible');
        cy.get('[data-cy=btn_edit-subject]').click();
        cy.get('input[data-cy=subjectNameInput]').type('{selectall}Edited Subject Name');
        cy.get('[data-cy=saveSubjectButton]').click();
        cy.wait('@saveSubject1');
        cy.contains('Editing Existing Subject').should('not.exist');
        cy.wait(300);
        cy.get('[data-cy=btn_edit-subject]').should('have.focus');
        cy.contains('SUBJECT: Subject 1').should('not.exist');
        cy.contains('SUBJECT: Edited Subject Name').should('be.visible');
        cy.contains('ID: subj1').should('be.visible');
        cy.get('[data-cy=breadcrumb-subj1]').should('be.visible');

        cy.get('[data-cy=btn_edit-subject]').click();
        cy.get('[data-cy=enableIdInput]').click({force: true});
        cy.get('input[data-cy=idInputValue]').type('{selectall}entirelyNewId');
        cy.get('[data-cy=saveSubjectButton]').click();
        cy.wait('@saveSubject1');
        cy.contains('Editing Existing Subject').should('not.exist');
        cy.wait(300);
        cy.get('[data-cy=btn_edit-subject]').should('have.focus');
        cy.contains('SUBJECT: Edited Subject Name').should('be.visible');
        cy.contains('ID: subj1').should('not.exist');
        cy.get('[data-cy=breadcrumb-subj1]').should('not.exist');
        cy.contains('ID: entirelyNewId').should('be.visible');
        cy.get('[data-cy=breadcrumb-entirelyNewId]').should('be.visible');

        cy.get('[data-cy=editSkillButton_skill1]').click();
        cy.get('input[data-cy=skillName]').type('{selectall}Edited Skill Name');
        cy.get('[data-cy=saveSkillButton]').click();
        cy.wait('@saveSkill');
        cy.contains('Edited Skill Name').should('be.visible');

        cy.get('[data-cy=manageSkillBtn_skill1]').click();
        cy.get('[data-cy=breadcrumb-entirelyNewId]').should('be.visible');
        cy.get('[data-cy=breadcrumb-entirelyNewId]').click();
        cy.wait('@loadSubject2');
        cy.contains('SUBJECT: Edited Subject Name').should('be.visible');

        cy.get('[data-cy=copySkillButton_skill1]').click();
        cy.get('[data-cy=saveSkillButton]').click();
        cy.wait('@saveSkill2');
        cy.contains('Copy of Edited Skill Name').should('be.visible');

        cy.get('[data-cy=deleteSkillButton_skill1]').click();
        cy.contains('This will remove Edited Skill Name').click();
        cy.acceptRemovalSafetyCheck();
        cy.wait('@deleteSkill');
        cy.contains('ID: skill1').should('not.exist');
    });

    it('navigate to skills by click on subject name and subject icon', () => {
        cy.createSubject(1, 1)
        cy.createSubject(1, 2)

        cy.visit('/administrator/projects/proj1');
        cy.get('[data-cy="subjectCard-subj1"] [data-cy="titleLink"]').click();
        cy.contains('No Skills Yet');
        cy.contains('ID: subj1')

        cy.visit('/administrator/projects/proj1');
        cy.get('[data-cy="subjectCard-subj2"] [data-cy="titleLink"]').click();
        cy.contains('No Skills Yet');
        cy.contains('ID: subj2')

        cy.visit('/administrator/projects/proj1');
        cy.get('[data-cy="subjectCard-subj1"] [data-cy="iconLink"]').click();
        cy.contains('No Skills Yet');
        cy.contains('ID: subj1')

        cy.visit('/administrator/projects/proj1');
        cy.get('[data-cy="subjectCard-subj2"] [data-cy="iconLink"]').click();
        cy.contains('No Skills Yet');
        cy.contains('ID: subj2')
    });

    it('delete subject', () => {
        cy.createSubject(1, 1)
        cy.createSubject(1, 2)
        cy.createSubject(1, 3)

        cy.visit('/administrator/projects/proj1');

        cy.get('[data-cy="subjectCard-subj1"]').should('exist');
        cy.get('[data-cy="subjectCard-subj2"]').should('exist');
        cy.get('[data-cy="subjectCard-subj3"]').should('exist');

        cy.get('[data-cy="subjectCard-subj2"] [data-cy="deleteBtn"]').click();
        cy.contains('Subject with id [subj2] will be removed.');
        cy.get('[data-cy=currentValidationText]').type('Delete Me');
        cy.get('[data-cy=removeButton]').should('be.enabled').click();

        cy.get('[data-cy="subjectCard-subj1"]').should('exist');
        cy.get('[data-cy="subjectCard-subj2"]').should('not.exist');
        cy.get('[data-cy="subjectCard-subj3"]').should('exist');

        cy.get('[data-cy="subjectCard-subj1"] [data-cy="deleteBtn"]').click();
        cy.contains('Removal Safety Check');
        cy.get('[data-cy=currentValidationText]').type('Delete Me');
        cy.get('[data-cy=removeButton]').should('be.enabled').click();

        cy.get('[data-cy="subjectCard-subj1"]').should('not.exist');
        cy.get('[data-cy="subjectCard-subj2"]').should('not.exist');
        cy.get('[data-cy="subjectCard-subj3"]').should('exist');

        cy.get('[data-cy="subjectCard-subj3"] [data-cy="deleteBtn"]').click();
        cy.contains('Subject with id [subj3] will be removed.');
        cy.get('[data-cy=currentValidationText]').type('Delete Me');
        cy.get('[data-cy=removeButton]').should('be.enabled').click();

        cy.get('[data-cy="subjectCard-subj1"]').should('not.exist');
        cy.get('[data-cy="subjectCard-subj2"]').should('not.exist');
        cy.get('[data-cy="subjectCard-subj3"]').should('not.exist');

        cy.contains('No Subjects Yet');
    });

    it('drag-and-drop sort order', () => {
        cy.intercept('/admin/projects/proj1/subjects/subj1').as('subj1Async')
        cy.intercept('/admin/projects/proj1/subjects/subj2').as('subj2Async')
        cy.intercept('/admin/projects/proj1/subjects/subj3').as('subj3Async')
        cy.intercept('/admin/projects/proj1/subjects/subj4').as('subj4Async')
        cy.intercept('/admin/projects/proj1/subjects/subj5').as('subj5Async')

        cy.createSubject(1, 1)
        cy.createSubject(1, 2)
        cy.createSubject(1, 3)
        cy.createSubject(1, 4)
        cy.createSubject(1, 5)

        cy.visit('/administrator/projects/proj1');

        const subj1Card = '[data-cy="subjectCard-subj1"] [data-cy="sortControlHandle"]';
        const subj2Card = '[data-cy="subjectCard-subj2"] [data-cy="sortControlHandle"]';
        const subj4Card = '[data-cy="subjectCard-subj4"] [data-cy="sortControlHandle"]';
        const subj5Card = '[data-cy="subjectCard-subj5"] [data-cy="sortControlHandle"]';


        cy.validateElementsOrder('[data-cy="subjectCard"]', ['Subject 1', 'Subject 2', 'Subject 3', 'Subject 4', 'Subject 5']);
        cy.get(subj1Card).dragAndDrop(subj4Card)
        cy.wait('@subj1Async')
        cy.validateElementsOrder('[data-cy="subjectCard"]', ['Subject 2', 'Subject 3', 'Subject 4', 'Subject 1', 'Subject 5']);

        // refresh to make sure it was saved
        cy.visit('/administrator/projects/proj1');
        cy.validateElementsOrder('[data-cy="subjectCard"]', ['Subject 2', 'Subject 3', 'Subject 4', 'Subject 1', 'Subject 5']);

        cy.get(subj5Card).dragAndDrop(subj2Card)
        cy.wait('@subj5Async')
        cy.validateElementsOrder('[data-cy="subjectCard"]', ['Subject 5', 'Subject 2', 'Subject 3', 'Subject 4', 'Subject 1']);

        cy.get(subj2Card).dragAndDrop(subj1Card)
        cy.wait('@subj2Async')
        cy.validateElementsOrder('[data-cy="subjectCard"]', ['Subject 5', 'Subject 3', 'Subject 4', 'Subject 1', 'Subject 2']);

        // refresh to make sure it was saved
        cy.visit('/administrator/projects/proj1');
        cy.validateElementsOrder('[data-cy="subjectCard"]', ['Subject 5', 'Subject 3', 'Subject 4', 'Subject 1', 'Subject 2']);
    })

    it('no drag-and-drag sort controls when there is only 1 subject', () => {
        cy.createSubject(1, 1)

        cy.visit('/administrator/projects/proj1');
        cy.get('[data-cy="subjectCard-subj1"]');
        cy.get('[data-cy="subjectCard-subj1"] [data-cy="sortControlHandle"]').should('not.exist');

        cy.createSubject(1, 2)
        cy.visit('/administrator/projects/proj1');
        cy.get('[data-cy="subjectCard-subj1"]');
        cy.get('[data-cy="subjectCard-subj1"] [data-cy="sortControlHandle"]');
    })

    it('drag-and-drag sort should spinner while backend operation is happening', () => {
        cy.intercept('/admin/projects/proj1/subjects/subj1', (req) => {
            req.reply((res) => {
                res.send({ delay: 6000})
            })
        }).as('subj1Async');

        cy.createSubject(1, 1)
        cy.createSubject(1, 2)

        const subj1Card = '[data-cy="subjectCard-subj1"] [data-cy="sortControlHandle"]';
        const subj2Card = '[data-cy="subjectCard-subj2"] [data-cy="sortControlHandle"]';

        cy.visit('/administrator/projects/proj1');
        cy.validateElementsOrder('[data-cy="subjectCard"]', ['Subject 1', 'Subject 2']);
        cy.get(subj1Card).dragAndDrop(subj2Card)

        // overlay over both cards but loading message only on subject 1
        cy.get('[data-cy="subj1_overlayShown"] [data-cy="updatingSortMsg"]').contains('Updating sort order');
        cy.get('[data-cy="subj2_overlayShown"]');
        cy.get('[data-cy="subj2_overlayShown"] [data-cy="updatingSortMsg"]').should('not.exist');
        cy.wait('@subj1Async')
        cy.get('[data-cy="subj1_overlayShown"]').should('not.exist');
        cy.get('[data-cy="subj2_overlayShown"]').should('not.exist');
    })

    it('subject card stats', () => {
        cy.createSubject(1, 1)
        cy.createSubject(1, 2)

        cy.visit('/administrator/projects/proj1');
        cy.get('[data-cy="subjectCard-subj1"] [data-cy="pagePreviewCardStat_# Skills"] [data-cy="statNum"]').contains(0);
        cy.get('[data-cy="subjectCard-subj1"] [data-cy="pagePreviewCardStat_Points"] [data-cy="statNum"]').contains(0);
        cy.get('[data-cy="subjectCard-subj2"] [data-cy="pagePreviewCardStat_# Skills"] [data-cy="statNum"]').contains(0);
        cy.get('[data-cy="subjectCard-subj2"] [data-cy="pagePreviewCardStat_Points"] [data-cy="statNum"]').contains(0);

        cy.createSkill(1, 1, 1)
        cy.createSkill(1, 1, 2)
        cy.visit('/administrator/projects/proj1');
        cy.get('[data-cy="subjectCard-subj1"] [data-cy="pagePreviewCardStat_# Skills"] [data-cy="statNum"]').contains(2);
        cy.get('[data-cy="subjectCard-subj1"] [data-cy="pagePreviewCardStat_Points"] [data-cy="statNum"]').contains(400);
        cy.get('[data-cy="subjectCard-subj2"] [data-cy="pagePreviewCardStat_# Skills"] [data-cy="statNum"]').contains(0);
        cy.get('[data-cy="subjectCard-subj2"] [data-cy="pagePreviewCardStat_Points"] [data-cy="statNum"]').contains(0);
    });

    it('subject card ponts %', () => {
        cy.createSubject(1, 1)
        cy.createSubject(1, 2)
        cy.createSubject(1, 3)

        cy.visit('/administrator/projects/proj1');
        cy.get('[data-cy="subjectCard-subj1"] [data-cy="pointsPercent"]').contains('0');
        cy.get('[data-cy="subjectCard-subj2"] [data-cy="pointsPercent"]').contains('0');
        cy.get('[data-cy="subjectCard-subj3"] [data-cy="pointsPercent"]').contains('0');

        cy.createSkill(1, 1, 1)
        cy.createSkill(1, 1, 2)

        cy.visit('/administrator/projects/proj1');
        cy.get('[data-cy="subjectCard-subj1"] [data-cy="pointsPercent"]').contains('100');
        cy.get('[data-cy="subjectCard-subj2"] [data-cy="pointsPercent"]').contains('0');
        cy.get('[data-cy="subjectCard-subj3"] [data-cy="pointsPercent"]').contains('0');

        cy.createSkill(1, 2, 3)

        cy.visit('/administrator/projects/proj1');
        cy.get('[data-cy="subjectCard-subj1"] [data-cy="pointsPercent"]').contains('66');
        cy.get('[data-cy="subjectCard-subj2"] [data-cy="pointsPercent"]').contains('34');
        cy.get('[data-cy="subjectCard-subj3"] [data-cy="pointsPercent"]').contains('0');

    });

    it('subject name should not wrap prematurely', () => {
        cy.request('POST', '/admin/projects/proj1/subjects/areallylongsubjectnamethatmaywraptoosoonSubject', {
            projectId: 'proj1',
            subjectId: 'areallylongsubjectnamethatmaywraptoosoonSubject',
            name: 'a really long subject name that may wrap too soon'
        });

        cy.intercept('GET', '/admin/projects/proj1/subjects/areallylongsubjectnamethatmaywraptoosoonSubject').as('loadSubj');
        // resolutions over 1280 are ignored in headless mode so we can only test at this resolution
        cy.viewport(1280, 900);

        cy.wait(200);
        cy.visit('/administrator/projects/proj1/subjects/areallylongsubjectnamethatmaywraptoosoonSubject/');
        cy.wait('@loadSubj');

        cy.get('[data-cy=pageHeaderStat]').first().invoke('width').then((val)=>{
            cy.get('[data-cy=pageHeaderStat]').eq(1).invoke('width').should('eq', val);
        });
    });

    it('subject modal shows Root Help Url when configured', () => {
        cy.request('POST', '/admin/projects/proj1/settings/help.url.root', {
            projectId: 'proj1',
            setting: 'help.url.root',
            value: 'https://SomeArticleRepo.com/'
        });
        cy.createSubject(1, 2, {helpUrl: '/some/path'})
        cy.createSubject(1, 3, {helpUrl: 'https://www.OverrideHelpUrl.com/other/path'})

        cy.visit('/administrator/projects/proj1/');
        cy.get('[data-cy="btn_Subjects"]').click();
        cy.get('[data-cy="rootHelpUrlSetting"]').contains('https://SomeArticleRepo.com')

        const textDecorationMatch = 'line-through solid rgb(38, 70, 83)';

        // strike-through when url starts with http:// or https://
        cy.get('[data-cy="skillHelpUrl"]').type('https:/');
        cy.get('[data-cy="rootHelpUrlSetting"]').should('not.have.css', 'text-decoration', textDecorationMatch);
        cy.get('[data-cy="skillHelpUrl"]').type('/');
        cy.get('[data-cy="rootHelpUrlSetting"]').should('have.css', 'text-decoration', textDecorationMatch);

        cy.get('[data-cy="skillHelpUrl"]').clear().type('http:/');
        cy.get('[data-cy="rootHelpUrlSetting"]').should('not.have.css', 'text-decoration', textDecorationMatch);
        cy.get('[data-cy="skillHelpUrl"]').type('/');
        cy.get('[data-cy="rootHelpUrlSetting"]').should('have.css', 'text-decoration', textDecorationMatch);

        // now test edit
        cy.get('[data-cy="closeSubjectButton"]').click();
        cy.get('[data-cy="subjectCard-subj2"] [data-cy="editBtn"]').click();
        cy.get('[data-cy="rootHelpUrlSetting"]').contains('https://SomeArticleRepo.com')
        cy.get('[data-cy="rootHelpUrlSetting"]').should('not.have.css', 'text-decoration', textDecorationMatch);

        // edit again - anything that starts with https or http must not use Root Help Url
        cy.get('[data-cy="closeSubjectButton"]').click();
        cy.get('[data-cy="subjectCard-subj3"] [data-cy="editBtn"]').click();
        cy.get('[data-cy="rootHelpUrlSetting"]').contains('https://SomeArticleRepo.com')
        cy.get('[data-cy="rootHelpUrlSetting"]').should('have.css', 'text-decoration', textDecorationMatch);

        // do not show Root Help Url if it's not configured
        cy.request('POST', '/admin/projects/proj1/settings/help.url.root', {
            projectId: 'proj1',
            setting: 'help.url.root',
            value: ''
        });
        cy.visit('/administrator/projects/proj1');
        cy.get('[data-cy="btn_Subjects"]').click();
        cy.get('[data-cy="skillHelpUrl"]');
        cy.get('[data-cy="rootHelpUrlSetting"]').should('not.exist');
        cy.get('[data-cy="closeSubjectButton"]').click();
        cy.get('[data-cy="subjectCard-subj2"] [data-cy="editBtn"]').click();
        cy.get('[data-cy="skillHelpUrl"]');
        cy.get('[data-cy="rootHelpUrlSetting"]').should('not.exist');
    });

    it('subject modal shows Root Help Url after it was update via UI', () => {
        cy.createSubject(1, 2, {helpUrl: '/some/path'})
        cy.createSubject(1, 3, {helpUrl: 'https://www.OverrideHelpUrl.com/other/path'})

        cy.visit('/administrator/projects/proj1/');
        cy.get('[data-cy="btn_Subjects"]').click();
        cy.get('[data-cy="skillHelpUrl"]')
        cy.get('[data-cy="rootHelpUrlSetting"]').should('not.exist')
        cy.get('[data-cy="closeSubjectButton"]').click();

        cy.clickNav('Settings');
        cy.get('[data-cy="rootHelpUrlInput"]').type('https://someCoolWebsite.com/');
        cy.get('[data-cy="saveSettingsBtn"]').click();

        cy.clickNav('Subjects');
        cy.get('[data-cy="manageBtn_subj2"]').should('exist')
        cy.get('[data-cy="btn_Subjects"]').click();
        cy.get('[data-cy="skillHelpUrl"]')
        cy.get('[data-cy="rootHelpUrlSetting"]').contains('https://someCoolWebsite.com')
    })

    it('subject modal allows Help Url to have spaces', () => {
        cy.visit('/administrator/projects/proj1/');
        cy.get('[data-cy="btn_Subjects"]').click();
        cy.get('[data-cy="subjectNameInput"]').type('subj1')
        cy.get('[data-cy="skillHelpUrl"]').type('https://someCoolWebsite.com/some url with spaces')
        cy.get('[data-cy="skillHelpUrlError"]').should('not.be.visible');
        cy.get('[data-cy="saveSubjectButton"]').click()
        cy.get('[data-cy="subjectCard-subj1Subject"] [data-cy="editBtn"]').click()
        cy.get('[data-cy="skillHelpUrl"]').should('have.value', 'https://someCoolWebsite.com/some%20url%20with%20spaces')
    })

    it('root help url is properly set for multiple projects', () => {
        cy.request('POST', '/admin/projects/proj1/settings/help.url.root', {
            projectId: 'proj1',
            setting: 'help.url.root',
            value: 'https://SomeArticleRepo.com/'
        });
        cy.createSubject(1, 2, { helpUrl: '/some/path' })
        cy.createSubject(1, 3, { helpUrl: 'https://www.OverrideHelpUrl.com/other/path' })

        cy.createProject(2);
        cy.createSubject(2, 4, { helpUrl: '/some/path' })
        cy.createSubject(2, 5, { helpUrl: 'https://www.OverrideHelpUrl.com/other/path' })
        cy.request('POST', '/admin/projects/proj2/settings/help.url.root', {
            projectId: 'proj2',
            setting: 'help.url.root',
            value: 'https://veryDifferentUrl.com'
        });

        cy.visit('/administrator/projects/proj1/');
        cy.get('[data-cy="btn_Subjects"]').click();
        cy.get('[data-cy="rootHelpUrlSetting"]').contains('https://SomeArticleRepo.com')
        cy.get('[data-cy="closeSubjectButton"]').click();

        cy.get('[data-cy="breadcrumb-Projects"]').click();
        cy.get('[data-cy="projCard_proj2_manageBtn"]').click();
        cy.get('[data-cy="btn_Subjects"]').click();
        cy.get('[data-cy="rootHelpUrlSetting"]').contains('https://veryDifferentUrl.com')
    });

    it('edit subject id when viewing subject skills should not break breadcrumb bar', () => {
      cy.createSubject(1, 1)
      cy.createSkill(1, 1, 1)
      cy.createSkill(1, 1, 2)
      cy.createSkill(1, 1, 3)
      cy.intercept('GET', '/admin/projects/proj1/subjects/subj111111111').as('getNewId');
      cy.intercept('GET', '/admin/projects/proj1/subjects/subj111111111/skills/skill3 ').as('loadSkill');

      cy.visit('/administrator/projects/proj1/subjects/subj1');
      cy.get('[data-cy=breadcrumb-subj1]').should('exist');
      cy.get('[data-cy=btn_edit-subject]').click();
      cy.contains('Editing Existing Subject').should('be.visible');
        cy.get('[data-cy=enableIdInput]').click({force: true});
        cy.get('[data-cy=idInputValue]')
            .type('11111111');
        cy.get('[data-cy=saveSubjectButton]')
            .click();
        cy.wait('@getNewId');
        cy.get('[data-cy=manageSkillLink_skill3]')
            .click();
        cy.wait('@loadSkill');
        cy.get('[data-cy=breadcrumb-subj1]')
            .should('not.exist');
        cy.get('[data-cy=breadcrumb-subj111111111]')
            .should('exist');

    });

    it('change sort order using keyboard', () => {
        cy.createSubject(1, 1);
        cy.createSubject(1, 2);
        cy.createSubject(1, 3);

        cy.visit('/administrator/projects/proj1');
        cy.validateElementsOrder('[data-cy="subjectCard"] [data-cy="titleLink"]', ['Subject 1', 'Subject 2', 'Subject 3']);

        // move down
        cy.get('[data-cy="subjectCard-subj1"] [data-cy="deleteBtn"]')
            .tab()
            .type('{downArrow}');
        cy.validateElementsOrder('[data-cy="subjectCard"] [data-cy="titleLink"]', ['Subject 2', 'Subject 1', 'Subject 3']);
        cy.get('[data-cy="subjectCard-subj1"] [data-cy="sortControlHandle"]')
            .should('have.focus');

        // move down
        cy.get('[data-cy="subjectCard-subj1"] [data-cy="deleteBtn"]')
            .tab()
            .type('{downArrow}');
        cy.validateElementsOrder('[data-cy="subjectCard"] [data-cy="titleLink"]', ['Subject 2', 'Subject 3', 'Subject 1']);
        cy.get('[data-cy="subjectCard-subj1"] [data-cy="sortControlHandle"]')
            .should('have.focus');

        // move down - already the last item
        cy.get('[data-cy="subjectCard-subj1"] [data-cy="deleteBtn"]')
            .tab()
            .type('{downArrow}');
        cy.validateElementsOrder('[data-cy="subjectCard"] [data-cy="titleLink"]', ['Subject 2', 'Subject 3', 'Subject 1']);
        cy.get('[data-cy="subjectCard-subj1"] [data-cy="sortControlHandle"]')
            .should('have.focus');

        // refresh and validate
        cy.visit('/administrator/projects/proj1');
        cy.validateElementsOrder('[data-cy="subjectCard"] [data-cy="titleLink"]', ['Subject 2', 'Subject 3', 'Subject 1']);
        cy.get('[data-cy="subjectCard-subj1"] [data-cy="sortControlHandle"]')
            .should('not.have.focus');

        // move up
        cy.get('[data-cy="subjectCard-subj3"] [data-cy="deleteBtn"]')
            .tab()
            .type('{upArrow}');
        cy.validateElementsOrder('[data-cy="subjectCard"] [data-cy="titleLink"]', ['Subject 3', 'Subject 2', 'Subject 1']);
        cy.get('[data-cy="subjectCard-subj3"] [data-cy="sortControlHandle"]')
            .should('have.focus');

        // move up - already first
        cy.get('[data-cy="subjectCard-subj3"] [data-cy="deleteBtn"]')
            .tab()
            .type('{upArrow}');
        cy.validateElementsOrder('[data-cy="subjectCard"] [data-cy="titleLink"]', ['Subject 3', 'Subject 2', 'Subject 1']);
        cy.get('[data-cy="subjectCard-subj3"] [data-cy="sortControlHandle"]')
            .should('have.focus');
    });

    it('cancelling subject delete dialog should return focus to delete button', () => {
        cy.createSubject(1, 1)
        cy.createSubject(1, 2)
        cy.createSubject(1, 3)
        cy.createSubject(1, 4)
        cy.createSubject(1, 5)

        cy.intercept('GET', '/admin/projects/proj1/subjects').as('getSubjects');
        cy.visit('/administrator/projects/proj1');
        cy.wait('@getSubjects');

        cy.get('[data-cy="subjectCard-subj2"] [data-cy="deleteBtn"]').click();
        cy.contains('Removal Safety Check');
        cy.get('[data-cy=closeRemovalSafetyCheck]').click();
        cy.get('[data-cy="subjectCard-subj2"] [data-cy="deleteBtn"]').should('have.focus');
    });

    it('edit subject - run validation on load in case validation improved and existing values fail to validate', () => {
        cy.intercept('POST', '/api/validation/description*', {
            valid: false,
            msg: 'Mocked up validation failure'
        }).as('validateDesc');

        cy.createSubject(1, 1, {description: 'Very cool project'})
        cy.visit('/administrator/projects/proj1');
        cy.get('[data-cy="editBtn"]').click()
        cy.wait('@validateDesc')
        cy.get('[data-cy="subjectDescError"]').contains('Mocked up validation failure')
    });
});
