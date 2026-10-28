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
 * Details of a GitHub commit.
 *
 * @author Tommy Ludwig
 */
public class Commit {

	private final User author;

	private final GitCommit gitCommit;

	@JsonCreator
	public Commit(@JsonProperty("author") User author, @JsonProperty("commit") GitCommit gitCommit) {
		this.author = author;
		this.gitCommit = gitCommit;
	}

	public Commit(User author) {
		this(author, null);
	}

	public User getAuthor() {
		return this.author;
	}

	public GitCommit getGitCommit() {
		return this.gitCommit;
	}

	/**
	 * Git commit details.
	 */
	public static class GitCommit {

		private final String message;

		@JsonCreator
		public GitCommit(@JsonProperty("message") String message) {
			this.message = message;
		}

		public String getMessage() {
			return this.message;
		}

	}

}
