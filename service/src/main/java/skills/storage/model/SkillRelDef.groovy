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
package skills.storage.model

import groovy.transform.ToString
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener

import jakarta.persistence.*

@ToString(excludes =['parent', 'child'])
@Entity()
@Table(name='skill_relationship_definition')
@EntityListeners(AuditingEntityListener)
class SkillRelDef {

    enum RelationshipType { RuleSetDefinition, Dependence, BadgeRequirement, Recommendation, SkillsGroupRequirement, GroupSkillToSubject, Tag }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer id

    @ManyToOne
    @JoinColumn(name="parentRefId")
    SkillDef parent

    @ManyToOne
    @JoinColumn(name="childRefId")
    SkillDef child

    @Enumerated(EnumType.STRING)
    RelationshipType type = RelationshipType.RuleSetDefinition

    @Temporal(TemporalType.TIMESTAMP)
    @CreatedDate
    Date created

    @Temporal(TemporalType.TIMESTAMP)
    @LastModifiedDate
    Date updated
}
