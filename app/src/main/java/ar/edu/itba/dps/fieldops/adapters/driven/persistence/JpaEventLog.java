package ar.edu.itba.dps.fieldops.adapters.driven.persistence;

import ar.edu.itba.dps.fieldops.business.models.events.ConsumerName;
import ar.edu.itba.dps.fieldops.business.models.events.DeliveryStatus;
import ar.edu.itba.dps.fieldops.business.models.events.EventId;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;
import ar.edu.itba.dps.fieldops.business.models.responses.EventDeliveryView;
import ar.edu.itba.dps.fieldops.business.models.responses.RecordedEventView;
import ar.edu.itba.dps.fieldops.business.providers.EventLog;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Transactional(readOnly = true)
public class JpaEventLog implements EventLog {
    private final DomainEventJpaRepository events;
    private final EventDeliveryJpaRepository deliveries;

    public JpaEventLog(DomainEventJpaRepository events, EventDeliveryJpaRepository deliveries) {
        this.events = Objects.requireNonNull(events, "events are required");
        this.deliveries = Objects.requireNonNull(deliveries, "deliveries are required");
    }

    @Override
    public List<RecordedEventView> ofExpedition(ExpeditionId expedition) {
        Objects.requireNonNull(expedition, "expedition is required");
        final var recorded = events.findByExpeditionIdOrderBySequenceAsc(expedition.value());
        if (recorded.isEmpty()) {
            return List.of();
        }
        final var byEvent = deliveriesOf(recorded);
        return recorded.stream()
                .map(event -> toView(event, byEvent.getOrDefault(event.getSequence(), List.of())))
                .toList();
    }

    private Map<Long, List<EventDeliveryEntity>> deliveriesOf(List<DomainEventEntity> recorded) {
        return deliveries.findById_EventSequenceIn(recorded.stream().map(DomainEventEntity::getSequence).toList())
                .stream()
                .collect(Collectors.groupingBy(delivery -> delivery.getId().getEventSequence()));
    }

    private RecordedEventView toView(DomainEventEntity event, List<EventDeliveryEntity> deliveredTo) {
        return new RecordedEventView(
                new EventId(event.getEventId()),
                event.getEventType(),
                event.getOccurredAt(),
                event.getRecordedAt(),
                deliveredTo.stream()
                        .sorted(Comparator.comparing(delivery -> delivery.getId().getConsumer()))
                        .map(this::toView)
                        .toList());
    }

    private EventDeliveryView toView(EventDeliveryEntity delivery) {
        return new EventDeliveryView(
                new ConsumerName(delivery.getId().getConsumer()),
                DeliveryStatus.valueOf(delivery.getStatus()),
                delivery.getAttempts(),
                Optional.ofNullable(delivery.getLastError()));
    }
}
