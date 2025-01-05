package com.product.service;

import java.util.List;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.product.dto.ProductRequest;
import com.product.dto.ProductResponse;
import com.product.entity.Product;
import com.product.repository.ProductRepo;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ProductServiceImpl implements ProductService {
	
	@Autowired
	private ProductRepo productRepo;
	
	@Autowired
	private ModelMapper modelMapper;
	
	@Override
	public void createproduct(ProductRequest productRequest) {
		Product product = modelMapper.map(productRequest, Product.class);
		productRepo.save(product);
		log.info("Product {} is saved", product.getId());
	}
 
	@Override
	public List<ProductResponse> getAllProducts() {
		List<Product> products = productRepo.findAll();
		return products.stream().map(product -> modelMapper.map(product, ProductResponse.class)).toList();
	}

}
