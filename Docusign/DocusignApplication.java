package com.example.Docusign;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Profile;

import java.util.Map;
import jakarta.annotation.PostConstruct;

@SpringBootApplication
@Profile("!test")
public class DocusignApplication {

    private static final Logger log = LoggerFactory.getLogger(DocusignApplication.class);

    public static void main(String[] args) {
        ApplicationContext ctx = SpringApplication.run(DocusignApplication.class, args);
        log.info("Docusign Application Started");
        
        // Log all registered endpoints
        logMappings(ctx);
    }
    private static void logMappings(ApplicationContext ctx) {
        try {
            var handlerMapping = ctx.getBean(RequestMappingHandlerMapping.class);
            Map<RequestMappingInfo, HandlerMethod> methods = handlerMapping.getHandlerMethods();
            log.info("\n===== Registered Endpoints =====");
            methods.forEach((info, method) -> {
                if (info.getPatternsCondition() != null && info.getMethodsCondition() != null) {
                    info.getPatternsCondition().getPatterns().forEach(pattern -> {
                        log.info("{} {}", 
                            info.getMethodsCondition().getMethods().stream()
                                .findFirst()
                                .map(Enum::name)
                                .orElse("GET"),
                            pattern);
                    });
                }
            });
            log.info("===== End of Registered Endpoints =====\n");
        } catch (Exception e) {
            log.warn("Could not log registered endpoints: {}", e.getMessage());
        }
    }
}
