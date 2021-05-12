package no.eksempel.kafka.connector.sources.github.utils;

public class VersionUtil {
    private VersionUtil() {
        throw new IllegalStateException("Utility class");
    }

    public static String getVersion() {
        try {
            return VersionUtil.class.getPackage().getImplementationVersion();
        } catch(Exception ex){
            return "0.0.0.0";
        }
    }
}
