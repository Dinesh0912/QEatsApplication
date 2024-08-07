/*
 *
 *  * Copyright (c) Crio.Do 2019. All rights reserved
 *
 */

package com.crio.qeats.repositoryservices;

import ch.hsr.geohash.GeoHash;
import com.crio.qeats.configs.RedisConfiguration;
import com.crio.qeats.dto.Restaurant;
import com.crio.qeats.globals.GlobalConstants;
import com.crio.qeats.models.ItemEntity;
import com.crio.qeats.models.MenuEntity;
import com.crio.qeats.models.RestaurantEntity;
import com.crio.qeats.repositories.ItemRepository;
import com.crio.qeats.repositories.MenuRepository;
import com.crio.qeats.repositories.RestaurantRepository;
import com.crio.qeats.utils.GeoLocation;
import com.crio.qeats.utils.GeoUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.inject.Provider;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;

@Primary
@Service
public class RestaurantRepositoryServiceImpl implements RestaurantRepositoryService {



  @Autowired
  private RestaurantRepository restaurantRepository;

  @Autowired
  private MenuRepository menuRepository;

  @Autowired
  private ItemRepository itemRepository;

  @Autowired
  private RedisConfiguration redisConfiguration;

  @Autowired
  private MongoTemplate mongoTemplate;

  @Autowired
  private Provider<ModelMapper> modelMapperProvider;

  private boolean isOpenNow(LocalTime time, RestaurantEntity res) {
    LocalTime openingTime = LocalTime.parse(res.getOpensAt());
    LocalTime closingTime = LocalTime.parse(res.getClosesAt());

    System.out.println("Opening Time = <---------->  : " + openingTime);
    System.out.println("Closing Time = <---------->  : " + closingTime);
    return time.isAfter(openingTime) && time.isBefore(closingTime);
  }

  // TODO: CRIO_TASK_MODULE_NOSQL
  // Objectives:
  // 1. Implement findAllRestaurantsCloseby.
  // 2. Remember to keep the precision of GeoHash in mind while using it as a key.
  // Check RestaurantRepositoryService.java file for the interface contract.
  public List<Restaurant> findAllRestaurantsCloseBy(Double latitude, Double longitude, LocalTime currentTime,
      Double servingRadiusInKms) {
    List<Restaurant> restaurants = null;
    if (redisConfiguration.isCacheAvailable()) {
      restaurants = findAllRestaurantsCloseByFromCache(latitude, longitude, currentTime, servingRadiusInKms);
    } else {
      restaurants = findAllRestaurantsCloseFromDb(latitude, longitude, currentTime, servingRadiusInKms);
    }
    return restaurants;
  }

  private List<Restaurant> findAllRestaurantsCloseFromDb(Double latitude, Double longitude, LocalTime currentTime,
      Double servingRadiusInKms) {
    ModelMapper modelMapper = modelMapperProvider.get();
    List<RestaurantEntity> restaurantEntities = restaurantRepository.findAll();
    List<Restaurant> restaurants = new ArrayList<Restaurant>();
    for (RestaurantEntity restaurantEntity : restaurantEntities) {
      if (isRestaurantCloseByAndOpen(restaurantEntity, currentTime, latitude, longitude, servingRadiusInKms)) {
        restaurants.add(modelMapper.map(restaurantEntity, Restaurant.class));
      }
    }
    return restaurants;
  }

