package dev.corexinc.corex.velocity;

import dev.corexinc.corex.api.processors.BaseTagProcessor;
import dev.corexinc.corex.api.processors.GlobalTagProcessor;
import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.engine.CorexRegistry;
import dev.corexinc.corex.environment.tags.core.MapTag;

public class VelocityEnvironmentLoader {

    public static void registerDefaults(CorexRegistry registry) {

        GlobalTagProcessor.register();

        BaseTagProcessor.registerBaseTag("", (attribute) -> {
            if (attribute.hasParam()) {
                String fullPath = attribute.getParam();

                if (!fullPath.contains(".")) {
                    return attribute.getQueue().getDefinition(fullPath);
                }

                String[] parts = fullPath.split("\\.", -1);
                AbstractTag current = attribute.getQueue().getDefinition(parts[0]);

                for (int i = 1; i < parts.length; i++) {
                    if (!(current instanceof MapTag map)) return null;
                    current = map.getObject(parts[i]);
                }

                return current;
            }
            return null;
        });

        registry.register(

                // Okak
        );
    }
}