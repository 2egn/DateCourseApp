package com.datecourse.service;

import com.datecourse.model.RoutePlan;
import java.io.IOException;

/** Creates an independent image without a browser, map tiles or external API. */
@FunctionalInterface
public interface CoursePreviewRenderer {
    byte[] render(RoutePlan plan) throws IOException;
}
