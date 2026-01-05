/*
 * Copyright 2009-2026 the original author or authors.
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
package org.metaeffekt.core.inventory.processor.report.model.aeaa.eol;

import org.apache.commons.lang.StringUtils;
import org.metaeffekt.core.inventory.processor.report.model.aeaa.AeaaTimeUtils;

import java.util.Date;
import java.util.function.Supplier;

public class AeaaStateMapping<T> {

    private static Supplier<Date> NOW = Date::new;

    private final T notState;
    private final T state;
    private final T onDateState;
    private final T upcomingState;
    private final T closeUpcomingState;
    private final boolean nullMeansSupported;
    private final String stateName;

    public AeaaStateMapping(T notState, T state, T onDateState, T upcomingState, boolean nullMeansSupported, String stateName) {
        this(notState, state, onDateState, upcomingState, upcomingState, nullMeansSupported, stateName);
    }

    public AeaaStateMapping(T notState, T state, T onDateState, T upcomingState, T closeUpcomingState, boolean nullMeansSupported, String stateName) {
        this.notState = notState;
        this.state = state;
        this.onDateState = onDateState;
        this.upcomingState = upcomingState;
        this.closeUpcomingState = closeUpcomingState;
        this.nullMeansSupported = nullMeansSupported;
        this.stateName = stateName;
    }

    public static void setNOW(Supplier<Date> NOW) {
        AeaaStateMapping.NOW = NOW;
    }

    public static Date getNOW() {
        return NOW.get();
    }

    public static void resetNOW() {
        NOW = Date::new;
    }

    public T getState(String stateString, long millisUntilCloseUpcoming) {
        final boolean isEmpty = StringUtils.isEmpty(stateString);
        if (isEmpty) {
            if (nullMeansSupported) {
                return state;
            } else {
                return notState;
            }
        }

        if (stateString.equals("true") || stateString.equals(stateName)) {
            return state;
        } else if (stateString.equals("false")) {
            return notState;
        }

        final Date stateDate = AeaaTimeUtils.tryParse(stateString);
        if (stateDate == null) {
            return notState;
        }

        final long distanceFromNowToDate = stateDate.getTime() - NOW.get().getTime();
        if (distanceFromNowToDate <= 0) {
            return onDateState;
        } else if (distanceFromNowToDate <= millisUntilCloseUpcoming) {
            return closeUpcomingState;
        } else {
            return upcomingState;
        }
    }
}
