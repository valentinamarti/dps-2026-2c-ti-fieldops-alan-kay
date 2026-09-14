package ar.edu.itba.dps.fieldops.business.models.resources;

import ar.edu.itba.dps.fieldops.business.models.common.TimePeriod;

import java.util.Objects;
import java.util.Set;

public class Person implements ReusableResource, Certifiable {

    private final String id;
    private final String name;
    private final Certifications certifications;
    private final AvailabilityCalendar calendar = new AvailabilityCalendar();

    public Person(String id, String name, Set<Certification> certifications) {
        this.id = Objects.requireNonNull(id, "id is required");
        this.name = Objects.requireNonNull(name, "name is required");
        this.certifications = new Certifications(certifications);
    }

    @Override
    public String id() {
        return id;
    }

    public String name() {
        return name;
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
