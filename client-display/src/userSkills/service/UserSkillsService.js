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
import axios from 'axios';

import store from '@/store/store';

import 'url-search-params-polyfill';
import SkillEnricherUtil from '../utils/SkillEnricherUtil';

axios.defaults.withCredentials = true;

export default {
  authenticationUrl: null,

  userId: new URLSearchParams(window.location.search).get('userId'),

  version: null,

  getUserIdParams() {
    if (!this.userId) {
      return {};
    }

    if (typeof this.userId === 'string') {
      return { userId: this.userId };
    }
    return {
      userId: this.userId.id,
      idType: this.userId.idType,
    };
  },

  getUserIdAndVersionParams() {
    const params = this.getUserIdParams();
    params.version = this.version;

    return params;
  },

  getUserSkills() {
    let response = null;
    response = axios.get(`${store.state.serviceUrl}${this.getServicePath()}/${encodeURIComponent(store.state.projectId)}/summary`, {
      params: this.getUserIdAndVersionParams(),
    }).then((result) => result.data);
    return response;
  },

  getCustomIconCss() {
    let response = null;
    response = axios.get(`${store.state.serviceUrl}${this.getServicePath()}/${encodeURIComponent(store.state.projectId)}/customIconCss`, {
    }).then((result) => result.data);
    return response;
  },

  getCustomGlobalIconCss() {
    let response = null;
    response = axios.get(`${store.state.serviceUrl}/api/icons/customIconCss`, {
    }).then((result) => result.data);
    return response;
  },

  getSubjectSummary(subjectId, includeSkills = true) {
    const params = this.getUserIdAndVersionParams();
    params.includeSkills = includeSkills;
    return axios.get(`${store.state.serviceUrl}${this.getServicePath()}/${encodeURIComponent(store.state.projectId)}/subjects/${encodeURIComponent(subjectId)}/summary`, {
      params,
    }).then((result) => SkillEnricherUtil.addMetaToSummary(result.data));
  },

  getSkillDependencies(skillId) {
    return axios.get(`${store.state.serviceUrl}${this.getServicePath()}/${encodeURIComponent(store.state.projectId)}/skills/${encodeURIComponent(skillId)}/dependencies`, {
      params: this.getUserIdParams(),
    }).then((result) => result.data);
  },

  getSkillSummary(skillId, optionalCrossProjectId, subjectId) {
    let url = `${store.state.serviceUrl}${this.getServicePath()}/${encodeURIComponent(store.state.projectId)}/`;
    if (optionalCrossProjectId) {
        url += `projects/${encodeURIComponent(optionalCrossProjectId)}/`;
    }

    if (subjectId) {
        url += `subjects/${subjectId}/`;
    }
    url += `skills/${encodeURIComponent(skillId)}/summary`;

    return axios.get(url, {
      params: this.getUserIdParams(),
      withCredentials: true,
    }).then((result) => SkillEnricherUtil.addMeta(result.data));
  },

  getBadgeSkills(badgeId, global, includeSkills = true) {
    const requestParams = this.getUserIdAndVersionParams();
    requestParams.global = global;
    requestParams.includeSkills = includeSkills;
    return axios.get(`${store.state.serviceUrl}${this.getServicePath()}/${encodeURIComponent(store.state.projectId)}/badges/${encodeURIComponent(badgeId)}/summary`, {
      params: requestParams,
    }).then((result) => {
      if (includeSkills) {
        const res = SkillEnricherUtil.addMetaToSummary(result.data);
        if (res.projectLevelsAndSkillsSummaries) {
          res.projectLevelsAndSkillsSummaries = res.projectLevelsAndSkillsSummaries.map((summary) => SkillEnricherUtil.addMetaToSummary(summary));
        }
        return res;
      }
      return result.data;
    });
  },

  getBadgeSummaries() {
    return axios.get(`${store.state.serviceUrl}${this.getServicePath()}/${encodeURIComponent(store.state.projectId)}/badges/summary`, {
      params: this.getUserIdAndVersionParams(),
    }).then((result) => result.data.map((summary) => SkillEnricherUtil.addMetaToSummary(summary)));
  },

  getPointsHistory(subjectId) {
    let response = null;
    let url = `${store.state.serviceUrl}${this.getServicePath()}/${encodeURIComponent(store.state.projectId)}/subjects/${encodeURIComponent(subjectId)}/pointHistory`;
    if (!subjectId) {
      url = `${store.state.serviceUrl}${this.getServicePath()}/${encodeURIComponent(store.state.projectId)}/pointHistory`;
    }
    response = axios.get(url, {
      params: this.getUserIdAndVersionParams(),
    }).then((result) => result.data);
    return response;
  },

  reportSkill(skillId, approvalRequestedMsg) {
    let response = null;
    const userIdParams = this.getUserIdParams();
    response = axios.post(`${store.state.serviceUrl}${this.getServicePath()}/${encodeURIComponent(store.state.projectId)}/skills/${encodeURIComponent(skillId)}`, {
      userId: userIdParams.userId,
      idType: userIdParams.idType,
      approvalRequestedMsg,
    }, { handleErrorCode: 400 }).then((result) => result.data);
    return response;
  },

  removeApprovalRejection(rejectionId) {
    let response = null;
    response = axios.delete(`${store.state.serviceUrl}${this.getServicePath()}/${encodeURIComponent(store.state.projectId)}/rejections/${encodeURIComponent(rejectionId)}`, {
      params: this.getUserIdAndVersionParams(),
    }).then((result) => result.data);
    return response;
  },

  getUserSkillsRanking(subjectId) {
    let response = null;
    let url = `${store.state.serviceUrl}${this.getServicePath()}/${encodeURIComponent(store.state.projectId)}/subjects/${encodeURIComponent(subjectId)}/rank`;
    if (!subjectId) {
      url = `${store.state.serviceUrl}${this.getServicePath()}/${encodeURIComponent(store.state.projectId)}/rank`;
    }
    response = axios.get(url, {
      params: this.getUserIdParams(),
    }).then((result) => result.data);
    return response;
  },

  getLeaderboard(subjectId, type) {
    let response = null;
    let url = `${store.state.serviceUrl}${this.getServicePath()}/${encodeURIComponent(store.state.projectId)}/subjects/${encodeURIComponent(subjectId)}/leaderboard?type=${type}`;
    if (!subjectId) {
      url = `${store.state.serviceUrl}${this.getServicePath()}/${encodeURIComponent(store.state.projectId)}/leaderboard?type=${type}`;
    }
    response = axios.get(url, {
      params: this.getUserIdParams(),
    }).then((result) => result.data);
    return response;
  },

  getUserSkillsRankingDistribution(subjectId) {
    let response = null;
    let url = `${store.state.serviceUrl}${this.getServicePath()}/${encodeURIComponent(store.state.projectId)}/subjects/${encodeURIComponent(subjectId)}/rankDistribution`;
    if (!subjectId) {
      url = `${store.state.serviceUrl}${this.getServicePath()}/${encodeURIComponent(store.state.projectId)}/rankDistribution`;
    }
    const requestParams = this.getUserIdParams();
    requestParams.subjectId = subjectId;
    response = axios.get(url, {
      params: requestParams,
    }).then((result) => result.data);
    return response;
  },

  getRankingDistributionUsersPerLevel(subjectId) {
    let response = null;
    let url = `${store.state.serviceUrl}${this.getServicePath()}/${encodeURIComponent(store.state.projectId)}/subjects/${encodeURIComponent(subjectId)}/rankDistribution/usersPerLevel`;
    if (!subjectId) {
      url = `${store.state.serviceUrl}${this.getServicePath()}/${encodeURIComponent(store.state.projectId)}/rankDistribution/usersPerLevel`;
    }
    response = axios.get(url, {
      params: {
        subjectId,
      },
    }).then((result) => result.data);
    return response;
  },

  getDescriptions(parentId, type = 'subject') {
    let url = `${store.state.serviceUrl}${this.getServicePath()}/${encodeURIComponent(store.state.projectId)}/subjects/${encodeURIComponent(parentId)}/descriptions`;
    if (type === 'badge' || type === 'global-badge') {
      url = `${store.state.serviceUrl}${this.getServicePath()}/${encodeURIComponent(store.state.projectId)}/badges/${encodeURIComponent(parentId)}/descriptions`;
    }
    const response = axios.get(url, {
      params: {
        version: this.version,
        global: type === 'global-badge',
      },
    }).then((result) => result.data);
    return response;
  },

  searchSkills(query) {
    return axios.get(`${store.state.serviceUrl}${this.getServicePath()}/${encodeURIComponent(store.state.projectId)}/skills`, {
      params: ({ ...this.getUserIdAndVersionParams(), query, limit: 5 }),
    }).then((result) => result.data);
  },

  validateDescription(description) {
    const body = {
      value: description,
    };
    return axios.post(`${store.state.serviceUrl}/api/validation/description`, body)
        .then((result) => result.data);
  },

  reportPageVisit(path, fullPath) {
    const domain = (window.location !== window.parent.location)
      ? new URL(document.referrer)
      : new URL(document.location.href);
    axios.put(`${store.state.serviceUrl}/api/pageVisit`, {
      path,
      fullPath,
      hostname: domain.hostname,
      port: domain.port,
      protocol: domain.protocol,
      skillDisplay: true,
      projectId: store.state.projectId,
    }, { handleErrorCode: false });
  },

  getVideoTranscript(skillId) {
    return axios.get(`${store.state.serviceUrl}${this.getServicePath()}/${encodeURIComponent(store.state.projectId)}/skills/${skillId}/videoTranscript`)
        .then((result) => result.data);
  },

  getServicePath() {
    return '/api/projects';
  },

  setVersion(version) {
    store.commit('version', version);
    this.version = version;
  },

  setServiceUrl(serviceUrl) {
    this.serviceUrl = serviceUrl;
  },

  setUserId(userId) {
    this.userId = userId;
  },
};
