package ar.edu.itba.dps.fieldops.adapters.driven.persistence;

import ar.edu.itba.dps.fieldops.business.models.activities.ActivityId;
import ar.edu.itba.dps.fieldops.business.models.events.EventId;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;
import ar.edu.itba.dps.fieldops.business.models.report.ProgressEntry;
import ar.edu.itba.dps.fieldops.business.providers.ProgressHistory;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Transactional
public class JpaProgressHistory implements ProgressHistory {
    private final ProgressEntryJpaRepository entries;

    public JpaProgressHistory(ProgressEntryJpaRepository entries) {
        this.entries = Objects.requireNonNull(entries, "entries are required");
    }

    @Override
    public void add(ProgressEntry entry) {
        Objects.requireNonNull(entry, "entry is required");
        entries.save(toEntity(entry));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProgressEntry> ofExpedition(ExpeditionId expedition) {
        Objects.requireNonNull(expedition, "expedition is required");
        return entries.findByExpeditionIdOrderBySequenceAsc(expedition.value()).stream()
                .map(JpaProgressHistory::toEntry)
                .toList();
    }

    private static ProgressEntryEntity toEntity(ProgressEntry entry) {
        final var entity = new ProgressEntryEntity();
        entity.setEventId(entry.event().value());
        entity.setExpeditionId(entry.expedition().value());
        entity.setActivityId(entry.activity().value());
        entity.setOccurredAt(entry.occurredAt());
        entity.setFinishedActivities(entry.finishedActivities());
        entity.setScheduledActivities(entry.scheduledActivities());
        return entity;
    }

    private static ProgressEntry toEntry(ProgressEntryEntity entity) {
        return new ProgressEntry(new EventId(entity.getEventId()), new ExpeditionId(entity.getExpeditionId()),
                new ActivityId(entity.getActivityId()), entity.getOccurredAt(), entity.getFinishedActivities(),
                entity.getScheduledActivities());
    }
}
