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
export default class TableStateUtil {
  static saveTableSortState(id, sortBy, sortDesc) {
    let sorting = JSON.parse(localStorage.getItem('tableState'));
    if (!sorting) {
      sorting = {};
    }
    sorting[id] = {
      sortBy,
      sortDesc,
    };
    localStorage.setItem('tableState', JSON.stringify(sorting));
  }

  static loadTableState(id) {
    const sorting = JSON.parse(localStorage.getItem('tableState'));
    if (sorting && sorting[id]) {
      return sorting[id];
    }
    return null;
  }
}
