# GitHub Pages Consolidation Implementation Plan

## Project Overview
Consolidate GitHub Pages content from the `gh-pages` branch to the `main` branch and automate Dokka documentation generation. This will enable easier maintenance and updates by keeping all project source files and documentation in a single branch.

## Current State Analysis

### Existing GitHub Pages Structure (gh-pages branch)
- **Static HTML pages:**
  - `index.html` - Homepage with library overview
  - `getting-started.html` - Installation and setup guide
  - `features.html` - Feature documentation
  - `demo.html` - Demo application showcase
  - `404.html` - Error page
- **Assets:**
  - `css/style.css` - Styling
  - `js/main.js` - JavaScript functionality
  - `images/` - Logo and other images
- **API Documentation:**
  - `api/` - Dokka-generated HTML documentation
  
### Current Dokka Setup
- Configured in `quo-vadis-core/build.gradle.kts`
- Plugin: `alias(libs.plugins.dokka)`
- Output directory: `build/dokka/html`
- Module name: "Quo Vadis Navigation Library"
- Includes source links to GitHub
- Task: `dokkaGenerateHtml` (found in project tasks)

### GitHub Pages Current Publishing
- Source: `gh-pages` branch (separate from main)
- Publishing method: Deploy from branch (traditional approach)
- URL: https://jermeyyy.github.io/quo-vadis/

## Target Architecture

### Branch Structure
- **main branch:**
  - All source code (existing)
  - Documentation source files (new: `/docs` folder)
  - GitHub Actions workflow for automated publishing (new)
  
### Publishing Approach
Use **GitHub Actions workflow** instead of branch-based deployment to:
1. Build Dokka documentation from source
2. Copy static site files
3. Deploy to GitHub Pages
4. Enable automation on every push to main

## Implementation Plan

---

## HIGH-LEVEL STEPS

### 1. Prepare Documentation Source Structure
### 2. Migrate GitHub Pages Content to Main Branch
### 3. Create GitHub Actions Workflow for Automated Publishing
### 4. Configure GitHub Repository Settings
### 5. Test and Verify Deployment
### 6. Update Project Documentation
### 7. Clean Up and Decommission gh-pages Branch

---

## DETAILED IMPLEMENTATION

### Step 1: Prepare Documentation Source Structure

**Objective:** Create a structured folder in the main branch to hold all GitHub Pages content.

**Sub-steps:**

1.1. **Create `/docs` directory structure**
   - Create `/docs/` folder in project root
   - Create subdirectories:
     - `/docs/site/` - For static HTML/CSS/JS files
     - `/docs/site/css/`
     - `/docs/site/js/`
     - `/docs/site/images/`
   
1.2. **Verify directory structure**
   ```
   /docs/
   ├── site/
   │   ├── index.html
   │   ├── getting-started.html
   │   ├── features.html
   │   ├── demo.html
   │   ├── 404.html
   │   ├── css/
   │   │   └── style.css
   │   ├── js/
   │   │   └── main.js
   │   └── images/
   │       └── logo.jpg
   ```

**Prerequisites:** None

**Expected Output:** Empty directory structure ready for content migration

---

### Step 2: Migrate GitHub Pages Content to Main Branch

**Objective:** Copy all existing content from gh-pages branch to the new structure in main.

**Sub-steps:**

2.1. **Extract files from gh-pages branch**
   - Checkout files from gh-pages without switching branches:
     ```bash
     git checkout gh-pages -- index.html getting-started.html features.html demo.html 404.html css/ js/ images/
     ```
   - This stages the files in the current working directory

2.2. **Move files to /docs/site/ directory**
   ```bash
   mkdir -p docs/site
   mv index.html getting-started.html features.html demo.html 404.html docs/site/
   mv css js images docs/site/
   ```

2.3. **Update internal links in HTML files (if needed)**
   - Review HTML files to ensure relative paths still work
   - Update any references to `api/` to point to `./api/` (relative)
   - The API docs will be generated during build and placed in the same output directory

