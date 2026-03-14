---
description: 'Commit & push code changes'
---
Analyze your context and check staged and unstaged changes.

**Staging:** Only stage files explicitly specified by the user or when the user provides a `--stage` flag. If no explicit list or flag is given, list the unstaged changes and ask the user for confirmation before staging.

**Committing:** Commit the staged changes with a meaningful, concise commit message that describes the changes made. If changes are big and alter core architectural concepts or APIs, advise the user to update the CHANGELOG file.

**Pushing:** Only push to the remote repository if the user explicitly requests it or provides a `--push` flag. Do not push automatically.