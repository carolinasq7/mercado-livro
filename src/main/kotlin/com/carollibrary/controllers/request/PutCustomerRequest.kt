package com.carollibrary.controllers.request

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotEmpty

data class PutCustomerRequest (
        @field:NotEmpty
        var name: String,

        @field:Email
        var email: String
    )

