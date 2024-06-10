package com.hka.webshop;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
//import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import java.util.Optional;
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
    public ResponseEntity<Category> getCategory(@PathVariable Long id, HttpServletResponse response){
        response.setHeader("Pod", System.getenv("HOSTNAME"));
        Optional<Category> category = categoryRepository.findById(id);
        if (!category.isPresent()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(category.get());
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
        if(!categoryRepository.existsById(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Category doesn't exist");
        }
    
        // Do products with this categoyr exist?
        Product[] products = getProductsByCategoryId(id);
        if (products != null && products.length > 0) {
            // if yes, delete all
            deleteProducts(products);
            //return ResponseEntity.ok("Deleted all products using this category");
        }
    
        categoryRepository.deleteById(id);
        return ResponseEntity.ok("Category and products still using it deleted successfully");
    }
    
    private Product[] getProductsByCategoryId(Long categoryId) {
        WebClient client = createWebClient();
        try {
            return client.get()
                    .uri(uriBuilder -> uriBuilder
                            .queryParam("categoryId", categoryId)
                            .build())
                    .retrieve()
                    .bodyToMono(Product[].class)
                    .block();
        } catch (WebClientResponseException wcre) {
            System.out.println("Failed to fetch products: " + wcre.getMessage());
            return null;
        } catch (Exception e) {
            System.out.println("Unexpected error: " + e.getMessage());
            return null;
        }
    }
    
    private void deleteProducts(Product[] products) {
        WebClient client = createWebClient();
        for (Product product : products) {
            try {
                client.delete()
                      .uri(""+product.getId())
                      .retrieve()
                      .toBodilessEntity()
                      .block();
            } catch (Exception e) {
                System.out.println("Failed to delete product: " + product.getId() + ", Error: " + e.getMessage());
            }
        }
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

    // When using k8s
    private WebClient createWebClient() {
        return WebClient.create("http://product-service:8080/products/");
    }
    
    // When using Docker
    //private WebClient createWebClient() {
    //    return WebClient.create("http://" + productServiceEndpoint + ":8080/products/");
    //}
}