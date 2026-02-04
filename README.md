# SnapTrace Desktop

A native, high-velocity evidence collection tool for QA engineers built with Java and JavaFX.

## Features

- **Global Hotkeys**: Capture screenshots without switching windows
  - `Alt + Shift + S`: Capture screenshot
  - `Alt + Shift + D`: Toggle dashboard
- **Annotation Tools**: Draw rectangles and add text labels to highlight issues
- **Session Management**: Organize screenshots into logical testing sessions
- **Word Export**: Export sessions to `.docx` files for bug reports
- **System Tray**: Runs in background with minimal resource usage

## Requirements

- Java 17+ (JDK with JavaFX support)
- Maven 3.8+
- Windows/macOS/Linux

## Building

```bash
# Compile the project
mvn clean compile

# Package as JAR
mvn clean package

# Run with Maven
mvn javafx:run
```

## Running

### With Maven (Development)
```bash
mvn javafx:run
```

### With JAR
```bash
java --module-path /path/to/javafx/lib --add-modules javafx.controls,javafx.fxml,javafx.swing -jar target/snaptrace-1.0.0.jar
```

### On Windows (with bundled JavaFX)
The shaded JAR includes all dependencies. For Windows deployment:
```bash
java -jar target/snaptrace-1.0.0.jar
```

## Usage

### Capturing Screenshots
1. Press `Alt + Shift + S` to capture
2. Draw rectangles by clicking and dragging (red outline for highlighting bugs)
3. Press `T` to switch to text tool, click to add labels
4. Press `R` to switch back to rectangle tool
5. Press `Ctrl + Enter` to save, or `Esc` to discard

### Managing Sessions
1. Press `Alt + Shift + D` to open the dashboard
2. Click "New Session" to create a testing session
3. All captures are saved to the current session
4. Double-click thumbnails to view full-size images
5. Click "Export to Word" to generate a `.docx` report

### System Tray
- Right-click the tray icon for quick access menu
- Double-click to open dashboard
- Select "Exit" to close the application

## Data Storage

All data is stored in `~/.snaptrace/`:
```
~/.snaptrace/
├── sessions/
│   └── {session-id}/
│       ├── metadata.json
│       └── {timestamp}.png
├── exports/
└── logs/
```

## Project Structure

```
com.snaptrace
├── Main.java                  // Application Entry Point
├── config/
│   └── AppConfig.java         // Singleton for paths & constants
├── controller/
│   ├── DashboardController.java // Main UI logic
│   └── OverlayController.java   // Drawing/Canvas logic
├── model/
│   ├── Session.java           // Data class for a test session
│   └── Evidence.java          // Data class for a single screenshot
├── service/
│   ├── CaptureService.java    // java.awt.Robot logic
│   ├── HotkeyService.java     // JNativeHook management
│   ├── StorageService.java    // JSON/File I/O
│   └── ExportService.java     // Apache POI logic
└── view/
    ├── dashboard.fxml         // Main UI Layout
    └── overlay.fxml           // Canvas Layout
```

## Troubleshooting

### Hotkeys Not Working
- On macOS: Grant Accessibility permissions in System Preferences
- On Windows: Run as Administrator if needed
- On Linux: Ensure X11 input extension is available

### High DPI Issues
The application uses JavaFX's built-in DPI scaling. If issues persist:
```bash
java -Dprism.allowhidpi=true -jar target/snaptrace-1.0.0.jar
```

## License

MIT License
