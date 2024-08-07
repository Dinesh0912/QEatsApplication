
/*
 *
 *  * Copyright (c) Crio.Do 2019. All rights reserved
 *
 */

package com.crio.qeats.repositories;

import com.crio.qeats.models.MenuEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

public interface MenuRepository extends MongoRepository<MenuEntity, String> {

  @Query(value = "{ 'restaurantId': ?0 }")
  Optional<MenuEntity> findMenuByRestaurantId(String restaurantId);

  @Query(value = "{ 'items.itemId': { $in: ?0 } }")
  Optional<List<MenuEntity>> findMenusByItemsItemIdIn(List<String> itemIdList);

}
