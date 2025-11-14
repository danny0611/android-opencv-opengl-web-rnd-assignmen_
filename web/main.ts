/**
 * OpenCV GL Camera - Web Viewer
 * TypeScript module for displaying processed camera frames
 */

interface FrameData {
    data: string; // Base64 encoded image data
    width: number;
    height: number;
    timestamp: number;
}

class CameraViewer {
    private canvas: HTMLCanvasElement;
    private ctx: CanvasRenderingContext2D;
    private fpsElement: HTMLElement;
    private resolutionElement: HTMLElement;
    private statusElement: HTMLElement;
    private statusIndicator: HTMLElement;
    private statusDot: HTMLElement;
    private frameCountElement: HTMLElement;
    private lastUpdateElement: HTMLElement;
    private processModeElement: HTMLElement;
    private modeChip: HTMLElement;
    private frameSurface: HTMLElement;
    private sparklineCanvas: HTMLCanvasElement;
    private sparklineCtx: CanvasRenderingContext2D;
    private latencyElement: HTMLElement;
    private diagnosticsHeader: HTMLElement;
    private diagnosticsBody: HTMLElement;
    private avgFpsElement: HTMLElement;
    private maxFpsElement: HTMLElement;
    private minFpsElement: HTMLElement;
    private avgLatencyElement: HTMLElement;
    private maxLatencyElement: HTMLElement;
    private modeDiagnosticElement: HTMLElement;
    private cameraStateElement: HTMLElement;
    private totalFramesElement: HTMLElement;
    
    private frameCount: number = 0;
    private lastFpsTime: number = 0;
    private currentFps: number = 0;
    private frameUpdateInterval: number = 0;
    private isProcessingEnabled: boolean = true;
    private processMode: 'raw' | 'grayscale' | 'canny' = 'grayscale';
    
    // Live camera properties
    private videoElement: HTMLVideoElement | null = null;
    private stream: MediaStream | null = null;
    private isCameraActive: boolean = false;
    private animationFrameId: number | null = null;
    private tempCanvas: HTMLCanvasElement;
    private tempCtx: CanvasRenderingContext2D;
    private isFrontCamera: boolean = true;
    
    // Metrics
    private fpsHistory: number[] = [];
    private readonly maxFpsHistory = 30;
    private processingHistory: number[] = [];
    private readonly maxProcessingHistory = 50;
    private lastProcessingTimeMs = 0;
    private modeTransitionTimeout: number | null = null;
    private frameTransitionTimeout: number | null = null;
    private diagnosticsExpanded = false;
    
