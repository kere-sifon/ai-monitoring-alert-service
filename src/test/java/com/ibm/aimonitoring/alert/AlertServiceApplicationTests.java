package com.ibm.aimonitoring.alert;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.RestTemplate;

/**
 * Integration test for Alert Service Application.
 * Uses mocked external dependencies to avoid requiring actual services.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
    "spring.jpa.properties.hibernate.default_schema=PUBLIC",
    "spring.mail.host=localhost",
    "spring.mail.port=1025",
    "eureka.client.enabled=false",
    "alert.monitoring.enabled=false"
})
class AlertServiceApplicationTests {

    @MockBean
    private JavaMailSender javaMailSender;
    
    @MockBean
    private RestTemplate restTemplate;

    @Test
    void contextLoads() {
        // This test verifies that the Spring application context loads successfully
        // with mocked external dependencies (JavaMailSender, RestTemplate)
    }
}

// Made with Bob