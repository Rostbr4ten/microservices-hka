package com.hka.webshop;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
//import org.springframework.http.MediaType;
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

    @GetMapping(path = "/info")
    public String handleRequest() {
        String hostName = System.getenv("HOSTNAME");
        return "Response from pod: " + hostName;
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
        if(productsExist(id)) return ResponseEntity.badRequest().body("Category is still used by some products");

        categoryRepository.deleteById(id);
        return ResponseEntity.ok("");
    }

    private boolean productsExist(Long categoryId) {
        WebClient client = createWebClient();
        try {
            Product[] response = client.get()
                    .uri(uriBuilder -> uriBuilder
                            .queryParam("categoryId", categoryId)
                            .build())
                    .retrieve()
                    .bodyToMono(Product[].class)
                    .block();
            return response != null && response.length > 0;
        } catch (WebClientResponseException wcre) {
            return false;
        } catch (Exception e) {
            System.out.println(e);
            return true;
        }
    }
    
    private WebClient createWebClient() {
        return WebClient.create("http://" + productServiceEndpoint + ":8080/products/");
    }
}