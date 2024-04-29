package de.hkamicroservices.productservice;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

public interface ProductRepository extends CrudRepository<Product, Long> {
    @Query("from Product p where p.categoryId=:categoryId")
    Iterable<Product> getProductsByCategoryId(int categoryId);

    @Query("from Product p where (p.name LIKE :keyword OR p.details LIKE :keyword) AND p.price BETWEEN :minPrice AND :maxPrice")
    Iterable<Product> getProductsBySearchCriteria(String keyword, double minPrice, double maxPrice);
}
