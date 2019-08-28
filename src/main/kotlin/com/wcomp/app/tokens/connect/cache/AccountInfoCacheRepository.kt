package com.wcomp.app.tokens.connect.cache

import com.wcomp.app.tokens.connect.model.AccountInfo
import org.springframework.data.repository.CrudRepository

interface AccountInfoCacheRepository : CrudRepository<AccountInfo, String>
