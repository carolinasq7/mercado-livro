package com.carollibrary.exception

class AuthenticationException(override val message: String, val errorCode: String): Exception()