    constructor() {
        // Initialize canvas
        this.canvas = document.getElementById('frameCanvas') as HTMLCanvasElement;
        if (!this.canvas) {
            throw new Error('Canvas element not found');
        }
        
        const context = this.canvas.getContext('2d');
        if (!context) {
            throw new Error('Could not get 2D context');
        }
        this.ctx = context;
        
        // Create temporary canvas for processing
        this.tempCanvas = document.createElement('canvas');
        this.tempCtx = this.tempCanvas.getContext('2d')!;
        
        // Initialize UI elements
        this.fpsElement = document.getElementById('fpsValue')!;
        this.resolutionElement = document.getElementById('resolutionValue')!;
        this.statusElement = document.getElementById('statusValue')!;
        this.statusIndicator = document.getElementById('statusIndicator')!;
        this.statusDot = this.statusIndicator.querySelector('span') as HTMLElement;
        if (!this.statusDot) {
            throw new Error('Status indicator dot not found');
        }
        this.frameCountElement = document.getElementById('frameCount')!;
        this.lastUpdateElement = document.getElementById('lastUpdate')!;
        this.processModeElement = document.getElementById('processMode')!;
        this.modeChip = document.getElementById('modeChip')!;
        this.frameSurface = document.getElementById('frameSurface')!;
        this.sparklineCanvas = document.getElementById('fpsSparkline') as HTMLCanvasElement;
        if (!this.sparklineCanvas) {
            throw new Error('FPS sparkline canvas not found');
        }
        const sparkCtx = this.sparklineCanvas.getContext('2d');
        if (!sparkCtx) {
            throw new Error('Could not get 2D context for sparkline');
        }
        this.sparklineCtx = sparkCtx;
        this.latencyElement = document.getElementById('latencyValue')!;
        this.diagnosticsHeader = document.getElementById('diagnosticsHeader')!;
        this.diagnosticsBody = document.getElementById('diagnosticsBody')!;
        this.avgFpsElement = document.getElementById('avgFpsValue')!;
        this.maxFpsElement = document.getElementById('maxFpsValue')!;
        this.minFpsElement = document.getElementById('minFpsValue')!;
        this.avgLatencyElement = document.getElementById('avgLatencyValue')!;
        this.maxLatencyElement = document.getElementById('maxLatencyValue')!;
        this.modeDiagnosticElement = document.getElementById('diagModeValue')!;
        this.cameraStateElement = document.getElementById('cameraStateValue')!;
        this.totalFramesElement = document.getElementById('totalFramesValue')!;
        
        // Setup event listeners
        this.setupEventListeners();
        document.addEventListener('keydown', (event) => this.handleKeydown(event));
        window.addEventListener('resize', () => this.resizeSparkline());
        this.diagnosticsHeader.addEventListener('click', () => this.toggleDiagnostics());
        
        // Initialize with sample frame
        this.loadSampleFrame();
        this.updateModeChip();
        
        // Prepare visualizations
        this.resizeSparkline();
        this.clearSparkline();
        
        // Start FPS counter
        this.startFpsCounter();
    }
    
    /**
     * Setup event listeners for controls
     */
    private setupEventListeners(): void {
        const startCameraBtn = document.getElementById('startCameraBtn');
        const stopCameraBtn = document.getElementById('stopCameraBtn');
        const loadFrameBtn = document.getElementById('loadFrameBtn');
        const toggleProcessBtn = document.getElementById('toggleProcessBtn');
        const resetBtn = document.getElementById('resetBtn');
        
        if (startCameraBtn) {
            startCameraBtn.addEventListener('click', () => this.startLiveCamera());
        }
        
        if (stopCameraBtn) {
            stopCameraBtn.addEventListener('click', () => this.stopLiveCamera());
        }
        
        if (loadFrameBtn) {
            loadFrameBtn.addEventListener('click', () => this.loadSampleFrame());
        }
        
        if (toggleProcessBtn) {
            toggleProcessBtn.addEventListener('click', () => this.toggleProcessing());
        }
        
        if (resetBtn) {
            resetBtn.addEventListener('click', () => this.reset());
        }
    }
    
    /**
     * Load a sample processed frame (simulated)
     * In a real implementation, this would receive data from the Android app
     */
    private loadSampleFrame(): void {
        // Create a sample image (gradient with some pattern to simulate processed frame)
        const width = 1920;
        const height = 1080;
        
        this.canvas.width = width;
        this.canvas.height = height;
        
        // Draw a sample processed frame
        // Simulate grayscale/Canny edge detection pattern
        const imageData = this.ctx.createImageData(width, height);
        const data = imageData.data;
        
        for (let y = 0; y < height; y++) {
            for (let x = 0; x < width; x++) {
                const idx = (y * width + x) * 4;
                
                // Create a pattern that simulates processed camera frame
                let value: number;
                if (this.processMode === 'canny') {
                    // Simulate edge detection pattern
                    const pattern = Math.sin(x * 0.1) * Math.cos(y * 0.1);
                    value = pattern > 0.5 ? 255 : 0;
                } else if (this.processMode === 'grayscale') {
                    // Simulate grayscale gradient
                    value = ((x + y) % 256);
                } else {
                    // Raw mode - color gradient
                    value = ((x + y) % 256);
                }
                
                data[idx] = value;     // R
                data[idx + 1] = value; // G
                data[idx + 2] = value; // B
                data[idx + 3] = 255;   // A
            }
        }
        
        this.ctx.putImageData(imageData, 0, 0);
        
        // Update UI
        this.updateFrameInfo(width, height, 0);
        this.updateStatus(true);
        this.updateModeChip();
        this.triggerFrameTransition();
    }
    
