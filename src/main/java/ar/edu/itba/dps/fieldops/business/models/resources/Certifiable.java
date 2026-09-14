package ar.edu.itba.dps.fieldops.business.models.resources;

/**
 * Anything that can hold certifications, so validations can ask for them without knowing the concrete resource.
 */
public interface Certifiable {

    boolean hasCertification(Certification certification);
}