2.4. **Commit migration to main branch**
   ```bash
   git add docs/
   git commit -m "Migrate GitHub Pages content from gh-pages to main branch"
   ```

**Prerequisites:** Step 1 completed

**Expected Output:** All static site content in `/docs/site/` on main branch

---

### Step 3: Create GitHub Actions Workflow for Automated Publishing

**Objective:** Set up CI/CD pipeline to automatically build Dokka docs and deploy to GitHub Pages on every push to main.

**Sub-steps:**

3.1. **Create workflow directory**
   ```bash
   mkdir -p .github/workflows
   ```

3.2. **Create GitHub Pages deployment workflow**
   - Create file: `.github/workflows/deploy-pages.yml`
   - Workflow structure:
     ```yaml
     name: Deploy GitHub Pages
     
     on:
       push:
         branches: [ main ]
       workflow_dispatch:
     
     permissions:
       contents: read
       pages: write
       id-token: write
     
     concurrency:
       group: "pages"
       cancel-in-progress: false
     
     jobs:
       build:
         runs-on: ubuntu-latest
         steps:
           - name: Checkout
             uses: actions/checkout@v4
           
           - name: Setup Java
             uses: actions/setup-java@v4
             with:
               distribution: 'temurin'
               java-version: '17'
           
           - name: Setup Gradle
             uses: gradle/gradle-build-action@v3
           
           - name: Generate Dokka documentation
             run: ./gradlew :quo-vadis-core:dokkaGenerateHtml --no-daemon
           
           - name: Prepare GitHub Pages content
             run: |
               mkdir -p _site
               cp -r docs/site/* _site/
               cp -r quo-vadis-core/build/dokka/html _site/api
           
           - name: Upload artifact
             uses: actions/upload-pages-artifact@v3
             with:
               path: '_site'
       
       deploy:
         needs: build
         runs-on: ubuntu-latest
         environment:
           name: github-pages
           url: ${{ steps.deployment.outputs.page_url }}
         steps:
           - name: Deploy to GitHub Pages
             id: deployment
             uses: actions/deploy-pages@v4
     ```

3.3. **Add .gitignore entry for build artifacts**
   - Add to `.gitignore`:
     ```
     _site/
     ```

3.4. **Commit workflow**
   ```bash
   git add .github/workflows/deploy-pages.yml .gitignore
   git commit -m "Add GitHub Actions workflow for automated GitHub Pages deployment"
   ```

**Prerequisites:** Step 2 completed

**Considerations:**
- Workflow uses official GitHub Actions for Pages deployment
- Dokka generation happens on every main branch push
- Build artifacts are not committed to the repository
- Java 17 matches the project's requirements (verify in `build.gradle.kts`)
- Gradle build caching improves CI performance

**Expected Output:** Functional workflow file ready for GitHub Actions execution

---

### Step 4: Configure GitHub Repository Settings

**Objective:** Update GitHub repository settings to use GitHub Actions for Pages deployment instead of branch-based deployment.

**Sub-steps:**

4.1. **Navigate to repository settings**
   - Go to: `https://github.com/jermeyyy/quo-vadis/settings/pages`
   
4.2. **Change Pages source**
   - Under "Build and deployment" → "Source"
   - Select: **GitHub Actions** (instead of "Deploy from a branch")
   
4.3. **Configure custom domain (if applicable)**
   - If custom domain is configured, verify it remains set
   - Usually found in the same Pages settings section

4.4. **Verify environment**
   - GitHub automatically creates `github-pages` environment
   - Check under: `Settings` → `Environments` → `github-pages`
   - Optionally add deployment protection rules

**Prerequisites:** Step 3 completed, workflow committed to main

**Manual Action Required:** Yes - GitHub UI configuration

**Expected Output:** GitHub Pages configured to deploy via GitHub Actions

---

### Step 5: Test and Verify Deployment

