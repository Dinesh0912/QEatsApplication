/*
 *
 *  * Copyright (c) Crio.Do 2019. All rights reserved
 *
 */

package com.crio.qeats.repositories;

import com.crio.qeats.models.RestaurantEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

public interface RestaurantRepository extends MongoRepository<RestaurantEntity, String> {
    
    @Query(value = "{'name': ?0 }")
    Optional<List<RestaurantEntity>> findRestaurantsByNameExact(String name);
    
    @Query("{ 'attributes': { $elemMatch: { $in: ?0 } } }")
    Optional<List<RestaurantEntity>> findRestaurantsByAttributesAttributeIn(List<String> attributesList);

    @Query("{ 'restaurantId': { $in: ?0 } }")
    Optional<List<RestaurantEntity>> findRestaurantsById(List<String> idsList);
}

