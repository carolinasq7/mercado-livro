package com.carollibrary.controllers

import com.carollibrary.controllers.request.PostCustomerRequest
import com.carollibrary.controllers.request.PutCustomerRequest
import com.carollibrary.enums.CustomerStatus
import com.carollibrary.helper.buildCustomer
import com.carollibrary.repository.CustomerRepository
import com.carollibrary.security.UserCustomDetails
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
@ContextConfiguration
@ActiveProfiles("test")
@WithMockUser(roles = ["ADMIN"])
class CustomerControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var customerRepository: CustomerRepository

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @BeforeEach
    fun setup() = customerRepository.deleteAll()

    @AfterEach
    fun tearDown() = customerRepository.deleteAll()

    @Test
    fun `Should return all customers`() {
        val customer1 = customerRepository.save(buildCustomer())
        val customer2 = customerRepository.save(buildCustomer())

        mockMvc.perform(get("/customers"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].id").value(customer1.id))
            .andExpect(jsonPath("$[0].name").value(customer1.name))
            .andExpect(jsonPath("$[0].email").value(customer1.email))
            .andExpect(jsonPath("$[0].status").value(customer1.status.name))
            .andExpect(jsonPath("$[1].id").value(customer2.id))
            .andExpect(jsonPath("$[1].name").value(customer2.name))
            .andExpect(jsonPath("$[1].email").value(customer2.email))
            .andExpect(jsonPath("$[1].status").value(customer2.status.name))
    }

    @Test
    fun `Should filter all customers by name when get all`() {
        customerRepository.save(buildCustomer(name = "Carolina"))
        val customer = customerRepository.save(buildCustomer(name = "Fiona"))

        mockMvc.perform(get("/customers?name=Fiona"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").value(customer.id))
            .andExpect(jsonPath("$[0].name").value(customer.name))
            .andExpect(jsonPath("$[0].email").value(customer.email))
            .andExpect(jsonPath("$[0].status").value(customer.status.name))
    }

    @Test
    fun `Should create customer`() {
        val request = PostCustomerRequest(name = "fake name","fake@emaill.com", "fakeSenha")

        mockMvc.perform(post("/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated)

        val customers = customerRepository.findAll().toList()
        assertEquals(1, customers.size)
        assertEquals(request.name, customers[0].name)
    }

    @Test
    fun `Should throw error when create customer has invalid information`() {
        val request = PostCustomerRequest(name = "","fake@emaill.com", "fakeSenha")

        mockMvc.perform(post("/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnprocessableEntity)
            .andExpect(jsonPath("$.httpCode").value(422))
            .andExpect(jsonPath("$.message").value("Invalid Request"))
            .andExpect(jsonPath("$.internalCode").value("ML-001"))

    }

    @Test
    @WithMockUser(roles = ["CUSTOMER"])
    fun `Should get user by id when user has the same id`() {
        val customer = customerRepository.save(buildCustomer())

        mockMvc.perform(get("/customers/${customer.id}").with(user(UserCustomDetails(customer))))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(customer.id))
            .andExpect(jsonPath("$.name").value(customer.name))
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `Should get user by id when user is ADMIN`() {
        val customer = customerRepository.save(buildCustomer())

        mockMvc.perform(get("/customers/${customer.id}"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(customer.id))
            .andExpect(jsonPath("$.name").value(customer.name))
    }

    @Test
    @WithMockUser(roles = ["CUSTOMER"])
    fun `Should return forbidden when user has different id `() {
        val customer = customerRepository.save(buildCustomer())

        mockMvc.perform(get("/customers/0").with(user(UserCustomDetails(customer))))
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.httpCode").value(403))
            .andExpect(jsonPath("$.message").value("Access Denied"))
            .andExpect(jsonPath("$.internalCode").value("ML-000"))
    }

    @Test
    fun `Should update customer`() {
        val customer = customerRepository.save(buildCustomer())
        val request = PutCustomerRequest("Test Update", "update@email.com")

        mockMvc.perform(put("/customers/${customer.id}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isNoContent)

        val customers = customerRepository.findAll().toList()
        assertEquals(1, customers.size)
        assertEquals(request.name, customers[0].name)
        assertEquals(request.email, customers[0].email)
    }

    @Test
    fun `Should return not found when update customer not exists`() {
        val customer = customerRepository.save(buildCustomer())
        val request = PutCustomerRequest("Test Update", "update@email.com")

        mockMvc.perform(put("/customers/0")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.httpCode").value(404))
            .andExpect(jsonPath("$.message").value("Customer [0] not exists"))
            .andExpect(jsonPath("$.internalCode").value("ML-201"))
    }

    @Test
    fun `Should throw error when update customer has invalid information`() {
        val request = PutCustomerRequest(name = "","fake@emaill.com")

        mockMvc.perform(put("/customers/1")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnprocessableEntity)
            .andExpect(jsonPath("$.httpCode").value(422))
            .andExpect(jsonPath("$.message").value("Invalid Request"))
            .andExpect(jsonPath("$.internalCode").value("ML-001"))

    }

    @Test
    fun `Should delete customer` (){
        val customer = customerRepository.save(buildCustomer())

        mockMvc.perform(delete("/customers/${customer.id}"))
            .andExpect(status().isNoContent)

        val customerDeleted = customerRepository.findById((customer.id!!))
        assertEquals(CustomerStatus.INACTIVE, customerDeleted.get().status)
    }

    @Test
    fun `Should return not found when delete customer not exists` (){

        mockMvc.perform(delete("/customers/1"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.httpCode").value(404))
            .andExpect(jsonPath("$.message").value("Customer [1] not exists"))
            .andExpect(jsonPath("$.internalCode").value("ML-201"))
    }
}