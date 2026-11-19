package expert.nbt.nutrimind.reporting;

import io.qameta.allure.Allure;
import java.io.ByteArrayInputStream;

public class AllureArtifactSink {
    public void attach(String name, String mimeType, byte[] content, String extension) {
        Allure.addAttachment(name, mimeType, new ByteArrayInputStream(content), extension);
    }
}
