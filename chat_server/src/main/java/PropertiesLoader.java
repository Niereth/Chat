import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

class PropertiesLoader {

    private static final Logger log = LoggerFactory.getLogger(PropertiesLoader.class);
    private static final String PROPERTIES_FILE_NAME = "server.properties";

    String getPropertyValue(String key) {
        String propertyValue = "";
        Properties instance = new Properties();
        try (InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(PROPERTIES_FILE_NAME)) {
            instance.load(inputStream);
            propertyValue = instance.getProperty(key);
        } catch (IOException e) {
            log.error("Failed to load {}", PROPERTIES_FILE_NAME, e);
        }
        return propertyValue;
    }
}