package com.cozumtr.leave_management_system.integration;

import com.cozumtr.leave_management_system.config.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Basit PostgreSQL Trigger testi.
 * JUnit'in doğru çalıştığını doğrulamak için.
 */
public class SimpleTriggerTest extends AbstractIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(SimpleTriggerTest.class);

    @Autowired
    private DataSource dataSource;

    @Test
    @DisplayName("1. PostgreSQL Container çalışıyor mu?")
    void testContainerIsRunning() {
        log.info("🧪 Test: Container kontrolü başladı");
        assertThat(isContainerRunning()).isTrue();
        log.info("✅ Container çalışıyor: {}", getJdbcUrl());
    }

    @Test
    @DisplayName("2. Veritabanı bağlantısı çalışıyor mu?")
    void testDatabaseConnection() throws Exception {
        log.info("🧪 Test: Veritabanı bağlantısı kontrolü");
        
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT 1 as test")) {
            
            assertThat(rs.next()).isTrue();
            assertThat(rs.getInt("test")).isEqualTo(1);
            log.info("✅ Veritabanı bağlantısı başarılı");
        }
    }

    @Test
    @DisplayName("3. Trigger'lar yüklendikten sonra mevcut mu?")
    void testTriggersAfterLoad() throws Exception {
        log.info("🧪 Test: Trigger'ları yükle ve kontrol et");
        
        // Trigger'ları yükle
        loadTriggers(dataSource);
        
        // Trigger'ları kontrol et
        List<String> triggers = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                 "SELECT trigger_name FROM information_schema.triggers WHERE trigger_schema = 'public'")) {
            
            while (rs.next()) {
                triggers.add(rs.getString("trigger_name"));
            }
        }
        
        log.info("📋 Bulunan trigger'lar: {}", triggers);
        assertThat(triggers).isNotEmpty();
        log.info("✅ {} trigger yüklendi", triggers.size());
    }
}

