package com.hka.webshop;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import javax.servlet.http.HttpServletResponse;

// Rest Controller
@RestController()
@RequestMapping(path="/categories")
public class CategoryController {

    private final CategoryRepository categoryRepository;

    @Autowired
    public CategoryController(CategoryRepository repository){
        categoryRepository = repository;
    }

    @GetMapping(path="/")
    public Iterable<Category> getAllCategories(HttpServletResponse response){
        response.setHeader("Pod", System.getenv("HOSTNAME"));
        return categoryRepository.findAll();
    }

    @GetMapping(path="/{id}", produces = "application/json")
    public Category getCategory(@PathVariable Long id, HttpServletResponse response){
        response.setHeader("Pod", System.getenv("HOSTNAME"));
        return categoryRepository.findById(id).orElseThrow(RuntimeException::new);
    }

    @PostMapping(path="/", consumes = "application/json", produces = "application/json")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<?> addCategory(@RequestBody Category category, HttpServletResponse response){
        response.setHeader("Pod", System.getenv("HOSTNAME"));
        var createdCategory =  categoryRepository.save(category);
        var location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(createdCategory.getId()).toUri();
        return ResponseEntity.created(location).body(createdCategory);
    }

    @DeleteMapping(path="/{id}")
    public void deleteCategory(@PathVariable Long id, HttpServletResponse response) {
        response.setHeader("Pod", System.getenv("HOSTNAME"));
    
        if (!categoryRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found");
        }
        categoryRepository.deleteById(id);
    }

}
