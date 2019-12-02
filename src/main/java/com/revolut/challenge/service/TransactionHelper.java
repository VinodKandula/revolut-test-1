package com.revolut.challenge.service;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.function.Supplier;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.inject.Singleton;
import javax.transaction.Transactional;

@ParametersAreNonnullByDefault
@Singleton
class TransactionHelper {

    @Transactional(rollbackOn = Exception.class)
    @NonNull
    <T> T getFromTransaction(Supplier<T> supplier) {
        var result = supplier.get();
        if (result == null) {
            throw new IllegalStateException("A null result returned");
        }
        return result;
    }
}
