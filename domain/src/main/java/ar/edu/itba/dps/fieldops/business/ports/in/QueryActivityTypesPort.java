package ar.edu.itba.dps.fieldops.business.ports.in;

import ar.edu.itba.dps.fieldops.business.interfaces.activities.ActivityType;

import java.util.List;

public interface QueryActivityTypesPort {
    List<ActivityType> list();
}
