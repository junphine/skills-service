/*
Copyright 2024 SkillTree

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
<script setup>
import { ref, computed } from 'vue'
import { useAppInfoState } from '@/stores/UseAppInfoState.js'
import { useRoute } from 'vue-router'
import { useColors } from '@/skills-display/components/utilities/UseColors.js'
import ContactOwnersDialog from '@/components/myProgress/ContactOwnersDialog.vue'

const appInfo = useAppInfoState()
const route = useRoute()
const colors = useColors()

const showContact = ref(false)

const projectId = computed(() => route.params.projectId)
</script>

<template>
  <div class="mt-8">
    <div class="flex justify-content-center">
      <div class="border-circle w-6rem h-6rem m-2 surface-500 font-bold flex align-items-center justify-content-center">
        <i class="text-0 text-7xl fas fa-shield-alt"></i>
      </div>
    </div>
    <div class="text-center">
      <div class="text-2xl text-primary">Invite Only Project</div>
    </div>

    <div class="row justify-content-center text-danger mt-3">
      <div class="col col-sm-8 col-md-6 col-lg-4 text-center" data-cy="notAuthorizedExplanation">
        <p>
          This Project is configured for Invite Only access. You can concat project's administrators to request access.
        </p>
        <p v-if="appInfo.emailEnabled">
          <SkillsButton
            label="Contact Project"
            icon="fas fa-mail-bulk"
            @click="showContact=true"
            data-cy="contactOwnerBtn" />
        </p>
      </div>
    </div>
    <contact-owners-dialog v-if="showContact"
                           v-model="showContact"
                           :project-id="projectId"
                           :project-name="projectId"
                            />
  </div>
</template>

<style scoped>

</style>