/*
 * Copyright 2018-2025 the original author or authors.
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

package io.spring.githubchangeloggenerator;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StringUtils;

import io.spring.githubchangeloggenerator.ApplicationProperties.ExternalLink;
import io.spring.githubchangeloggenerator.ApplicationProperties.IssueSort;
import io.spring.githubchangeloggenerator.github.payload.Commit;
import io.spring.githubchangeloggenerator.github.payload.Commit.GitCommit;
import io.spring.githubchangeloggenerator.github.payload.Issue;
import io.spring.githubchangeloggenerator.github.payload.Label;
import io.spring.githubchangeloggenerator.github.payload.PullRequest;
import io.spring.githubchangeloggenerator.github.payload.TimelineEvent;
import io.spring.githubchangeloggenerator.github.payload.TimelineEvent.Source;
import io.spring.githubchangeloggenerator.github.payload.TimelineEvent.SourceIssue;
import io.spring.githubchangeloggenerator.github.payload.TimelineEvent.SourceRepository;
import io.spring.githubchangeloggenerator.github.payload.User;
import io.spring.githubchangeloggenerator.github.service.GitHubService;
import io.spring.githubchangeloggenerator.github.service.Repository;

/**
 * Generates a changelog markdown file which includes bug fixes, enhancements and
 * contributors for a given milestone.
 *
 * @author Madhura Bhave
 * @author Phillip Webb
 * @author Mahendra Bishnoi
 * @author Tommy Ludwig
 */
@Component
public class ChangelogGenerator {

	private static final Logger logger = LoggerFactory.getLogger(ChangelogGenerator.class);

	private static final Comparator<Issue> TITLE_COMPARATOR = Comparator.comparing(Issue::getTitle,
			String.CASE_INSENSITIVE_ORDER);

	private static final Pattern CO_AUTHORED_BY_PATTERN = Pattern
		.compile("(?im)^co-authored-by:\\s*.*?<(?:\\d+\\+)?([^@>]+)@users\\.noreply\\.github\\.com>");

	private static final List<Escape> escapes = Arrays.asList(gitHubUserMentions(), htmlTags(), markdownStyling());

	private final GitHubService service;

	private final Repository repository;

	private final MilestoneReference milestoneReference;

	private final IssueSort sort;

	private final Set<String> excludeLabels;

	private final Set<String> excludeContributors;

	private final String contributorsTitle;

	private final boolean includeClosingContributors;

	private final ChangelogSections sections;

	private final List<ExternalLink> externalLinks;

	private final boolean generateLinks;

	private final IssueChain issueChain;

	public ChangelogGenerator(GitHubService service, ApplicationProperties properties) {
		this.service = service;
		this.repository = properties.getRepository();
		this.milestoneReference = properties.getMilestoneReference();
		this.sort = properties.getIssues().getSort();
		this.excludeLabels = properties.getIssues().getExcludes().getLabels();
		this.excludeContributors = properties.getContributors().getExclude().getNames();
		this.contributorsTitle = properties.getContributors().getTitle();
		this.includeClosingContributors = properties.getContributors().isIncludeClosingContributors();
		this.externalLinks = properties.getExternalLinks();
		this.generateLinks = properties.getIssues().isGenerateLinks();
		this.issueChain = new PortedIssueChain(properties.getIssues().getPorts(), this.service, this.repository);
		this.sections = new ChangelogSections(properties, service, this.issueChain);
	}

	/**
	 * Generates a file at the given path which includes bug fixes, enhancements and
	 * contributors for the given milestone.
	 * @param milestone the milestone to generate the changelog for
	 * @param path the path to the file
	 * @throws IOException if writing to file failed
	 */
	public void generate(String milestone, String path) throws IOException {
		int milestoneNumber = resolveMilestoneReference(milestone);
		List<Issue> issues = getIssues(milestoneNumber);
		String content = generateContent(issues);
		writeContentToFile(content, path);
	}

	private List<Issue> getIssues(int milestoneNumber) {
		List<Issue> issues = new ArrayList<>(this.service.getIssuesForMilestone(milestoneNumber, this.repository));
		issues.removeIf(this::isExcluded);
		return issues;
	}

	private boolean isExcluded(Issue issue) {
		return issue.getLabels().stream().anyMatch(this::isExcluded);
	}

