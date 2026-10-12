package ar.edu.itba.dps.fieldops.business.rules;

import ar.edu.itba.dps.fieldops.business.interfaces.activities.ActivityRules;
import ar.edu.itba.dps.fieldops.business.models.activities.ReusableRequirement;
import ar.edu.itba.dps.fieldops.business.models.activities.RiskLevel;
import ar.edu.itba.dps.fieldops.business.models.resources.Certification;
import ar.edu.itba.dps.fieldops.business.models.resources.ResourceCategory;
import ar.edu.itba.dps.fieldops.business.models.zones.Permit;
import ar.edu.itba.dps.fieldops.business.models.zones.Zone;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class NightOperationRules extends ActivityRulesDecorator {

    private static final String NIGHT_OPERATIONS = "Night operations";
    private static final Certification NIGHT_CERTIFICATION = new Certification(NIGHT_OPERATIONS);
    private static final ReusableRequirement LIGHTING = new ReusableRequirement(new ResourceCategory("Lighting"), 1);

    public NightOperationRules(ActivityRules base) {
        super(base);
    }

    @Override
    public RiskLevel estimatedRisk() {
        return super.estimatedRisk().next();
    }

    @Override
    public List<ReusableRequirement> requiredEquipment() {
        return Stream.concat(super.requiredEquipment().stream(), Stream.of(LIGHTING)).toList();
    }

    @Override
    public Set<Certification> requiredCertificationsForAllStaff() {
        return Stream.concat(super.requiredCertificationsForAllStaff().stream(), Stream.of(NIGHT_CERTIFICATION))
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public Set<Permit> requiredPermitsIn(Zone zone) {
        return Stream.concat(super.requiredPermitsIn(zone).stream(), Stream.of(zone.specialPermitFor(NIGHT_OPERATIONS)))
                .collect(Collectors.toUnmodifiableSet());
    }
}
