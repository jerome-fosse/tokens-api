package com.wcomp.app.tokens.connect

import org.springframework.http.HttpStatus

class AccessTokenException(statusCode: HttpStatus, error: String) : PartnerException("Access token error", "Connect", statusCode, error)