**Objective:** Ensure the new setup works correctly by triggering a deployment and verifying the site.

**Sub-steps:**

5.1. **Push changes to main branch**
   ```bash
   git push origin main
   ```

5.2. **Monitor GitHub Actions workflow**
   - Navigate to: `https://github.com/jermeyyy/quo-vadis/actions`
   - Watch the "Deploy GitHub Pages" workflow execution
   - Check both `build` and `deploy` jobs succeed

5.3. **Verify deployment**
   - Wait for deployment to complete (~2-5 minutes)
   - Visit: `https://jermeyyy.github.io/quo-vadis/`
   - Test all pages:
     - Homepage
     - Getting Started
     - Features
     - Demo
     - API Reference (`/api/index.html`)

5.4. **Check functionality**
   - Verify CSS/JS loads correctly
   - Test navigation between pages
   - Verify images display
   - Test API documentation navigation
   - Test theme switcher (if present)
   - Check responsive design on mobile

5.5. **Compare with previous version**
   - Open old site (if still accessible)
   - Compare content parity
   - Verify no missing resources

5.6. **Debug if issues occur**
   - Check GitHub Actions logs for errors
   - Verify file paths in workflow
   - Ensure Dokka output directory matches workflow expectations
   - Check browser console for 404 errors

**Prerequisites:** Step 4 completed

**Expected Output:** Fully functional GitHub Pages site deployed from main branch

---

### Step 6: Update Project Documentation

**Objective:** Update project documentation to reflect the new structure and workflow.

**Sub-steps:**

