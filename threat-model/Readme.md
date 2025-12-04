# Automated Threat Modelling Results

In this folder you can find the results produced by an automated threat-modelling tool (`rapidinsights`).
These outputs have been reviewed and processed by humans, and should be used to derive clear, actionable security improvements.

Please treat these results as guidance for identifying risks, prioritizing mitigations, and strengthening the overall security of the system when integrating Chicory in a project.

## Generation

# Install repomix

```bash
npm install -g repomix
```

# OR: brew install repomix

# Generate project documentation

```bash
repomix
```

# Interactively analyze with RapidInsights

```bash
rapidinsights tui --model gemini-2.5-pro repomix-output.xml
```
