
package com.crio.qeats.repositories;

import com.crio.qeats.models.ItemEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

public interface ItemRepository extends MongoRepository<ItemEntity, String> {

    @Query("{ 'itemId': ?0 }")
    Optional<ItemEntity> findItemByItemId(String itemId);

    @Query(value = "{'name': ?0 }")
    Optional<List<ItemEntity>> findItemsByNameExact(String name);

    @Query("{ 'attributes': { $elemMatch: { $in: ?0 } } }")
    Optional<List<ItemEntity>> findItemsByAttributesAttributeIn(List<String> attributeList);
}