    /**
     * Update frame information display and diagnostics
     */
    private updateFrameInfo(width: number, height: number, processingTime: number = 0): void {
        this.resolutionElement.textContent = `${width}x${height}`;
        this.frameCount++;
        this.frameCountElement.textContent = this.frameCount.toString();
        this.lastUpdateElement.textContent = new Date().toLocaleTimeString();
        this.updateLatency(processingTime);
        this.updateDiagnostics();
    }
    
    /**
     * Update connection status
     */
    private updateStatus(connected: boolean): void {
        if (connected) {
            this.statusElement.textContent = 'Connected';
            this.statusIndicator.classList.add('connected');
            this.statusDot.setAttribute('data-state', 'connected');
        } else {
            this.statusElement.textContent = 'Disconnected';
            this.statusIndicator.classList.remove('connected');
            this.statusDot.setAttribute('data-state', 'disconnected');
        }
        const cameraState = connected
            ? (this.isCameraActive ? 'Streaming' : 'Sample Preview')
            : 'Idle';
        this.cameraStateElement.textContent = cameraState;
    }
    
    private updateLatency(processingTime: number): void {
        this.lastProcessingTimeMs = processingTime;
        this.latencyElement.textContent = `${processingTime.toFixed(2)} ms`;
        this.processingHistory.push(processingTime);
        if (this.processingHistory.length > this.maxProcessingHistory) {
            this.processingHistory.shift();
        }
    }
    
    /**
     * Toggle processing mode
     */
    private toggleProcessing(): void {
        const modes: Array<'raw' | 'grayscale' | 'canny'> = ['raw', 'grayscale', 'canny'];
        const currentIndex = modes.indexOf(this.processMode);
        const nextMode = modes[(currentIndex + 1) % modes.length];
        this.setProcessMode(nextMode);
    }
    
    /**
     * Start live camera feed
     */
    private async startLiveCamera(): Promise<void> {
        try {
            // Request camera access
            const stream = await navigator.mediaDevices.getUserMedia({
                video: {
                    width: { ideal: 1920 },
                    height: { ideal: 1080 },
                    facingMode: 'user' // Use front camera by default
                }
            });
            
            this.stream = stream;
            const videoTrack = stream.getVideoTracks()[0];
            const settings = videoTrack.getSettings();
            this.isFrontCamera = settings.facingMode ? settings.facingMode.toLowerCase() !== 'environment' : true;
            
            // Create video element
            this.videoElement = document.createElement('video');
            this.videoElement.srcObject = stream;
            this.videoElement.play();
            
            // Wait for video to be ready
            await new Promise((resolve) => {
                this.videoElement!.addEventListener('loadedmetadata', () => {
                    const width = this.videoElement!.videoWidth;
                    const height = this.videoElement!.videoHeight;
                    
                    // Set canvas size to match video
                    this.canvas.width = width;
                    this.canvas.height = height;
                    this.tempCanvas.width = width;
                    this.tempCanvas.height = height;
                    
                    this.updateFrameInfo(width, height, 0);
                    resolve(null);
                });
            });
            
            this.isCameraActive = true;
            this.updateStatus(true);
            this.updateModeChip();
            this.triggerFrameTransition();
            
            // Update UI buttons
            const startBtn = document.getElementById('startCameraBtn');
            const stopBtn = document.getElementById('stopCameraBtn');
            if (startBtn) startBtn.style.display = 'none';
            if (stopBtn) stopBtn.style.display = 'inline-block';
            
            // Start processing frames
            this.processCameraFrame();
            
            console.log('Live camera started');
        } catch (error) {
            console.error('Error accessing camera:', error);
            alert('Failed to access camera. Please grant camera permissions.');
            this.updateStatus(false);
        }
    }
    
