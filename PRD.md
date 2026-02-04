# Product Requirement Document (PRD): SnapTrace Desktop

| **Project Name**    | SnapTrace (Java Desktop Edition)                                          |
| ------------------- | ------------------------------------------------------------------------- |
| **Version**         | 1.0.0 (MVP)                                                               |
| **Status**          | **Approved for Development**                                              |
| **Target Platform** | Windows / macOS / Linux (JVM 17+)                                         |
| **Primary Goal**    | Create a native, high-velocity evidence collection tool for QA engineers. |

---

## 1. Executive Summary

**SnapTrace** is a native desktop application designed to streamline the manual testing evidence collection process. Unlike standard screenshot tools, SnapTrace is workflow-centric. It resides in the system background, allowing users to capture, annotate, and organize screenshots into logical "Sessions" using global hotkeys, without context-switching away from the application under test.

---

## 2. User Stories & Core Workflows

### 2.1 The "Capture & Annotate" Loop

- **As a** QA Engineer,
- **I want** to trigger a screen capture via a global shortcut without opening the SnapTrace window,
- **So that** I can capture a bug the instant it happens without losing focus.

### 2.2 The "Evidence Management" Loop

- **As a** QA Engineer,
- **I want** my screenshots to be automatically grouped into a specific "Testing Session,"
- **So that** I don't have to manually sort dozens of files later.

### 2.3 The "Export" Loop

- **As a** QA Engineer,
- **I want** to export a full session into a Word document (`.docx`),
- **So that** I can attach a single file to a Jira/bug ticket.

---

## 3. Functional Requirements

### 3.1 Global Input Handling

- **FR-01:** The application must launch into the System Tray (background execution).
- **FR-02:** The application must listen for global hotkeys even when minimized or not in focus using `JNativeHook`.
- **FR-03:** **Global Capture Shortcut:** `Alt + Shift + S`. (Chosen to avoid Windows Snipping Tool `Win+Shift+S` and generic `Alt+S` menu conflicts).
- **FR-04:** **Global Dashboard Shortcut:** `Alt + Shift + D`.

### 3.2 Screen Capture & Overlay

- **FR-05:** Upon triggering capture, the system must freeze the current screen state (Multi-monitor support required).
- **FR-06:** A full-screen, transparent JavaFX Stage must overlay the screen immediately.
- **FR-07:** The cursor must change to a crosshair to indicate "Edit Mode."

### 3.3 Annotation Tools

- **FR-08:** **Rectangle Tool:** Users can drag to draw a red, empty rectangle (standard bug highlighting).
- **FR-09:** **Text Tool:** Users can click to place a text label (Red text, Arial font) for context.
- **FR-10:** **Save Action:** Pressing `Ctrl + Enter` saves the screenshot to disk and closes the overlay.
- **FR-11:** **Discard Action:** Pressing `Esc` discards the screenshot and closes the overlay.

### 3.4 Data Persistence

- **FR-12:** Data must be stored in `%USER_HOME%/.snaptrace/`.
- **FR-13:** Each "Session" is a folder containing:
- `metadata.json` (Session details, timestamps, order of images).
- `{timestamp}_id.png` (Raw image files).

### 3.5 Dashboard & Export

- **FR-14:** A main GUI Dashboard (JavaFX) to list previous sessions.
- **FR-15:** Capability to delete sessions or individual images.
- **FR-16:** **Export to Word:** Generate a `.docx` file using Apache POI. The document must include the image + timestamp + optional user notes.

---

## 4. Non-Functional Requirements

- **NFR-01 Performance:** The "Time to Capture" (Hotkey press -> Overlay visible) must be under 200ms.
- **NFR-02 Memory:** Idle memory usage (in Tray) should be optimized (<100MB preferred).
- **NFR-03 Compatibility:** Must run on Java 17+ (LTS).
- **NFR-04 Installation:** Must be deliverable as a runnable JAR or packaged EXE (via jpackage).

---

## 5. Technical Architecture

