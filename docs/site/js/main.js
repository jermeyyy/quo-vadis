// Quo Vadis GitHub Pages - Interactive Features

document.addEventListener('DOMContentLoaded', function() {
    // Initialize Highlight.js for syntax highlighting
    if (typeof hljs !== 'undefined') {
        hljs.highlightAll();
    }
    
    // Theme Switcher
    initThemeSwitcher();
    
    // Smooth scrolling for anchor links
    document.querySelectorAll('a[href^="#"]').forEach(anchor => {
        anchor.addEventListener('click', function (e) {
            e.preventDefault();
            const target = document.querySelector(this.getAttribute('href'));
            if (target) {
                target.scrollIntoView({
                    behavior: 'smooth',
                    block: 'start'
                });
            }
        });
    });

    // Add active class to current nav item based on current page
    const currentPage = window.location.pathname.split('/').pop() || 'index.html';
    document.querySelectorAll('nav a').forEach(link => {
        if (link.getAttribute('href') === currentPage) {
            link.classList.add('active');
        }
    });

    // Code copy functionality
    document.querySelectorAll('pre code').forEach(block => {
        const button = document.createElement('button');
        button.className = 'copy-button';
        button.textContent = 'Copy';
        button.style.cssText = `
            position: absolute;
            top: 8px;
            right: 8px;
            padding: 4px 12px;
            background: #2196F3;
            color: white;
            border: none;
            border-radius: 4px;
            cursor: pointer;
            font-size: 0.8rem;
            opacity: 0;
            transition: opacity 0.3s;
        `;
        
        const wrapper = document.createElement('div');
        wrapper.style.position = 'relative';
        block.parentNode.insertBefore(wrapper, block);
        wrapper.appendChild(block);
        wrapper.appendChild(button);
        
        wrapper.addEventListener('mouseenter', () => {
            button.style.opacity = '1';
        });
        
        wrapper.addEventListener('mouseleave', () => {
            button.style.opacity = '0';
        });
        
        button.addEventListener('click', async () => {
            const code = block.textContent;
            try {
                await navigator.clipboard.writeText(code);
                button.textContent = 'Copied!';
                setTimeout(() => {
                    button.textContent = 'Copy';
                }, 2000);
            } catch (err) {
                console.error('Failed to copy:', err);
            }
        });
    });

    // Screenshot modal functionality
    document.querySelectorAll('.screenshot-card img').forEach(img => {
        img.style.cursor = 'pointer';
        img.addEventListener('click', function() {
            const modal = document.createElement('div');
            modal.style.cssText = `
                position: fixed;
                top: 0;
                left: 0;
                width: 100%;
                height: 100%;
                background: rgba(0, 0, 0, 0.9);
                display: flex;
                align-items: center;
                justify-content: center;
                z-index: 9999;
                cursor: pointer;
            `;
            
            const modalImg = document.createElement('img');
            modalImg.src = this.src;
            modalImg.style.cssText = `
                max-width: 90%;
                max-height: 90%;
                object-fit: contain;
            `;
            
            modal.appendChild(modalImg);
            document.body.appendChild(modal);
            
            modal.addEventListener('click', () => {
                document.body.removeChild(modal);
            });
        });
    });

    // Add scroll-to-top button
    const scrollButton = document.createElement('button');
    scrollButton.textContent = '↑';
    scrollButton.className = 'scroll-to-top';
    scrollButton.style.cssText = `
        position: fixed;
        bottom: 2rem;
        right: 2rem;
        width: 50px;
        height: 50px;
        border-radius: 50%;
        background: #2196F3;
        color: white;
        border: none;
        font-size: 1.5rem;
        cursor: pointer;
        opacity: 0;
        transition: opacity 0.3s;
        box-shadow: 0 4px 8px rgba(0,0,0,0.2);
        z-index: 1000;
    `;
    
    document.body.appendChild(scrollButton);
    
    window.addEventListener('scroll', () => {
        if (window.pageYOffset > 300) {
            scrollButton.style.opacity = '1';
        } else {
            scrollButton.style.opacity = '0';
        }
    });
    
    scrollButton.addEventListener('click', () => {
        window.scrollTo({
            top: 0,
            behavior: 'smooth'
        });
    });
});

// Theme Switcher Functionality
function initThemeSwitcher() {
    // Default to dark mode
    const savedTheme = localStorage.getItem('theme') || 'dark';
    document.documentElement.setAttribute('data-theme', savedTheme);
    updateHighlightJsTheme(savedTheme);
    
    // Create theme switcher button if it doesn't exist
    const existingSwitcher = document.querySelector('.theme-switcher');
    if (!existingSwitcher) {
        const nav = document.querySelector('nav ul');
        if (nav) {
            const switcherLi = document.createElement('li');
            const switcher = document.createElement('button');
            switcher.className = 'theme-switcher';
            switcher.setAttribute('aria-label', 'Toggle theme');
            updateThemeSwitcherText(switcher, savedTheme);
            
            switcher.addEventListener('click', toggleTheme);
            switcherLi.appendChild(switcher);
            nav.appendChild(switcherLi);
        }
    } else {
        updateThemeSwitcherText(existingSwitcher, savedTheme);
        existingSwitcher.addEventListener('click', toggleTheme);
    }
}

function updateHighlightJsTheme(theme) {
    // Remove existing highlight.js theme links
    document.querySelectorAll('link[href*="highlight.js"]').forEach(link => link.remove());
    
    // Add the appropriate theme
    const link = document.createElement('link');
    link.rel = 'stylesheet';
    
    if (theme === 'dark') {
        link.href = 'https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.9.0/styles/atom-one-dark.min.css';
    } else {
        link.href = 'https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.9.0/styles/atom-one-light.min.css';
    }
    
    document.head.appendChild(link);
}

function toggleTheme() {
    const currentTheme = document.documentElement.getAttribute('data-theme');
    const newTheme = currentTheme === 'dark' ? 'light' : 'dark';
    
    document.documentElement.setAttribute('data-theme', newTheme);
    localStorage.setItem('theme', newTheme);
    updateHighlightJsTheme(newTheme);
    
    const switcher = document.querySelector('.theme-switcher');
    if (switcher) {
        updateThemeSwitcherText(switcher, newTheme);
    }
}

function updateThemeSwitcherText(button, theme) {
    button.textContent = theme === 'dark' ? '☀ Light' : '🌙 Dark';
}
