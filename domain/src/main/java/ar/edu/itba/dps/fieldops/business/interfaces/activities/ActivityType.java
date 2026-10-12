package ar.edu.itba.dps.fieldops.business.interfaces.activities;

import ar.edu.itba.dps.fieldops.business.models.activities.ActivityParameters;
import ar.edu.itba.dps.fieldops.business.models.activities.ActivityTypeName;

import java.util.List;

public interface ActivityType {

    ActivityTypeName name();

    List<String> parameterNames();

    ActivityRules rulesWith(ActivityParameters parameters);
}