    /**
     * Stop live camera feed
     */
    private stopLiveCamera(): void {
        this.isCameraActive = false;
        
        // Stop animation frame
        if (this.animationFrameId !== null) {
            cancelAnimationFrame(this.animationFrameId);
            this.animationFrameId = null;
        }
        
        // Stop video stream
        if (this.stream) {
            this.stream.getTracks().forEach(track => track.stop());
            this.stream = null;
        }
        
        if (this.videoElement) {
            this.videoElement.srcObject = null;
            this.videoElement = null;
        }
        
        // Update UI buttons
        const startBtn = document.getElementById('startCameraBtn');
        const stopBtn = document.getElementById('stopCameraBtn');
        if (startBtn) startBtn.style.display = 'inline-block';
        if (stopBtn) stopBtn.style.display = 'none';
        
        this.updateStatus(false);
        console.log('Live camera stopped');
    }
    
    /**
     * Process camera frame and apply filters
     */
    private processCameraFrame(): void {
        if (!this.isCameraActive || !this.videoElement) {
            return;
        }
        
        const video = this.videoElement;
        const width = video.videoWidth;
        const height = video.videoHeight;
        
        const startTime = performance.now();
        
        // Draw video frame to temp canvas
        this.tempCtx.save();
        if (this.isFrontCamera) {
            this.tempCtx.scale(-1, 1);
            this.tempCtx.drawImage(video, -width, 0, width, height);
        } else {
            this.tempCtx.drawImage(video, 0, 0, width, height);
        }
        this.tempCtx.restore();
        
        // Get image data
        const imageData = this.tempCtx.getImageData(0, 0, width, height);
        const data = imageData.data;
        
        // Apply processing based on mode
        if (this.processMode === 'grayscale') {
            this.applyGrayscale(data);
        } else if (this.processMode === 'canny') {
            this.applyCannyEdgeDetection(data, width, height);
        }
        // Raw mode: no processing needed
        
        // Draw processed frame to main canvas
        this.ctx.putImageData(imageData, 0, 0);
        
        const processingTime = performance.now() - startTime;
        
        // Update frame info
        this.updateFrameInfo(width, height, processingTime);
        this.updateStatus(true);
        
        // Continue processing
        this.animationFrameId = requestAnimationFrame(() => this.processCameraFrame());
    }
    
    /**
     * Apply grayscale filter to image data
     */
    private applyGrayscale(data: Uint8ClampedArray): void {
        for (let i = 0; i < data.length; i += 4) {
            // Calculate grayscale value using luminance formula
            const gray = Math.round(0.299 * data[i] + 0.587 * data[i + 1] + 0.114 * data[i + 2]);
            data[i] = gray;     // R
            data[i + 1] = gray; // G
            data[i + 2] = gray; // B
            // Alpha channel (data[i + 3]) remains unchanged
        }
    }
    
    /**
     * Apply Canny edge detection (simplified version using Sobel operator)
     */
    private applyCannyEdgeDetection(data: Uint8ClampedArray, width: number, height: number): void {
        // First convert to grayscale
        this.applyGrayscale(data);
        
        // Create grayscale array
        const gray = new Uint8Array(width * height);
        for (let i = 0; i < data.length; i += 4) {
            gray[i / 4] = data[i];
        }
        
        // Sobel operator kernels
        const sobelX = [
            -1, 0, 1,
            -2, 0, 2,
            -1, 0, 1
        ];
        
        const sobelY = [
            -1, -2, -1,
             0,  0,  0,
             1,  2,  1
        ];
        
        // Apply Sobel operator
        const edges = new Uint8Array(width * height);
        for (let y = 1; y < height - 1; y++) {
            for (let x = 1; x < width - 1; x++) {
                let gx = 0;
                let gy = 0;
                
                // Convolve with Sobel kernels
                for (let ky = -1; ky <= 1; ky++) {
                    for (let kx = -1; kx <= 1; kx++) {
                        const idx = (y + ky) * width + (x + kx);
                        const kernelIdx = (ky + 1) * 3 + (kx + 1);
                        gx += gray[idx] * sobelX[kernelIdx];
                        gy += gray[idx] * sobelY[kernelIdx];
                    }
                }
                
                // Calculate magnitude
                const magnitude = Math.sqrt(gx * gx + gy * gy);
                const edgeValue = Math.min(255, magnitude);
                
                // Apply threshold (simplified Canny)
                edges[y * width + x] = edgeValue > 50 ? 255 : 0;
            }
        }
        
        // Convert edges back to RGBA
        for (let i = 0; i < data.length; i += 4) {
            const idx = i / 4;
            const edge = edges[idx];
            data[i] = edge;     // R
            data[i + 1] = edge; // G
            data[i + 2] = edge; // B
            // Alpha remains unchanged
        }
    }
    
