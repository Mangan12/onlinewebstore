package com.inventory.service;

import java.util.List;

import com.inventory.dto.InventoryResponse;

public interface InventoryService {

	public List<InventoryResponse> getStockDetails(List<String> skuCode);

	public List<InventoryResponse> getAllItems();

}
