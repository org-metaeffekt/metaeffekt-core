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
package org.metaeffekt.core.inventory.processor.report.model;

import org.json.JSONObject;

/*
[
  {
    "all": [
      [
        "deserialization"
      ],
      [
        "untrusted"
      ]
    ],
    "score": 2,
    "notes": "This is an example keyword set (2)",
    "min": [],
    "max": [],
    "name": "example-keywords-2",
    "none": [],
    "category": "deserialization"
  }
]
 */
public class KeywordsInstance {
    private final Double score;
    private final String name;
    private final String category;
    private final String notes;

    public KeywordsInstance(Double score, String name, String category, String notes) {
        this.score = score;
        this.name = name;
        this.category = category;
        this.notes = notes;
    }

    public KeywordsInstance(JSONObject json) {
        this.score = json.getDouble("score");
        this.name = json.getString("name");
        this.category = json.getString("category");
        this.notes = json.getString("notes");
    }

    public Double getScore() {
        return score;
    }

    public String getName() {
        return name;
    }

    public String getCategory() {
        return category;
    }

    public String getNotes() {
        return notes;
    }
}
