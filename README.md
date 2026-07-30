# 10-Aether-Skills

A curated repository of AI agent skills for the Aether ecosystem. This repository provides a standardized way to define, share, and reuse skills across AI agents.

## Overview

AI agent skills are reusable, composable units of capability that an AI agent can invoke to accomplish specific tasks. Each skill is defined as a structured YAML file that describes:

- **What the skill does** – clear description and use cases
- **What inputs it accepts** – typed parameters with descriptions
- **What it returns** – output format and type
- **How it works** – implementation details and examples

## Repository Structure

```
10-Aether-Skills/
├── README.md              # This file
├── CONTRIBUTING.md        # Contribution guidelines
├── schemas/               # JSON schemas for validating skill definitions
│   └── skill.schema.json
├── templates/             # Templates for creating new skills
│   └── skill_template.yaml
└── skills/                # All skill definitions, organized by category
    ├── code/              # Code generation, review, and analysis
    ├── communication/     # Drafting emails, messages, reports
    ├── data/              # Data retrieval, transformation, analysis
    └── search/            # Web, document, and database search
```

## Getting Started

### Using a Skill

Each skill file is a self-contained YAML document. To use a skill in your AI agent:

1. Browse the `skills/` directory and find the skill you need.
2. Load the YAML file to read its `parameters` and `output` specification.
3. Invoke the skill with the required inputs as described.

### Adding a New Skill

1. Copy `templates/skill_template.yaml` to the appropriate category folder under `skills/`.
2. Fill in every field according to the [Skill Schema](schemas/skill.schema.json).
3. Validate your file against the schema.
4. Submit a pull request following the guidelines in [CONTRIBUTING.md](CONTRIBUTING.md).

## Skill Schema

Every skill file must conform to the [skill schema](schemas/skill.schema.json). The top-level fields are:

| Field | Type | Required | Description |
|---|---|---|---|
| `name` | string | ✅ | Unique snake_case identifier for the skill |
| `version` | string | ✅ | Semantic version (e.g. `1.0.0`) |
| `description` | string | ✅ | Short, clear description of what the skill does |
| `category` | string | ✅ | One of: `search`, `code`, `data`, `communication` |
| `author` | string | ✅ | Author name or team |
| `tags` | array | ✅ | Keywords to aid discovery |
| `parameters` | array | ✅ | List of input parameter definitions |
| `output` | object | ✅ | Output type and description |
| `examples` | array | ✅ | At least one example with input and expected output |

## Available Skills

### 🔍 Search

| Skill | Description |
|---|---|
| [web_search](skills/search/web_search.yaml) | Search the web and return relevant results |

### 💻 Code

| Skill | Description |
|---|---|
| [code_review](skills/code/code_review.yaml) | Review code for quality, bugs, and best practices |

### 📊 Data

| Skill | Description |
|---|---|
| [data_analysis](skills/data/data_analysis.yaml) | Analyze structured data and generate insights |

### 📧 Communication

| Skill | Description |
|---|---|
| [email_draft](skills/communication/email_draft.yaml) | Draft a professional email based on context |

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for detailed guidelines on how to contribute new skills or improve existing ones.

## License

This repository is open source. See [LICENSE](LICENSE) for details.
