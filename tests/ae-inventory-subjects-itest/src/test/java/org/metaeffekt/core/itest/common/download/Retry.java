/*
 * Copyright 2009-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.metaeffekt.core.itest.common.download;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class Retry<T> {

    private final static Logger LOG = LoggerFactory.getLogger(Retry.class);

    private final RetrySupplier<T> supplier;

    private final List<Class<? extends Throwable>> exceptions = new ArrayList<>();
    private final List<Validator<T>> validators = new ArrayList<>();
    private int retryCount = 0;
    private int delay = 0;
    private Consumer<Throwable> onFailure;

    public interface RetrySupplier<T> {
        T get() throws Throwable;
    }

    public interface RetrySupplierNoReturnValue {
        void get() throws Throwable;
    }

    public Retry(RetrySupplier<T> supplier) {
        this.supplier = supplier;
    }

    public Retry(RetrySupplierNoReturnValue supplier) {
        this.supplier = () -> {
            supplier.get();
            return null;
        };
    }

    public Retry<T> onException(Class<? extends Throwable> exceptionClass) {
        this.exceptions.add(exceptionClass);
        return this;
    }

    public Retry<T> retryCount(int retryCount) {
        this.retryCount = retryCount;
        return this;
    }

    public Retry<T> withValidator(Validator<T> validator) {
        this.validators.add(validator);
        return this;
    }

    public Retry<T> onFailure(Consumer<Throwable> onFailure) {
        this.onFailure = onFailure;
        return this;
    }

    public Retry<T> withDelay(int delay) {
        this.delay = delay;
        return this;
    }

    private boolean isThrowableContained(Throwable throwable) {
        for (Class<? extends Throwable> exceptionClass : exceptions) {
            if (exceptionClass.isAssignableFrom(throwable.getClass())) {
                return true;
            }
        }
        return false;
    }

    public T run() {
        Throwable caughtException = null;

        for (int i = 0; i < retryCount; i++) {
            try {
                final T result = supplier.get();
                if (!validators.isEmpty() && validators.stream().noneMatch(validator -> validator.isValid(result))) {
                    throw new RuntimeException("Retry validation failed on attempt [" + (i + 1) + " / " + retryCount + "]");
                }
                return result;
            } catch (Throwable throwable) {
                if (i == retryCount - 1) {
                    LOG.error("failed last attempt [{}] due to invalid run result: {}", retryCount, throwable.getMessage());
                } else {
                    LOG.warn("retrying next attempt [{} / {}] due to invalid run result: {}", i + 2, retryCount, throwable.getMessage());
                }
                if (isThrowableContained(throwable)) {
                    caughtException = throwable;
                } else {
                    if (onFailure != null) {
                        onFailure.accept(throwable);
                    }
                    throw new RuntimeException(throwable);
                }
            }

            if (delay > 0) {
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException e) {
                    throw new RuntimeException("Unable to sleep", e);
                }
            }
        }

        if (onFailure != null) {
            onFailure.accept(caughtException);
        }

        if (caughtException != null) {
            throw new RuntimeException(caughtException);
        }

        throw new RuntimeException("Retry failed due to unknown reason");
    }

    @FunctionalInterface
    public interface Validator<T> {
        boolean isValid(T result);
    }
}

