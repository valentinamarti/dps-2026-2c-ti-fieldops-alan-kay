package ar.edu.itba.dps.fieldops.business.interfaces.resources;

import ar.edu.itba.dps.fieldops.business.models.resources.ResourceCategory;

public interface Categorized {

    boolean belongsTo(ResourceCategory category);
}
