package com.hka.webshop;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import reactor.core.publisher.Mono;

import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

// Rest Controller
@RestController()
@RequestMapping(path = "/products")
public class ProductController {

    private final String categoryServiceEndpoint = !Objects.equals(System.getenv("CATEGORY_ENDPOINT"), "") ? System.getenv("CATEGORY_ENDPOINT") : "localhost";
    private final ProductRepository productRepository;
    private final Logger log = LoggerFactory.getLogger(ProductController.class);

    @Autowired
    public ProductController(ProductRepository repository) {
        productRepository = repository;
    }

    @GetMapping(path = "/info")
    public String handleRequest() {
        String hostName = System.getenv("HOSTNAME");
        return "Response from pod: " + hostName;
    }

    @GetMapping(path = "/")
    public Iterable<Product> getAllProducts(@RequestParam(required = false) Boolean full,
                                            @RequestParam(required = false) Integer categoryId, //hier request ob product mit category exisitiert
                                            @RequestParam(required = false) String search,
                                            @RequestParam(required = false, defaultValue = "0.0") Double minPrice,
                                            @RequestParam(required = false) Double maxPrice, HttpServletResponse response) {
        response.setHeader("Pod", System.getenv("HOSTNAME"));
        if (categoryId != null)
            return productRepository.getProductsByCategoryId(categoryId);

        Iterable<Product> products;
        if (search != null) {
            if (maxPrice == null) maxPrice = Double.MAX_VALUE;
            if (minPrice == null) minPrice = 0.0;
            products = productRepository.getProductsBySearchCriteria("%" + search + "%", minPrice, maxPrice);
        }else {
            products = productRepository.findAll();
        }
        if(full == null || !full) return products;
        return StreamSupport.stream(products.spliterator(), false).collect(Collectors.toList());

    }

    @GetMapping(path = "/{id}", produces = "application/json")
    public Product getProduct(@PathVariable Long id, @RequestParam(required = false) Boolean full, HttpServletResponse response) {
        response.setHeader("Pod", System.getenv("HOSTNAME"));
        var product = productRepository.findById(id).orElseThrow(RuntimeException::new);
        if (full == null || !full) return product;
        return product;
    }


    @PostMapping(path = "/", consumes = {"application/json"}, produces = {"application/json"})
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<?> addProduct(@RequestBody Product product, HttpServletResponse response) {
        log.info("Adding product: {}", product);
        response.setHeader("Pod", System.getenv("HOSTNAME"));

        // Überprüfung für ungültige Kategorie-IDs hinzufügen
        if (product.getCategoryId() <= 0) {
            return ResponseEntity.badRequest().body("Invalid category ID provided");
        }

        if (product.getCategoryId() != 0) {
            Category category = getCategory(product.getCategoryId());
            if (category == null) {
                log.warn("Category not found for id: {}", product.getCategoryId());
                return ResponseEntity.badRequest().body("Product can't be created due to non-existent category");
            }
        }

        Product createdProduct = productRepository.save(product);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(createdProduct.getId()).toUri();
        return ResponseEntity.created(location).body(createdProduct);
    }

    @DeleteMapping(path = "/{id}")
    public ResponseEntity<?> deleteProduct(@PathVariable Long id, HttpServletResponse response) {
        response.setHeader("Pod", System.getenv("HOSTNAME"));
        if (!productRepository.existsById(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Product doesn't exist");  
        }
        productRepository.deleteById(id);
        return ResponseEntity.ok().build(); 
    }
    

private Category getCategory(int categoryId) {
    WebClient client = createWebClient();
    try {
        return client.get()
                     .uri(String.valueOf(categoryId))
                     .retrieve()
                     .onStatus(HttpStatus::is4xxClientError, response -> {
                         if (response.statusCode().equals(HttpStatus.NOT_FOUND)) {
                             return Mono.empty();  // Wenn 404, dann gebe null zurück
                         }
                         return response.createException().flatMap(Mono::error);
                     })
                     .onStatus(HttpStatus::is5xxServerError, response -> 
                         response.createException().flatMap(Mono::error))
                     .bodyToMono(Category.class)
                     .block();
    } catch (Exception e) {
        log.error("Error fetching category with id: {}", categoryId, e);
        throw new RuntimeException("Error fetching category", e);
    }
}
    //When using k8s
    private WebClient createWebClient() {
        return WebClient.create("http://category-service:8080/categories/");
    }

    // When using docker
    //private WebClient createWebClient() {
    //    return WebClient.create("http://" + categoryServiceEndpoint + ":8080/categories/");
    //}
}
