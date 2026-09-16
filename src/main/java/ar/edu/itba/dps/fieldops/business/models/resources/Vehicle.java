package ar.edu.itba.dps.fieldops.business.models.resources;

import ar.edu.itba.dps.fieldops.business.interfaces.resources.Certifiable;
import ar.edu.itba.dps.fieldops.business.interfaces.resources.Equipment;
import ar.edu.itba.dps.fieldops.business.models.common.DomainArguments;
import ar.edu.itba.dps.fieldops.business.models.common.TimePeriod;
import lombok.Getter;

import java.util.Objects;
import java.util.Set;

public class Vehicle implements Equipment, Certifiable {

    @Getter
    private final String id;
    @Getter
    private final String name;
    @Getter
    private final String licensePlate;
    private final ResourceCategory category;
    private final Certifications certifications;
    private final AvailabilityCalendar calendar = new AvailabilityCalendar();

    public Vehicle(String id, String name, ResourceCategory category, String licensePlate, Set<Certification> certifications) {
        this.id = DomainArguments.requireText(id, "id");
        this.name = DomainArguments.requireText(name, "name");
        this.category = Objects.requireNonNull(category, "category is required");
        this.licensePlate = DomainArguments.requireText(licensePlate, "licensePlate");
        this.certifications = new Certifications(certifications);
    }

    @Override
    public boolean belongsTo(ResourceCategory category) {
        return this.category.equals(category);
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
