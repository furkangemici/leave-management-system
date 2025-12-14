package com.cozumtr.leave_management_system.config;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.Statement;
import java.util.stream.Collectors;

/**
 * Testcontainers ile PostgreSQL kullanan entegrasyon testleri için temel sınıf.
 * 
 * Bu sınıf, testler başlamadan önce PostgreSQL konteynerini başlatır
 * ve gerekli veritabanı bağlantı ayarlarını dinamik olarak yapılandırır.
 * 
 * Trigger'ların test edilebilmesi için gerçek PostgreSQL veritabanı gereklidir.
 * H2 veritabanı PL/pgSQL trigger'ları desteklemez.
 * 
 * ÇALIŞMA SIRASI:
 * 1. PostgreSQL container başlatılır (static block)
 * 2. Spring context başlatılır ve Hibernate tabloları oluşturur (ddl-auto=create-drop)
 * 3. Testler çalışır
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("integration-test")
public abstract class AbstractIntegrationTest {

    /**
     * PostgreSQL konteyner tanımı.
     * Singleton pattern - tüm testler için tek bir konteyner kullanılır.
     * Static block içinde başlatılır ki @DynamicPropertySource çağrıldığında hazır olsun.
     */
    protected static final PostgreSQLContainer<?> postgresContainer;
    
    static {
        postgresContainer = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("leave_management_test_db")
            .withUsername("test_user")
            .withPassword("test_password");
        
        // Container'ı hemen başlat - @DynamicPropertySource'tan önce hazır olmalı
        postgresContainer.start();
    }

    /**
     * Spring datasource özelliklerini dinamik olarak ayarlar.
     * Testcontainers tarafından başlatılan PostgreSQL konteynerine bağlanır.
     * 
     * @param registry Spring property registry
     */
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgresContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgresContainer::getUsername);
        registry.add("spring.datasource.password", postgresContainer::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        
        // JPA/Hibernate ayarları - Hibernate tabloları oluştursun
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.PostgreSQLDialect");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.show-sql", () -> "true");
        
        // Flyway devre dışı - Hibernate tabloları oluşturacak
        registry.add("spring.flyway.enabled", () -> "false");
        
        // Security placeholder'ları
        registry.add("spring.security.user.name", () -> "test-admin");
        registry.add("spring.security.user.password", () -> "test-password");
        
        // Email devre dışı
        registry.add("app.email.enabled", () -> "false");
    }

    /**
     * Container'ın çalışır durumda olduğunu kontrol eder.
     * 
     * @return true eğer container çalışıyorsa
     */
    protected boolean isContainerRunning() {
        return postgresContainer.isRunning();
    }

    /**
     * PostgreSQL JDBC URL'ini döndürür.
     * 
     * @return JDBC URL
     */
    protected String getJdbcUrl() {
        return postgresContainer.getJdbcUrl();
    }

    /**
     * Trigger SQL dosyasını yükler.
     * Bu metod, Hibernate tabloları oluşturduktan SONRA çağrılmalıdır.
     * Genellikle @BeforeAll metodunda kullanılır.
     * 
     * NOT: Spring'in ScriptUtils'i PL/pgSQL fonksiyonlarını düzgün parse edemez
     * çünkü $$ delimiter'ları içindeki ; karakterlerini yanlış yerde keser.
     * Bu yüzden doğrudan JDBC kullanıyoruz.
     * 
     * @param dataSource Spring DataSource
     */
    protected void loadTriggers(DataSource dataSource) {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             InputStream inputStream = getClass().getClassLoader()
                 .getResourceAsStream("db/migration/V1__create_triggers.sql")) {
            
            if (inputStream == null) {
                throw new RuntimeException("Trigger SQL dosyası bulunamadı: db/migration/V1__create_triggers.sql");
            }
            
            String sql;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                sql = reader.lines().collect(Collectors.joining("\n"));
            }
            
            // PostgreSQL JDBC driver birden fazla statement'ı tek execute() ile destekler
            stmt.execute(sql);
            
            System.out.println("✅ PostgreSQL Trigger'ları yüklendi!");
            
        } catch (Exception e) {
            System.err.println("⚠️ Trigger yükleme hatası: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

