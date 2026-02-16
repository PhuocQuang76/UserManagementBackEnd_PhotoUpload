package com.syn.usermanagement.config;

import com.syn.usermanagement.entity.User;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;


import java.time.Duration;




@EnableCaching
@Configuration
public class RedisConfig {

    @Bean
    public LettuceConnectionFactory connectionFactory() {  // ✅ Use Lettuce to create a connection
        RedisStandaloneConfiguration configuration = new RedisStandaloneConfiguration();
        configuration.setHostName("localhost");
        configuration.setPort(6379);
        return new LettuceConnectionFactory(configuration);
        //Tell Redis to connect to localhost with port 6379
    }

    @Bean
    public RedisTemplate<String, Object> template() {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory());      //Tells the template which connection factory to use for Redis connections
        template.setKeySerializer(new StringRedisSerializer());  // Sets key serializer.  Converts String keys to bytes for Redis storage
        template.setHashKeySerializer(new StringRedisSerializer()); //Converts hash field names to bytes for Redis hash operations
        template.setHashKeySerializer(new JdkSerializationRedisSerializer()); //Converts Object values to bytes using Java serialization
        template.setValueSerializer(new JdkSerializationRedisSerializer());   //Converts Object values to bytes using Java serialization
        template.setEnableTransactionSupport(true); //Allows Redis operations to be part of transactions
        template.afterPropertiesSet();  // Initializes the template.Calls initialization method after all properties are set
        return template;
    }
}