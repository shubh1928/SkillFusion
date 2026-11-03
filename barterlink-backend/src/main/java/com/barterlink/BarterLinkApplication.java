package com.barterlink;

import com.barterlink.auth.config.AppConstants;
import com.barterlink.auth.entities.Role;
import com.barterlink.auth.repositories.RoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.UUID;

@EnableJpaAuditing
@SpringBootApplication
public class
BarterLinkApplication implements CommandLineRunner {

	@Autowired
	private RoleRepository roleRepository;

	public static void main(String[] args) {
		SpringApplication.run(BarterLinkApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {

		roleRepository.findByName("ROLE_"+ AppConstants.ADMIN_ROLE).ifPresentOrElse(role -> {
			System.out.println("Admin role already exist: " + role.getName());
		}, () -> {

			Role role = new Role();
			role.setName("ROLE_"+AppConstants.ADMIN_ROLE);
			role.setId(UUID.randomUUID());

			roleRepository.save(role);
		});

		roleRepository.findByName("ROLE_"+AppConstants.GUEST_ROLE).ifPresentOrElse(role -> {
			System.out.println("Guest role already exist: " + role.getName());
		}, () -> {

			Role role = new Role();
			role.setName("ROLE_"+AppConstants.GUEST_ROLE);
			role.setId(UUID.randomUUID());

			roleRepository.save(role);
		});

	}
}
