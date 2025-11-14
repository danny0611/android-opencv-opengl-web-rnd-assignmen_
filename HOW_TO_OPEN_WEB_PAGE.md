# How to Open the Web Page

## 📍 Web Page Location

The web page is located at:
```
web/index.html
```

Full path:
```
C:\collage\PLacement\Android + OpenCV-C++ + OpenGL Assessment + Web - RnD Intern\web\index.html
```

## 🌐 Ways to Open the Web Page

### Method 1: Direct File Open (Quick Test)
1. Navigate to the `web` folder in File Explorer
2. Double-click `index.html`
3. It will open in your default browser

**Note**: This method works, but the JavaScript might have CORS issues. Use Method 2 for best results.

### Method 2: Using HTTP Server (Recommended)

#### Option A: Using npm (if you have Node.js)
```bash
cd web
npm run serve
```
Then open: `http://localhost:8080`

#### Option B: Using Python
```bash
cd web
python -m http.server 8080
```
Then open: `http://localhost:8080`

#### Option C: Using npx http-server
```bash
cd web
npx http-server . -p 8080
```
Then open: `http://localhost:8080`

### Method 3: Using VS Code Live Server
1. Install "Live Server" extension in VS Code
2. Right-click on `web/index.html`
3. Select "Open with Live Server"

## 📁 Web Directory Structure

```
web/
├── index.html          ← Main HTML file (THE WEB PAGE)
├── main.ts            ← TypeScript source
├── dist/
│   └── main.js        ← Compiled JavaScript (already built)
├── package.json
└── tsconfig.json
```

## ✅ What You'll See

When you open the web page, you'll see:

1. **Header**: "📷 OpenCV GL Camera - Real-time Processed Frame Viewer"
2. **Canvas**: Displaying a processed frame (simulated pattern)
3. **Overlay**: 
   - FPS: 30 (updating)
   - Resolution: 1920x1080
   - Status: Connected (green indicator)
4. **Control Buttons**:
   - Load Sample Frame
   - Toggle Processing
   - Reset
5. **Information Panel**: Frame count, last update, processing mode

## 🚀 Quick Start

**Easiest way to open it right now:**

1. Open File Explorer
2. Navigate to: `C:\collage\PLacement\Android + OpenCV-C++ + OpenGL Assessment + Web - RnD Intern\web`
3. Double-click `index.html`

Or use PowerShell:
```powershell
cd "C:\collage\PLacement\Android + OpenCV-C++ + OpenGL Assessment + Web - RnD Intern\web"
Start-Process index.html
```

## 🔧 Troubleshooting

**If the page doesn't load properly:**
- Make sure `dist/main.js` exists (it should - we already compiled it)
- Use an HTTP server instead of opening directly
- Check browser console for errors (F12)

**If buttons don't work:**
- Check browser console (F12) for JavaScript errors
- Make sure `dist/main.js` is accessible

---

**The web page is ready to use!** Just open `web/index.html` in your browser.

