package ar.edu.itba.dps.fieldops.business.interfaces.resources;

import ar.edu.itba.dps.fieldops.business.models.resources.Certification;

public interface Certifiable {
    boolean hasCertification(Certification certification);
}
