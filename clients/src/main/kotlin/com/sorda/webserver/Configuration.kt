package com.sorda.webserver

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder

@Configuration
open class Configuration {
    @Bean
    @Primary
    open fun objectMapper(builder: Jackson2ObjectMapperBuilder): ObjectMapper {
        val objectMapper: ObjectMapper = builder.build()
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
        return objectMapper
    }
}
