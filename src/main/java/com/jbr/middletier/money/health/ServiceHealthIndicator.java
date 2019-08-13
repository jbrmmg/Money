package com.jbr.middletier.money.health;

import com.jbr.middletier.money.data.Category;
import com.jbr.middletier.money.dataaccess.CategoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Created by jason on 26/04/17.
 */

@Component
public class ServiceHealthIndicator implements HealthIndicator {
    final static private Logger LOG = LoggerFactory.getLogger(ServiceHealthIndicator.class);

    @Value("${middle.tier.service.name}")
    private String serviceName;

    private final
    CategoryRepository categoryRepository;

    @Autowired
    public ServiceHealthIndicator(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    public Health health() {
        try {
            List<Category> categoryList = (List<Category>) categoryRepository.findAll();
            LOG.info(String.format("Check Database %s.", categoryList.size()));

            return Health.up().withDetail("service", serviceName).withDetail("Category Types",categoryList.size()).build();
        } catch (Exception ignored) {

        }

        return Health.down().withDetail("service", serviceName).build();
    }
}
