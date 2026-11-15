package com.datecourse.service;

import com.datecourse.model.Place;
import java.util.List;

@FunctionalInterface
public interface PlaceSource {
    List<Place> findAll() throws Exception;
}