6.1. **Update README.md**
   - Add section about GitHub Pages
   - Document the new `/docs/site/` structure
   - Explain how to update the website
   
   Example addition:
   ```markdown
   ## Documentation Website
   
   The project documentation is published at https://jermeyyy.github.io/quo-vadis/
   
   ### Updating the Website
   
   The website is automatically deployed when changes are pushed to the `main` branch.
   
   - Static site content: `/docs/site/`
   - API documentation: Auto-generated from Dokka
   
   To preview locally:
   1. Generate Dokka: `./gradlew :quo-vadis-core:dokkaGenerateHtml`
   2. Dokka output: `quo-vadis-core/build/dokka/html/`
   3. Open `/docs/site/index.html` in browser
   
   ### Website Structure
   - Homepage and guides: `/docs/site/*.html`
   - Styles: `/docs/site/css/`
   - Scripts: `/docs/site/js/`
   - Images: `/docs/site/images/`
   - API Reference: Auto-generated during deployment
   ```

6.2. **Create CONTRIBUTING.md (if not exists)**
   - Add section about documentation updates
   - Explain the deployment workflow
   
   Example:
   ```markdown
   ## Updating Documentation Website
   
   The documentation website is automatically deployed via GitHub Actions.
   
   ### Making Changes
   1. Edit files in `/docs/site/`
   2. For API docs, update KDoc comments in source code
   3. Commit changes to `main` branch
   4. GitHub Actions will automatically rebuild and deploy
   
   ### Local Testing
   - Static site: Open `/docs/site/index.html`
   - API docs: Run `./gradlew :quo-vadis-core:dokkaGenerateHtml`
   ```

6.3. **Update PUBLISHING.md (if relevant)**
   - Add note about documentation publishing
   - Mention that GitHub Pages updates automatically

6.4. **Create docs/SITE_MAINTENANCE.md**
   - Detailed guide for maintaining the website
   - Include:
     - File structure explanation
     - How to add new pages
     - How to update styles
     - Troubleshooting common issues
   
   Example:
   ```markdown
   # Website Maintenance Guide
   
   ## Structure
   - `/docs/site/` - Static website files
   - Deployed URL: https://jermeyyy.github.io/quo-vadis/
   - Deployment: Automatic via GitHub Actions
   
   ## Adding a New Page
   1. Create HTML file in `/docs/site/`
   2. Follow existing page structure (copy `features.html` as template)
   3. Add navigation link to all pages
   4. Commit and push to `main`
   
   ## Updating API Documentation
   - Edit KDoc comments in source code
   - Dokka automatically regenerates on deployment
   - No manual steps required
   
   ## Troubleshooting
   - Check GitHub Actions logs: https://github.com/jermeyyy/quo-vadis/actions
   - Verify file paths are correct
   - Test locally before pushing
   ```

6.5. **Update instructions file**
   - Add to `.github/instructions/copilot.instructions.md`
   - Document the new workflow for AI assistance

6.6. **Commit documentation updates**
   ```bash
   git add README.md CONTRIBUTING.md docs/SITE_MAINTENANCE.md .github/instructions/
   git commit -m "Update documentation to reflect new GitHub Pages workflow"
   git push origin main
   ```

**Prerequisites:** Step 5 completed and verified

**Expected Output:** Comprehensive documentation for maintaining the website

---

### Step 7: Clean Up and Decommission gh-pages Branch

**Objective:** Remove the obsolete gh-pages branch after confirming the new setup works perfectly.

**Sub-steps:**

7.1. **Wait for confirmation period**
   - Keep gh-pages branch for at least 1-2 weeks
   - Monitor new deployment for issues
   - Ensure no broken links or missing content

7.2. **Create backup (optional but recommended)**
   ```bash
   git checkout gh-pages
   git tag gh-pages-archive
   git push origin gh-pages-archive
   ```

7.3. **Delete local gh-pages branch**
   ```bash
   git branch -D gh-pages
   ```

7.4. **Delete remote gh-pages branch**
   ```bash
   git push origin --delete gh-pages
   ```
   
   **WARNING:** This action is irreversible without the backup tag!

7.5. **Verify deletion**
   ```bash
   git branch -a  # Should not show gh-pages
   ```

7.6. **Update any documentation references**
   - Search for mentions of gh-pages branch
   - Update any deployment guides
   - Update contributor documentation

7.7. **Optional: Create announcement**
   - Update repository README
   - Post in discussions (if used)
   - Notify contributors of the change

**Prerequisites:** Step 6 completed, 1-2 weeks of successful deployments

**Considerations:**
- Keep the `gh-pages-archive` tag indefinitely
- The tag allows recovery if needed: `git checkout gh-pages-archive`
- GitHub Pages will continue working from Actions

**Expected Output:** Clean repository with single-branch workflow

---

## AUTOMATION ENHANCEMENTS (Optional Future Improvements)

### A. Automated Dokka Updates on Source Changes

**Goal:** Only regenerate Dokka when Kotlin source files change (performance optimization).

**Implementation:**
```yaml
- name: Check for source changes
  id: changes
  uses: dorny/paths-filter@v2
  with:
    filters: |
      kotlin:
        - 'quo-vadis-core/src/**/*.kt'

- name: Generate Dokka documentation
  if: steps.changes.outputs.kotlin == 'true'
  run: ./gradlew :quo-vadis-core:dokkaGenerateHtml --no-daemon
```

### B. Preview Deployments for Pull Requests

**Goal:** Deploy preview of documentation changes in PRs.

**Implementation:**
- Use `pull_request` trigger
- Deploy to Netlify/Vercel preview environment
- Add comment to PR with preview link

### C. Link Validation

**Goal:** Automatically check for broken links in documentation.

**Implementation:**
```yaml
- name: Check links
  uses: lycheeverse/lychee-action@v1
  with:
    args: --verbose --no-progress '_site/**/*.html'
