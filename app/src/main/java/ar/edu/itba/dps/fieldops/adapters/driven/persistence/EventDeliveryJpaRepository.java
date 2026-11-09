package ar.edu.itba.dps.fieldops.adapters.driven.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface EventDeliveryJpaRepository extends JpaRepository<EventDeliveryEntity, EventDeliveryKey> {
    List<EventDeliveryEntity> findById_EventSequenceIn(Collection<Long> sequences);

    @Query("""
            SELECT delivery.id FROM EventDeliveryEntity delivery
            JOIN DomainEventEntity event ON event.sequence = delivery.id.eventSequence
            WHERE delivery.status = 'PENDING'
              AND delivery.nextAttemptAt <= :now
              AND NOT EXISTS (
                  SELECT earlier.id.eventSequence FROM EventDeliveryEntity earlier
                  JOIN DomainEventEntity earlierEvent ON earlierEvent.sequence = earlier.id.eventSequence
                  WHERE earlier.status = 'PENDING'
                    AND earlier.id.consumer = delivery.id.consumer
                    AND earlierEvent.expeditionId = event.expeditionId
                    AND earlier.id.eventSequence < delivery.id.eventSequence)
            ORDER BY delivery.id.eventSequence
            """)
    List<EventDeliveryKey> findReadyToDeliver(@Param("now") LocalDateTime now);
}
