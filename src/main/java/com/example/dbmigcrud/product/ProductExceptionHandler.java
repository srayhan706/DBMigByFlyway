package com.example.dbmigcrud.product;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ProductExceptionHandler {

	// Thrown when the database rejects a write, here the uk_products_name constraint.
	@ExceptionHandler(DataIntegrityViolationException.class)
	public ProblemDetail handleDuplicateName() {
		return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "A product with this name already exists");
	}

}
