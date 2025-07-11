package com.example.demo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class LettuceIntegrationTest {

    @Autowired
    private RedisConnectionFactory connectionFactory;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private RedisMessageListenerContainer messageListenerContainer;

    @BeforeEach
    void setUp() {
        // Clear Redis
        redisTemplate.getConnectionFactory().getConnection().flushAll();
    }

    @Test
    void testLettuceConnectionFactory() {
        assertThat(connectionFactory).isInstanceOf(LettuceConnectionFactory.class);
        
        LettuceConnectionFactory lettuceFactory = (LettuceConnectionFactory) connectionFactory;
        assertThat(lettuceFactory.getHostName()).isNotNull();
        assertThat(lettuceFactory.getPort()).isGreaterThan(0);
        assertThat(lettuceFactory.getDatabase()).isEqualTo(0);
    }

    @Test
    void testRedisTemplateOperations() {
        String key = "test:key";
        String value = "test:value";
        
        // Test basic operations
        redisTemplate.opsForValue().set(key, value);
        Object retrieved = redisTemplate.opsForValue().get(key);
        
        assertThat(retrieved).isEqualTo(value);
        
        // Test TTL operations
        redisTemplate.opsForValue().set(key, value, Duration.ofSeconds(2));
        assertThat(redisTemplate.hasKey(key)).isTrue();
        
        // Wait for expiration
        await().atMost(3, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThat(redisTemplate.hasKey(key)).isFalse();
        });
    }

    @Test
    void testRedisPublishSubscribe() {
        String channel = "test:channel";
        String message = "test:message";
        
        boolean[] messageReceived = {false};
        String[] receivedMessage = {null};
        
        // Subscribe to channel
        messageListenerContainer.addMessageListener((msg, pattern) -> {
            messageReceived[0] = true;
            receivedMessage[0] = new String(msg.getBody());
        }, new org.springframework.data.redis.listener.PatternTopic(channel));
        
        // Give subscriber time to connect
        await().atMost(1, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThat(messageListenerContainer.isRunning()).isTrue();
        });
        
        // Publish message
        redisTemplate.convertAndSend(channel, message);
        
        // Wait for message to be received
        await().atMost(2, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThat(messageReceived[0]).isTrue();
            assertThat(receivedMessage[0]).isEqualTo("\"" + message + "\""); // JSON serialized
        });
    }

    @Test
    void testRedisConnectionHealthCheck() {
        // Test connection is alive
        assertThat(connectionFactory.getConnection().ping()).isEqualTo("PONG");
    }

    @Test
    void testRedisComplexObjectSerialization() {
        String key = "test:complex";
        
        // Test with Map as a simpler complex object
        var testMap = java.util.Map.of(
            "name", "test",
            "number", 42,
            "flag", true
        );
        
        redisTemplate.opsForValue().set(key, testMap);
        
        Object retrieved = redisTemplate.opsForValue().get(key);
        assertThat(retrieved).isInstanceOf(java.util.Map.class);
        
        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> retrievedMap = (java.util.Map<String, Object>) retrieved;
        assertThat(retrievedMap.get("name")).isEqualTo("test");
        assertThat(retrievedMap.get("number")).isEqualTo(42);
        assertThat(retrievedMap.get("flag")).isEqualTo(true);
    }

    @Test
    void testRedisHashOperations() {
        String hashKey = "test:hash";
        String field1 = "field1";
        String field2 = "field2";
        String value1 = "value1";
        String value2 = "value2";
        
        // Test hash operations
        redisTemplate.opsForHash().put(hashKey, field1, value1);
        redisTemplate.opsForHash().put(hashKey, field2, value2);
        
        Object retrieved1 = redisTemplate.opsForHash().get(hashKey, field1);
        Object retrieved2 = redisTemplate.opsForHash().get(hashKey, field2);
        
        assertThat(retrieved1).isEqualTo(value1);
        assertThat(retrieved2).isEqualTo(value2);
        
        assertThat(redisTemplate.opsForHash().hasKey(hashKey, field1)).isTrue();
        assertThat(redisTemplate.opsForHash().hasKey(hashKey, "nonexistent")).isFalse();
    }

    @Test
    void testRedisListOperations() {
        String listKey = "test:list";
        String value1 = "item1";
        String value2 = "item2";
        String value3 = "item3";
        
        // Test list operations
        redisTemplate.opsForList().rightPush(listKey, value1);
        redisTemplate.opsForList().rightPush(listKey, value2);
        redisTemplate.opsForList().rightPush(listKey, value3);
        
        Long size = redisTemplate.opsForList().size(listKey);
        assertThat(size).isEqualTo(3);
        
        Object first = redisTemplate.opsForList().index(listKey, 0);
        Object last = redisTemplate.opsForList().index(listKey, -1);
        
        assertThat(first).isEqualTo(value1);
        assertThat(last).isEqualTo(value3);
    }

    @Test
    void testRedisSetOperations() {
        String setKey = "test:set";
        String member1 = "member1";
        String member2 = "member2";
        String member3 = "member3";
        
        // Test set operations
        redisTemplate.opsForSet().add(setKey, member1, member2, member3);
        
        Long size = redisTemplate.opsForSet().size(setKey);
        assertThat(size).isEqualTo(3);
        
        assertThat(redisTemplate.opsForSet().isMember(setKey, member1)).isTrue();
        assertThat(redisTemplate.opsForSet().isMember(setKey, "nonexistent")).isFalse();
        
        // Add duplicate - should not increase size
        redisTemplate.opsForSet().add(setKey, member1);
        Long newSize = redisTemplate.opsForSet().size(setKey);
        assertThat(newSize).isEqualTo(3);
    }

    // Test object for serialization
    public static class TestObject {
        public String name;
        public int number;
        public boolean flag;
        
        public TestObject() {} // Default constructor for Jackson
        
        public TestObject(String name, int number, boolean flag) {
            this.name = name;
            this.number = number;
            this.flag = flag;
        }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            TestObject that = (TestObject) obj;
            return number == that.number && flag == that.flag && 
                   java.util.Objects.equals(name, that.name);
        }
        
        @Override
        public int hashCode() {
            return java.util.Objects.hash(name, number, flag);
        }
    }
}