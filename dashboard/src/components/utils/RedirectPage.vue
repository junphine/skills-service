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
import { ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';

const route = useRoute();
const router = useRouter();

const timeoutForDisplay = ref(20);

const timer = setInterval(() => {
  timeoutForDisplay.value = timeoutForDisplay.value - 1;
  if(timeoutForDisplay.value === 0) {
    clearInterval(timer);
    router.push( route.query.nextPage );
  }
}, 1000)

</script>

<template>
  <div class="my-5">
    <div class="text-center text-color-secondary">
      <div class="flex justify-content-center">
        <div class="border-circle w-6rem h-6rem m-2 surface-500 font-bold flex align-items-center justify-content-center">
          <i class="text-0 text-6xl fas fa-exclamation-triangle"></i>
        </div>
      </div>
    </div>
    <div class="text-center text-color-secondary text-2xl mt-2">
      This page has moved
    </div>

    <div class="container-fluid">
      <div class="row justify-content-center mt-3">
        <div class="col col-sm-8 col-md-6 col-lg-4 text-center" data-cy="redirectExplanation">
          You seem to have followed an old link.
          You will be redirected to <router-link :to="route.query.nextPage" data-cy="newLink">{{ route.query.nextPage }}</router-link> shortly.
          Please update your bookmarks to the new link.

          <div class="mt-5">
            <div v-if="timeoutForDisplay > 0">
              Redirecting you to the new page in {{ timeoutForDisplay }} seconds...
            </div>
            <div v-else>
              Redirecting...
            </div>
          </div>
        </div>
      </div>
      <div class="text-center mt-5">
        <router-link :to="route.query.nextPage" tabindex="-1">
          <SkillsButton
              label="Take Me There Now"
              icon="fas fa-arrow-circle-right"
              outlined
              size="medium"
              severity="info"
              data-cy="takeMeThere"
              class="" />
        </router-link>
      </div>
    </div>
  </div>
</template>

<style scoped>

</style>