	private boolean isExcluded(Label label) {
		return this.excludeLabels.contains(label.getName());
	}

	private int resolveMilestoneReference(String milestone) {
		return switch (this.milestoneReference) {
			case TITLE -> this.service.getMilestoneNumber(milestone, this.repository);
			case ID -> Integer.parseInt(milestone);
		};
	}

	private String generateContent(List<Issue> issues) {
		StringBuilder content = new StringBuilder();
		addSectionContent(content, this.sections.collate(issues));
		Set<User> contributors = getContributors(issues);
		if (!contributors.isEmpty()) {
			addContributorsContent(content, contributors);
		}
		if (!this.externalLinks.isEmpty()) {
			addExternalLinksContent(content, this.externalLinks);
		}
		return content.toString();
	}

	private void addSectionContent(StringBuilder content, Map<ChangelogSection, List<Issue>> sectionIssues) {
		sectionIssues.forEach((section, issues) -> {
			sort(section.getSort(), issues);
			content.append((content.length() != 0) ? String.format("%n") : "");
			content.append("## ").append(section).append(String.format("%n%n"));
			issues.stream().map((issue) -> getFormattedIssue(issue, section)).forEach(content::append);
		});
	}

	private void sort(IssueSort sort, List<Issue> issues) {
		sort = (sort != null) ? sort : this.sort;
		if (sort == IssueSort.TITLE) {
			issues.sort(TITLE_COMPARATOR);
		}
	}

	private String getFormattedIssue(Issue issue, ChangelogSection section) {
		String title = section.summarize(issue);
		for (Escape escape : escapes) {
			title = escape.apply(title);
		}
		return (this.generateLinks) ? String.format("- %s %s%n", title, getLinkToIssue(issue))
				: String.format("- %s%n", title);
	}

	private String getLinkToIssue(Issue issue) {
		return "[#" + issue.getNumber() + "]" + "(" + issue.getUrl() + ")";
	}

	private Set<User> getContributors(List<Issue> issues) {
		if (this.excludeContributors.contains("*")) {
			return Collections.emptySet();
		}
		return issues.stream()
			.map(this::getPortedReferenceIssue)
			.flatMap(this::getContributorsForIssue)
			.filter(this::isIncludedContributor)
			.collect(Collectors.toSet());
	}

	private Stream<User> getContributorsForIssue(Issue issue) {
		if (issue.getPullRequest() != null) {
			return Stream.of(issue.getUser());
		}
		if (this.includeClosingContributors) {
			Set<User> contributors = getClosingContributors(issue);
			if (contributors.isEmpty() && isIncludedContributor(issue.getUser())) {
				logger.warn("No contributor found for issue #{}: {}", issue.getNumber(), issue.getUrl());
			}
			return contributors.stream();
		}
		return Stream.empty();
	}

	private Set<User> getClosingContributors(Issue issue) {
		List<TimelineEvent> timeline = this.service.getTimelineForIssue(Integer.parseInt(issue.getNumber()),
				this.repository);
		Set<User> contributors = new HashSet<>();
		String commitId = getClosingCommitId(timeline);
		if (commitId != null) {
			Commit commit = this.service.getCommit(commitId, this.repository);
			if (commit != null) {
				if (commit.getAuthor() != null) {
					contributors.add(commit.getAuthor());
				}
				contributors.addAll(getCoAuthors(commit));
			}
		}
		contributors.addAll(getClosingPullRequestAuthors(timeline));
		return contributors;
	}

	private String getClosingCommitId(List<TimelineEvent> timeline) {
		for (int i = timeline.size() - 1; i >= 0; i--) {
			TimelineEvent event = timeline.get(i);
			if ("closed".equals(event.getEvent()) && StringUtils.hasText(event.getCommitId())) {
				return event.getCommitId();
			}
		}
		return null;
	}

	private Set<User> getClosingPullRequestAuthors(List<TimelineEvent> timeline) {
		Set<User> authors = new HashSet<>();
		for (TimelineEvent event : timeline) {
			if ("cross-referenced".equals(event.getEvent())) {
				Source source = event.getSource();
				if (source != null && source.getIssue() != null) {
					SourceIssue sourceIssue = source.getIssue();
					PullRequest pullRequest = sourceIssue.getPullRequest();
					SourceRepository sourceRepository = sourceIssue.getRepository();
					if (pullRequest != null && pullRequest.isMerged() && isMatchingRepository(sourceRepository)
							&& sourceIssue.getUser() != null) {
						authors.add(sourceIssue.getUser());
					}
				}
			}
		}
		return authors;
	}

