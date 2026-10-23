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
 * Details of a GitHub issue event.
 *
 * @author Tommy Ludwig
 */
public class IssueEvent {

	private final String event;

	private final String commitId;

	@JsonCreator
	public IssueEvent(@JsonProperty("event") String event, @JsonProperty("commit_id") String commitId) {
		this.event = event;
		this.commitId = commitId;
	}

	public String getEvent() {
		return this.event;
	}

	public String getCommitId() {
		return this.commitId;
	}

}
