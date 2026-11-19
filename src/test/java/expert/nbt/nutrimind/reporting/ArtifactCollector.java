package expert.nbt.nutrimind.reporting;

import com.microsoft.playwright.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Diagnostic failures must never become TestNG configuration failures. */
public final class ArtifactCollector {
    private static final Logger log = LoggerFactory.getLogger(ArtifactCollector.class);
    private final AllureArtifactSink sink;
    private final Path root;
    public ArtifactCollector(AllureArtifactSink sink) { this(sink, Path.of("target", "artifacts")); }
    public ArtifactCollector(AllureArtifactSink sink, Path root) { this.sink = sink; this.root = root; }

    public void finish(BrowserContext context, boolean tracing, boolean sensitive, String testId, boolean failed) {
        if (context == null) return;
        Path directory = root.resolve(testId);
        try {
            if (failed && !sensitive) {
                int index = 0;
                for (Page current : context.pages()) {
                    String name = "page-" + index++;
                    try {
                        if (current.isClosed()) continue;
                        byte[] screenshot = current.screenshot(new Page.ScreenshotOptions().setFullPage(true));
                        save(directory, name, "image/png", screenshot, ".png");
                        save(directory, name + "-url", "text/plain", current.url().getBytes(StandardCharsets.UTF_8), ".txt");
                    } catch (RuntimeException e) { diagnosticFailure("screenshot", e); }
                }
            }
        } catch (RuntimeException e) { diagnosticFailure("page enumeration", e); }
        finally {
            if (tracing) stopTrace(context, failed && !sensitive ? directory : null);
        }
        if (failed && sensitive) log.info("Private session: screenshots and traces are omitted");
    }
    private void stopTrace(BrowserContext context, Path directory) {
        try {
            if (directory == null) { context.tracing().stop(); return; }
            Files.createDirectories(directory);
            Path trace = directory.resolve("trace.zip");
            context.tracing().stop(new Tracing.StopOptions().setPath(trace));
            attach("Playwright trace", "application/zip", Files.readAllBytes(trace), ".zip");
        } catch (Exception e) {
            diagnosticFailure("trace", e);
            try { context.tracing().stop(); }
            catch (RuntimeException stopError) { diagnosticFailure("trace cleanup", stopError); }
        }
    }
    private void save(Path directory, String name, String type, byte[] bytes, String extension) {
        try {
            Files.createDirectories(directory);
            Files.write(directory.resolve(name + extension), bytes);
        } catch (Exception e) { diagnosticFailure("artifact file", e); }
        attach(name, type, bytes, extension);
    }
    private void attach(String name, String type, byte[] bytes, String extension) {
        try { sink.attach(name, type, bytes, extension); }
        catch (RuntimeException e) { diagnosticFailure("report attachment", e); }
    }
    private void diagnosticFailure(String operation, Exception error) {
        // Exception messages can include browser values. Log only the error type.
        log.warn("Unable to collect {} ({})", operation, error.getClass().getSimpleName());
    }
}
