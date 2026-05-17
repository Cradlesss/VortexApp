package com.project_void.vortexapp.ui.common.help;

import java.util.List;

public interface CoachScript {
    List<CoachStep> steps();

    default boolean allowSkipWhenTargetMissing() {
        return true;
    }
}
