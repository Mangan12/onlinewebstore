package com.inventory;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;

import com.inventory.entity.Inventory;
import com.inventory.repository.InventoryRepo;

@SpringBootApplication
@EnableDiscoveryClient
public class InventoryApplication {

	public static void main(String[] args) {
		SpringApplication.run(InventoryApplication.class, args);
	}  
	
	@Bean
	CommandLineRunner loadData(InventoryRepo inventoryRepo) {
		return args->{
			Inventory inventory = new Inventory();
			inventory.setSkuCode("iphone 15");
			inventory.setQuantity(100);
			
			Inventory inventory1 = new Inventory();
			inventory1.setSkuCode("samsung s21");
			inventory1.setQuantity(50);
			
			Inventory inventory2 = new Inventory();
			inventory2.setSkuCode("xaomi");
			inventory2.setQuantity(10);
			
			Inventory inventory3 = new Inventory();
			inventory3.setSkuCode("lenovo");
			inventory3.setQuantity(0);
			
			inventoryRepo.save(inventory);
			inventoryRepo.save(inventory1);
			inventoryRepo.save(inventory2);
			inventoryRepo.save(inventory3);
		};
	}

}
