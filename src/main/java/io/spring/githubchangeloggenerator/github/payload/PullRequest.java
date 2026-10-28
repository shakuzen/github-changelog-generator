/*
 * Copyright 2018-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.spring.githubchangeloggenerator.github.payload;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a Github Pull Request.
 *
 * @author Madhura Bhave
 * @author Tommy Ludwig
 */
public class PullRequest {

	private final String url;

	private final String mergedAt;

	@JsonCreator
	public PullRequest(@JsonProperty("url") String url, @JsonProperty("merged_at") String mergedAt) {
		this.url = url;
		this.mergedAt = mergedAt;
	}

	public PullRequest(String url) {
		this(url, null);
	}

	public String getUrl() {
		return this.url;
	}

	public String getMergedAt() {
		return this.mergedAt;
	}

	public boolean isMerged() {
		return this.mergedAt != null;
	}

}
