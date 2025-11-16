#!/usr/bin/env node

const fs = require('fs');
const path = require('path');
const { execSync } = require('child_process');

console.log('📦 Building production release...');
execSync('pnpm run release', { stdio: 'inherit' });

console.log('📁 Copying static assets to docs/...');

// Helper function to copy directory recursively
function copyDir(src, dest) {
  if (!fs.existsSync(dest)) {
    fs.mkdirSync(dest, { recursive: true });
  }
  
  const entries = fs.readdirSync(src, { withFileTypes: true });
  
  for (const entry of entries) {
    const srcPath = path.join(src, entry.name);
    const destPath = path.join(dest, entry.name);
    
    if (entry.isDirectory()) {
      copyDir(srcPath, destPath);
    } else {
      fs.copyFileSync(srcPath, destPath);
    }
  }
}

// Helper function to copy single file
function copyFile(src, dest) {
  const destDir = path.dirname(dest);
  if (!fs.existsSync(destDir)) {
    fs.mkdirSync(destDir, { recursive: true });
  }
  fs.copyFileSync(src, dest);
}

// Copy CSS
copyDir('resources/public/css', 'docs/css');
console.log('  ✓ CSS files copied');

// Copy fonts
if (fs.existsSync('resources/public/fonts')) {
  copyDir('resources/public/fonts', 'docs/fonts');
  console.log('  ✓ Fonts copied');
}

// Copy JavaScript assets (like rounding.js)
if (fs.existsSync('resources/public/js/rounding.js')) {
  copyFile('resources/public/js/rounding.js', 'docs/js/rounding.js');
  console.log('  ✓ JavaScript assets copied');
}

// Copy favicon
if (fs.existsSync('resources/public/favicon.svg')) {
  copyFile('resources/public/favicon.svg', 'docs/favicon.svg');
  console.log('  ✓ Favicon copied');
}

// Create .nojekyll file for GitHub Pages
fs.writeFileSync('docs/.nojekyll', '');
console.log('  ✓ .nojekyll file created');

console.log('\n✅ Build complete! Ready to deploy to GitHub Pages.');
console.log('📝 Next steps:');
console.log('   1. git add docs/');
console.log('   2. git commit -m "Deploy to GitHub Pages"');
console.log('   3. git push origin main');
console.log('   4. Enable GitHub Pages in your repo settings (Pages -> Source -> main branch -> /docs folder)');

