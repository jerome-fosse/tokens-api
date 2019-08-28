package com.wcomp.app.tokens.connect

import org.springframework.http.HttpStatus

class InvalidRefreshTokenException(statusCode: HttpStatus, error: String) : PartnerException("Refresh token error", "Connect", statusCode, error)
