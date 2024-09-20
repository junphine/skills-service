/**
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
package skills.storage.repos

import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.lang.Nullable
import skills.storage.model.ProjDef
import skills.storage.model.SkillsDBLock

import jakarta.persistence.LockModeType

interface SkillsDBLockRepo extends CrudRepository<SkillsDBLock, Integer> {

    @Nullable
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    SkillsDBLock findByLock(String lock)

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query('''select p.id 
        from ProjDef p 
        where
            lower(p.projectId) = lower(?1)''')
    Integer findByProjectIdIgnoreCase(String projectId)

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query('''select q.id 
        from QuizDef q 
        where
            lower(q.quizId) = lower(?1)''')
    Integer findByQuizDefIdIgnoreCase(String quizId)

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query('''select attrs.id 
        from UserAttrs attrs 
        where
            attrs.userId = ?1''')
    Integer findUserAttrsByUserId(String userId)

    @Modifying
    @Query(value="delete from skills_db_locks where created < ?1  and expires='true'", nativeQuery=true)
    void deleteByCreatedBeforeAndExpires(Date date)

    @Query(value="select * from f_select_lock_and_insert(?1);", nativeQuery=true)
    SkillsDBLock insertLockOrSelectExisting(String lockKey)
}
