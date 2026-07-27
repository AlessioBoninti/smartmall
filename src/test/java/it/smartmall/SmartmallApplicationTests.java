package it.smartmall;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
		"firebase.project-id=test-project",
		"firebase.service-account-json=",
		"DB_URL=jdbc:h2:mem:smartmall;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false",
		"DB_USERNAME=sa",
		"DB_PASSWORD=",
		"spring.datasource.driver-class-name=org.h2.Driver",
		"spring.jpa.hibernate.ddl-auto=create-drop",
		"spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
class SmartmallApplicationTests {

	@Test
	void contextLoads() {
	}
}
