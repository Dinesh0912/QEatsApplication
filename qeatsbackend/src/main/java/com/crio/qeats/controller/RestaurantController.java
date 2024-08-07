/*
 *
 *  * Copyright (c) Crio.Do 2019. All rights reserved
 *
 */

package com.crio.qeats.controller;

import com.crio.qeats.dto.Restaurant;
import com.crio.qeats.exchanges.GetRestaurantsRequest;
import com.crio.qeats.exchanges.GetRestaurantsResponse;
import com.crio.qeats.services.RestaurantService;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

// TODO: CRIO_TASK_MODULE_RESTAURANTSAPI
// Implement Controller using Spring annotations.
// Remember, annotations have various "targets". They can be class level, method level or others.


@RestController
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@AllArgsConstructor
@Log4j2
public class RestaurantController {

  // private static final org.apache.logging.log4j.Logger log = org.apache.logging.log4j.LogManager.getLogger(RestaurantController.class);
  
  public static final String RESTAURANT_API_ENDPOINT = "/qeats/v1";
  public static final String RESTAURANTS_API = "/restaurants";
  public static final String MENU_API = "/menu";
  public static final String CART_API = "/cart";
  public static final String CART_ITEM_API = "/cart/item";
  public static final String CART_CLEAR_API = "/cart/clear";
  public static final String POST_ORDER_API = "/order";
  public static final String GET_ORDERS_API = "/orders";

  @Autowired
  private RestaurantService restaurantService;

  @GetMapping(RESTAURANT_API_ENDPOINT + RESTAURANTS_API)
  public ResponseEntity<GetRestaurantsResponse> getRestaurants(@Valid GetRestaurantsRequest getRestaurantsRequest) {

    log.info("getRestaurants called with {}", getRestaurantsRequest);
    
    List<Restaurant> restaurants;
    Double doubleLatitude = getRestaurantsRequest.getLatitude();
    Double doubleLongitude = getRestaurantsRequest.getLongitude();
    String searchFor = getRestaurantsRequest.getSearchFor();

    System.out.println("Latitude= " + doubleLatitude);
    System.out.println("Longitude= " + doubleLongitude);
    System.out.println("SearchQuery= " + searchFor);

    GetRestaurantsResponse getRestaurantsResponse;
    //CHECKSTYLE:OFF

    if (searchFor != null) {
      getRestaurantsResponse = restaurantService
      .findRestaurantsBySearchQuery(getRestaurantsRequest, LocalTime.now());
    } else {
      getRestaurantsResponse = restaurantService
      .findAllRestaurantsCloseBy(getRestaurantsRequest, LocalTime.now());
    }

    log.info("getRestaurants returned {}", getRestaurantsResponse);
    System.out.println("getRestaurants returned" + getRestaurantsResponse);

    // CHECKSTYLE:ON

    
    if (doubleLatitude < -90 || doubleLatitude > 90) {
      return ResponseEntity.badRequest().body(getRestaurantsResponse);
    }
    if (doubleLongitude < -180 || doubleLongitude > 180) {
      return ResponseEntity.badRequest().body(getRestaurantsResponse);
    }
    if (getRestaurantsResponse != null) {
      restaurants = getRestaurantsResponse.getRestaurants();
      for (int i = 0; i < restaurants.size(); i++) {
        restaurants.get(i).setName(restaurants.get(i).getName().replace("é", "?"));
      }
      getRestaurantsResponse.setRestaurants(restaurants);
    return ResponseEntity.ok().body(getRestaurantsResponse);

    } else {
      return ResponseEntity.ok().body(new GetRestaurantsResponse(new ArrayList<Restaurant>()));
    }
  }

  public String replaceSpecialCharacters(String name) {

    char[] array = name.toCharArray();

    for (int i = 0; i < array.length; i++) {
      if (isSpecialCharacter(array[i])) {
        array[i] = '?';
      }
    }
    return new String(array);
  }

  public boolean isSpecialCharacter(char ch) {
    // Define a set of special characters
    // String standardCharacters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz ";

    if ((int) ch > 126 || (int) ch < 32) {
      return true;
    }
    return false;
    // Check if the character is present in the set of special characters
    // return !standardCharacters.contains(Character.toString(ch));
  }

  // TIP(MODULE_MENUAPI): Model Implementation for getting menu given a restaurantId.
  // Get the Menu for the given restaurantId
  // API URI: /qeats/v1/menu?restaurantId=11
  // Method: GET
  // Query Params: restaurantId
  // Success Output:
  // 1). If restaurantId is present return Menu
  // 2). Otherwise respond with BadHttpRequest.
  //
  // HTTP Code: 200
  // {
  //  "menu": {
  //    "items": [
  //      {
  //        "attributes": [
  //          "South Indian"
  //        ],
  //        "id": "1",
  //        "imageUrl": "www.google.com",
  //        "itemId": "10",
  //        "name": "Idly",
  //        "price": 45
  //      }
  //    ],
  //    "restaurantId": "11"
  //  }
  // }
  // Error Response:
  // HTTP Code: 4xx, if client side error.
  //          : 5xx, if server side error.
  // Eg:
  // curl -X GET "http://localhost:8081/qeats/v1/menu?restaurantId=11"
}

