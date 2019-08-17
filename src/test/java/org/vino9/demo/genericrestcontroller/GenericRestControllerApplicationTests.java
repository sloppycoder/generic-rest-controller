package org.vino9.demo.genericrestcontroller;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
public class GenericRestControllerApplicationTests {

	@Autowired
	private MockMvc mock;

	@Test
	public void contextLoads() {
	}

	@Test
	public void get_by_id() throws Exception {
		mock.perform(get("/transactions/1"))
				.andDo(print())
				.andExpect(status().isOk());
	}

	@Test
	public void find_by_id() throws Exception {
		mock.perform(get("/transactions/?id=1"))
				.andDo(print())
				.andExpect(status().isOk());
	}

	@Test
	public void create_new_transaction() throws Exception {
		String newTransaction = "{ \"id\": 100, \"amount\": 11.11, \"memo\": \"new stuff\" }";
		mock.perform(
				post("/transactions").contentType("application/json").content(newTransaction))
				.andDo(print())
				.andExpect(status().isCreated());
	}

	@Test
	public void mal_formed_transaction() throws Exception {
		String newTransaction = "{ \"id\": 1, \"amount\":\" 11.a1\", \"memo\": \"new stuff\" }";
		mock.perform(
				post("/transactions").contentType("application/json").content(newTransaction))
				.andDo(print())
				.andExpect(status().isInternalServerError());
	}

}
