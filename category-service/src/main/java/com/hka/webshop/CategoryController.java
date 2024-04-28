package com.hka.webshop;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import javax.servlet.http.HttpServletResponse;

import java.util.Objects;


@RestController()
@RequestMapping(path="/categories")
public class CategoryController {

    private final String productServiceEndpoint = !Objects.equals(System.getenv("PRODUCT_ENDPOINT"), "") ? System.getenv("PRODUCT_ENDPOINT") : "localhost";
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
    public ResponseEntity<?> deleteCategory(@PathVariable Long id, HttpServletResponse response){
        response.setHeader("Pod", System.getenv("HOSTNAME"));
        if(!categoryRepository.existsById(id)) throw new RuntimeException();
        if(checkProductsExist(id)) return ResponseEntity.badRequest().body("Cannot delete category if products using this category exist");

        return ResponseEntity.ok("");
    }

    private boolean checkProductsExist(Long categoryId){
        WebClient client = WebClient.create("http://" + productServiceEndpoint + ":8080/products/");
        try {
            var response = client.get().uri("?categoryId="+String.valueOf(categoryId)).accept(MediaType.APPLICATION_JSON).retrieve().bodyToMono(Product[].class).block();
            return response.length >0;
        } catch (WebClientResponseException wcre) {
            return false;
        }catch (Exception e){
            System.out.println(e);
            return true;
        }
    }
}
