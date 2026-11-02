package ar.edu.itba.dps.fieldops.business.ports.in;

import ar.edu.itba.dps.fieldops.business.models.responses.ActivityTypeView;

import java.util.List;

public interface QueryActivityTypesPort {
    List<ActivityTypeView> list();
}
