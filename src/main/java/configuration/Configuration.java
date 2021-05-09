package configuration;

import java.io.*;
import java.util.Properties;

public final class Configuration {

    public enum PropertyName {
        RECEIVER_PORT("receiverPort"),
        SENDER_PORT("senderPort"),
        DESTINATION_IP("destinationIp"),
        DESTINATION_PORT("destinationPort");

        private final String propertyName;

        PropertyName(String propertyName) {
            this.propertyName = propertyName;
        }
    }

    public static final String CONFIGURATION_FILE_PATH_NAME = "src" + File.separator + "main" + File.separator + "resources" + File.separator + "configuration.properties";
    public static final int RECEIVER_PORT;
    public static final int SENDER_PORT;
    public static final String DESTINATION_IP;
    public static final int DESTINATION_PORT;

    static {
        File configurationFile = new File(CONFIGURATION_FILE_PATH_NAME);
        try (FileInputStream fis = new FileInputStream(configurationFile)) {
            Properties properties = new Properties();
            properties.load(fis);
            RECEIVER_PORT    = Integer.parseInt(properties.getProperty(PropertyName.RECEIVER_PORT.propertyName));
            SENDER_PORT      = Integer.parseInt(properties.getProperty(PropertyName.SENDER_PORT.propertyName));
            DESTINATION_IP   = properties.getProperty(PropertyName.DESTINATION_IP.propertyName);
            DESTINATION_PORT = Integer.parseInt(properties.getProperty(PropertyName.DESTINATION_PORT.propertyName));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("数値プロパティを数値に変換できませんでした: " + e.getMessage());
        } catch (FileNotFoundException e) {
            throw new IllegalStateException("プロパティファイルが見つかりません。");
        } catch (IOException e) {
            throw new Error("プロパティファイルを開けませんでした: " + e.getMessage());
        }
    }
}
