package com.revolut.challenge.service;

import java.util.function.Supplier;
import javax.inject.Singleton;
import javax.transaction.Transactional;

@Singleton
class TransactionHelper {

    @Transactional
    <T> T runInTransaction(Supplier<T> supplier) {
        return supplier.get();
    }
}
