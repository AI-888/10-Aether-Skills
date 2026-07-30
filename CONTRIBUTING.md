# Contributing to 10-Aether-Skills

Thank you for your interest in contributing to the Aether Skills repository! This document explains how to add new skills, improve existing ones, and get your changes merged.

## Table of Contents

- [Code of Conduct](#code-of-conduct)
- [How to Contribute](#how-to-contribute)
- [Skill Guidelines](#skill-guidelines)
- [Validation](#validation)
- [Pull Request Process](#pull-request-process)

## Code of Conduct

Be respectful and constructive. All contributions are welcome regardless of experience level.

## How to Contribute

### Adding a New Skill

1. **Choose a category** – Pick the most appropriate folder under `skills/`:
   - `search/` – information retrieval from web, databases, or documents
   - `code/` – code generation, review, debugging, or analysis
   - `data/` – data transformation, aggregation, or analysis
   - `communication/` – writing emails, reports, summaries, or messages

2. **Copy the template**:
   ```bash
   cp templates/skill_template.yaml skills/<category>/<skill_name>.yaml
   ```

3. **Fill in all fields** – Every required field in the schema must be present. Run validation (see below) to check your file.

4. **Write at least two examples** – Examples make skills easy to understand and test. Cover the common case and at least one edge case or variation.

5. **Open a pull request** – Follow the [Pull Request Process](#pull-request-process) below.

### Improving an Existing Skill

- Fix incorrect descriptions, examples, or parameter definitions.
- Increment the `version` field following [Semantic Versioning](https://semver.org):
  - **Patch** (`x.y.Z`): typo fixes, clarifications
  - **Minor** (`x.Y.0`): new optional parameters, new examples
  - **Major** (`X.0.0`): breaking changes to required parameters or output schema

## Skill Guidelines

### Naming

- Use `snake_case` for the `name` field and the filename.
- Names should be short, descriptive, and unique across the repository.

### Descriptions

- Start the description with a verb: *"Search …"*, *"Generate …"*, *"Analyze …"*.
- Be specific about what the skill does *and* when to use it.
- Keep the description under 200 words.

### Parameters

- Always mark parameters as `required: true` or `required: false`.
- Provide `default` values for all optional parameters.
- Use `enum` to restrict values to a known set whenever applicable.

### Output

- Describe the output thoroughly. For complex `object` or `array` types, include an inline `schema`.

### Examples

- Each example must have a `description`, `input`, and `output`.
- `input` values must satisfy all required parameters.
- `output` must be realistic and consistent with the skill's description.

## Validation

Before submitting, validate your skill file against the JSON schema:

```bash
# Install a YAML/JSON schema validator (e.g. ajv-cli)
npm install -g ajv-cli js-yaml

# Convert YAML to JSON and validate
js-yaml skills/<category>/<skill_name>.yaml | ajv validate -s schemas/skill.schema.json -d -
```

A valid file produces no errors. Fix any reported issues before opening a PR.

## Pull Request Process

1. **One skill per PR** – Keep changes focused to simplify review.
2. **Branch naming** – Use `skill/<skill-name>` (e.g. `skill/web_search`).
3. **PR title** – Use the format `Add skill: <skill_name>` or `Update skill: <skill_name> vX.Y.Z`.
4. **Description** – Briefly explain what the skill does and why it is useful.
5. **Checklist** – Your PR description should confirm:
   - [ ] File is placed in the correct `skills/<category>/` folder
   - [ ] All required schema fields are present
   - [ ] At least two examples are included
   - [ ] `version` follows semantic versioning
   - [ ] Validation passes with no errors

Once approved, a maintainer will merge your PR. Thank you for contributing!
