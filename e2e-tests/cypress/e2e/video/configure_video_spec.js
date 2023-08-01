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

describe('Configure Video Tests', () => {

    const testVideo = '/static/videos/create-quiz.mp4'
    beforeEach(() => {
    });

    it('configure Video URL, transcript and captions', () => {
        cy.createProject(1)
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1)
        cy.visit('/administrator/projects/proj1/subjects/subj1/skills/skill1/configVideo');
        cy.get('[data-cy="saveVideoSettingsBtn"]').should('be.disabled')
        cy.get('[data-cy="showExternalUrlBtn"]').click()
        cy.get('[data-cy="showFileUploadBtn"]').should('be.visible')
        cy.get('[data-cy="showExternalUrlBtn"]').should('not.exist')
        cy.get('[data-cy="videoUrl"]').type('http://some.vid')
        cy.get('[data-cy="videoCaptions"]').type('captions')
        cy.get('[data-cy="videoTranscript"]').type('transcript')
        cy.get('[data-cy="saveVideoSettingsBtn"]').click()
        cy.get('[data-cy="savedMsg"]')

        cy.visit('/administrator/projects/proj1/subjects/subj1/skills/skill1/configVideo');
        cy.get('[data-cy="videoUrl"]').should('have.value', 'http://some.vid')
        cy.get('[data-cy="videoCaptions"]').should('have.value','captions')
        cy.get('[data-cy="videoTranscript"]').should('have.value','transcript')
        cy.get('[data-cy="saveVideoSettingsBtn"]').should('be.enabled')
        cy.get('[data-cy="previewVideoSettingsBtn"]').should('be.enabled')
        cy.get('[data-cy="clearVideoSettingsBtn"]').should('be.enabled')
    });

    it('clear attributes', () => {
        cy.createProject(1)
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1)
        const vidAttr = { videoUrl: 'http://someurl.mp4', captions: 'some', transcript: 'another' }
        cy.saveVideoAttrs(1, 1, vidAttr)
        cy.visit('/administrator/projects/proj1/subjects/subj1/skills/skill1/configVideo');

        cy.get('[data-cy="videoUrl"]').should('have.value', vidAttr.videoUrl)
        cy.get('[data-cy="videoCaptions"]').should('have.value', vidAttr.captions)
        cy.get('[data-cy="videoTranscript"]').should('have.value', vidAttr.transcript)

        cy.get('[data-cy="saveVideoSettingsBtn"]').should('be.enabled')
        cy.get('[data-cy="previewVideoSettingsBtn"]').should('be.enabled')
        cy.get('[data-cy="clearVideoSettingsBtn"]').should('be.enabled')

        cy.get('[data-cy="clearVideoSettingsBtn"]').click()
        cy.get('footer .btn-danger').contains('Yes, Do clear').click()

        cy.get('[data-cy="showFileUploadBtn"]').should('not.exist')
        cy.get('[data-cy="showExternalUrlBtn"]').should('be.visible')
        cy.get('[data-cy="videoUrl"]').should('not.exist')
        cy.get('[data-cy="videoFileUpload"]').should('exist')
        cy.get('[data-cy="videoCaptions"]').should('be.empty')
        cy.get('[data-cy="videoTranscript"]').should('be.empty')

        cy.get('[data-cy="saveVideoSettingsBtn"]').should('be.disabled')
        cy.get('[data-cy="clearVideoSettingsBtn"]').should('be.disabled')

        cy.visit('/administrator/projects/proj1/subjects/subj1/skills/skill1/configVideo');
        cy.get('[data-cy="showFileUploadBtn"]').should('not.exist')
        cy.get('[data-cy="showExternalUrlBtn"]').should('be.visible')
        cy.get('[data-cy="videoUrl"]').should('not.exist')
        cy.get('[data-cy="videoFileUpload"]').should('exist')
        cy.get('[data-cy="videoCaptions"]').should('be.empty')
        cy.get('[data-cy="videoTranscript"]').should('be.empty')

        cy.get('[data-cy="saveVideoSettingsBtn"]').should('be.disabled')
        cy.get('[data-cy="clearVideoSettingsBtn"]').should('be.disabled')
    });

    it('fill caption example', () => {
        cy.createProject(1)
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1)
        cy.visit('/administrator/projects/proj1/subjects/subj1/skills/skill1/configVideo');
        cy.get('[data-cy="fillCaptionsExamples"]').click()
        cy.get('[data-cy="videoCaptions"]').should('contain.value', 'WEBVTT')
        cy.get('[data-cy="fillCaptionsExamples"]').should('not.exist')
    });

    it('preview video with Video URL', () => {
        cy.createProject(1)
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1)
        cy.visit('/administrator/projects/proj1/subjects/subj1/skills/skill1/configVideo');
        cy.get('[data-cy="saveVideoSettingsBtn"]').should('be.disabled')
        cy.get('[data-cy="clearVideoSettingsBtn"]').should('be.disabled')

        cy.get('[data-cy="showExternalUrlBtn"]').click()
        cy.get('[data-cy="videoUrl"]').type(testVideo, {delay: 0})

        cy.get('[data-cy="saveVideoSettingsBtn"]').should('be.enabled')
        cy.get('[data-cy="clearVideoSettingsBtn"]').should('be.enabled')

        cy.get('[data-cy="videoPreviewCard"]').should('not.exist')
        cy.get('[data-cy="saveVideoSettingsBtn"]').click()
        cy.get('[data-cy="videoPreviewCard"] [data-cy="percentWatched"]').should('have.text', '0%')
        cy.get('[data-cy="videoPreviewCard"] [title="Play Video"]').click()
        // video is 15 seconds
        cy.wait(15000)
        cy.get('[data-cy="videoPreviewCard"] [data-cy="percentWatched"]').should('have.text', '100%')
    });

});