    /**
     * Reset viewer
     */
    private reset(): void {
        if (this.isCameraActive) {
            this.stopLiveCamera();
        }
        
        this.frameCount = 0;
        this.fpsHistory = [];
        this.processingHistory = [];
        this.currentFps = 0;
        this.lastFpsTime = 0;
        this.lastProcessingTimeMs = 0;
        this.fpsElement.textContent = '0';
        this.frameCountElement.textContent = '0';
        this.latencyElement.textContent = '0.0 ms';
        this.avgFpsElement.textContent = '0';
        this.maxFpsElement.textContent = '0';
        this.minFpsElement.textContent = '0';
        this.avgLatencyElement.textContent = '0.0 ms';
        this.maxLatencyElement.textContent = '0.0 ms';
        this.totalFramesElement.textContent = '0';
        this.cameraStateElement.textContent = 'Idle';
        this.clearSparkline();
        this.diagnosticsExpanded = false;
        this.diagnosticsHeader.classList.remove('open');
        this.diagnosticsBody.classList.remove('open');
        
        this.processMode = 'grayscale';
        this.processModeElement.textContent = 'Grayscale';
        this.updateModeChip();
        
        this.loadSampleFrame();
    }
    
    /**
     * Start FPS counter
     */
    private startFpsCounter(): void {
        let frameCounter = 0;
        this.frameUpdateInterval = window.setInterval(() => {
            const now = Date.now();
            if (this.lastFpsTime === 0) {
                this.lastFpsTime = now;
            }
            
            const elapsed = (now - this.lastFpsTime) / 1000;
            if (elapsed >= 1.0) {
                this.currentFps = Math.round(frameCounter / elapsed);
                this.fpsElement.textContent = this.currentFps.toString();
                this.lastFpsTime = now;
                frameCounter = 0;
                
                this.fpsHistory.push(this.currentFps);
                if (this.fpsHistory.length > this.maxFpsHistory) {
                    this.fpsHistory.shift();
                }
                this.updateSparkline();
            }
            
            frameCounter++;
        }, 100);
    }
    
    /**
     * Display frame from base64 data
     * @param base64Data Base64 encoded image data
     * @param width Frame width
     * @param height Frame height
     */
    public displayFrame(base64Data: string, width: number, height: number): void {
        const img = new Image();
        img.onload = () => {
            this.canvas.width = width;
            this.canvas.height = height;
            this.ctx.drawImage(img, 0, 0);
            this.updateFrameInfo(width, height, 0);
            this.updateStatus(true);
            this.triggerFrameTransition();
        };
        img.onerror = () => {
            console.error('Failed to load image');
            this.updateStatus(false);
        };
        img.src = `data:image/png;base64,${base64Data}`;
    }
    
    /**
     * Display frame from FrameData object
     */
    public displayFrameData(frameData: FrameData): void {
        this.displayFrame(frameData.data, frameData.width, frameData.height);
    }

    /**
     * Set processing mode directly
     */
    private setProcessMode(mode: 'raw' | 'grayscale' | 'canny'): void {
        if (this.processMode === mode) {
            return;
        }
        this.processMode = mode;
        this.processModeElement.textContent = mode.charAt(0).toUpperCase() + mode.slice(1);
        this.updateModeChip();
        this.triggerFrameTransition();
        
        if (!this.isCameraActive) {
            this.loadSampleFrame();
        }
    }

