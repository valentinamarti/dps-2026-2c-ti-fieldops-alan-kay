package ar.edu.itba.dps.fieldops.business.models.resources;

import ar.edu.itba.dps.fieldops.business.models.common.TimePeriod;
import lombok.Getter;

import java.util.Objects;
import java.util.Set;

@Getter
public class Instrument implements ReusableResource, Certifiable {

    private final String id;
    private final String name;
    private final Certifications certifications;
    private final AvailabilityCalendar calendar = new AvailabilityCalendar();

    public Instrument(String id, String name, Set<Certification> certifications) {
        this.id = Objects.requireNonNull(id, "id is required");
        this.name = Objects.requireNonNull(name, "name is required");
        this.certifications = new Certifications(certifications);
    }

    @Override
    public boolean hasCertification(Certification certification) {
        return certifications.includes(certification);
    }

    @Override
    public boolean isAvailableDuring(TimePeriod period) {
        return calendar.isFreeDuring(period);
    }

    @Override
    public void reserve(TimePeriod period) {
        calendar.book(period);
    }
}