```

### D. Lighthouse CI for Performance

**Goal:** Monitor site performance metrics.

**Implementation:**
- Add Lighthouse CI action
- Track performance scores
- Fail if scores drop below threshold

---

## ROLLBACK PLAN

### If Deployment Fails

**Scenario 1: Workflow Errors**
- Fix workflow file
- Push correction to main
- Workflow will automatically retry

**Scenario 2: Site Broken After Deployment**
1. Revert last commit:
   ```bash
   git revert HEAD
   git push origin main
   ```
2. Wait for automatic redeployment

**Scenario 3: Critical Issues - Emergency Rollback**
1. Change GitHub Pages source back to `gh-pages` branch (via Settings)
2. Old site immediately restored
3. Fix issues on main branch at leisure
4. Re-enable Actions deployment when ready

---

## SUCCESS CRITERIA

✅ **Technical:**
- [ ] All static files from gh-pages exist in `/docs/site/`
- [ ] GitHub Actions workflow runs successfully
- [ ] Dokka documentation generates correctly
- [ ] Site deploys to https://jermeyyy.github.io/quo-vadis/
- [ ] All pages load without 404 errors
- [ ] CSS and JavaScript function correctly
- [ ] API documentation is current and navigable

✅ **Documentation:**
- [ ] README updated with website information
- [ ] Site maintenance guide created
- [ ] Contributing guidelines include documentation workflow
- [ ] Copilot instructions updated

✅ **Process:**
- [ ] Deployment triggers automatically on main branch push
- [ ] Build completes in under 10 minutes
- [ ] No manual intervention required for updates
- [ ] Old gh-pages branch safely archived

---

## TIMELINE ESTIMATE

| Phase | Estimated Time | Notes |
|-------|---------------|-------|
| Step 1: Directory Setup | 5 minutes | Simple directory creation |
| Step 2: Content Migration | 15-30 minutes | Includes testing file moves |
| Step 3: Workflow Creation | 30-60 minutes | Writing and testing workflow YAML |
| Step 4: GitHub Settings | 5 minutes | UI configuration |
| Step 5: Testing & Verification | 30-60 minutes | Multiple deployment tests |
| Step 6: Documentation Updates | 30-45 minutes | Writing guides |
| Step 7: Cleanup | 10 minutes | After confirmation period |
| **Total Active Work** | **2-4 hours** | Excluding waiting periods |
| **Confirmation Period** | **1-2 weeks** | Before final cleanup |

---

## DEPENDENCIES & REQUIREMENTS

### Technical Requirements
- **Gradle:** Version compatible with Dokka plugin (already configured)
- **Java:** Version 17 (verify in project settings)
- **Dokka Plugin:** Already configured in `quo-vadis-core/build.gradle.kts`
- **Git:** For branch operations
- **GitHub Repository:** Admin access required

### GitHub Actions Requirements
- **Actions enabled:** On repository
- **Permissions:** `pages: write`, `id-token: write`
- **Runners:** Uses `ubuntu-latest`
- **Artifact storage:** For Pages artifact (provided by GitHub)

### Knowledge Requirements
- Git branch management
- GitHub Actions YAML syntax
- Basic Gradle tasks
- GitHub Pages configuration
- HTML/CSS (for troubleshooting)

---

## RISK ASSESSMENT

| Risk | Impact | Likelihood | Mitigation |
|------|--------|------------|------------|
| Broken links after migration | Medium | Low | Test thoroughly in Step 5 |
| Dokka build failure in CI | High | Low | Test locally first, use `--no-daemon` |
| GitHub Pages deployment delay | Low | Medium | Monitor Actions, retry if needed |
| Missing assets (images, CSS) | Medium | Low | Verify all files copied, check paths |
| Lost content during migration | High | Very Low | Keep gh-pages branch until verified |
| Workflow permission issues | Medium | Low | Follow GitHub Actions docs exactly |
| Build time too long | Low | Medium | Consider caching, selective builds |

---

## POST-IMPLEMENTATION CHECKLIST

- [ ] Site accessible at https://jermeyyy.github.io/quo-vadis/
- [ ] All navigation links work
- [ ] API documentation current and complete
- [ ] CSS/JS loaded correctly
- [ ] Images display properly
- [ ] Mobile responsive design works
- [ ] Theme switcher functional (if present)
- [ ] GitHub Actions workflow passes
- [ ] Documentation updated in README
- [ ] Site maintenance guide created
- [ ] Team/contributors notified
- [ ] gh-pages branch archived
- [ ] Monitor for 1-2 weeks before final cleanup

---

## REFERENCE LINKS

### GitHub Documentation
- [GitHub Pages Setup](https://docs.github.com/en/pages/getting-started-with-github-pages/configuring-a-publishing-source-for-your-github-pages-site)
- [GitHub Actions for Pages](https://docs.github.com/en/pages/getting-started-with-github-pages/using-custom-workflows-with-github-pages)
- [Workflow Syntax](https://docs.github.com/en/actions/using-workflows/workflow-syntax-for-github-actions)

### Dokka Documentation
- [Dokka Plugin](https://kotlinlang.org/docs/dokka-introduction.html)
- [Dokka HTML Format](https://kotlinlang.org/docs/dokka-html.html)

### Project Files
- Current Dokka config: `quo-vadis-core/build.gradle.kts` (lines 215-240)
- Build script: `build.gradle.kts`
- Current site: https://jermeyyy.github.io/quo-vadis/

---

## NOTES

### Why GitHub Actions Instead of Branch-Based Deployment?
1. **Automation:** Dokka regenerates automatically on source changes
2. **Single Source of Truth:** Everything lives on main branch
3. **Simplified Workflow:** No manual branch switching
4. **Better Control:** Can add testing, validation, optimizations
5. **Modern Approach:** Recommended by GitHub

### Alternative Considered: /docs Folder on Main Branch
- GitHub supports publishing from `/docs` folder
- **Rejected because:** Would commit build artifacts (Dokka output)
- GitHub Actions avoids committing generated files

### Dokka Output Directory
- Current: `quo-vadis-core/build/dokka/html`
- This is in `.gitignore` (build artifacts)
- Generated fresh on each deployment
- Ensures API docs always match code

---

## APPENDIX

### A. Example Workflow Trigger Variations

**Deploy on Release:**
```yaml
on:
  release:
    types: [published]
