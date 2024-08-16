/*
 * Copyright 2024 SkillTree
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
describe('Transcript export tests', () => {

  let user
  beforeEach(() => {
    const userInfo = {
      'first': 'Joe',
      'last': 'Doe',
      'nickname': 'Joe Doe'
    }
    cy.request('POST', '/app/userInfo', userInfo)
    user = Cypress.env('proxyUser')
    const userIdInFileName = Cypress.env('oauthMode') ? 'foo' : user
    const buildPath = (projName) => {
      return `cypress/downloads/${projName} - ${userInfo.nickname} (${userIdInFileName}) - Transcript.pdf`
    }
    Cypress.Commands.add("readTranscript", (projName) => {
      const pathToPdf = buildPath(projName)
      return cy.readPdf(pathToPdf)
    });
  })

  const expectedHeaderAndFooter = 'For All Dragons enjoyment'
  const expectedHeaderAndFooterCommunityProtected = 'For Divine Dragon enjoyment'

  const createSubjectAndSkills = (subjNum, numOfSkills) => {
    cy.createSubject(1, subjNum)
    for (let i = 1; i <= numOfSkills; i++) {
      cy.createSkill(1, subjNum, i, { numPerformToCompletion: 1 })
    }
  }
  const clean = (text) => {
    return text.replace(/\n/g, '')
  }

  it('transcript cards is not shown when there are no skills', () => {
    cy.createProject(1)

    cy.cdVisit('/')
    cy.get('[data-cy="noContent"]').contains('Subjects have not been added yet')
    cy.get('[data-cy="downloadTranscriptCard"]').should('not.exist')

    cy.createSubject(1, 1)
    cy.createSubject(1, 2)
    cy.cdVisit('/')
    cy.get('[data-cy="subjectTileBtn"]').should('have.length', 2)
    cy.get('[data-cy="downloadTranscriptCard"]').should('not.exist')
  })

  it('transcript cards shows skills counts', () => {
    cy.createProject(1)
    cy.createSubject(1, 1)
    cy.createSkill(1, 1, 1, { numPerformToCompletion: 1 })
    cy.cdVisit('/')
    cy.get('[data-cy="downloadTranscriptCard"]').contains('You have Completed 0 out of 1 skill!')

    cy.createSkill(1, 1, 2, { numPerformToCompletion: 1 })
    cy.createSkill(1, 1, 3, { numPerformToCompletion: 1 })
    cy.cdVisit('/')
    cy.get('[data-cy="downloadTranscriptCard"]').contains('You have Completed 0 out of 3 skills!')

    cy.reportSkill(1, 1, Cypress.env('proxyUser'), 'now')
    cy.reportSkill(1, 2, Cypress.env('proxyUser'), 'now')
    cy.cdVisit('/')
    cy.get('[data-cy="downloadTranscriptCard"]').contains('You have Completed 2 out of 3 skills!')
  })

  it('transcript with 1 subject', () => {
    const projName = 'Has Subject and Skills'
    cy.createProject(1, { name: projName })
    cy.createSubject(1)
    cy.createSkill(1, 1, 1)
    cy.createSkill(1, 1, 2)
    cy.createSkill(1, 1, 3)

    cy.reportSkill(1, 1, user, 'now')

    cy.cdVisit('/')
    cy.get('[data-cy="downloadTranscriptBtn"]').click()

    cy.readTranscript(projName).then((doc) => {
      expect(doc.numpages).to.equal(2)
      expect(clean(doc.text)).to.include('SkillTree TRANSCRIPT')
      expect(clean(doc.text)).to.include(projName)
      expect(clean(doc.text)).to.include('Level: 1 / 5 ')
      expect(clean(doc.text)).to.include('Points: 100 / 600 ')
      expect(clean(doc.text)).to.include('Skills: 0 / 3 ')
      expect(clean(doc.text)).to.not.include('Badges')

      // should be a title on the 2nd page
      expect(clean(doc.text)).to.include('Subject: Subject 1'.toUpperCase())

      expect(clean(doc.text)).to.not.include(expectedHeaderAndFooter)
      expect(clean(doc.text)).to.not.include(expectedHeaderAndFooterCommunityProtected)
      expect(clean(doc.text)).to.not.include('null')
    })
  })

  it('transcript with multiple subject and some progress', () => {
    const projName = 'Many Subj and Progress'
    cy.request('POST', '/app/userInfo', {
      'first': 'Joe',
      'last': 'Doe',
      'nickname': 'Joe Doe'
    })
    cy.createProject(1, { name: projName })
    const createSubjectAndSkills = (subjNum, numOfSkills) => {
      cy.createSubject(1, subjNum)
      for (let i = 1; i <= numOfSkills; i++) {
        cy.createSkill(1, subjNum, i, { numPerformToCompletion: 1 })
      }
    }
    createSubjectAndSkills(1, 12)
    createSubjectAndSkills(2, 6)
    createSubjectAndSkills(3, 8)
    cy.createSubject(1, 4)

    const user = Cypress.env('proxyUser')
    cy.doReportSkill({ project: 1, skill: 1, subjNum: 1, userId: user, date: 'now' })
    cy.doReportSkill({ project: 1, skill: 3, subjNum: 1, userId: user, date: 'now' })
    cy.doReportSkill({ project: 1, skill: 3, subjNum: 3, userId: user, date: 'now' })
    cy.doReportSkill({ project: 1, skill: 4, subjNum: 3, userId: user, date: 'now' })
    cy.doReportSkill({ project: 1, skill: 5, subjNum: 3, userId: user, date: 'now' })
    cy.doReportSkill({ project: 1, skill: 7, subjNum: 3, userId: user, date: 'now' })


    cy.cdVisit('/')
    cy.get('[data-cy="downloadTranscriptBtn"]').click()

    cy.readTranscript(projName).then((doc) => {
      expect(doc.numpages).to.equal(4)
      expect(clean(doc.text)).to.include('SkillTree TRANSCRIPT')
      expect(clean(doc.text)).to.include(projName)
      expect(clean(doc.text)).to.include('Level: 1 / 5 ')
      expect(clean(doc.text)).to.include('Points: 600 / 2,600 ')
      expect(clean(doc.text)).to.include('Skills: 6 / 26 ')
      expect(clean(doc.text)).to.not.include('Badges')

      // should be a title on the 2nd-4th pages
      expect(clean(doc.text)).to.include('Subject: Subject 1'.toUpperCase())
      expect(clean(doc.text)).to.include('Subject: Subject 2'.toUpperCase())
      expect(clean(doc.text)).to.include('Subject: Subject 3'.toUpperCase())
      // 4th subject doesn't have any skills so there shouldn't be a page for it
      expect(clean(doc.text)).to.not.include('Subject: Subject 4'.toUpperCase())

      expect(clean(doc.text)).to.not.include(expectedHeaderAndFooter)
      expect(clean(doc.text)).to.not.include(expectedHeaderAndFooterCommunityProtected)
      expect(clean(doc.text)).to.not.include('null')
    })
  })

  it('transcript with multiple subject and some progress and one badge', () => {
    const projName = 'Subj and 1 Badge'
    cy.request('POST', '/app/userInfo', {
      'first': 'Joe',
      'last': 'Doe',
      'nickname': 'Joe Doe'
    })
    cy.createProject(1, { name: projName })
    const createSubjectAndSkills = (subjNum, numOfSkills) => {
      cy.createSubject(1, subjNum)
      for (let i = 1; i <= numOfSkills; i++) {
        cy.createSkill(1, subjNum, i, { numPerformToCompletion: 1 })
      }
    }
    createSubjectAndSkills(1, 12)
    createSubjectAndSkills(2, 6)
    createSubjectAndSkills(3, 8)

    const user = Cypress.env('proxyUser')
    cy.doReportSkill({ project: 1, skill: 1, subjNum: 1, userId: user, date: 'now' })
    cy.doReportSkill({ project: 1, skill: 3, subjNum: 1, userId: user, date: 'now' })
    cy.doReportSkill({ project: 1, skill: 3, subjNum: 3, userId: user, date: 'now' })
    cy.doReportSkill({ project: 1, skill: 4, subjNum: 3, userId: user, date: 'now' })
    cy.doReportSkill({ project: 1, skill: 5, subjNum: 3, userId: user, date: 'now' })
    cy.doReportSkill({ project: 1, skill: 7, subjNum: 3, userId: user, date: 'now' })

    cy.createBadge(1)
    cy.assignSkillToBadge(1, 1, 1)
    cy.enableBadge(1, 1)

    cy.cdVisit('/')
    cy.get('[data-cy="downloadTranscriptBtn"]').click()

    cy.readTranscript(projName).then((doc) => {
      expect(doc.numpages).to.equal(5)
      expect(clean(doc.text)).to.include('SkillTree TRANSCRIPT')
      expect(clean(doc.text)).to.include(projName)
      expect(clean(doc.text)).to.include('Level: 1 / 5 ')
      expect(clean(doc.text)).to.include('Points: 600 / 2,600 ')
      expect(clean(doc.text)).to.include('Skills: 6 / 26 ')
      expect(clean(doc.text)).to.include('Badges: 1 ')

      // should be a title on the 2nd page
      expect(clean(doc.text)).to.include('Subject: Subject 1'.toUpperCase())

      expect(clean(doc.text)).to.not.include(expectedHeaderAndFooter)
      expect(clean(doc.text)).to.not.include(expectedHeaderAndFooterCommunityProtected)
      expect(clean(doc.text)).to.not.include('null')
    })
  })

  it('ability to configure footer and header text', () => {
    cy.intercept('GET', '/public/config', (req) => {
      req.reply((res) => {
        const conf = res.body;
        conf.exportHeaderAndFooter = 'For {{community.project.descriptor}} enjoyment';
        res.send(conf);
      });
    })
    const projName = `Footer and Header`
    cy.createProject(1, { name: projName })
    cy.request('POST', '/app/userInfo', {
      'first': 'Joe',
      'last': 'Doe',
      'nickname': 'Joe Doe'
    })
    cy.createSubject(1)
    cy.createSkill(1, 1, 1)
    cy.createSkill(1, 1, 2)
    cy.createSkill(1, 1, 3)

    cy.cdVisit('/')
    cy.get('[data-cy="downloadTranscriptBtn"]').click()
    const numExpectedPages = 2
    cy.readTranscript(projName).then((doc) => {
      expect(doc.numpages).to.equal(numExpectedPages)
      expect(clean(doc.text)).to.include('SkillTree TRANSCRIPT')
      expect(clean(doc.text)).to.include(projName)
      expect((clean(doc.text).match(new RegExp(expectedHeaderAndFooter, 'g')) || []).length).to.equal(numExpectedPages*2)
      expect(clean(doc.text)).to.not.include(expectedHeaderAndFooterCommunityProtected)

      expect(clean(doc.text)).to.not.include('null')
    })
  })

  it('ability to configure footer and header text - community protected', () => {
    cy.intercept('GET', '/public/config', (req) => {
      req.reply((res) => {
        const conf = res.body;
        conf.exportHeaderAndFooter = 'For {{community.project.descriptor}} enjoyment';
        res.send(conf);
      });
    })
    const allDragonsUser = 'allDragons@email.org'
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

    const projName = `Footer and Header for User Community`
    cy.createProject(1, { name: projName, enableProtectedUserCommunity: true })
    cy.request('POST', '/app/userInfo', {
      'first': 'Joe',
      'last': 'Doe',
      'nickname': 'Joe Doe'
    })
    cy.createSubject(1)
    cy.createSkill(1, 1, 1)
    cy.createSkill(1, 1, 2)
    cy.createSkill(1, 1, 3)

    cy.cdVisit('/')
    cy.get('[data-cy="downloadTranscriptBtn"]').click()
    const numExpectedPages = 2
    cy.readTranscript(projName).then((doc) => {
      expect(doc.numpages).to.equal(numExpectedPages)
      expect(clean(doc.text)).to.include('SkillTree TRANSCRIPT')
      expect(clean(doc.text)).to.include(projName)
      expect(clean(doc.text)).to.not.include(expectedHeaderAndFooter)
      expect((clean(doc.text).match(new RegExp(expectedHeaderAndFooterCommunityProtected, 'g')) || []).length).to.equal(numExpectedPages*2)

      expect(clean(doc.text)).to.not.include('null')
    })
  })

  it('ability to configure footer and header text with badges', () => {
    cy.intercept('GET', '/public/config', (req) => {
      req.reply((res) => {
        const conf = res.body;
        conf.exportHeaderAndFooter = 'For {{community.project.descriptor}} enjoyment';
        res.send(conf);
      });
    })
    const allDragonsUser = 'allDragons@email.org'
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

    const projName = `Footer and Header Badge on its own page`
    cy.createProject(1, { name: projName, enableProtectedUserCommunity: true })
    cy.request('POST', '/app/userInfo', {
      'first': 'Joe',
      'last': 'Doe',
      'nickname': 'Joe Doe'
    })
    for (let i = 1; i <= 16; i++) {
      createSubjectAndSkills(i, 1)
    }
    const user = Cypress.env('proxyUser')
    cy.doReportSkill({ project: 1, skill: 1, subjNum: 1, userId: user, date: 'now' })
    cy.createBadge(1)
    cy.assignSkillToBadge(1, 1, 1)
    cy.enableBadge(1, 1)

    cy.cdVisit('/')
    cy.get('[data-cy="downloadTranscriptBtn"]').click()

    // 16 subj pages, first page + earned badges page
    const numExpectedPages = 18
    cy.readTranscript(projName).then((doc) => {
      expect(doc.numpages).to.equal(numExpectedPages)
      expect(clean(doc.text)).to.include('SkillTree TRANSCRIPT')
      expect(clean(doc.text)).to.include(projName)
      // 18 pages * 2
      expect((clean(doc.text).match(new RegExp(expectedHeaderAndFooterCommunityProtected, 'g')) || []).length).to.equal(numExpectedPages*2)
      expect(clean(doc.text)).to.not.include(expectedHeaderAndFooter)

      expect(clean(doc.text)).to.not.include('null')
    })
  })

  it('transcript with multiple subject and many skills in each subject', () => {
    cy.intercept('GET', '/public/config', (req) => {
      req.reply((res) => {
        const conf = res.body;
        conf.exportHeaderAndFooter = 'For {{community.project.descriptor}} enjoyment';
        res.send(conf);
      });
    })
    const projName = 'Many Skills'
    cy.request('POST', '/app/userInfo', {
      'first': 'Joe',
      'last': 'Doe',
      'nickname': 'Joe Doe'
    })
    cy.createProject(1, { name: projName })
    const createSubjectAndSkills = (subjNum, numOfSkills) => {
      cy.createSubject(1, subjNum)
      for (let i = 1; i <= numOfSkills; i++) {
        cy.createSkill(1, subjNum, i, { numPerformToCompletion: 1 })
      }
    }
    createSubjectAndSkills(1, 80)
    createSubjectAndSkills(2, 30)
    createSubjectAndSkills(3, 100)

    const user = Cypress.env('proxyUser')
    cy.doReportSkill({ project: 1, skill: 1, subjNum: 1, userId: user, date: 'now' })
    cy.doReportSkill({ project: 1, skill: 3, subjNum: 1, userId: user, date: 'now' })
    cy.doReportSkill({ project: 1, skill: 3, subjNum: 3, userId: user, date: 'now' })
    cy.doReportSkill({ project: 1, skill: 4, subjNum: 3, userId: user, date: 'now' })
    cy.doReportSkill({ project: 1, skill: 5, subjNum: 3, userId: user, date: 'now' })
    cy.doReportSkill({ project: 1, skill: 7, subjNum: 3, userId: user, date: 'now' })


    cy.cdVisit('/')
    cy.get('[data-cy="downloadTranscriptBtn"]').click()

    const numExpectedPages = 9
    cy.readTranscript(projName).then((doc) => {
      expect(doc.numpages).to.equal(numExpectedPages)
      expect(clean(doc.text)).to.include('SkillTree TRANSCRIPT')
      expect(clean(doc.text)).to.include(projName)
      expect(clean(doc.text)).to.include('Level: 0 / 5 ')
      expect(clean(doc.text)).to.include('Points: 600 / 21,000 ')
      expect(clean(doc.text)).to.include('Skills: 6 / 210 ')
      expect(clean(doc.text)).to.not.include('Badges ')

      // should be a title on the 2nd-4th pages
      expect(clean(doc.text)).to.include('Subject: Subject 1'.toUpperCase())
      expect(clean(doc.text)).to.include('Subject: Subject 2'.toUpperCase())
      expect(clean(doc.text)).to.include('Subject: Subject 3'.toUpperCase())
      // 4th subject doesn't have any skills so there shouldn't be a page for it
      expect(clean(doc.text)).to.not.include('Subject: Subject 4'.toUpperCase())

      expect(clean(doc.text)).to.not.include(expectedHeaderAndFooterCommunityProtected)
      expect((clean(doc.text).match(new RegExp(expectedHeaderAndFooter, 'g')) || []).length).to.equal(numExpectedPages*2)
      expect(clean(doc.text)).to.not.include('null')

      // validate that title appears on 3 pages as skills are split between 3 pages
      expect((clean(doc.text).match(new RegExp('SUBJECT: SUBJECT 1', 'g')) || []).length).to.equal(3)
      expect((clean(doc.text).match(new RegExp('SUBJECT: SUBJECT 2', 'g')) || []).length).to.equal(1)
      expect((clean(doc.text).match(new RegExp('SUBJECT: SUBJECT 3', 'g')) || []).length).to.equal(4)

      expect(clean(doc.text)).to.include('Very Great Skill 100 Subj3')
      expect(clean(doc.text)).to.include('Very Great Skill 20 Subj3')
      expect(clean(doc.text)).to.include('Very Great Skill 40 Subj3')
      expect(clean(doc.text)).to.include('Very Great Skill 60 Subj3')
      expect(clean(doc.text)).to.include('Very Great Skill 80 Subj3')
      expect(clean(doc.text)).to.include('Very Great Skill 100 Subj3')
    })
  })

})