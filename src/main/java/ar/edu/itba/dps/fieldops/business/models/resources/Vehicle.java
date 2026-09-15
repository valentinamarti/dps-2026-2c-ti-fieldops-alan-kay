package ar.edu.itba.dps.fieldops.business.models.resources;

import ar.edu.itba.dps.fieldops.business.interfaces.resources.Certifiable;
import ar.edu.itba.dps.fieldops.business.interfaces.resources.ReusableResource;
import ar.edu.itba.dps.fieldops.business.models.common.TimePeriod;
import lombok.Getter;

import java.util.Objects;
import java.util.Set;

public class Vehicle implements ReusableResource, Certifiable {

    @Getter
    private final String id;
    @Getter
    private final String name;
    @Getter
    private final String licensePlate;
    private final Certifications certifications;
    private final AvailabilityCalendar calendar = new AvailabilityCalendar();

    public Vehicle(String id, String name, String licensePlate, Set<Certification> certifications) {
        this.id = Objects.requireNonNull(id, "id is required");
        this.name = Objects.requireNonNull(name, "name is required");
        this.licensePlate = Objects.requireNonNull(licensePlate, "licensePlate is required");
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