  private List<Restaurant> findAllRestaurantsCloseByFromCache(Double latitude, Double longitude, LocalTime currentTime,
      Double servingRadiusInKms) {
    List<Restaurant> restaurantList = new ArrayList<>();

    GeoLocation geoLocation = new GeoLocation(latitude, longitude);
    GeoHash geoHash = GeoHash.withCharacterPrecision(geoLocation.getLatitude(), geoLocation.getLongitude(), 7);

    try (Jedis jedis = redisConfiguration.getJedisPool().getResource()) {
      String jsonStringFromCache = jedis.get(geoHash.toBase32());

      if (jsonStringFromCache == null) {
        // Cache needs to be updated.
        String createdJsonString = "";
        try {
          restaurantList = findAllRestaurantsCloseFromDb(geoLocation.getLatitude(), geoLocation.getLongitude(),
              currentTime, servingRadiusInKms);
          createdJsonString = new ObjectMapper().writeValueAsString(restaurantList);
        } catch (JsonProcessingException e) {
          e.printStackTrace();
        }

        // Do operations with jedis resource
        jedis.setex(geoHash.toBase32(), GlobalConstants.REDIS_ENTRY_EXPIRY_IN_SECONDS, createdJsonString);
      } else {
        try {
          restaurantList = new ObjectMapper().readValue(jsonStringFromCache, new TypeReference<List<Restaurant>>() {
          });
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }

    return restaurantList;
  }

  // TODO: CRIO_TASK_MODULE_RESTAURANTSEARCH
  // Objective:
  // Find restaurants whose names have an exact or partial match with the search query.
  @Override
  public List<Restaurant> findRestaurantsByName(Double latitude, Double longitude,
      String searchString, LocalTime currentTime, Double servingRadiusInKms) {

    boolean foundMatchForSearchString = false;

    List<Restaurant> restaurantsLatLong = findAllRestaurantsCloseBy(latitude, longitude, currentTime, servingRadiusInKms);
    List<Restaurant> restaurantsSearchFor = new ArrayList<>();

    for (Restaurant restaurant: restaurantsLatLong) {
      if (restaurant.getName().contains(searchString) && !restaurantsSearchFor.contains(restaurant)) {
        foundMatchForSearchString = true;
        restaurantsSearchFor.add(restaurant);
      }
    }

    if (foundMatchForSearchString) {
      return restaurantsSearchFor;
    }
    return restaurantsLatLong;
  }


  // TODO: CRIO_TASK_MODULE_RESTAURANTSEARCH
  // Objective:
  // Find restaurants whose attributes (cuisines) intersect with the search query.
  @Override
  public List<Restaurant> findRestaurantsByAttributes(
      Double latitude, Double longitude,
      String searchString, LocalTime currentTime, Double servingRadiusInKms) {

    boolean foundMatchForSearchString = false;

    List<Restaurant> restaurantsLatLong = findAllRestaurantsCloseBy(latitude, longitude, currentTime, servingRadiusInKms);
    List<Restaurant> restaurantsSearchFor = new ArrayList<>();

    for (Restaurant restaurant: restaurantsLatLong) {
      List<String> attributesList = restaurant.getAttributes();
      for (String attribute : attributesList) {
        if (attribute.contains(searchString) && !restaurantsSearchFor.contains(restaurant)) {
          foundMatchForSearchString = true;
          restaurantsSearchFor.add(restaurant);
          break;
        }
      }
    }

    if (foundMatchForSearchString) {
      return restaurantsSearchFor;
    }
    return restaurantsLatLong;
  }



  // TODO: CRIO_TASK_MODULE_RESTAURANTSEARCH
  // Objective:
  // Find restaurants which serve food items whose names form a complete or partial match
  // with the search query.

  @Override
  public List<Restaurant> findRestaurantsByItemName(
      Double latitude, Double longitude,
      String searchString, LocalTime currentTime, Double servingRadiusInKms) {

    Optional<List<ItemEntity>> itemsOptional = itemRepository.findItemsByNameExact(searchString);
    List<ItemEntity> items = itemsOptional.get();

    if (items == null) {
      return new ArrayList<Restaurant>();
    }

    List<String> itemIdList = new ArrayList<>();
    for (ItemEntity item : items) {
      itemIdList.add(item.getItemId());
    }
    Optional<List<MenuEntity>> menusOptional = menuRepository.findMenusByItemsItemIdIn(itemIdList);
    List<MenuEntity> menus = menusOptional.get();

    if (menus == null) {
      return new ArrayList<Restaurant>();
    }

    List<String> restaurantIds = new ArrayList<>();
    for (MenuEntity menu : menus) {
      restaurantIds.add(menu.getRestaurantId());
    }

    //----------------------------------------
    // List<RestaurantEntity> restaurantEntities = (List<RestaurantEntity>)restaurantRepository.findAllById(restaurantIds);
    Optional<List<RestaurantEntity>> restaurantEntitiesOptional = restaurantRepository.findRestaurantsById(restaurantIds);
    List<RestaurantEntity> restaurantEntities = restaurantEntitiesOptional.get();
    if (restaurantEntities == null) {
      return new ArrayList<Restaurant>();
    }
    //-----------------------------------------

    Set<RestaurantEntity> restaurantEntitiesSet = new HashSet<>();
    for (RestaurantEntity restaurantEntity : restaurantEntities) {
      restaurantEntitiesSet.add(restaurantEntity);
    }
    ModelMapper modelMapper = modelMapperProvider.get();
    List<Restaurant> restaurants = new ArrayList<Restaurant>();
    for (RestaurantEntity restaurantEntity : restaurantEntitiesSet) {
      if (isRestaurantCloseByAndOpen(restaurantEntity, currentTime, latitude, longitude, servingRadiusInKms)) {
        restaurants.add(modelMapper.map(restaurantEntity, Restaurant.class));
      }
    }
    return restaurants;
  }

  // TODO: CRIO_TASK_MODULE_RESTAURANTSEARCH
  // Objective:
  // Find restaurants which serve food items whose attributes intersect with the search query.
  @Override
  public List<Restaurant> findRestaurantsByItemAttributes(Double latitude, Double longitude,
      String searchString, LocalTime currentTime, Double servingRadiusInKms) {
    
    List<String> attributesList = new ArrayList<>();
    attributesList.add(searchString);
    Optional<List<ItemEntity>> itemsOptional = itemRepository.findItemsByAttributesAttributeIn(attributesList);
    List<ItemEntity> items = itemsOptional.get();

    if (items == null) {
      return new ArrayList<Restaurant>();
    }

    List<String> itemIdList = new ArrayList<>();
    for (ItemEntity item : items) {
      itemIdList.add(item.getItemId());
    }
    Optional<List<MenuEntity>> menusOptional = menuRepository.findMenusByItemsItemIdIn(itemIdList);
    List<MenuEntity> menus = menusOptional.get();

    if (menus == null) {
      return new ArrayList<Restaurant>();
    }

    List<String> restaurantIds = new ArrayList<>();
    for (MenuEntity menu : menus) {
      restaurantIds.add(menu.getRestaurantId());
    }

    //---------------------------------------
    // List<RestaurantEntity> restaurantEntities = (List<RestaurantEntity>)restaurantRepository.findAllById(restaurantIds);
    Optional<List<RestaurantEntity>> restaurantEntitiesOptional = restaurantRepository.findRestaurantsById(restaurantIds);
    List<RestaurantEntity> restaurantEntities = restaurantEntitiesOptional.get();
    if (restaurantEntities == null) {
      return new ArrayList<Restaurant>();
    }
    //---------------------------------------
    
    Set<RestaurantEntity> restaurantEntitiesSet = new HashSet<>();
    for (RestaurantEntity restaurantEntity : restaurantEntities) {
      restaurantEntitiesSet.add(restaurantEntity);
    }
    ModelMapper modelMapper = modelMapperProvider.get();
    List<Restaurant> restaurants = new ArrayList<Restaurant>();
    for (RestaurantEntity restaurantEntity : restaurantEntitiesSet) {
      if (isRestaurantCloseByAndOpen(restaurantEntity, currentTime, latitude, longitude, servingRadiusInKms)) {
        restaurants.add(modelMapper.map(restaurantEntity, Restaurant.class));
      }
    }
    return restaurants;
  }





  /**
   * Utility method to check if a restaurant is within the serving radius at a given time.
   * @return boolean True if restaurant falls within serving radius and is open, false otherwise
   */
  private boolean isRestaurantCloseByAndOpen(RestaurantEntity restaurantEntity,
      LocalTime currentTime, Double latitude, Double longitude, Double servingRadiusInKms) {
    if (isOpenNow(currentTime, restaurantEntity)) {
      return GeoUtils.findDistanceInKm(latitude, longitude,
          restaurantEntity.getLatitude(), restaurantEntity.getLongitude())
          < servingRadiusInKms;
    }
    return false;
  }
}

