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
import { useAppConfig } from '@/common-components/stores/UseAppConfig.js'

const appConfig = useAppConfig()
</script>

<template>
  <Message
    v-if="appConfig.dbUpgradeInProgress"
    icon="fas fa-tools"
    severity="error"
    :closable="false"
    class="mx-2"
    data-cy="upgradeInProgressWarning">
    An upgrade is currently in process. Please note that <b>no changes</b> will be permitted until the upgrade is complete.
          Any reported skills will be queued for application once the upgrade has completed.
  </Message>
</template>

<style scoped>

</style>