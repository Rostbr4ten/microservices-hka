package com.hka.webshop;

import org.springframework.data.repository.CrudRepository;

// Data access
public interface CategoryRepository extends CrudRepository<Category, Long> {
}
