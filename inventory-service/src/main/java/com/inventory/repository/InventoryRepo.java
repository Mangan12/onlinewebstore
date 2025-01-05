package com.inventory.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.inventory.entity.Inventory;

public interface InventoryRepo extends JpaRepository<Inventory, Long>{
	@Query("SELECT i FROM Inventory i WHERE i.skuCode in :skuCodes")
	List<Inventory> findBySkuCode(@Param("skuCodes") List<String> skuCodes);}
