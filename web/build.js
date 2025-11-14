/**
 * Simple build script for TypeScript compilation
 * Run with: node build.js
 */

const { execSync } = require('child_process');
const fs = require('fs');
const path = require('path');

console.log('Building TypeScript...');

try {
    // Check if TypeScript is installed
    try {
        execSync('tsc --version', { stdio: 'ignore' });
    } catch (e) {
        console.log('TypeScript not found. Installing...');
        execSync('npm install typescript --save-dev', { stdio: 'inherit', cwd: __dirname });
    }
    
    // Compile TypeScript
    execSync('tsc', { stdio: 'inherit', cwd: __dirname });
    
    // Copy HTML file if needed
    if (!fs.existsSync(path.join(__dirname, 'dist', 'index.html'))) {
        console.log('Copying index.html...');
        fs.copyFileSync(
            path.join(__dirname, 'index.html'),
            path.join(__dirname, 'dist', 'index.html')
        );
    }
    
    console.log('Build complete!');
    console.log('Output files in: dist/');
    console.log('Serve with: npm run serve');
} catch (error) {
    console.error('Build failed:', error.message);
    process.exit(1);
}

