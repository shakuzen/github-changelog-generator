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

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link Commit}.
 *
 * @author Tommy Ludwig
 */
@JsonTest
class CommitJsonTests {

	@Autowired
	private JacksonTester<Commit> json;

	@Test
	void commitDeserialization() throws Exception {
		Commit commit = this.json.read("commit.json").getObject();
		assertThat(commit.getAuthor()).isNotNull();
		assertThat(commit.getAuthor().getName()).isEqualTo("octocat");
		assertThat(commit.getGitCommit()).isNotNull();
		assertThat(commit.getGitCommit().getMessage())
			.isEqualTo("Fix issue\n\nCo-authored-by: Contributor <contributor@users.noreply.github.com>");
	}

}
