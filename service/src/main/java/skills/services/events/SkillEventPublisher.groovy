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
package skills.services.events

import callStack.profiler.Profile
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.event.EventListener
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.messaging.simp.broker.BrokerAvailabilityEvent
import org.springframework.stereotype.Component
import skills.websocket.SubscribedDestinationRegistry

@Component
@Slf4j
class SkillEventPublisher {

    private brokerAvailable = false

    @Autowired
    SubscribedDestinationRegistry destinationRegistry

    @Autowired
    SimpMessagingTemplate messagingTemplate;

    @Profile
    void publishSkillUpdate(SkillEventResult result, String userId) {
        log.debug("Reporting user skill for user [{}}], result [{}}]", userId, result)
        if (brokerAvailable) {
            if (result.projectId) {
                messagingTemplate.convertAndSendToUser(userId, "/queue/${result.projectId}-skill-updates", result)
            } else {
                List<String> destinations = destinationRegistry.getAllDestinationsForUser(userId)
                if (log.isDebugEnabled()) {
                    log.debug("got [${destinations?.size()}] subscribed destinations for user [$userId]")
                }
                destinations = destinations?.unique()
                destinations?.each {
                    it = it.replace("/user", "")
                    messagingTemplate.convertAndSendToUser(userId, it, result)
                }
            }
        } else {
            log.warn("Failed to publish skill update since the broker is unavailable. user [${userId}], result [${result}]")
        }
    }

    @EventListener
    void handleBrokerAvailabilityEvent(BrokerAvailabilityEvent event) {
        log.info("BrokerAvailabilityEvent ["+event+"]")
        this.brokerAvailable = event.brokerAvailable
    }
}
