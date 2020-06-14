package ru.itis.springbot.config;

import com.google.cloud.dialogflow.v2.SessionName;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan("ru.itis.springbot")
public class RootConfig {

    @Bean
    SessionName sessionName() {
        String projectId = "newagent-oqyfnl";
        String sessionId = "123456789";
        SessionName session = SessionName.of(projectId, sessionId);
        return session;
    }
}
