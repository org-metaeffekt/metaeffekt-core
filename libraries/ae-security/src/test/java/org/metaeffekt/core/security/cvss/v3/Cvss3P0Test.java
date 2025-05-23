/*
 * Copyright 2009-2024 the original author or authors.
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
package org.metaeffekt.core.security.cvss.v3;

import org.junit.Assert;
import org.junit.Test;
import org.metaeffekt.core.security.cvss.CvssVector;
import org.metaeffekt.core.security.cvss.MultiScoreCvssVector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;


public class Cvss3P0Test {
    private final static Logger LOG = LoggerFactory.getLogger(Cvss3P0Test.class);

    //CVSS 3.0 specific edgecases, the rest of tests in Cvss3P1Test should still apply for Cvss3P0
    @Test
    public void evaluateCvssVectorsTest() {
        List<String> vectors = Arrays.asList("AV:N/AC:L/PR:N/UI:N/S:U/C:N/I:H/A:L/E:H/RL:U/RC:U/MAV:P/MAC:H/MPR:H/MUI:R/MS:X/MC:X/MI:X/MA:L/CR:H/IR:L/AR:L 8.2 7.6 2.4",
                "AV:L/AC:L/PR:N/UI:R/S:C/C:L/I:L/A:N/E:X/RL:X/RC:U/MAV:N/MAC:H/MPR:N/MUI:N/MS:U/MC:N/MI:N/MA:L/CR:L/IR:H/AR:L 5.0 4.7 2.8",
                "AV:A/AC:L/PR:N/UI:N/S:U/C:N/I:H/A:H/E:H/RL:X/RC:U/MAV:N/MAC:L/MPR:N/MUI:N/MS:C/MC:H/MI:H/MA:H/CR:H/IR:M/AR:M 8.1 7.5 9.3",
                "AV:L/AC:L/PR:L/UI:R/S:U/C:N/I:N/A:H/E:X/RL:U/RC:U/MAV:L/MAC:H/MPR:H/MUI:N/MS:C/MC:N/MI:L/MA:H/CR:M/IR:L/AR:H 5.0 4.7 6.9",
                "AV:A/AC:L/PR:H/UI:R/S:C/C:N/I:H/A:N/E:H/RL:U/RC:U/MAV:N/MAC:L/MPR:L/MUI:N/MS:C/MC:L/MI:N/MA:N/CR:M/IR:L/AR:H 5.7 5.3 4.7",
                "AV:P/AC:H/PR:L/UI:R/S:U/C:L/I:L/A:H/E:H/RL:U/RC:U/MAV:P/MAC:L/MPR:H/MUI:R/MS:U/MC:X/MI:X/MA:X/CR:M/IR:X/AR:M 5.0 4.7 4.7",
                "AV:L/AC:H/PR:L/UI:R/S:U/C:H/I:L/A:N/E:H/RL:U/RC:U/MAV:X/MAC:X/MPR:X/MUI:X/MS:X/MC:X/MI:X/MA:X/CR:X/IR:X/AR:X 5.0 4.7 NaN",
                "AV:L/AC:H/PR:L/UI:R/S:C/C:N/I:N/A:L/E:X/RL:X/RC:U/MAV:X/MAC:H/MPR:H/MUI:R/MS:C/MC:N/MI:H/MA:X/CR:H/IR:X/AR:H 2.5 2.4 5.8",
                "AV:N/AC:L/PR:L/UI:N/S:C/C:L/I:N/A:L/E:H/RL:X/RC:U/MAV:A/MAC:L/MPR:H/MUI:R/MS:C/MC:X/MI:X/MA:X/CR:M/IR:M/AR:H 6.4 5.9 4.7",
                "AV:A/AC:H/PR:H/UI:R/S:C/C:H/I:H/A:H/E:X/RL:X/RC:U/MAV:P/MAC:L/MPR:H/MUI:R/MS:U/MC:L/MI:L/MA:H/CR:H/IR:L/AR:M 7.3 6.8 4.7",
                "AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:L/A:L/E:X/RL:U/RC:U/MAV:L/MAC:L/MPR:L/MUI:R/MS:C/MC:H/MI:L/MA:N/CR:L/IR:M/AR:H 8.6 8.0 4.7",
                "AV:P/AC:H/PR:L/UI:N/S:U/C:L/I:L/A:L/E:H/RL:X/RC:U/MAV:L/MAC:L/MPR:L/MUI:R/MS:C/MC:H/MI:L/MA:N/CR:L/IR:M/AR:H 3.8 3.5 4.7",
                "AV:L/AC:H/PR:L/UI:R/S:U/C:N/I:H/A:L/E:H/RL:U/RC:U/MAV:A/MAC:X/MPR:H/MUI:X/MS:C/MC:N/MI:X/MA:N/CR:L/IR:H/AR:M 5.0 4.7 6.7",
                "AV:P/AC:H/PR:H/UI:R/S:C/C:H/I:H/A:L/E:H/RL:U/RC:U/MAV:L/MAC:H/MPR:H/MUI:N/MS:C/MC:N/MI:N/MA:L/CR:M/IR:H/AR:M 6.7 6.2 2.4",
                "AV:A/AC:H/PR:L/UI:N/S:C/C:H/I:L/A:N/E:X/RL:U/RC:U/MAV:N/MAC:L/MPR:X/MUI:N/MS:C/MC:L/MI:X/MA:H/CR:X/IR:M/AR:H 6.5 6.0 9.3",
                "AV:A/AC:L/PR:H/UI:R/S:C/C:N/I:H/A:N/E:H/RL:U/RC:U/MAV:L/MAC:H/MPR:X/MUI:N/MS:U/MC:X/MI:L/MA:H/CR:X/IR:H/AR:X 5.7 5.3 4.7",
                "AV:N/AC:L/PR:N/UI:N/S:C/C:N/I:H/A:H/E:X/RL:X/RC:U/MAV:P/MAC:H/MPR:H/MUI:R/MS:U/MC:H/MI:L/MA:N/CR:M/IR:L/AR:L 10.0 9.3 3.8",
                "AV:P/AC:L/PR:H/UI:N/S:U/C:L/I:H/A:L/E:H/RL:U/RC:U/MAV:N/MAC:H/MPR:N/MUI:R/MS:C/MC:L/MI:N/MA:H/CR:H/IR:L/AR:L 5.0 4.7 5.4",
                "AV:L/AC:H/PR:L/UI:R/S:C/C:L/I:L/A:L/E:X/RL:U/RC:U/MAV:L/MAC:H/MPR:H/MUI:R/MS:C/MC:L/MI:H/MA:H/CR:H/IR:H/AR:M 5.0 4.7 6.7",
                "AV:P/AC:L/PR:H/UI:N/S:U/C:N/I:H/A:L/E:H/RL:U/RC:U/MAV:L/MAC:L/MPR:H/MUI:N/MS:C/MC:L/MI:H/MA:N/CR:M/IR:L/AR:L 4.6 4.3 4.7",
                "AV:L/AC:H/PR:L/UI:R/S:U/C:L/I:N/A:H/E:H/RL:U/RC:U/MAV:X/MAC:X/MPR:X/MUI:X/MS:X/MC:X/MI:X/MA:X/CR:X/IR:X/AR:X 5.0 4.7 NaN",
                "AV:P/AC:L/PR:H/UI:N/S:U/C:H/I:L/A:L/E:H/RL:X/RC:U/MAV:X/MAC:X/MPR:X/MUI:X/MS:X/MC:X/MI:X/MA:X/CR:X/IR:X/AR:X 5.0 4.7 NaN",
                "AV:N/AC:H/PR:H/UI:N/S:U/C:H/I:L/A:N/E:X/RL:X/RC:U/MAV:X/MAC:X/MPR:X/MUI:X/MS:X/MC:X/MI:X/MA:X/CR:X/IR:X/AR:X 5.0 4.7 NaN",
                "AV:L/AC:H/PR:H/UI:R/S:C/C:N/I:H/A:N/E:H/RL:U/RC:U/MAV:X/MAC:X/MPR:X/MUI:X/MS:X/MC:X/MI:X/MA:X/CR:X/IR:X/AR:X 5.0 4.7 NaN",
                "AV:L/AC:L/PR:L/UI:R/S:U/C:N/I:N/A:H/E:H/RL:X/RC:U/MAV:A/MAC:H/MPR:H/MUI:X/MS:C/MC:X/MI:N/MA:H/CR:M/IR:X/AR:M 5.0 4.7 4.7",
                "AV:P/AC:H/PR:H/UI:R/S:C/C:N/I:N/A:H/E:X/RL:U/RC:U/MAV:L/MAC:X/MPR:X/MUI:X/MS:U/MC:L/MI:X/MA:N/CR:H/IR:H/AR:M 4.6 4.3 2.4",
                "AV:P/AC:H/PR:N/UI:N/S:C/C:H/I:H/A:H/E:X/RL:X/RC:U/MAV:L/MAC:X/MPR:L/MUI:N/MS:U/MC:H/MI:L/MA:N/CR:M/IR:L/AR:L 7.1 6.6 4.7",
                "AV:A/AC:H/PR:N/UI:R/S:U/C:L/I:N/A:L/E:X/RL:X/RC:U/MAV:A/MAC:L/MPR:N/MUI:N/MS:X/MC:N/MI:N/MA:X/CR:L/IR:M/AR:H 3.7 3.5 4.7",
                "AV:N/AC:H/PR:H/UI:N/S:U/C:H/I:H/A:N/E:X/RL:X/RC:U/MAV:X/MAC:H/MPR:X/MUI:R/MS:X/MC:H/MI:H/MA:X/CR:L/IR:X/AR:H 5.9 5.5 4.7",
                "AV:N/AC:L/PR:H/UI:N/S:C/C:H/I:N/A:N/E:X/RL:U/RC:U/MAV:N/MAC:H/MPR:L/MUI:N/MS:U/MC:X/MI:N/MA:L/CR:L/IR:H/AR:H 6.8 6.3 4.7",
                "AV:N/AC:H/PR:L/UI:N/S:U/C:L/I:L/A:L/E:X/RL:U/RC:U/MAV:N/MAC:X/MPR:X/MUI:N/MS:C/MC:L/MI:X/MA:N/CR:H/IR:L/AR:H 5.0 4.7 4.7",
                "AV:L/AC:L/PR:L/UI:R/S:U/C:N/I:H/A:N/E:X/RL:X/RC:U/MAV:A/MAC:L/MPR:H/MUI:N/MS:U/MC:H/MI:X/MA:X/CR:L/IR:M/AR:H 5.0 4.7 4.9",
                "AV:L/AC:H/PR:L/UI:N/S:C/C:L/I:N/A:L/E:X/RL:U/RC:U/MAV:L/MAC:H/MPR:L/MUI:N/MS:C/MC:H/MI:H/MA:N/CR:L/IR:L/AR:L 4.2 3.9 4.7",
                "AV:P/AC:L/PR:L/UI:R/S:U/C:N/I:L/A:L/E:H/RL:X/RC:U/MAV:P/MAC:H/MPR:H/MUI:N/MS:C/MC:L/MI:L/MA:L/CR:H/IR:M/AR:M 3.1 2.9 4.7",
                "AV:A/AC:L/PR:N/UI:R/S:U/C:H/I:H/A:N/E:H/RL:U/RC:U/MAV:N/MAC:L/MPR:N/MUI:N/MS:C/MC:H/MI:N/MA:H/CR:M/IR:L/AR:H 7.3 6.8 9.3",
                "AV:P/AC:L/PR:L/UI:R/S:C/C:L/I:N/A:L/E:H/RL:X/RC:U/MAV:N/MAC:L/MPR:L/MUI:N/MS:C/MC:H/MI:L/MA:L/CR:H/IR:L/AR:M 3.6 3.4 9.3",
                "AV:L/AC:H/PR:H/UI:R/S:C/C:H/I:N/A:N/E:X/RL:U/RC:U/MAV:P/MAC:H/MPR:L/MUI:R/MS:C/MC:N/MI:N/MA:L/CR:M/IR:M/AR:H 5.0 4.7 2.6",
                "AV:N/AC:L/PR:H/UI:N/S:U/C:H/I:H/A:N/E:H/RL:X/RC:U/MAV:P/MAC:H/MPR:H/MUI:R/MS:C/MC:L/MI:L/MA:N/CR:L/IR:M/AR:M 6.5 6.0 2.4",
                "AV:P/AC:H/PR:H/UI:R/S:C/C:H/I:H/A:N/E:X/RL:U/RC:U/MAV:L/MAC:H/MPR:L/MUI:N/MS:U/MC:N/MI:L/MA:N/CR:L/IR:M/AR:L 6.5 6.0 2.4",
                "AV:N/AC:L/PR:L/UI:N/S:C/C:N/I:N/A:H/E:H/RL:U/RC:U/MAV:N/MAC:L/MPR:H/MUI:N/MS:C/MC:N/MI:L/MA:N/CR:L/IR:H/AR:M 7.7 7.1 4.7",
                "AV:L/AC:H/PR:H/UI:N/S:C/C:N/I:N/A:L/E:H/RL:U/RC:U/MAV:X/MAC:X/MPR:X/MUI:X/MS:X/MC:X/MI:X/MA:X/CR:X/IR:X/AR:X 2.5 2.4 NaN",
                "AV:L/AC:H/PR:L/UI:N/S:U/C:N/I:L/A:N/E:H/RL:U/RC:U/MAV:L/MAC:X/MPR:H/MUI:X/MS:X/MC:L/MI:H/MA:H/CR:L/IR:H/AR:X 2.5 2.4 5.9",
                "AV:N/AC:H/PR:N/UI:N/S:U/C:N/I:L/A:H/E:H/RL:U/RC:U/MAV:L/MAC:H/MPR:L/MUI:N/MS:U/MC:X/MI:L/MA:N/CR:X/IR:X/AR:X 6.5 6.0 2.4",
                "AV:P/AC:L/PR:H/UI:N/S:U/C:L/I:H/A:L/E:H/RL:U/RC:U/MAV:N/MAC:X/MPR:H/MUI:X/MS:C/MC:X/MI:L/MA:N/CR:X/IR:H/AR:X 5.0 4.7 5.8",
                "AV:N/AC:L/PR:N/UI:N/S:U/C:L/I:N/A:L/E:H/RL:U/RC:U/MAV:N/MAC:L/MPR:N/MUI:X/MS:C/MC:N/MI:H/MA:X/CR:L/IR:H/AR:M 6.5 6.0 9.3",
                "AV:P/AC:H/PR:L/UI:R/S:C/C:N/I:N/A:H/E:H/RL:U/RC:U/MAV:P/MAC:X/MPR:L/MUI:N/MS:X/MC:X/MI:N/MA:X/CR:L/IR:M/AR:L 4.7 4.4 2.4",
                "AV:P/AC:H/PR:H/UI:N/S:U/C:N/I:N/A:H/E:H/RL:U/RC:U/MAV:X/MAC:L/MPR:H/MUI:X/MS:X/MC:H/MI:L/MA:X/CR:L/IR:L/AR:X 3.8 3.5 4.7",
                "AV:A/AC:L/PR:L/UI:R/S:C/C:H/I:N/A:L/E:H/RL:U/RC:U/MAV:N/MAC:L/MPR:L/MUI:N/MS:C/MC:N/MI:H/MA:H/CR:H/IR:H/AR:L 6.9 6.4 9.3",
                "AV:N/AC:H/PR:H/UI:R/S:U/C:L/I:H/A:N/E:H/RL:U/RC:U/MAV:L/MAC:H/MPR:L/MUI:N/MS:U/MC:L/MI:N/MA:H/CR:L/IR:L/AR:M 4.8 4.5 4.7",
                "AV:N/AC:H/PR:N/UI:N/S:U/C:N/I:N/A:L/E:H/RL:U/RC:U/MAV:A/MAC:H/MPR:H/MUI:R/MS:U/MC:L/MI:N/MA:N/CR:H/IR:M/AR:H 3.7 3.5 2.4",
                "AV:A/AC:H/PR:N/UI:N/S:U/C:L/I:L/A:L/E:H/RL:U/RC:U/MAV:N/MAC:H/MPR:H/MUI:R/MS:C/MC:N/MI:N/MA:L/CR:L/IR:M/AR:L 5.0 4.7 1.6",
                "AV:L/AC:H/PR:L/UI:R/S:U/C:L/I:H/A:N/E:H/RL:U/RC:U/MAV:P/MAC:L/MPR:H/MUI:N/MS:U/MC:H/MI:L/MA:N/CR:M/IR:H/AR:L 5.0 4.7 4.6",
                "AV:L/AC:L/PR:L/UI:R/S:U/C:N/I:N/A:H/E:H/RL:U/RC:U/MAV:A/MAC:L/MPR:N/MUI:N/MS:C/MC:N/MI:H/MA:N/CR:L/IR:L/AR:H 5.0 4.7 4.8",
                "AV:N/AC:L/PR:L/UI:R/S:U/C:H/I:H/A:H/E:H/RL:U/RC:U/MAV:N/MAC:L/MPR:L/MUI:N/MS:C/MC:L/MI:L/MA:H/CR:H/IR:L/AR:H 8.0 7.4 9.3",
                "AV:L/AC:H/PR:L/UI:R/S:U/C:H/I:L/A:N/E:X/RL:X/RC:U/MAV:X/MAC:X/MPR:X/MUI:X/MS:X/MC:X/MI:X/MA:X/CR:X/IR:X/AR:X 5.0 4.7 NaN",
                "AV:L/AC:H/PR:L/UI:N/S:U/C:N/I:N/A:L/E:H/RL:X/RC:U/MAV:X/MAC:X/MPR:X/MUI:X/MS:X/MC:X/MI:X/MA:X/CR:X/IR:X/AR:X 2.5 2.4 NaN",
                "AV:L/AC:H/PR:L/UI:N/S:U/C:N/I:L/A:N/E:X/RL:U/RC:U/MAV:X/MAC:X/MPR:X/MUI:X/MS:X/MC:X/MI:X/MA:X/CR:X/IR:X/AR:X 2.5 2.4 NaN",
                "AV:L/AC:H/PR:L/UI:R/S:U/C:N/I:H/A:L/E:H/RL:X/RC:U/MAV:X/MAC:X/MPR:X/MUI:X/MS:X/MC:X/MI:X/MA:X/CR:X/IR:X/AR:X 5.0 4.7 NaN",
                "AV:N/AC:H/PR:H/UI:N/S:U/C:H/I:N/A:L/E:X/RL:U/RC:U/MAV:X/MAC:X/MPR:X/MUI:X/MS:X/MC:X/MI:X/MA:X/CR:X/IR:X/AR:X 5.0 4.7 NaN",
                "AV:P/AC:H/PR:L/UI:R/S:U/C:L/I:H/A:L/E:H/RL:U/RC:U/MAV:P/MAC:X/MPR:N/MUI:N/MS:X/MC:N/MI:X/MA:L/CR:H/IR:X/AR:L 5.0 4.7 4.2",
                "AV:A/AC:H/PR:N/UI:N/S:U/C:L/I:L/A:L/E:H/RL:X/RC:U/MAV:A/MAC:L/MPR:L/MUI:N/MS:X/MC:L/MI:L/MA:H/CR:H/IR:H/AR:L 5.0 4.7 6.0",
                "AV:L/AC:L/PR:H/UI:R/S:C/C:N/I:H/A:H/E:X/RL:U/RC:U/MAV:X/MAC:H/MPR:L/MUI:X/MS:U/MC:N/MI:L/MA:H/CR:L/IR:M/AR:X 7.4 6.9 4.7",
                "AV:P/AC:L/PR:H/UI:N/S:U/C:L/I:H/A:L/E:H/RL:U/RC:U/MAV:N/MAC:H/MPR:H/MUI:X/MS:C/MC:X/MI:X/MA:N/CR:X/IR:X/AR:M 5.0 4.7 6.1",
                "AV:L/AC:H/PR:H/UI:N/S:C/C:N/I:L/A:N/E:X/RL:U/RC:U/MAV:N/MAC:X/MPR:N/MUI:R/MS:X/MC:H/MI:L/MA:H/CR:L/IR:L/AR:M 2.5 2.4 6.9",
                "AV:P/AC:L/PR:H/UI:N/S:U/C:L/I:L/A:H/E:H/RL:X/RC:U/MAV:P/MAC:L/MPR:L/MUI:N/MS:U/MC:X/MI:N/MA:L/CR:L/IR:X/AR:M 5.0 4.7 2.5",
                "AV:N/AC:L/PR:N/UI:N/S:C/C:H/I:N/A:H/E:H/RL:U/RC:U/MAV:P/MAC:X/MPR:X/MUI:N/MS:C/MC:L/MI:X/MA:H/CR:M/IR:X/AR:L 10.0 9.3 4.1",
                "AV:A/AC:H/PR:N/UI:R/S:U/C:H/I:N/A:L/E:H/RL:U/RC:U/MAV:N/MAC:L/MPR:H/MUI:R/MS:C/MC:L/MI:N/MA:N/CR:L/IR:M/AR:M 5.4 5.0 2.4",
                "AV:N/AC:L/PR:H/UI:R/S:U/C:H/I:N/A:N/E:X/RL:X/RC:U/MAV:N/MAC:H/MPR:H/MUI:R/MS:C/MC:L/MI:N/MA:L/CR:L/IR:L/AR:L 4.5 4.2 2.4",
                "AV:N/AC:L/PR:L/UI:R/S:U/C:L/I:N/A:H/E:X/RL:X/RC:U/MAV:A/MAC:L/MPR:N/MUI:N/MS:U/MC:N/MI:N/MA:L/CR:H/IR:M/AR:H 6.3 5.8 4.7",
                "AV:L/AC:H/PR:L/UI:R/S:U/C:N/I:L/A:H/E:X/RL:X/RC:U/MAV:L/MAC:L/MPR:L/MUI:N/MS:C/MC:N/MI:H/MA:L/CR:H/IR:M/AR:L 5.0 4.7 6.4",
                "AV:L/AC:H/PR:L/UI:R/S:U/C:L/I:N/A:H/E:H/RL:U/RC:U/MAV:P/MAC:L/MPR:N/MUI:N/MS:C/MC:H/MI:H/MA:L/CR:H/IR:L/AR:H 5.0 4.7 7.0",
                "AV:L/AC:L/PR:N/UI:R/S:C/C:L/I:L/A:N/E:H/RL:X/RC:U/MAV:N/MAC:L/MPR:N/MUI:N/MS:U/MC:L/MI:H/MA:L/CR:H/IR:M/AR:M 5.0 4.7 8.2",
                "AV:A/AC:H/PR:N/UI:N/S:U/C:H/I:N/A:H/E:X/RL:X/RC:U/MAV:A/MAC:L/MPR:H/MUI:R/MS:C/MC:H/MI:H/MA:N/CR:L/IR:L/AR:L 6.8 6.3 4.7",
                "AV:L/AC:H/PR:L/UI:R/S:C/C:L/I:L/A:L/E:H/RL:U/RC:U/MAV:N/MAC:L/MPR:H/MUI:N/MS:U/MC:H/MI:H/MA:L/CR:H/IR:H/AR:M 5.0 4.7 6.7",
                "AV:L/AC:L/PR:N/UI:N/S:U/C:H/I:N/A:N/E:X/RL:U/RC:U/MAV:P/MAC:H/MPR:N/MUI:R/MS:U/MC:N/MI:L/MA:H/CR:H/IR:H/AR:M 6.2 5.8 4.7",
                "AV:P/AC:L/PR:H/UI:R/S:U/C:H/I:L/A:L/E:X/RL:X/RC:U/MAV:P/MAC:H/MPR:N/MUI:N/MS:U/MC:L/MI:L/MA:L/CR:L/IR:H/AR:M 5.0 4.7 3.7",
                "AV:A/AC:H/PR:L/UI:N/S:C/C:N/I:H/A:L/E:X/RL:U/RC:U/MAV:N/MAC:H/MPR:N/MUI:R/MS:U/MC:N/MI:H/MA:L/CR:M/IR:L/AR:H 6.5 6.0 4.7",
                "AV:L/AC:H/PR:L/UI:R/S:U/C:L/I:N/A:H/E:H/RL:U/RC:U/MAV:A/MAC:L/MPR:H/MUI:N/MS:U/MC:N/MI:N/MA:N/CR:M/IR:L/AR:M 5.0 4.7 0.0",
                "AV:P/AC:H/PR:L/UI:R/S:U/C:L/I:L/A:H/E:H/RL:U/RC:U/MAV:X/MAC:X/MPR:X/MUI:X/MS:X/MC:X/MI:X/MA:X/CR:X/IR:X/AR:X 5.0 4.7 NaN",
                "AV:P/AC:L/PR:H/UI:N/S:U/C:L/I:H/A:L/E:H/RL:U/RC:U/MAV:X/MAC:X/MPR:X/MUI:X/MS:X/MC:X/MI:X/MA:X/CR:X/IR:X/AR:X 5.0 4.7 NaN",
                "AV:A/AC:H/PR:L/UI:R/S:U/C:H/I:L/A:H/E:H/RL:U/RC:U/MAV:P/MAC:L/MPR:H/MUI:X/MS:C/MC:X/MI:N/MA:N/CR:L/IR:L/AR:H 6.4 5.9 2.4",
                "AV:N/AC:L/PR:H/UI:R/S:C/C:L/I:H/A:L/E:H/RL:U/RC:U/MAV:L/MAC:H/MPR:X/MUI:X/MS:X/MC:N/MI:X/MA:N/CR:L/IR:M/AR:L 7.5 6.9 4.7",
                "AV:P/AC:H/PR:N/UI:R/S:U/C:N/I:H/A:N/E:H/RL:U/RC:U/MAV:X/MAC:X/MPR:X/MUI:X/MS:C/MC:X/MI:H/MA:X/CR:H/IR:L/AR:M 4.0 3.7 2.4",
                "AV:L/AC:H/PR:L/UI:R/S:U/C:N/I:L/A:H/E:H/RL:U/RC:U/MAV:X/MAC:L/MPR:H/MUI:R/MS:U/MC:X/MI:H/MA:X/CR:M/IR:H/AR:M 5.0 4.7 6.0",
                "AV:L/AC:L/PR:H/UI:N/S:C/C:N/I:H/A:N/E:H/RL:U/RC:U/MAV:P/MAC:H/MPR:N/MUI:R/MS:C/MC:L/MI:L/MA:L/CR:H/IR:M/AR:M 6.0 5.6 4.7",
                "AV:P/AC:H/PR:L/UI:N/S:U/C:L/I:L/A:N/E:H/RL:U/RC:U/MAV:L/MAC:H/MPR:H/MUI:N/MS:U/MC:N/MI:L/MA:L/CR:H/IR:L/AR:M 2.9 2.7 2.4",
                "AV:L/AC:L/PR:N/UI:R/S:C/C:N/I:H/A:N/E:H/RL:U/RC:U/MAV:L/MAC:H/MPR:H/MUI:N/MS:U/MC:N/MI:L/MA:H/CR:L/IR:H/AR:M 6.3 5.8 4.7",
                "AV:L/AC:H/PR:L/UI:N/S:U/C:L/I:N/A:N/E:H/RL:U/RC:U/MAV:A/MAC:L/MPR:N/MUI:R/MS:U/MC:H/MI:N/MA:H/CR:H/IR:L/AR:H 2.5 2.4 7.4",
                "AV:L/AC:H/PR:L/UI:R/S:U/C:H/I:L/A:N/E:H/RL:X/RC:U/MAV:X/MAC:X/MPR:X/MUI:X/MS:X/MC:X/MI:X/MA:X/CR:X/IR:X/AR:X 5.0 4.7 NaN",
                "AV:L/AC:H/PR:H/UI:N/S:C/C:L/I:N/A:N/E:X/RL:X/RC:U/MAV:X/MAC:X/MPR:X/MUI:X/MS:X/MC:X/MI:X/MA:X/CR:X/IR:X/AR:X 2.5 2.4 NaN",
                "AV:L/AC:H/PR:L/UI:R/S:C/C:N/I:L/A:N/E:H/RL:U/RC:U/MAV:X/MAC:X/MPR:X/MUI:X/MS:X/MC:X/MI:X/MA:X/CR:X/IR:X/AR:X 2.5 2.4 NaN",
                "AV:N/AC:H/PR:H/UI:N/S:U/C:N/I:H/A:L/E:X/RL:X/RC:U/MAV:X/MAC:X/MPR:X/MUI:X/MS:X/MC:X/MI:X/MA:X/CR:X/IR:X/AR:X 5.0 4.7 NaN",
                "AV:P/AC:L/PR:H/UI:N/S:U/C:L/I:L/A:H/E:X/RL:U/RC:U/MAV:X/MAC:X/MPR:X/MUI:X/MS:X/MC:X/MI:X/MA:X/CR:X/IR:X/AR:X 5.0 4.7 NaN",
                "AV:L/AC:H/PR:L/UI:R/S:C/C:N/I:L/A:N/E:X/RL:X/RC:U/MAV:X/MAC:X/MPR:X/MUI:X/MS:X/MC:X/MI:X/MA:X/CR:X/IR:X/AR:X 2.5 2.4 NaN",
                "AV:N/AC:H/PR:H/UI:N/S:U/C:L/I:N/A:H/E:H/RL:U/RC:U/MAV:X/MAC:X/MPR:X/MUI:X/MS:X/MC:X/MI:X/MA:X/CR:X/IR:X/AR:X 5.0 4.7 NaN",
                "AV:L/AC:H/PR:H/UI:N/S:C/C:L/I:L/A:L/E:H/RL:U/RC:U/MAV:X/MAC:X/MPR:X/MUI:X/MS:X/MC:X/MI:X/MA:X/CR:X/IR:X/AR:X 5.0 4.7 NaN",
                "AV:A/AC:L/PR:L/UI:R/S:U/C:L/I:L/A:L/E:H/RL:U/RC:U/MAV:L/MAC:H/MPR:X/MUI:X/MS:X/MC:H/MI:L/MA:L/CR:M/IR:L/AR:L 4.9 4.6 4.7",
                "AV:P/AC:H/PR:L/UI:N/S:U/C:L/I:N/A:L/E:H/RL:U/RC:U/MAV:X/MAC:X/MPR:N/MUI:X/MS:U/MC:H/MI:L/MA:X/CR:X/IR:X/AR:L 2.9 2.7 4.7",
                "AV:L/AC:H/PR:H/UI:R/S:C/C:H/I:N/A:N/E:H/RL:X/RC:U/MAV:A/MAC:H/MPR:L/MUI:N/MS:X/MC:H/MI:X/MA:N/CR:L/IR:X/AR:M 5.0 4.7 3.3",
                "AV:N/AC:H/PR:H/UI:R/S:U/C:L/I:N/A:L/E:H/RL:U/RC:U/MAV:P/MAC:L/MPR:L/MUI:X/MS:U/MC:L/MI:N/MA:X/CR:X/IR:H/AR:L 3.1 2.9 2.4",
                "AV:P/AC:H/PR:N/UI:N/S:U/C:L/I:L/A:H/E:H/RL:X/RC:U/MAV:X/MAC:H/MPR:X/MUI:R/MS:C/MC:H/MI:N/MA:N/CR:L/IR:L/AR:X 5.3 4.9 2.4",
                "AV:P/AC:L/PR:L/UI:R/S:U/C:N/I:H/A:L/E:H/RL:U/RC:U/MAV:L/MAC:H/MPR:H/MUI:N/MS:U/MC:L/MI:N/MA:L/CR:L/IR:L/AR:M 4.8 4.5 2.4",
                "AV:A/AC:L/PR:L/UI:R/S:U/C:L/I:L/A:H/E:H/RL:X/RC:U/MAV:L/MAC:H/MPR:H/MUI:R/MS:C/MC:N/MI:L/MA:L/CR:M/IR:H/AR:H 6.3 5.8 4.7",
                "AV:P/AC:L/PR:H/UI:R/S:U/C:L/I:L/A:H/E:X/RL:U/RC:U/MAV:P/MAC:H/MPR:H/MUI:R/MS:U/MC:N/MI:L/MA:N/CR:L/IR:M/AR:L 5.0 4.7 1.5",
                "AV:L/AC:H/PR:H/UI:R/S:C/C:H/I:H/A:L/E:H/RL:X/RC:U/MAV:N/MAC:H/MPR:H/MUI:R/MS:U/MC:L/MI:L/MA:H/CR:L/IR:M/AR:M 7.2 6.7 4.7",
                "AV:P/AC:L/PR:H/UI:N/S:U/C:H/I:L/A:L/E:X/RL:U/RC:U/MAV:A/MAC:L/MPR:N/MUI:R/MS:C/MC:H/MI:L/MA:N/CR:L/IR:H/AR:M 5.0 4.7 5.8",
                "AV:L/AC:L/PR:H/UI:R/S:C/C:H/I:L/A:L/E:H/RL:X/RC:U/MAV:P/MAC:H/MPR:H/MUI:R/MS:C/MC:L/MI:L/MA:N/CR:M/IR:L/AR:L 6.9 6.4 2.4",
                "AV:P/AC:L/PR:L/UI:N/S:C/C:H/I:L/A:N/E:H/RL:X/RC:U/MAV:P/MAC:H/MPR:L/MUI:N/MS:U/MC:N/MI:L/MA:H/CR:M/IR:H/AR:M 5.9 5.5 4.7",
                "AV:L/AC:H/PR:L/UI:R/S:U/C:N/I:H/A:N/E:X/RL:U/RC:U/MAV:N/MAC:H/MPR:H/MUI:N/MS:U/MC:H/MI:N/MA:L/CR:M/IR:H/AR:M 4.4 4.1 4.7",
                "AV:L/AC:H/PR:N/UI:R/S:U/C:L/I:N/A:N/E:X/RL:X/RC:U/MAV:N/MAC:H/MPR:L/MUI:N/MS:C/MC:L/MI:H/MA:N/CR:H/IR:M/AR:M 2.5 2.4 6.9",
                "AV:N/AC:L/PR:N/UI:R/S:C/C:L/I:N/A:H/E:H/RL:U/RC:U/MAV:X/MAC:X/MPR:N/MUI:N/MS:X/MC:H/MI:L/MA:X/CR:X/IR:H/AR:L 8.2 7.6 9.3",
                "AV:P/AC:L/PR:H/UI:N/S:U/C:L/I:L/A:H/E:H/RL:U/RC:U/MAV:A/MAC:X/MPR:N/MUI:N/MS:X/MC:L/MI:L/MA:N/CR:X/IR:L/AR:M 5.0 4.7 4.5",
                "AV:N/AC:L/PR:L/UI:N/S:C/C:H/I:H/A:H/E:H/RL:U/RC:U/MAV:X/MAC:L/MPR:N/MUI:N/MS:X/MC:X/MI:L/MA:N/CR:H/IR:X/AR:L 9.9 9.2 9.3",
                "AV:L/AC:H/PR:L/UI:N/S:U/C:H/I:L/A:N/E:H/RL:U/RC:U/MAV:N/MAC:X/MPR:N/MUI:N/MS:X/MC:X/MI:X/MA:L/CR:L/IR:L/AR:L 5.3 4.9 4.7",
                "AV:P/AC:L/PR:H/UI:N/S:C/C:H/I:N/A:N/E:H/RL:U/RC:U/MAV:X/MAC:X/MPR:X/MUI:R/MS:X/MC:N/MI:N/MA:H/CR:X/IR:H/AR:L 4.9 4.6 2.4",
                "AV:L/AC:H/PR:L/UI:R/S:U/C:L/I:H/A:N/E:H/RL:U/RC:U/MAV:L/MAC:X/MPR:N/MUI:R/MS:U/MC:X/MI:X/MA:X/CR:L/IR:M/AR:X 5.0 4.7 4.7",
                "AV:L/AC:H/PR:L/UI:R/S:U/C:L/I:H/A:N/E:H/RL:U/RC:U/MAV:P/MAC:L/MPR:H/MUI:R/MS:U/MC:L/MI:L/MA:L/CR:L/IR:H/AR:M 5.0 4.7 3.5",
                "AV:N/AC:H/PR:H/UI:N/S:U/C:L/I:L/A:L/E:H/RL:U/RC:U/MAV:L/MAC:H/MPR:L/MUI:R/MS:C/MC:L/MI:N/MA:N/CR:M/IR:H/AR:H 4.1 3.8 2.4",
                "AV:L/AC:H/PR:L/UI:R/S:U/C:H/I:N/A:L/E:H/RL:U/RC:U/MAV:P/MAC:L/MPR:N/MUI:R/MS:C/MC:N/MI:L/MA:N/CR:M/IR:H/AR:M 5.0 4.7 3.0",
                "AV:N/AC:L/PR:N/UI:N/S:C/C:H/I:H/A:L/E:H/RL:U/RC:U/MAV:L/MAC:L/MPR:N/MUI:N/MS:C/MC:H/MI:N/MA:L/CR:H/IR:M/AR:H 10.0 9.3 8.6");

        for (String fullvector : vectors) {
            String[] split = fullvector.split(" ");
            double base = Double.parseDouble(split[1]);
            double temporal = Double.parseDouble(split[2]);
            double enviromental = Double.parseDouble(split[3]);
            Cvss3P0 cvss3P0 = new Cvss3P0(split[0]);
            LOG.info(String.format("CVSS3P0: %.2f  %.2f  %.2f", cvss3P0.getBaseScore(), cvss3P0.getTemporalScore(), cvss3P0.getEnvironmentalScore()));
            LOG.info(String.format("ACTUAL: %.2f  %.2f  %.2f", base, temporal, enviromental));

            Assert.assertEquals(cvss3P0.getBaseScore(), base, 0.01);
            Assert.assertEquals(cvss3P0.getTemporalScore(), temporal, 0.01);
            Assert.assertEquals(cvss3P0.getEnvironmentalScore(), enviromental, 0.01);

            MultiScoreCvssVector reParsed = (MultiScoreCvssVector) CvssVector.parseVector(cvss3P0.toString());
            Assert.assertEquals(reParsed.toString(), cvss3P0.toString());
            Assert.assertTrue(reParsed + " " + cvss3P0, reParsed.toString().startsWith("CVSS:3.0/"));
            Assert.assertEquals(reParsed.getBaseScore(), base, 0.01);
            Assert.assertEquals(reParsed.getTemporalScore(), temporal, 0.01);
            Assert.assertEquals(reParsed.getEnvironmentalScore(), enviromental, 0.01);
        }
    }
}
