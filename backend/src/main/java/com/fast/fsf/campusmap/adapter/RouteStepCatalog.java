package com.fast.fsf.campusmap.adapter;

import com.fast.fsf.campusmap.domain.CampusMapRoute;
import java.util.List;

/**
 * Port for accessing route step data.
 */
public interface RouteStepCatalog {
    List<CampusMapRoute> findStepsInOrder(String from, String to);
    boolean routeExists(String from, String to);
}
