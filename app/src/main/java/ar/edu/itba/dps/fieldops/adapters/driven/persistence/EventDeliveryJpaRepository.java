package ar.edu.itba.dps.fieldops.adapters.driven.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface EventDeliveryJpaRepository extends JpaRepository<EventDeliveryEntity, EventDeliveryKey> {
    List<EventDeliveryEntity> findById_EventSequenceIn(Collection<Long> sequences);
}
