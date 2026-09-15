package ar.edu.itba.dps.fieldops.business.interfaces.resources;

import ar.edu.itba.dps.fieldops.business.models.common.TimePeriod;

public interface ReusableResource extends Resource {

    boolean isAvailableDuring(TimePeriod period);

    void reserve(TimePeriod period);
}
