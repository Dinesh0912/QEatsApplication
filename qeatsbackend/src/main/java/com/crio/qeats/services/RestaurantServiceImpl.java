
/*
 *
 *  * Copyright (c) Crio.Do 2019. All rights reserved
 *
 */

package com.crio.qeats.services;

import com.crio.qeats.dto.Restaurant;
import com.crio.qeats.exchanges.GetRestaurantsRequest;
import com.crio.qeats.exchanges.GetRestaurantsResponse;
import com.crio.qeats.repositoryservices.RestaurantRepositoryService;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Log4j2
public class RestaurantServiceImpl implements RestaurantService {

  private final Double peakHoursServingRadiusInKms = 3.0;
  private final Double normalHoursServingRadiusInKms = 5.0;
  @Autowired
  private RestaurantRepositoryService restaurantRepositoryService;


  // TODO: CRIO_TASK_MODULE_RESTAURANTSAPI - Implement findAllRestaurantsCloseby.
  // Check RestaurantService.java file for the interface contract.
  @Override
  public GetRestaurantsResponse findAllRestaurantsCloseBy(GetRestaurantsRequest getRestaurantsRequest, LocalTime currentTime) {

    boolean isPeakHour = findWhetherPeakHour(currentTime);

    List<Restaurant> restaurantsList = restaurantRepositoryService.findAllRestaurantsCloseBy(getRestaurantsRequest.getLatitude(), 
    getRestaurantsRequest.getLongitude(), currentTime, isPeakHour ? peakHoursServingRadiusInKms : normalHoursServingRadiusInKms);

    return new GetRestaurantsResponse(restaurantsList);
  }

  public boolean findWhetherPeakHour(LocalTime currentTime) {

    LocalTime peakHourSlot1StartTime = LocalTime.parse("07:59:59");
    LocalTime peakHourSlot1EndTime = LocalTime.parse("10:00:01");
    LocalTime peakHourSlot2StartTime = LocalTime.parse("12:59:59");
    LocalTime peakHourSlot2EndTime = LocalTime.parse("15:00:01");
    LocalTime peakHourSlot3StartTime = LocalTime.parse("18:59:59");
    LocalTime peakHourSlot3EndTime = LocalTime.parse("21:00:01");

    boolean isWithinSlot1 = currentTime.isAfter(peakHourSlot1StartTime) && currentTime.isBefore(peakHourSlot1EndTime) ? true : false;
    boolean isWithinSlot2 = currentTime.isAfter(peakHourSlot2StartTime) && currentTime.isBefore(peakHourSlot2EndTime) ? true : false;
    boolean isWithinSlot3 = currentTime.isAfter(peakHourSlot3StartTime) && currentTime.isBefore(peakHourSlot3EndTime) ? true : false;

    boolean isPeakHour = isWithinSlot1 || isWithinSlot2 || isWithinSlot3 ? true : false;
    return isPeakHour;

  }

  // TODO: CRIO_TASK_MODULE_RESTAURANTSEARCH
  // Implement findRestaurantsBySearchQuery. The request object has the search string.
  // We have to combine results from multiple sources:
  // 1. Restaurants by name (exact and inexact)
  // 2. Restaurants by cuisines (also called attributes)
  // 3. Restaurants by food items it serves
  // 4. Restaurants by food item attributes (spicy, sweet, etc)
  // Remember, a restaurant must be present only once in the resulting list.
  // Check RestaurantService.java file for the interface contract.
  @Override
  public GetRestaurantsResponse findRestaurantsBySearchQuery(
      GetRestaurantsRequest getRestaurantsRequest, LocalTime currentTime) {

        List<Restaurant> restaurantsList = new ArrayList<>();

        if (getRestaurantsRequest.getSearchFor().isEmpty()) {
          return new GetRestaurantsResponse(restaurantsList);
        }

        boolean isPeakHour = findWhetherPeakHour(currentTime);

        List<Restaurant> restaurantsListByName = restaurantRepositoryService.findRestaurantsByName(getRestaurantsRequest.getLatitude(), 
        getRestaurantsRequest.getLongitude(), getRestaurantsRequest.getSearchFor(), currentTime, isPeakHour ? peakHoursServingRadiusInKms : normalHoursServingRadiusInKms);

        List<Restaurant> restaurantsListByAttributes = restaurantRepositoryService.findRestaurantsByAttributes(getRestaurantsRequest.getLatitude(), 
        getRestaurantsRequest.getLongitude(), getRestaurantsRequest.getSearchFor(), currentTime, isPeakHour ? peakHoursServingRadiusInKms : normalHoursServingRadiusInKms);

        // List<Restaurant> restaurantsListByFoodItems = restaurantRepositoryService.findRestaurantsByItemName(getRestaurantsRequest.getLatitude(), 
        // getRestaurantsRequest.getLongitude(), getRestaurantsRequest.getSearchFor(), currentTime, isPeakHour ? peakHoursServingRadiusInKms : normalHoursServingRadiusInKms);

        // List<Restaurant> restaurantsListByFodItemAttributes = restaurantRepositoryService.findRestaurantsByItemAttributes(getRestaurantsRequest.getLatitude(), 
        // getRestaurantsRequest.getLongitude(), getRestaurantsRequest.getSearchFor(), currentTime, isPeakHour ? peakHoursServingRadiusInKms : normalHoursServingRadiusInKms);


        if (restaurantsListByName != null) {
          for (Restaurant restaurant : restaurantsListByName) {
            if (!restaurantsList.contains(restaurant)) {
              restaurantsList.add(restaurant);
            }
          }
        }
        
        if (restaurantsListByAttributes != null) {
          for (Restaurant restaurant : restaurantsListByAttributes) {
            if (!restaurantsList.contains(restaurant)) {
              restaurantsList.add(restaurant);
            }
          }
        }
        
        // if (restaurantsListByFoodItems != null) {
        //   for (Restaurant restaurant : restaurantsListByFoodItems) {
        //     restaurantsSet.add(restaurant);
        //   }
        // }
        
        // if (restaurantsListByFodItemAttributes != null) {
        //   for (Restaurant restaurant : restaurantsListByFodItemAttributes) {
        //     restaurantsSet.add(restaurant);
        //   }
        // }
        
        return new GetRestaurantsResponse(restaurantsList);
  }

  // TODO: CRIO_TASK_MODULE_MULTITHREADING
  // Implement multi-threaded version of RestaurantSearch.
  // Implement variant of findRestaurantsBySearchQuery which is at least 1.5x time faster than
  // findRestaurantsBySearchQuery.
  @Override
  public GetRestaurantsResponse findRestaurantsBySearchQueryMt(
      GetRestaurantsRequest getRestaurantsRequest, LocalTime currentTime) {

     return null;
  }
}

