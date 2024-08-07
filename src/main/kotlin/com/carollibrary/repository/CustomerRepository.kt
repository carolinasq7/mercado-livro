package com.carollibrary.repository

import com.carollibrary.models.CustomerModel
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface CustomerRepository : JpaRepository<CustomerModel, Int> {

    fun findByNameContaining (name: String, pageable: Pageable?): Page<CustomerModel>
    fun existsByEmail(email: String): Boolean
    fun findByEmail(email: String): CustomerModel?
}
