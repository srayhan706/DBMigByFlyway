package com.example.dbmigcrud.product;

import java.util.List;

import com.example.dbmigcrud.category.Category;
import com.example.dbmigcrud.category.CategoryRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ProductService {

	private final ProductRepository repository;

	private final CategoryRepository categoryRepository;

	public ProductService(ProductRepository repository, CategoryRepository categoryRepository) {
		this.repository = repository;
		this.categoryRepository = categoryRepository;
	}

	public List<Product> findAll() {
		return repository.findAll();
	}

	public Product findById(Long id) {
		return repository.findById(id)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product " + id + " not found"));
	}

	public Product create(Product product) {
		product.setId(null);
		product.setCategory(resolveCategory(product.getCategory()));
		return repository.save(product);
	}

	@Transactional
	public Product update(Long id, Product changes) {
		Product product = findById(id);
		product.setName(changes.getName());
		product.setPrice(changes.getPrice());
		product.setStock(changes.getStock());
		product.setCategory(resolveCategory(changes.getCategory()));
		return product;
	}

	public void delete(Long id) {
		repository.delete(findById(id));
	}

	// The request only carries {"category": {"id": 1}}; load the real row so the id is checked.
	private Category resolveCategory(Category requested) {
		if (requested == null) {
			return null;
		}
		if (requested.getId() == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "category.id is required");
		}
		return categoryRepository.findById(requested.getId())
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
					"Category " + requested.getId() + " not found"));
	}

}
