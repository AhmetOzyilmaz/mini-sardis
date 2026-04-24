# Markdown Documentation Standards

## Key Rules

### Line Length

- *Maximum*: 120 characters per line
- *Break Strategy*: Split at natural word boundaries
- *Exception*: URLs and code blocks

### Headings

- *No Trailing Punctuation*: Use ## Overview not ## Overview.
- *Spacing*: Empty line before and after headings
- *Hierarchy*: Use proper heading levels (h1 -> h2 -> h3)

### Code Blocks

- *Language Required*: Always specify language after triple backticks
- *Supported Languages*: java, bash, yaml, json, text, mermaid, sql
- *Spacing*: Empty line before and after code blocks

### Code Spans

- *No Spaces*: Use code not ` code `
- *Consistency*: Use backticks for all inline code references

### Lists

- *Spacing*: Empty line before list start
- *Nesting*: Use proper indentation for nested lists
- *Mixed Lists*: Don't mix ordered and unordered at same level

## Examples

### Good Heading

markdown
## Overview

This section describes...


### Bad Heading

markdown
## Overview.
This section describes...


### Good Code Block

markdown
Example command:

bash
./gradlew clean build


The command above...


### Bad Code Block

markdown
Example command:

./gradlew clean build

The command above...


## Validation

### Pre-Commit Check

bash
markdownlint '**/*.md'


### Configuration

Linting rules are defined in .github/linters/.markdown-lint.yml

### CI/CD Enforcement

- Markdown linting runs in CI pipeline
- Pull requests must pass markdown validation
- Failed linting blocks merge

## Common Fixes

### Fix Line Length

markdown
<!-- Bad: Line too long -->
This is a very long line that exceeds the 120 character limit and should be broken up into multiple lines for better readability and compliance.

<!-- Good: Lines under 120 chars -->
This is a very long line that exceeds the 120 character limit and should be broken
up into multiple lines for better readability and compliance.


### Fix Heading Punctuation

markdown
<!-- Bad -->
## Installation Guide.

<!-- Good -->
## Installation Guide


### Fix Code Block Language

markdown
<!-- Bad: No language specified -->

./gradlew test


<!-- Good: Language specified -->
bash
./gradlew test
```
```

## Documentation Types

### README Files

- Project overview
- Setup instructions
- Quick start guide
- Link to detailed docs

### CLAUDE.md Files

- AI-specific instructions
- Development patterns
- Tool-specific guidance

### docs/ Folder

- Detailed architecture docs
- Feature specifications
- Design decisions
- API documentation