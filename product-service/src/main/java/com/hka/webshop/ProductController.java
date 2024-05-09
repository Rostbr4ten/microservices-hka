package com.hka.webshop;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import javax.servlet.http.HttpServletResponse;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

// Rest Controller
@RestController()
@RequestMapping(path = "/products")
public class ProductController {

    private final ProductRepository productRepository;

    @Autowired
    public ProductController(ProductRepository repository) {
        productRepository = repository;
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
        response.setHeader("Pod", System.getenv("HOSTNAME"));
        var createdProduct = productRepository.save(product);
        var location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(createdProduct.getId()).toUri();
        return ResponseEntity.created(location).body(createdProduct);
    }

    @DeleteMapping(path = "/{id}")
    public void deleteProduct(@PathVariable Long id, HttpServletResponse response) {
        response.setHeader("Pod", System.getenv("HOSTNAME"));
        if (!productRepository.existsById(id)) throw new RuntimeException();
        productRepository.deleteById(id);
    }
}
