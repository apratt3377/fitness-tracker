package com.fitnesstracker.userservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@AutoConfigureTestDatabase
class UserServiceApplicationTests {

	@Test
	void contextLoads() {
	}

	@Test
    void mainMethodTest() {
        UserServiceApplication.main(new String[] {});
    }

}
