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

import io.spring.githubchangeloggenerator.github.service.Repository;

/**
 * Details of a GitHub issue timeline event.
 *
 * @author Tommy Ludwig
 */
public class TimelineEvent {

	private final String event;

	private final String commitId;

	private final Source source;

	@JsonCreator
	public TimelineEvent(@JsonProperty("event") String event, @JsonProperty("commit_id") String commitId,
			@JsonProperty("source") Source source) {
		this.event = event;
		this.commitId = commitId;
		this.source = source;
	}

	public TimelineEvent(String event) {
		this(event, null, null);
	}

	public TimelineEvent(String event, String commitId) {
		this(event, commitId, null);
	}

	public TimelineEvent(String event, Source source) {
		this(event, null, source);
	}

	public String getEvent() {
		return this.event;
	}

	public String getCommitId() {
		return this.commitId;
	}

	public Source getSource() {
		return this.source;
	}

	/**
	 * Source details for cross-referenced events.
	 */
	public static class Source {

		private final SourceIssue issue;

		@JsonCreator
		public Source(@JsonProperty("issue") SourceIssue issue) {
			this.issue = issue;
		}

		public SourceIssue getIssue() {
			return this.issue;
		}

	}

	/**
	 * Source issue details.
	 */
	public static class SourceIssue {

		private final User user;

		private final PullRequest pullRequest;

		private final SourceRepository repository;

		@JsonCreator
		public SourceIssue(@JsonProperty("user") User user, @JsonProperty("pull_request") PullRequest pullRequest,
				@JsonProperty("repository") SourceRepository repository) {
			this.user = user;
			this.pullRequest = pullRequest;
			this.repository = repository;
		}

		public User getUser() {
			return this.user;
		}

		public PullRequest getPullRequest() {
			return this.pullRequest;
		}

		public SourceRepository getRepository() {
			return this.repository;
		}

	}

	/**
	 * Source repository details.
	 */
	public static class SourceRepository {

		private final String name;

		private final User owner;

		@JsonCreator
		public SourceRepository(@JsonProperty("name") String name, @JsonProperty("owner") User owner) {
			this.name = name;
			this.owner = owner;
		}

		public String getName() {
			return this.name;
		}

		public User getOwner() {
			return this.owner;
		}

		public boolean matches(Repository repository) {
			return repository != null && repository.getName().equalsIgnoreCase(this.name) && this.owner != null
					&& repository.getOwner().equalsIgnoreCase(this.owner.getName());
		}

	}

}
