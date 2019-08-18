package org.vino9.demo.genericrestcontroller;

import org.hamcrest.core.IsNull;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.junit.Assume.assumeTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.vino9.demo.genericrestcontroller.RestApiConstants.PAGINATION_DATA;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
public class GenericRestControllerApplicationTests {

	@Autowired
	private MockMvc mock;

	@Autowired
	private CardTransactionApiController controller;

	@Test
	public void context_loads() {}

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
				.andExpect(status().isOk())
				.andExpect(jsonPath("$." + PAGINATION_DATA).value(IsNull.notNullValue()));
	}

	@Test
	public void first_page_has_no_prev() throws Exception {
		assumeTrue(controller.isPaginationSupported());

		// DB has 10 records total, page number is 0 based
		mock.perform(get("/transactions/?page=0&per_page=4"))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$._meta_.next").value(IsNull.notNullValue()))
				.andExpect(jsonPath("$._meta_.prev").doesNotExist());

	}

	@Test
	public void last_page_has_no_prev() throws Exception {
		assumeTrue(controller.isPaginationSupported());

		// DB has 10 records total, page number is 0 based
		mock.perform(get("/transactions/?page=2&per_page=4"))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$._meta_.prev").value(IsNull.notNullValue()))
				.andExpect(jsonPath("$._meta_.next").doesNotExist());
	}

	@Test
	public void only_one_page() throws Exception {
		assumeTrue(controller.isPaginationSupported());

		// DB has 10 records total, page number is 0 based
		mock.perform(get("/transactions/?page=0&per_page=20"))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$._meta_.curr").value(IsNull.notNullValue()))
				.andExpect(jsonPath("$._meta_.prev").doesNotExist())
				.andExpect(jsonPath("$._meta_.next").doesNotExist());

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
	public void updating_existing_transaction() throws Exception {
		String patch = "{ \"memo\": \"updated stuff\" }";
		mock.perform(
				patch("/transactions/1").contentType("application/json").content(patch))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath(".memo", hasItem("updated stuff")));
	}

	@Test
	public void test_head_method() throws Exception {
		mock.perform(head("/transactions/1"))
				.andDo(print())
				.andExpect(status().isOk());
	}

	@Test
	public void test_options_method() throws Exception {
		mock.perform(options("/transactions"))
				.andDo(print())
				.andExpect(header().exists("Allow"));
	}

	// error conditions
	@Test
	public void get_by_id_not_found() throws Exception {
		mock.perform(get("/transactions/10000"))
				.andDo(print())
				.andExpect(status().isNotFound());
	}

	@Test
	public void patch_by_id_not_found() throws Exception {
		mock.perform(patch("/transactions/10000").contentType("application/json").content("{}"))
				.andDo(print())
				.andExpect(status().isNotFound());
	}

	@Test
	public void put_by_id_not_found() throws Exception {
		mock.perform(patch("/transactions/10000").contentType("application/json").content("{}"))
				.andDo(print())
				.andExpect(status().isNotFound());
	}

	@Test
	public void mal_formed_new_transaction() throws Exception {
		String newTransaction = "{ \"id\": 1, \"amount\":\" 11.a1\", \"memo\": \"new stuff\" }";
		mock.perform(
				post("/transactions").contentType("application/json").content(newTransaction))
				.andDo(print())
				.andExpect(status().isInternalServerError());
	}


}