	private boolean isMatchingRepository(SourceRepository sourceRepository) {
		return sourceRepository != null && sourceRepository.matches(this.repository);
	}

	private Set<User> getCoAuthors(Commit commit) {
		GitCommit gitCommit = commit.getGitCommit();
		if (gitCommit == null || !StringUtils.hasText(gitCommit.getMessage())) {
			return Collections.emptySet();
		}
		Set<User> coAuthors = new HashSet<>();
		Matcher matcher = CO_AUTHORED_BY_PATTERN.matcher(gitCommit.getMessage());
		while (matcher.find()) {
			coAuthors.add(new User(matcher.group(1)));
		}
		return coAuthors;
	}

	private Issue getPortedReferenceIssue(Issue issue) {
		Issue next;
		while ((next = this.issueChain.nextIssue(issue)) != null) {
			issue = next;
		}
		return issue;
	}

	private boolean isIncludedContributor(User user) {
		if (user == null || !StringUtils.hasText(user.getName())) {
			return false;
		}
		String name = user.getName();
		return !this.excludeContributors.contains(name) && !name.endsWith("[bot]");
	}

	private void addContributorsContent(StringBuilder content, Set<User> contributors) {
		content.append(String.format("%n## "));
		content.append((this.contributorsTitle != null) ? this.contributorsTitle : ":heart: Contributors");
		content.append(String.format("%n%nThank you to all the contributors who worked on this release:%n%n"));
		content.append(formatContributors(contributors));
	}

	private String formatContributors(Set<User> contributors) {
		List<String> names = contributors.stream().map(User::getName).map((name) -> "@" + name).sorted().toList();
		StringBuilder formatted = new StringBuilder();
		String separator = (names.size() > 2) ? ", " : " ";
		for (int i = 0; i < names.size(); i++) {
			if (i > 0) {
				formatted.append(separator);
				if (i == names.size() - 1) {
					formatted.append("and ");
				}
			}
			formatted.append(names.get(i));
		}
		return formatted.toString();
	}

	private void addExternalLinksContent(StringBuilder content, List<ExternalLink> externalLinks) {
		content.append(String.format("%n## "));
		content.append(String.format("External Links%n%n"));
		externalLinks.stream().map(this::formatExternalLinks).forEach(content::append);
	}

	private String formatExternalLinks(ExternalLink externalLink) {
		return String.format("- [%s](%s)%n", externalLink.getName(), externalLink.getLocation());
	}

	private void writeContentToFile(String content, String path) throws IOException {
		File file = new File(path).getAbsoluteFile();
		File parent = file.getParentFile();
		if (parent != null) {
			parent.mkdirs();
		}
		FileCopyUtils.copy(content, new FileWriter(file));
	}

	private static Escape gitHubUserMentions() {
		return new PatternEscape(Pattern.compile("(^|[^\\w`])(@[\\w-]+)"), "$1`$2`");
	}

	private static Escape htmlTags() {
		return new PatternEscape(Pattern.compile("(^|[^\\w`])(<[\\w\\-/<>]+>)"), "$1`$2`");
	}

	private static Escape markdownStyling() {
		return (input) -> {
			boolean withinBackticks = false;
			char previous = ' ';
			StringBuilder result = new StringBuilder(input.length());
			for (char c : input.toCharArray()) {
				if (!withinBackticks && previous != '\\' && (c == '*' || c == '_' || c == '~')) {
					result.append('\\');
				}
				result.append(c);
				if (c == '`') {
					withinBackticks = !withinBackticks;
				}
				previous = c;
			}
			return result.toString();
		};
	}

	private interface Escape {

		String apply(String input);

	}

	private static final class PatternEscape implements Escape {

		private final Pattern pattern;

		private final String replacement;

		private PatternEscape(Pattern pattern, String replacement) {
			this.pattern = pattern;
			this.replacement = replacement;
		}

		@Override
		public String apply(String input) {
			return this.pattern.matcher(input).replaceAll(this.replacement);
		}

	}

}
