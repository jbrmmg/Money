package com.jbr.middletier.money.manage;

import com.jbr.middletier.money.config.ApplicationProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;

@Component
public class WebLogManager {
    final static private Logger LOG = LoggerFactory.getLogger(WebLogManager.class);

    private final ApplicationProperties applicationProperties;
    private final RestTemplateBuilder restTemplateBuilder;

    @PostConstruct
    public void initialise() {
        postWebLog(webLogLevel.INFO, applicationProperties.getServiceName() + " starting up.");
    }

    public WebLogManager(ApplicationProperties applicationProperties,
                         RestTemplateBuilder restTemplateBuilder) {
        this.applicationProperties = applicationProperties;
        this.restTemplateBuilder = restTemplateBuilder;
    }

    public enum webLogLevel { DEBUG, INFO, WARN, ERROR }

    public void postWebLog(webLogLevel level, String message) {
        try {
            RestTemplate restTemplate = this.restTemplateBuilder.build();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            StringBuilder requestJson = new StringBuilder();

            requestJson.append("{");

            requestJson.append("\"levelString\": \"");
            switch (level) {
                case DEBUG:
                    requestJson.append("DEBUG");
                    break;
                case INFO:
                    requestJson.append("INFO");
                    break;
                case WARN:
                    requestJson.append("WARN");
                    break;
                case ERROR:
                    requestJson.append("ERROR");
                    break;
            }
            requestJson.append("\",");

            requestJson.append("\"formattedMessage\": \"");
            requestJson.append(message);
            requestJson.append("\",");

            requestJson.append("\"callerFilename\": \"WebLogManager.java\",");
            requestJson.append("\"callerLine\": \"0\",");
            requestJson.append("\"callerMethod\": \"postWebLog\",");
            requestJson.append("\"loggerName\": \"Money Logger\",");

            requestJson.append("\"callerClass\": \"com.jbr.middletier.money.manager\"");
            requestJson.append("}");

            HttpEntity<String> request = new HttpEntity<>(requestJson.toString(), headers);

            restTemplate.postForEntity(applicationProperties.getWebLogUrl(), request,String.class);
        } catch(Exception ex) {
            LOG.warn("Unable to post to web log.",ex);
        }
    }
}