    /**
     * Update mode chip display
     */
    private updateModeChip(): void {
        const label = this.processMode.charAt(0).toUpperCase() + this.processMode.slice(1);
        this.modeChip.textContent = `Mode: ${label}`;
        this.modeChip.setAttribute('data-mode', this.processMode);
        this.modeDiagnosticElement.textContent = label;
        if (this.modeTransitionTimeout !== null) {
            window.clearTimeout(this.modeTransitionTimeout);
        }
        this.modeChip.classList.add('flash');
        this.modeTransitionTimeout = window.setTimeout(() => {
            this.modeChip.classList.remove('flash');
            this.modeTransitionTimeout = null;
        }, 600);
    }
    
    /**
     * Trigger frame transition animation
     */
    private triggerFrameTransition(): void {
        this.frameSurface.setAttribute('data-transition', 'true');
        if (this.frameTransitionTimeout !== null) {
            window.clearTimeout(this.frameTransitionTimeout);
        }
        this.frameTransitionTimeout = window.setTimeout(() => {
            this.frameSurface.setAttribute('data-transition', 'false');
            this.frameTransitionTimeout = null;
        }, 600);
    }
    
    private resizeSparkline(): void {
        const parent = this.sparklineCanvas.parentElement as HTMLElement | null;
        const width = parent ? parent.clientWidth : 320;
        const height = 90;
        this.sparklineCanvas.width = Math.max(width, 220);
        this.sparklineCanvas.height = height;
        this.sparklineCanvas.style.width = '100%';
        this.sparklineCanvas.style.height = `${height}px`;
        this.clearSparkline();
        this.updateSparkline();
    }
    
    /**
     * Clear sparkline canvas
     */
    private clearSparkline(): void {
        const width = this.sparklineCanvas.width;
        const height = this.sparklineCanvas.height;
        this.sparklineCtx.clearRect(0, 0, width, height);
        const gradient = this.sparklineCtx.createLinearGradient(0, 0, width, 0);
        gradient.addColorStop(0, 'rgba(79, 70, 229, 0.15)');
        gradient.addColorStop(1, 'rgba(56, 189, 248, 0.10)');
        this.sparklineCtx.fillStyle = gradient;
        this.sparklineCtx.fillRect(0, 0, width, height);
    }
    
    /**
     * Update FPS sparkline visualization
     */
    private updateSparkline(): void {
        this.clearSparkline();
        if (this.fpsHistory.length < 2) {
            return;
        }
        
        const width = this.sparklineCanvas.width;
        const height = this.sparklineCanvas.height;
        const maxFps = Math.max(...this.fpsHistory, 1);
        const minFps = Math.min(...this.fpsHistory);
        const range = Math.max(maxFps - minFps, 1);
        const padding = 10;
        const xStep = (width - padding * 2) / (this.fpsHistory.length - 1);
        
        const gradient = this.sparklineCtx.createLinearGradient(0, 0, 0, height);
        gradient.addColorStop(0, 'rgba(56, 189, 248, 0.28)');
        gradient.addColorStop(1, 'rgba(56, 189, 248, 0.05)');
        
        // Fill path
        this.sparklineCtx.beginPath();
        this.fpsHistory.forEach((fps, index) => {
            const x = padding + index * xStep;
            const normalized = (fps - minFps) / range;
            const y = height - padding - normalized * (height - padding * 2);
            if (index === 0) {
                this.sparklineCtx.moveTo(x, y);
            } else {
                this.sparklineCtx.lineTo(x, y);
            }
        });
        const lastX = padding + (this.fpsHistory.length - 1) * xStep;
        this.sparklineCtx.lineTo(lastX, height - padding);
        this.sparklineCtx.lineTo(padding, height - padding);
        this.sparklineCtx.closePath();
        this.sparklineCtx.fillStyle = gradient;
        this.sparklineCtx.fill();
        
        // Stroke path
        this.sparklineCtx.beginPath();
        this.fpsHistory.forEach((fps, index) => {
            const x = padding + index * xStep;
            const normalized = (fps - minFps) / range;
            const y = height - padding - normalized * (height - padding * 2);
            if (index === 0) {
                this.sparklineCtx.moveTo(x, y);
            } else {
                this.sparklineCtx.lineTo(x, y);
            }
        });
        this.sparklineCtx.strokeStyle = 'rgba(56, 189, 248, 0.95)';
        this.sparklineCtx.lineWidth = 2.25;
        this.sparklineCtx.stroke();
    }
    
