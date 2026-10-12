package ar.edu.itba.dps.fieldops.business.interfaces.replanning;

import ar.edu.itba.dps.fieldops.business.models.expeditions.ItineraryItem;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ReplannedActivity;

import java.util.Optional;

public interface ReplanReason {

    Optional<ReplannedActivity> replan(ItineraryItem affected);
}
