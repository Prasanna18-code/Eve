package com.collegeeventhub.config;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class StudentDirectoryConfig {

    @Bean
    @Primary
    @ConfigurationProperties("spring.datasource")
    public DataSourceProperties primaryDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @Primary
    public DataSource primaryDataSource() {
        return primaryDataSourceProperties()
                .initializeDataSourceBuilder()
                .build();
    }

    @Bean
    @ConfigurationProperties("student.directory.datasource")
    public DataSourceProperties studentDirectoryDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean(name = "studentDirectoryDataSource")
    public DataSource studentDirectoryDataSource() {
        return studentDirectoryDataSourceProperties()
                .initializeDataSourceBuilder()
                .build();
    }

    @Bean(name = "studentDirectoryJdbcTemplate")
    public JdbcTemplate studentDirectoryJdbcTemplate(
            @Qualifier("studentDirectoryDataSource")
            DataSource dataSource) {

        return new JdbcTemplate(dataSource);
    }
}