```

**Deploy on Tag:**
```yaml
on:
  push:
    tags:
      - 'v*'
```

**Manual Trigger Only:**
```yaml
on:
  workflow_dispatch:
```

### B. Gradle Command Alternatives

**Generate Dokka (current approach):**
```bash
./gradlew :quo-vadis-core:dokkaGenerateHtml
```

**Clean and generate:**
```bash
./gradlew :quo-vadis-core:clean :quo-vadis-core:dokkaGenerateHtml
```

**Parallel execution:**
```bash
./gradlew :quo-vadis-core:dokkaGenerateHtml --parallel
```

### C. Troubleshooting Common Issues

**Issue: Dokka fails with OOM**
- Solution: Add to workflow:
  ```yaml
  env:
    GRADLE_OPTS: -Xmx4g
  ```

**Issue: Workflow permission denied**
- Solution: Check repository Settings → Actions → General → Workflow permissions

**Issue: 404 on deployment**
- Solution: Verify `_site` folder structure matches expected paths

**Issue: CSS not loading**
- Solution: Check paths are relative, not absolute

---

## CONCLUSION

This plan provides a comprehensive approach to consolidating the GitHub Pages infrastructure into the main branch with automated Dokka generation. The implementation is structured in clear, sequential steps with detailed sub-tasks, prerequisites, and expected outputs.

**Key Benefits:**
- ✅ Single-branch workflow (main only)
- ✅ Automated documentation updates
- ✅ No manual deployment steps
- ✅ Version-controlled static site content
- ✅ Fresh API docs on every deploy
- ✅ Easier maintenance and updates

**Implementation Time:** 2-4 hours of active work + 1-2 weeks confirmation period

**Risk Level:** Low (with proper testing and backup strategy)

**Next Steps:** Begin with Step 1 - Directory Structure Preparation
