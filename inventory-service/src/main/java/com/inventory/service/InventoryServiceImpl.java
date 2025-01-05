package com.inventory.service;

import java.util.List;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.inventory.dto.InventoryResponse;
import com.inventory.entity.Inventory;
import com.inventory.repository.InventoryRepo;

@Service
public class InventoryServiceImpl implements InventoryService {

	@Autowired
	private InventoryRepo inventoryRepo;
	
	@Autowired
	private ModelMapper modelMapper;
	
	@Override
	@Transactional(readOnly = true)
	public List<InventoryResponse> isPresent(List<String> skuCodes) {
		// TODO Auto-generated method stub
		List<Inventory> items = inventoryRepo.findBySkuCode(skuCodes);
		List<InventoryResponse> inventoryDTO = items.stream().map(item-> modelMapper.map(item, InventoryResponse.class)).toList();
		return inventoryDTO;
	}

	@Override
	public List<InventoryResponse> getAllItems() {
		// TODO Auto-generated method stub
		return (inventoryRepo.findAll().stream().map(list -> modelMapper.map(list, InventoryResponse.class)).toList());
	}

}
