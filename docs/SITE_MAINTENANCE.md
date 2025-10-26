# Website Maintenance Guide

## Structure
- `/docs/site/` - Static website files
- Deployed URL: https://jermeyyy.github.io/quo-vadis/
- Deployment: Automatic via GitHub Actions

## Website Structure

```
/docs/site/
├── index.html              # Homepage with library overview
├── getting-started.html    # Installation and setup guide
├── features.html           # Feature documentation
├── demo.html               # Demo application showcase
├── 404.html                # Error page
├── css/
│   └── style.css           # Main stylesheet
├── js/
│   └── main.js             # JavaScript functionality
└── images/
    └── logo.jpg            # Logo and other images
```

The `api/` directory is auto-generated during deployment and contains the Dokka API documentation.

## Deployment Workflow

The website is automatically deployed via GitHub Actions when changes are pushed to the `main` branch.

### Workflow Steps:
1. GitHub Actions triggers on push to `main`
2. Dokka documentation is generated from source code
3. Static site files are copied from `/docs/site/`
4. Dokka output is copied to `/api/` directory
5. Combined content is deployed to GitHub Pages

## Adding a New Page

1. Create HTML file in `/docs/site/`
   ```bash
   cp docs/site/features.html docs/site/new-page.html
   ```

2. Follow existing page structure:
   - Use the same header/navigation
   - Maintain consistent styling
   - Include theme switcher button
   - Add proper meta tags

3. Add navigation link to all existing pages:
   - Update the `<nav>` section in header
   - Add link to new page in all HTML files

4. Commit and push to trigger deployment:
   ```bash
   git add docs/site/new-page.html
   git commit -m "Add new documentation page"
   git push origin main
   ```

## Updating Existing Pages

1. Edit HTML files in `/docs/site/`
2. Test locally by opening in browser:
   ```bash
   open docs/site/index.html
   ```
3. Commit and push changes to trigger automatic deployment

## Updating Styles

1. Edit `/docs/site/css/style.css`
2. Test changes locally
3. Commit and push to deploy

CSS includes:
- Light and dark theme support
- Responsive design for mobile/tablet/desktop
- Syntax highlighting for code blocks
- Navigation and hero section styling

## Updating JavaScript

1. Edit `/docs/site/js/main.js`
2. Test functionality locally
3. Commit and push to deploy

Current JavaScript features:
- Theme switcher (light/dark mode)
- Syntax highlighting initialization
- Dynamic theme loading for highlight.js

## Updating API Documentation

API documentation is automatically generated from KDoc comments in the source code.

### To update:
1. Edit KDoc comments in Kotlin source files:
   ```kotlin
   /**
    * Brief description.
    * 
    * Detailed explanation.
    * 
    * @param paramName Parameter description
    * @return Return value description
    */
   ```

2. Dokka automatically regenerates on deployment
3. No manual steps required

### To preview locally:
```bash
./gradlew :quo-vadis-core:dokkaGenerateHtml
open quo-vadis-core/build/dokka/html/index.html
```

## Testing Locally

### Static Site
Simply open the HTML files in a browser:
```bash
open docs/site/index.html
```

### Full Site with API Documentation
1. Generate Dokka:
   ```bash
   ./gradlew :quo-vadis-core:dokkaGenerateHtml
   ```

2. Create temporary site structure:
   ```bash
   mkdir -p _site_preview
   cp -r docs/site/* _site_preview/
   cp -r quo-vadis-core/build/dokka/html _site_preview/api
   ```

3. Open in browser:
   ```bash
   open _site_preview/index.html
   ```

4. Clean up:
   ```bash
   rm -rf _site_preview
   ```

## Troubleshooting

### Site Not Updating After Push
1. Check GitHub Actions workflow status:
   - Go to: https://github.com/jermeyyy/quo-vadis/actions
   - Look for "Deploy GitHub Pages" workflow
   - Check for errors in build or deploy jobs

2. Common issues:
   - **Build failures**: Check Gradle/Dokka errors in logs
   - **Permission issues**: Verify Pages permissions in repository settings
   - **File path errors**: Ensure paths in workflow match actual structure

### Broken Links
1. Verify file paths are relative, not absolute
2. Check that all referenced files exist
3. Test navigation locally before pushing

### CSS/JS Not Loading
1. Verify paths in HTML are correct
2. Check browser console for 404 errors
3. Ensure files are in correct directories

### Images Not Displaying
1. Check image paths in HTML
2. Verify images exist in `/docs/site/images/`
3. Test locally first

### API Documentation Missing
1. Verify Dokka task runs successfully in GitHub Actions
2. Check that output directory matches workflow expectations:
   - Expected: `quo-vadis-core/build/dokka/html`
3. Review Dokka configuration in `quo-vadis-core/build.gradle.kts`

### Theme Switcher Not Working
1. Check JavaScript console for errors
2. Verify `js/main.js` is loaded correctly
3. Test theme switching in multiple browsers

## Monitoring Deployments

### GitHub Actions Dashboard
- URL: https://github.com/jermeyyy/quo-vadis/actions
- Shows all workflow runs with status
- Click on individual runs to see detailed logs

### Deployment Environments
- GitHub automatically creates `github-pages` environment
- View: Settings → Environments → github-pages
- Shows deployment history and status

### Build Times
- Expected: 2-5 minutes per deployment
- Includes: Dokka generation + deployment
- Gradle caching improves subsequent builds

## Best Practices

### Before Making Changes
1. Test locally first
2. Verify links work
3. Check responsive design on different screen sizes
4. Test both light and dark themes

### Commit Messages
Use clear, descriptive messages:
- `docs: update getting started guide`
- `style: improve mobile responsiveness`
- `fix: correct broken API documentation link`

### Code Quality
- Maintain consistent HTML structure across pages
- Keep CSS organized and commented
- Use semantic HTML elements
- Ensure accessibility (alt tags, ARIA labels)

### Performance
- Optimize images before adding
- Minimize CSS/JS when possible
- Use CDN for external libraries (already configured)

## Emergency Rollback

If the site breaks after deployment:

1. **Quick revert:**
   ```bash
   git revert HEAD
   git push origin main
   ```

2. **Restore from gh-pages (if still exists):**
   - Change Pages source to `gh-pages` branch in repository settings
   - Old site immediately restored
   - Fix issues at leisure, then re-enable Actions

## Additional Resources

- [GitHub Pages Documentation](https://docs.github.com/en/pages)
- [GitHub Actions Workflow Syntax](https://docs.github.com/en/actions/using-workflows/workflow-syntax-for-github-actions)
- [Dokka Documentation](https://kotlinlang.org/docs/dokka-introduction.html)
- [Project README](../README.md)

## Contact

For questions or issues with the website, please:
- Open an issue: https://github.com/jermeyyy/quo-vadis/issues
- Check existing documentation
- Review GitHub Actions logs for deployment issues