### 5.1 Technology Stack

| Component        | Technology      | Version | Usage                |
| ---------------- | --------------- | ------- | -------------------- |
| **Language**     | Java            | 17 LTS  | Core Logic           |
| **UI Framework** | JavaFX          | 17+     | Overlay & Dashboard  |
| **Build System** | Maven           | 3.8+    | Dependency Mgmt      |
| **Input Hook**   | JNativeHook     | Latest  | Global Key Listening |
| **JSON Parser**  | Jackson         | 2.15+   | Data Serialization   |
| **Doc Export**   | Apache POI      | 5.2+    | Word Doc Generation  |
| **Logging**      | SLF4J / Logback | Latest  | Debugging            |

### 5.2 Application Structure (Maven)

Follows a clean **MVC (Model-View-Controller)** pattern separation.

```text
com.snaptrace
├── Main.java                  // Application Entry Point
├── config
│   └── AppConfig.java         // Singleton for paths & constants
├── controller
│   ├── DashboardController.java // Main UI logic
│   └── OverlayController.java   // Drawing/Canvas logic
├── model
│   ├── Session.java           // Data class for a test session
│   └── Evidence.java          // Data class for a single screenshot
├── service
│   ├── CaptureService.java    // java.awt.Robot logic
│   ├── HotkeyService.java     // JNativeHook management
│   ├── StorageService.java    // JSON/File I/O
│   └── ExportService.java     // Apache POI logic
└── view
    ├── dashboard.fxml         // Main UI Layout
    └── overlay.fxml           // Canvas Layout

```

### 5.3 Data Model (JSON Schema)

**File:** `metadata.json`

```json
{
  "sessionId": "uuid-string",
  "sessionName": "Checkout Flow Test",
  "createdAt": "2023-10-27T10:00:00Z",
  "evidenceList": [
    {
      "id": "uuid-string",
      "filename": "173820922.png",
      "timestamp": "2023-10-27T10:05:00Z",
      "note": "Button overlaps with footer"
    }
  ]
}
```

---

## 6. UI/UX Design Specifications

### 6.1 The Overlay (Canvas)

- **Background:** The captured `BufferedImage`.
- **Canvas:** A `javafx.scene.canvas.Canvas` matches screen dimensions.
- **State Management:**
- `Mouse Pressed`: Start coordinate .
- `Mouse Dragged`: Update preview rectangle .
- `Mouse Released`: Commit shape to a data structure (to allow Undo if needed in V2).

### 6.2 The Dashboard

- **Left Pane:** `ListView` of Sessions (sorted by Date Descending).
- **Right Pane:** Grid view of thumbnails for the selected session.
- **Toolbar:** "New Session", "Export to Word", "Delete".

---

## 7. Development Phases

### Phase 1: Core Mechanics (Day 1-2)

- Set up Maven & JavaFX.
- Implement `CaptureService` (Robot) and `HotkeyService` (JNativeHook).
- Verify global key triggering the overlay.

### Phase 2: The Drawing Engine (Day 3-4)

- Implement `OverlayController`.
- Enable drawing rectangles on the Canvas.
- Implement `Ctrl+Enter` to save image to disk.

### Phase 3: Session Management (Day 5-6)

- Implement `StorageService` (JSON).
- Build the `DashboardController`.
- Wire up the UI to read from JSON files.

### Phase 4: Polish & Export (Day 7)

- Implement `ExportService` (Apache POI).
- Add System Tray icon and menu (Quit/Open).
- Final testing and packaging.

---

## 8. Risks & Mitigation

- **Risk:** JNativeHook might require "Accessibility" permissions on macOS or specific Admin rights on Windows.
- **Mitigation:** Catch `NativeHookException` gracefully and show a dialog instructing the user to grant permissions on first run.

- **Risk:** High DPI / 4K Monitors scaling issues.
- **Mitigation:** Use `Screen.getScreens()` in JavaFX to calculate bounds accurately and handle DPI scaling using `stage.setRenderScale`.
