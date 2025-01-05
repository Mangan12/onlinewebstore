package com.product.service;

import java.util.List;

import com.product.dto.ProductRequest;
import com.product.dto.ProductResponse;

public interface ProductService {
	public void createproduct(ProductRequest productRequest);
	public List<ProductResponse> getAllProducts();
}