    /**
     * Handle keyboard shortcuts
     */
    private handleKeydown(event: KeyboardEvent): void {
        if (event.target instanceof HTMLInputElement || event.target instanceof HTMLTextAreaElement || event.target instanceof HTMLSelectElement) {
            return;
        }
        if (event.repeat) {
            return;
        }
        const key = event.key.toLowerCase();
        switch (key) {
            case '1':
            case 'q':
                this.setProcessMode('raw');
                break;
            case '2':
            case 'g':
                this.setProcessMode('grayscale');
                break;
            case '3':
            case 'c':
                this.setProcessMode('canny');
                break;
            case 's':
                if (this.isCameraActive) {
                    this.stopLiveCamera();
                } else {
                    this.startLiveCamera().catch((error) => console.error('Camera start error:', error));
                }
                break;
            case 't':
                this.toggleProcessing();
                break;
            case 'l':
                this.loadSampleFrame();
                break;
            case 'r':
                this.reset();
                break;
            default:
                break;
        }
    }

    private toggleDiagnostics(): void {
        this.diagnosticsExpanded = !this.diagnosticsExpanded;
        if (this.diagnosticsExpanded) {
            this.diagnosticsHeader.classList.add('open');
            this.diagnosticsBody.classList.add('open');
        } else {
            this.diagnosticsHeader.classList.remove('open');
            this.diagnosticsBody.classList.remove('open');
        }
    }

    private updateDiagnostics(): void {
        const fpsValues = this.fpsHistory.length > 0
            ? this.fpsHistory
            : (this.currentFps > 0 ? [this.currentFps] : []);
        const avgFps = fpsValues.length
            ? fpsValues.reduce((acc, value) => acc + value, 0) / fpsValues.length
            : 0;
        const maxFps = fpsValues.length ? Math.max(...fpsValues) : 0;
        const minFps = fpsValues.length ? Math.min(...fpsValues) : 0;
        this.avgFpsElement.textContent = avgFps.toFixed(1);
        this.maxFpsElement.textContent = maxFps.toString();
        this.minFpsElement.textContent = minFps.toString();

        const latencyValues = this.processingHistory.length > 0
            ? this.processingHistory
            : (this.lastProcessingTimeMs > 0 ? [this.lastProcessingTimeMs] : []);
        const avgLatency = latencyValues.length
            ? latencyValues.reduce((acc, value) => acc + value, 0) / latencyValues.length
            : 0;
        const maxLatency = latencyValues.length ? Math.max(...latencyValues) : 0;
        this.avgLatencyElement.textContent = `${avgLatency.toFixed(2)} ms`;
        this.maxLatencyElement.textContent = `${maxLatency.toFixed(2)} ms`;

        const totalFramesLabel = this.frameCount.toString();
        this.totalFramesElement.textContent = totalFramesLabel;

        const modeLabel = this.processMode.charAt(0).toUpperCase() + this.processMode.slice(1);
        this.modeDiagnosticElement.textContent = modeLabel;

        const cameraState = this.isCameraActive
            ? 'Streaming'
            : (this.frameCount > 0 ? 'Sample Preview' : 'Idle');
        this.cameraStateElement.textContent = cameraState;
    }
}

// Initialize viewer when DOM is ready
document.addEventListener('DOMContentLoaded', () => {
    try {
        const viewer = new CameraViewer();
        console.log('Camera viewer initialized');
        
        // Expose viewer globally for external access
        (window as any).cameraViewer = viewer;
    } catch (error) {
        console.error('Failed to initialize camera viewer:', error);
    }
});

// Export for module usage
export { CameraViewer, FrameData };

