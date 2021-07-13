/*
 * Copyright 2009-2021 the original author or authors.
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
package org.metaeffekt.core.common.kernel.ant.log;


/**
 * This exception is thrown in case a logging message is configured to produce
 * a failure.
 *
 * @author Karsten Klein
 */
public class EscalationException extends RuntimeException {

    private static final long serialVersionUID = -2396549143165229501L;

    public EscalationException(String message, Throwable cause) {
        super(message, cause);
    }

    public EscalationException(String message) {
        super(message);
    }

}
