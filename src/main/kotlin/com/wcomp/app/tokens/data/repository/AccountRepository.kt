package com.wcomp.app.tokens.data.repository

import com.wcomp.app.tokens.data.model.Account
import org.springframework.data.mongodb.repository.ReactiveMongoRepository

interface AccountRepository : ReactiveMongoRepository<Account, String>, AccountCustomRepository {
}