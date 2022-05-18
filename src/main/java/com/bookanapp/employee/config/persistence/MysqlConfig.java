package com.bookanapp.employee.config.persistence;



import dev.miku.r2dbc.mysql.MySqlConnectionConfiguration;
import dev.miku.r2dbc.mysql.MySqlConnectionFactory;
import dev.miku.r2dbc.mysql.constant.TlsVersions;
import io.r2dbc.spi.ConnectionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.config.AbstractR2dbcConfiguration;

import java.time.Duration;


@Configuration
public class MysqlConfig extends AbstractR2dbcConfiguration {

//    private final String HOST = "db";
    private final String HOST = "localhost";

    MySqlConnectionConfiguration configuration = MySqlConnectionConfiguration.builder()
            .host(HOST)
            .user("root")
            .port(3306) // optional, default 3306
            .password("Dafundo1/") // optional, default null, null means has no password
            .database("employee") // optional, default null, null means not specifying the database
            .tlsVersion(TlsVersions.TLS1_3, TlsVersions.TLS1_2, TlsVersions.TLS1_1) // optional, default is auto-selected by the server
            .useServerPrepareStatement() // Use server-preparing statements, default use client-preparing statements
            .tcpKeepAlive(true) // optional, controls TCP Keep Alive, default is false
            .tcpNoDelay(true) // optional, controls TCP No Delay, default is false
            .autodetectExtensions(false)
            .connectTimeout(Duration.ofMillis(10))// optional, controls extension auto-detect, default is true
            .build();
    ConnectionFactory connectionFactory = MySqlConnectionFactory.from(configuration);

    @Override
    @Bean(name = "MySQLConnection")
    public ConnectionFactory connectionFactory() {

        return connectionFactory;
    }

}
