-- =====================================================
-- LEAVE MANAGEMENT SYSTEM - POSTGRESQL TRIGGERS
-- =====================================================
-- Bu migration dosyası, sistemin tüm trigger'larını oluşturur.
-- Tablolar Hibernate tarafından oluşturulur, bu dosya sadece
-- trigger'ları ve ilgili fonksiyonları içerir.
-- 
-- Tüm trigger'lar PL/pgSQL kullanılarak yazılmıştır.
-- =====================================================


-- =====================================================
-- YARDIMCI TABLO: AUTH_AUDIT_LOG
-- =====================================================
-- Bu tablo trigger tarafından kullanılır (Hibernate tarafından yönetilmez)
-- Kullanıcı giriş denemelerini kaydetmek için kullanılır.
-- =====================================================

CREATE TABLE IF NOT EXISTS auth_audit_log (
    id BIGSERIAL PRIMARY KEY,
    user_email VARCHAR(255) NOT NULL,
    user_id BIGINT,
    attempt_type VARCHAR(50) NOT NULL,
    -- Olası değerler: 'LOGIN_SUCCESS', 'LOGIN_FAILURE', 'LOGOUT', 
    -- 'PASSWORD_RESET_REQUEST', 'PASSWORD_RESET_SUCCESS', 'ACCOUNT_LOCKED'
    ip_address VARCHAR(50),
    user_agent TEXT,
    session_id VARCHAR(255),
    failure_reason VARCHAR(255),
    details JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Auth audit log indeksleri
CREATE INDEX IF NOT EXISTS idx_auth_audit_log_email ON auth_audit_log(user_email);
CREATE INDEX IF NOT EXISTS idx_auth_audit_log_user_id ON auth_audit_log(user_id);
CREATE INDEX IF NOT EXISTS idx_auth_audit_log_type ON auth_audit_log(attempt_type);
CREATE INDEX IF NOT EXISTS idx_auth_audit_log_created ON auth_audit_log(created_at);
CREATE INDEX IF NOT EXISTS idx_auth_audit_log_ip ON auth_audit_log(ip_address);

COMMENT ON TABLE auth_audit_log IS 'Kullanıcı kimlik doğrulama işlemlerinin denetim kaydı';


-- #############################################################################
-- A. DENETİM (AUDITING) VE HİSTORY TRIGGER'LARI
-- #############################################################################

-- =====================================================
-- A.1. DURUM DEĞİŞİKLİĞİ DENETİMİ
-- Trigger: trg_leave_status_history
-- =====================================================
-- AMAÇ: 
--   LeaveRequest tablosundaki 'request_status' alanı her güncellendiğinde,
--   bu değişikliği 'leave_approval_history' tablosuna otomatik olarak kaydetmek.
--
-- TETİKLEME ZAMANI: AFTER UPDATE
--
-- ÇALIŞMA MANTIĞI:
--   1. Eski ve yeni status değerlerini karşılaştır
--   2. Farklıysa leave_approval_history'ye kayıt ekle
--   3. Eski ve yeni değerleri comments alanında sakla
--
-- ÖRNEK SENARYO:
--   - İzin talebi PENDING_APPROVAL → APPROVED_HR olduğunda
--   - Otomatik olarak history kaydı oluşturulur
-- =====================================================

CREATE OR REPLACE FUNCTION fn_log_leave_status_change()
RETURNS TRIGGER AS $$
DECLARE
    v_comment TEXT;
BEGIN
    -- Sadece request_status değiştiğinde çalış
    IF OLD.request_status IS DISTINCT FROM NEW.request_status THEN
        
        -- Değişiklik detayını hazırla
        v_comment := format(
            '[TRIGGER] Durum değişikliği: %s → %s | Zaman: %s',
            COALESCE(OLD.request_status, 'NULL'),
            COALESCE(NEW.request_status, 'NULL'),
            TO_CHAR(CURRENT_TIMESTAMP, 'DD.MM.YYYY HH24:MI:SS')
        );
        
        -- Leave approval history tablosuna kaydet
        -- NOT: employee_id NULL olarak kaydedilir çünkü trigger seviyesinde
        -- onaylayan kişiyi bilemiyoruz. Java tarafı zaten ayrı bir kayıt oluşturuyor.
        -- Bu kayıt sadece audit/log amaçlıdır.
        INSERT INTO leave_approval_history (
            request_id,
            employee_id,
            action,
            comments,
            is_active,
            created_at,
            updated_at
        ) VALUES (
            NEW.id,
            NULL,  -- Trigger seviyesinde onaylayan bilinmiyor
            NEW.request_status,
            v_comment,
            TRUE,
            CURRENT_TIMESTAMP,
            CURRENT_TIMESTAMP
        );
        
        -- Log mesajı (PostgreSQL log'una yazılır)
        RAISE NOTICE '[trg_leave_status_history] LeaveRequest ID=% durumu değişti: % → %',
            NEW.id, OLD.request_status, NEW.request_status;
    END IF;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger'ı oluştur
DROP TRIGGER IF EXISTS trg_leave_status_history ON leave_requests;

CREATE TRIGGER trg_leave_status_history
    AFTER UPDATE OF request_status ON leave_requests
    FOR EACH ROW
    WHEN (OLD.request_status IS DISTINCT FROM NEW.request_status)
    EXECUTE FUNCTION fn_log_leave_status_change();

COMMENT ON FUNCTION fn_log_leave_status_change() IS 
    'İzin talebi durumu değiştiğinde leave_approval_history tablosuna kayıt ekler';

COMMENT ON TRIGGER trg_leave_status_history ON leave_requests IS 
    'İzin talebi durum değişikliklerini otomatik olarak denetim tablosuna kaydeder';


-- =====================================================
-- A.2. KULLANICI GİRİŞ DENETİMİ
-- Trigger: trg_log_login_attempt
-- =====================================================
-- AMAÇ:
--   auth_audit_log tablosuna eklenen her kayıt için ek güvenlik
--   analizleri yapmak ve şüpheli aktiviteleri işaretlemek.
--
-- TETİKLEME ZAMANI: AFTER INSERT
--
-- ÇALIŞMA MANTIĞI:
--   1. Yeni giriş denemesini analiz et
--   2. Son 15 dakikada aynı email'den kaç başarısız deneme var?
--   3. 3+ başarısız deneme varsa uyarı ekle
--   4. 5+ başarısız deneme varsa ACCOUNT_LOCKED olarak işaretle
--   5. Farklı IP'lerden gelen denemeleri tespit et
--
-- GÜVENLİK ÖZELLİKLERİ:
--   - Brute force saldırı tespiti
--   - Coğrafi anomali tespiti (farklı IP'ler)
--   - Şüpheli aktivite işaretleme
-- =====================================================

CREATE OR REPLACE FUNCTION fn_process_login_audit()
RETURNS TRIGGER AS $$
DECLARE
    v_recent_failures INTEGER;
    v_distinct_ips INTEGER;
    v_alert_details JSONB;
    v_time_window INTERVAL := INTERVAL '15 minutes';
BEGIN
    -- Sadece başarısız giriş denemeleri için analiz yap
    IF NEW.attempt_type = 'LOGIN_FAILURE' THEN
        
        -- Son 15 dakikadaki başarısız deneme sayısını hesapla
        SELECT COUNT(*) INTO v_recent_failures
        FROM auth_audit_log
        WHERE user_email = NEW.user_email
          AND attempt_type = 'LOGIN_FAILURE'
          AND created_at > (CURRENT_TIMESTAMP - v_time_window)
          AND id != NEW.id;  -- Kendisini hariç tut
        
        -- Son 15 dakikadaki farklı IP sayısını hesapla
        SELECT COUNT(DISTINCT ip_address) INTO v_distinct_ips
        FROM auth_audit_log
        WHERE user_email = NEW.user_email
          AND created_at > (CURRENT_TIMESTAMP - v_time_window);
        
        -- Alert detaylarını hazırla
        v_alert_details := jsonb_build_object(
            'recent_failures', v_recent_failures + 1,
            'distinct_ips', v_distinct_ips,
            'analysis_time', CURRENT_TIMESTAMP,
            'time_window_minutes', 15
        );
        
        -- 5 veya daha fazla başarısız deneme → Hesap kilitleme uyarısı
        IF v_recent_failures >= 4 THEN  -- +1 mevcut = 5
            UPDATE auth_audit_log 
            SET details = COALESCE(details, '{}'::jsonb) || 
                jsonb_build_object(
                    'security_alert', 'ACCOUNT_SHOULD_BE_LOCKED',
                    'alert_level', 'CRITICAL',
                    'message', 'Hesap kilitlenmeli! 5+ başarısız giriş denemesi.',
                    'analysis', v_alert_details
                )
            WHERE id = NEW.id;
            
            RAISE WARNING '[SECURITY] Kritik: % için hesap kilitleme önerisi. % başarısız deneme.',
                NEW.user_email, v_recent_failures + 1;
                
        -- 3-4 başarısız deneme → Uyarı
        ELSIF v_recent_failures >= 2 THEN  -- +1 mevcut = 3-4
            UPDATE auth_audit_log 
            SET details = COALESCE(details, '{}'::jsonb) || 
                jsonb_build_object(
                    'security_alert', 'MULTIPLE_FAILURES',
                    'alert_level', 'WARNING',
                    'message', format('Dikkat! %s başarısız giriş denemesi.', v_recent_failures + 1),
                    'analysis', v_alert_details
                )
            WHERE id = NEW.id;
            
            RAISE NOTICE '[SECURITY] Uyarı: % için çoklu başarısız deneme (%)',
                NEW.user_email, v_recent_failures + 1;
        END IF;
        
        -- Farklı IP'lerden gelen denemeler → Coğrafi anomali
        IF v_distinct_ips > 2 THEN
            UPDATE auth_audit_log 
            SET details = COALESCE(details, '{}'::jsonb) || 
                jsonb_build_object(
                    'geo_anomaly', TRUE,
                    'distinct_ip_count', v_distinct_ips,
                    'anomaly_message', 'Farklı IP adreslerinden giriş denemeleri tespit edildi'
                )
            WHERE id = NEW.id;
            
            RAISE NOTICE '[SECURITY] Coğrafi anomali: % için % farklı IP tespit edildi',
                NEW.user_email, v_distinct_ips;
        END IF;
        
    -- Başarılı giriş → Önceki başarısız denemeleri analiz et
    ELSIF NEW.attempt_type = 'LOGIN_SUCCESS' THEN
        
        -- Son başarısız denemelerin sayısını kontrol et
        SELECT COUNT(*) INTO v_recent_failures
        FROM auth_audit_log
        WHERE user_email = NEW.user_email
          AND attempt_type = 'LOGIN_FAILURE'
          AND created_at > (CURRENT_TIMESTAMP - INTERVAL '1 hour');
        
        -- Eğer öncesinde başarısız denemeler varsa, bunu kaydet
        IF v_recent_failures > 0 THEN
            UPDATE auth_audit_log 
            SET details = COALESCE(details, '{}'::jsonb) || 
                jsonb_build_object(
                    'previous_failures_cleared', v_recent_failures,
                    'message', format('Son 1 saatte %s başarısız denemeden sonra başarılı giriş', v_recent_failures)
                )
            WHERE id = NEW.id;
        END IF;
    END IF;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger'ı oluştur
DROP TRIGGER IF EXISTS trg_log_login_attempt ON auth_audit_log;

CREATE TRIGGER trg_log_login_attempt
    AFTER INSERT ON auth_audit_log
    FOR EACH ROW
    EXECUTE FUNCTION fn_process_login_audit();

COMMENT ON FUNCTION fn_process_login_audit() IS 
    'Giriş denemelerini analiz eder, brute force ve coğrafi anomalileri tespit eder';

COMMENT ON TRIGGER trg_log_login_attempt ON auth_audit_log IS 
    'Her giriş denemesi kaydından sonra güvenlik analizi yapar';


-- #############################################################################
-- B. İŞ KURALI ZORLAMA (ENFORCEMENT) TRIGGER'LARI
-- #############################################################################

-- =====================================================
-- B.1. ÇAKIŞAN İZİN KONTROLÜ
-- Trigger: trg_check_overlapping_leave
-- =====================================================
-- AMAÇ:
--   Yeni bir izin talebi eklenmeden veya güncellenmeden ÖNCE,
--   kullanıcının aynı tarihlerde başka bir aktif izni olup olmadığını kontrol etmek.
--
-- TETİKLEME ZAMANI: BEFORE INSERT OR UPDATE
--
-- ÇALIŞMA MANTIĞI:
--   1. Yeni talebin tarih aralığını al
--   2. Aynı çalışanın diğer aktif izinlerini kontrol et
--   3. Tarih çakışması varsa işlemi ENGELLE
--   4. Çakışma yoksa işleme izin ver
--
-- ÇAKIŞMA FORMÜLÜ:
--   (YeniBaslangic < MevcutBitis) AND (YeniBitis > MevcutBaslangic)
--
-- HARİÇ TUTULAN DURUMLAR:
--   - REJECTED (Reddedilmiş)
--   - CANCELLED (İptal edilmiş)
--   - Kendi kaydı (UPDATE durumunda)
--
-- HATA KODU: P0001 (ÇAKIŞMA_HATASI)
-- =====================================================

CREATE OR REPLACE FUNCTION fn_check_overlapping_leave()
RETURNS TRIGGER AS $$
DECLARE
    v_overlap_count INTEGER;
    v_overlapping_ids TEXT;
    v_excluded_statuses TEXT[] := ARRAY['REJECTED', 'CANCELLED'];
BEGIN
    -- Çakışan izinleri kontrol et
    SELECT 
        COUNT(*),
        STRING_AGG(id::TEXT, ', ')
    INTO v_overlap_count, v_overlapping_ids
    FROM leave_requests lr
    WHERE lr.employee_id = NEW.employee_id
      AND lr.id != COALESCE(NEW.id, 0)  -- UPDATE durumunda kendi kaydını hariç tut
      AND lr.request_status NOT IN (SELECT unnest(v_excluded_statuses))
      AND lr.is_active = TRUE
      -- Çakışma kontrolü: (A.start < B.end) AND (A.end > B.start)
      AND (NEW.start_date_time < lr.end_date_time)
      AND (NEW.end_date_time > lr.start_date_time);
    
    -- Çakışma varsa işlemi engelle
    IF v_overlap_count > 0 THEN
        RAISE EXCEPTION 'ÇAKIŞMA_HATASI: Bu tarih aralığında zaten aktif bir izin kaydınız var! Çalışan ID: %, Talep Edilen: % - %, Çakışan İzin ID(ler): %',
            NEW.employee_id,
            TO_CHAR(NEW.start_date_time, 'DD.MM.YYYY HH24:MI'),
            TO_CHAR(NEW.end_date_time, 'DD.MM.YYYY HH24:MI'),
            v_overlapping_ids
        USING ERRCODE = 'P0001',
              HINT = 'Önce mevcut izin talebinizi iptal edin veya farklı tarihler seçin.';
    END IF;
    
    RAISE NOTICE '[trg_check_overlapping_leave] Çakışma kontrolü geçti. Çalışan ID: %, Tarih: % - %',
        NEW.employee_id, NEW.start_date_time, NEW.end_date_time;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger'ı oluştur
DROP TRIGGER IF EXISTS trg_check_overlapping_leave ON leave_requests;

CREATE TRIGGER trg_check_overlapping_leave
    BEFORE INSERT OR UPDATE OF start_date_time, end_date_time, employee_id ON leave_requests
    FOR EACH ROW
    EXECUTE FUNCTION fn_check_overlapping_leave();

COMMENT ON FUNCTION fn_check_overlapping_leave() IS 
    'Çakışan izin taleplerini veritabanı seviyesinde engeller';

COMMENT ON TRIGGER trg_check_overlapping_leave ON leave_requests IS 
    'İzin talebi oluşturma/güncelleme öncesi tarih çakışması kontrolü yapar';


-- =====================================================
-- B.2. DEPARTMAN SİLME KISITLAMASI
-- Trigger: trg_prevent_dept_delete
-- =====================================================
-- AMAÇ:
--   Bir departman kaydı silinmeden ÖNCE, o departmana bağlı
--   aktif çalışan olup olmadığını kontrol etmek.
--
-- TETİKLEME ZAMANI: BEFORE DELETE
--
-- ÇALIŞMA MANTIĞI:
--   1. Silinecek departmanın ID'sini al
--   2. Bu departmana bağlı aktif çalışan sayısını hesapla
--   3. Aktif çalışan varsa silme işlemini ENGELLE
--   4. Çalışan yoksa silmeye izin ver
--
-- KONTROL EDİLEN DURUMLAR:
--   - Aktif çalışanlar (is_active = TRUE)
--   - Departman yöneticisi olarak atanmış çalışanlar
--
-- HATA KODU: P0002 (DEPARTMAN_SİLİNEMEZ)
-- =====================================================

CREATE OR REPLACE FUNCTION fn_prevent_dept_delete()
RETURNS TRIGGER AS $$
DECLARE
    v_active_employee_count INTEGER;
    v_employee_names TEXT;
    v_is_manager_of_other BOOLEAN;
BEGIN
    -- Bu departmana bağlı aktif çalışan sayısını ve isimlerini al
    SELECT 
        COUNT(*),
        STRING_AGG(first_name || ' ' || last_name, ', ' ORDER BY last_name)
    INTO v_active_employee_count, v_employee_names
    FROM employees
    WHERE department_id = OLD.id
      AND is_active = TRUE;
    
    -- Aktif çalışan varsa silme işlemini engelle
    IF v_active_employee_count > 0 THEN
        RAISE EXCEPTION 'DEPARTMAN_SİLİNEMEZ: Bu departmanda % aktif çalışan bulunmaktadır! Departman: % (ID: %), Çalışanlar: %',
            v_active_employee_count,
            OLD.name,
            OLD.id,
            LEFT(v_employee_names, 200)
        USING ERRCODE = 'P0002',
              HINT = 'Önce çalışanları başka bir departmana taşıyın veya pasif yapın.';
    END IF;
    
    -- Bu departman başka bir departmanın yöneticisi olarak atanmış mı kontrol et
    -- (Circular dependency kontrolü)
    SELECT EXISTS(
        SELECT 1 FROM departments d
        JOIN employees e ON d.manager_personnel_id = e.id
        WHERE e.department_id = OLD.id AND d.id != OLD.id
    ) INTO v_is_manager_of_other;
    
    IF v_is_manager_of_other THEN
        RAISE WARNING '[trg_prevent_dept_delete] Uyarı: % departmanındaki çalışanlar başka departmanların yöneticisi olarak atanmış!',
            OLD.name;
    END IF;
    
    RAISE NOTICE '[trg_prevent_dept_delete] Departman silinebilir: % (ID: %)',
        OLD.name, OLD.id;
    
    RETURN OLD;
END;
$$ LANGUAGE plpgsql;

-- Trigger'ı oluştur
DROP TRIGGER IF EXISTS trg_prevent_dept_delete ON departments;

CREATE TRIGGER trg_prevent_dept_delete
    BEFORE DELETE ON departments
    FOR EACH ROW
    EXECUTE FUNCTION fn_prevent_dept_delete();

COMMENT ON FUNCTION fn_prevent_dept_delete() IS 
    'Aktif çalışanları olan departmanların silinmesini engeller';

COMMENT ON TRIGGER trg_prevent_dept_delete ON departments IS 
    'Departman silme öncesi bağımlılık kontrolü yapar';


-- #############################################################################
-- C. İŞ AKIŞI OTOMASYON (AUTOMATION) TRIGGER'LARI
-- #############################################################################

-- =====================================================
-- C.1. İZİN BAKİYESİ GÜNCELLEMESİ
-- Trigger: trg_update_leave_balance
-- =====================================================
-- AMAÇ:
--   İzin talebi APPROVED durumuna geçtiğinde, ilgili çalışanın
--   yıllık izin bakiyesini otomatik olarak düşürmek.
--   İptal edildiğinde ise bakiyeyi geri almak.
--
-- TETİKLEME ZAMANI: AFTER UPDATE
--
-- ÇALIŞMA MANTIĞI:
--   1. request_status değişikliğini kontrol et
--   2. APPROVED olursa:
--      a. İzin türünün deducts_from_annual özelliğini kontrol et
--      b. TRUE ise leave_entitlements tablosunda hours_used'ı artır
--   3. CANCELLED olursa (önceki durum APPROVED ise):
--      a. Bakiyeyi geri al (hours_used'ı düşür)
--
-- KONTROL EDİLEN DURUMLAR:
--   - deducts_from_annual = TRUE olan izin türleri
--   - Sadece APPROVED durumuna geçişlerde bakiye düşer
--   - APPROVED → CANCELLED geçişlerinde bakiye geri alınır
--
-- ÖNEMLİ NOTLAR:
--   - Bakiye negatife düşemez (GREATEST kullanılır)
--   - Entitlement kaydı yoksa WARNING verilir
--   - Leave year bazında bakiye tutulur
-- =====================================================

CREATE OR REPLACE FUNCTION fn_update_leave_balance()
RETURNS TRIGGER AS $$
DECLARE
    v_leave_type RECORD;
    v_leave_year INTEGER;
    v_entitlement_id BIGINT;
    v_current_hours_used DECIMAL(10,2);
    v_new_hours_used DECIMAL(10,2);
BEGIN
    -- İzin türü bilgilerini al
    SELECT * INTO v_leave_type
    FROM leave_types
    WHERE id = NEW.leave_type_id;
    
    -- İzin yılını belirle (izin başlangıç tarihine göre)
    v_leave_year := EXTRACT(YEAR FROM NEW.start_date_time);
    
    -- =====================================================
    -- DURUM 1: APPROVED durumuna geçiş (bakiye düşür)
    -- =====================================================
    IF OLD.request_status IS DISTINCT FROM 'APPROVED' 
       AND NEW.request_status = 'APPROVED' THEN
        
        -- Sadece yıllık izin bakiyesinden düşen türler için işlem yap
        IF v_leave_type.deducts_from_annual = TRUE THEN
            
            -- Mevcut bakiyeyi al
            SELECT id, hours_used 
            INTO v_entitlement_id, v_current_hours_used
            FROM leave_entitlements
            WHERE employee_id = NEW.employee_id
              AND leave_year = v_leave_year
              AND is_active = TRUE;
            
            -- Entitlement kaydı yoksa uyarı ver
            IF v_entitlement_id IS NULL THEN
                RAISE WARNING '[trg_update_leave_balance] İzin hakkı kaydı bulunamadı! Çalışan ID: %, Yıl: %. Bakiye güncellenemedi.',
                    NEW.employee_id, v_leave_year;
                RETURN NEW;
            END IF;
            
            -- Yeni hours_used değerini hesapla
            v_new_hours_used := v_current_hours_used + NEW.duration_hours;
            
            -- Bakiyeyi güncelle
            UPDATE leave_entitlements
            SET hours_used = v_new_hours_used,
                updated_at = CURRENT_TIMESTAMP
            WHERE id = v_entitlement_id;
            
            RAISE NOTICE '[trg_update_leave_balance] Bakiye güncellendi. Çalışan ID: %, Eski: % saat, Yeni: % saat, Düşülen: % saat',
                NEW.employee_id, v_current_hours_used, v_new_hours_used, NEW.duration_hours;
        ELSE
            RAISE NOTICE '[trg_update_leave_balance] İzin türü (%) yıllık izinden düşmüyor. Bakiye güncellenmedi.',
                v_leave_type.name;
        END IF;
    
    -- =====================================================
    -- DURUM 2: APPROVED → CANCELLED geçişi (bakiye geri al)
    -- =====================================================
    ELSIF OLD.request_status = 'APPROVED' 
          AND NEW.request_status = 'CANCELLED' THEN
        
        IF v_leave_type.deducts_from_annual = TRUE THEN
            
            -- Mevcut bakiyeyi al
            SELECT id, hours_used 
            INTO v_entitlement_id, v_current_hours_used
            FROM leave_entitlements
            WHERE employee_id = NEW.employee_id
              AND leave_year = v_leave_year
              AND is_active = TRUE;
            
            IF v_entitlement_id IS NULL THEN
                RAISE WARNING '[trg_update_leave_balance] İzin hakkı kaydı bulunamadı! Bakiye geri alınamadı. Çalışan ID: %, Yıl: %',
                    NEW.employee_id, v_leave_year;
                RETURN NEW;
            END IF;
            
            -- Yeni hours_used değerini hesapla (negatif olamaz)
            v_new_hours_used := GREATEST(0, v_current_hours_used - NEW.duration_hours);
            
            -- Bakiyeyi geri al
            UPDATE leave_entitlements
            SET hours_used = v_new_hours_used,
                updated_at = CURRENT_TIMESTAMP
            WHERE id = v_entitlement_id;
            
            RAISE NOTICE '[trg_update_leave_balance] Bakiye geri alındı (iptal). Çalışan ID: %, Eski: % saat, Yeni: % saat, Geri Alınan: % saat',
                NEW.employee_id, v_current_hours_used, v_new_hours_used, NEW.duration_hours;
        END IF;
    
    -- =====================================================
    -- DURUM 3: APPROVED → REJECTED geçişi (bakiye geri al)
    -- =====================================================
    ELSIF OLD.request_status = 'APPROVED' 
          AND NEW.request_status = 'REJECTED' THEN
        
        IF v_leave_type.deducts_from_annual = TRUE THEN
            
            SELECT id, hours_used 
            INTO v_entitlement_id, v_current_hours_used
            FROM leave_entitlements
            WHERE employee_id = NEW.employee_id
              AND leave_year = v_leave_year
              AND is_active = TRUE;
            
            IF v_entitlement_id IS NOT NULL THEN
                v_new_hours_used := GREATEST(0, v_current_hours_used - NEW.duration_hours);
                
                UPDATE leave_entitlements
                SET hours_used = v_new_hours_used,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = v_entitlement_id;
                
                RAISE NOTICE '[trg_update_leave_balance] Bakiye geri alındı (red). Çalışan ID: %, Geri Alınan: % saat',
                    NEW.employee_id, NEW.duration_hours;
            END IF;
        END IF;
    END IF;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger'ı oluştur
DROP TRIGGER IF EXISTS trg_update_leave_balance ON leave_requests;

CREATE TRIGGER trg_update_leave_balance
    AFTER UPDATE OF request_status ON leave_requests
    FOR EACH ROW
    WHEN (OLD.request_status IS DISTINCT FROM NEW.request_status)
    EXECUTE FUNCTION fn_update_leave_balance();

COMMENT ON FUNCTION fn_update_leave_balance() IS 
    'İzin onaylandığında bakiyeyi düşürür, iptal/red edildiğinde geri alır';

COMMENT ON TRIGGER trg_update_leave_balance ON leave_requests IS 
    'İzin durumu değiştiğinde yıllık izin bakiyesini otomatik günceller';


-- #############################################################################
-- D. YARDIMCI TRIGGER'LAR
-- #############################################################################

-- =====================================================
-- D.1. UPDATED_AT OTOMATİK GÜNCELLEMESİ
-- =====================================================
-- AMAÇ:
--   Tüm tablolarda updated_at alanını otomatik güncellemek
-- =====================================================

CREATE OR REPLACE FUNCTION fn_update_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Her ana tablo için updated_at trigger'ı oluştur
DO $$
DECLARE
    t TEXT;
    table_names TEXT[] := ARRAY[
        'departments', 'employees', 'users', 'roles', 'permissions',
        'leave_types', 'leave_requests', 'leave_approval_history',
        'leave_attachments', 'leave_entitlements', 'public_holidays', 'sprints'
    ];
BEGIN
    FOREACH t IN ARRAY table_names
    LOOP
        -- Tablo var mı kontrol et
        IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = t) THEN
            EXECUTE format('DROP TRIGGER IF EXISTS trg_%s_updated_at ON %I', t, t);
            EXECUTE format('
                CREATE TRIGGER trg_%s_updated_at
                    BEFORE UPDATE ON %I
                    FOR EACH ROW
                    EXECUTE FUNCTION fn_update_timestamp()
            ', t, t);
            RAISE NOTICE 'Trigger oluşturuldu: trg_%_updated_at', t;
        ELSE
            RAISE NOTICE 'Tablo bulunamadı, trigger oluşturulmadı: %', t;
        END IF;
    END LOOP;
END;
$$;

COMMENT ON FUNCTION fn_update_timestamp() IS 
    'UPDATE işlemlerinde updated_at alanını otomatik günceller';


-- #############################################################################
-- E. KRİTİK VERİ BÜTÜNLÜĞÜ TRIGGER'LARI
-- #############################################################################

-- =====================================================
-- E.1. BAKİYE AŞIMI KONTROLÜ
-- Trigger: trg_prevent_negative_balance
-- =====================================================
-- AMAÇ:
--   hours_used değerinin total_hours_entitled + carried_forward_hours'u
--   aşmasını engellemek (negatif bakiye olmaması için)
-- =====================================================

CREATE OR REPLACE FUNCTION fn_prevent_negative_balance()
RETURNS TRIGGER AS $$
DECLARE
    v_max_hours DECIMAL(10,2);
BEGIN
    -- Maksimum kullanılabilir saat
    v_max_hours := NEW.total_hours_entitled + NEW.carried_forward_hours;
    
    -- Aşım kontrolü
    IF NEW.hours_used > v_max_hours THEN
        RAISE EXCEPTION 'BAKİYE_AŞIMI: Kullanılan saat (%) toplam haktan (%) fazla olamaz! Çalışan ID: %, Yıl: %',
            NEW.hours_used,
            v_max_hours,
            NEW.employee_id,
            NEW.leave_year
        USING ERRCODE = 'P0003',
              HINT = 'İzin bakiyesi yetersiz. Lütfen bakiyenizi kontrol edin.';
    END IF;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_prevent_negative_balance ON leave_entitlements;

CREATE TRIGGER trg_prevent_negative_balance
    BEFORE INSERT OR UPDATE OF hours_used ON leave_entitlements
    FOR EACH ROW
    EXECUTE FUNCTION fn_prevent_negative_balance();

COMMENT ON TRIGGER trg_prevent_negative_balance ON leave_entitlements IS 
    'İzin bakiyesinin negatife düşmesini engeller';


-- =====================================================
-- E.2. ÇALIŞAN SİLME KISITLAMASI
-- Trigger: trg_prevent_employee_delete
-- =====================================================
-- AMAÇ:
--   Aktif izin talepleri olan çalışanın silinmesini engellemek
-- =====================================================

CREATE OR REPLACE FUNCTION fn_prevent_employee_delete()
RETURNS TRIGGER AS $$
DECLARE
    v_active_leave_count INTEGER;
    v_pending_leave_ids TEXT;
BEGIN
    -- Aktif izin taleplerini say
    SELECT 
        COUNT(*),
        STRING_AGG(id::TEXT, ', ')
    INTO v_active_leave_count, v_pending_leave_ids
    FROM leave_requests
    WHERE employee_id = OLD.id
      AND request_status NOT IN ('REJECTED', 'CANCELLED')
      AND is_active = TRUE;
    
    IF v_active_leave_count > 0 THEN
        RAISE EXCEPTION 'ÇALIŞAN_SİLİNEMEZ: Bu çalışanın % aktif izin talebi bulunmaktadır! Çalışan: % % (ID: %), İzin ID(ler): %',
            v_active_leave_count,
            OLD.first_name,
            OLD.last_name,
            OLD.id,
            v_pending_leave_ids
        USING ERRCODE = 'P0004',
              HINT = 'Önce izin taleplerini iptal edin veya tamamlayın.';
    END IF;
    
    RETURN OLD;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_prevent_employee_delete ON employees;

CREATE TRIGGER trg_prevent_employee_delete
    BEFORE DELETE ON employees
    FOR EACH ROW
    EXECUTE FUNCTION fn_prevent_employee_delete();

COMMENT ON TRIGGER trg_prevent_employee_delete ON employees IS 
    'Aktif izin talepleri olan çalışanların silinmesini engeller';


-- =====================================================
-- E.3. GEÇMİŞ TARİHLİ İZİN ENGELLEMESİ
-- Trigger: trg_validate_leave_dates
-- =====================================================
-- AMAÇ:
--   Başlangıç tarihi bugünden önce olan izin talebi oluşturulmasını engellemek
--   (Sadece INSERT için, UPDATE'de eski taleplerin düzenlenmesine izin ver)
-- =====================================================

CREATE OR REPLACE FUNCTION fn_validate_leave_dates()
RETURNS TRIGGER AS $$
BEGIN
    -- Sadece yeni kayıtlar için geçmiş tarih kontrolü
    IF TG_OP = 'INSERT' THEN
        IF NEW.start_date_time < CURRENT_TIMESTAMP THEN
            RAISE EXCEPTION 'GEÇMİŞ_TARİH_HATASI: İzin başlangıç tarihi (%) geçmiş bir tarih olamaz! Şu anki zaman: %',
                TO_CHAR(NEW.start_date_time, 'DD.MM.YYYY HH24:MI'),
                TO_CHAR(CURRENT_TIMESTAMP, 'DD.MM.YYYY HH24:MI')
            USING ERRCODE = 'P0005',
                  HINT = 'Lütfen bugün veya gelecek bir tarih seçin.';
        END IF;
    END IF;
    
    -- Bitiş tarihi başlangıçtan önce olamaz (ek güvenlik)
    IF NEW.end_date_time <= NEW.start_date_time THEN
        RAISE EXCEPTION 'TARİH_SIRASI_HATASI: Bitiş tarihi (%) başlangıç tarihinden (%) sonra olmalıdır!',
            TO_CHAR(NEW.end_date_time, 'DD.MM.YYYY HH24:MI'),
            TO_CHAR(NEW.start_date_time, 'DD.MM.YYYY HH24:MI')
        USING ERRCODE = 'P0006',
              HINT = 'Bitiş tarihini düzeltin.';
    END IF;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_validate_leave_dates ON leave_requests;

CREATE TRIGGER trg_validate_leave_dates
    BEFORE INSERT ON leave_requests
    FOR EACH ROW
    EXECUTE FUNCTION fn_validate_leave_dates();

COMMENT ON TRIGGER trg_validate_leave_dates ON leave_requests IS 
    'Geçmiş tarihli izin taleplerini engeller';


-- =====================================================
-- E.4. KENDİ İZNİNİ ONAYLAMA ENGELİ
-- Trigger: trg_prevent_self_approval
-- =====================================================
-- AMAÇ:
--   Çalışanın kendi izin talebini onaylayamamasını sağlamak
--   (leave_approval_history tablosunda kontrol)
-- =====================================================

CREATE OR REPLACE FUNCTION fn_prevent_self_approval()
RETURNS TRIGGER AS $$
DECLARE
    v_request_owner_id BIGINT;
BEGIN
    -- employee_id NULL ise (trigger tarafından oluşturulan kayıt), kontrolü atla
    IF NEW.employee_id IS NULL THEN
        RETURN NEW;
    END IF;
    
    -- İzin talebinin sahibini bul
    SELECT employee_id INTO v_request_owner_id
    FROM leave_requests
    WHERE id = NEW.request_id;
    
    -- Onaylayan kişi izin sahibiyle aynı mı?
    IF NEW.employee_id = v_request_owner_id AND NEW.action IN ('APPROVED', 'APPROVED_HR', 'APPROVED_MANAGER') THEN
        RAISE EXCEPTION 'KENDİ_ONAY_HATASI: Kendi izin talebinizi onaylayamazsınız! İzin Talebi ID: %, Çalışan ID: %',
            NEW.request_id,
            NEW.employee_id
        USING ERRCODE = 'P0007',
              HINT = 'Başka bir yetkili tarafından onaylanmalıdır.';
    END IF;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_prevent_self_approval ON leave_approval_history;

CREATE TRIGGER trg_prevent_self_approval
    BEFORE INSERT ON leave_approval_history
    FOR EACH ROW
    EXECUTE FUNCTION fn_prevent_self_approval();

COMMENT ON TRIGGER trg_prevent_self_approval ON leave_approval_history IS 
    'Çalışanların kendi izin taleplerini onaylamasını engeller';


-- =====================================================
-- E.5. MAKSİMUM ARDIŞIK İZİN KONTROLÜ
-- Trigger: trg_max_consecutive_leave
-- =====================================================
-- AMAÇ:
--   Tek seferde maksimum 30 günden fazla izin talep edilememesi
-- =====================================================

CREATE OR REPLACE FUNCTION fn_max_consecutive_leave()
RETURNS TRIGGER AS $$
DECLARE
    v_max_days INTEGER := 30;
    v_requested_days INTEGER;
BEGIN
    -- Talep edilen gün sayısını hesapla
    v_requested_days := EXTRACT(DAY FROM (NEW.end_date_time - NEW.start_date_time))::INTEGER + 1;
    
    IF v_requested_days > v_max_days THEN
        RAISE EXCEPTION 'MAKSİMUM_İZİN_AŞIMI: Tek seferde en fazla % gün izin talep edebilirsiniz! Talep edilen: % gün',
            v_max_days,
            v_requested_days
        USING ERRCODE = 'P0008',
              HINT = 'İzin talebinizi bölerek daha kısa süreli talepler oluşturun.';
    END IF;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_max_consecutive_leave ON leave_requests;

CREATE TRIGGER trg_max_consecutive_leave
    BEFORE INSERT OR UPDATE OF start_date_time, end_date_time ON leave_requests
    FOR EACH ROW
    EXECUTE FUNCTION fn_max_consecutive_leave();

COMMENT ON TRIGGER trg_max_consecutive_leave ON leave_requests IS 
    'Tek seferde maksimum 30 gün izin kuralını uygular';


-- =====================================================
-- E.6. MİNİMUM ÖNCEDEN BİLDİRİM SÜRESİ
-- Trigger: trg_min_leave_notice
-- =====================================================
-- AMAÇ:
--   İzin talebinin en az 1 gün önceden yapılmasını zorunlu kılmak
--   (Acil durumlar hariç - belge gerektiren izinler muaf)
-- =====================================================

CREATE OR REPLACE FUNCTION fn_min_leave_notice()
RETURNS TRIGGER AS $$
DECLARE
    v_min_notice_hours INTEGER := 24;  -- 1 gün önceden
    v_hours_until_start DECIMAL;
    v_requires_document BOOLEAN;
BEGIN
    -- Belge gerektiren izin türlerini (hastalık vb.) muaf tut
    SELECT document_required INTO v_requires_document
    FROM leave_types
    WHERE id = NEW.leave_type_id;
    
    -- Belge gerektiren izinler acil olabilir, muaf tut
    IF v_requires_document = TRUE THEN
        RETURN NEW;
    END IF;
    
    -- Başlangıca kaç saat var?
    v_hours_until_start := EXTRACT(EPOCH FROM (NEW.start_date_time - CURRENT_TIMESTAMP)) / 3600;
    
    IF v_hours_until_start < v_min_notice_hours THEN
        RAISE EXCEPTION 'ERKEN_BİLDİRİM_HATASI: İzin talebi en az % saat önceden yapılmalıdır! Kalan süre: % saat',
            v_min_notice_hours,
            ROUND(v_hours_until_start::NUMERIC, 1)
        USING ERRCODE = 'P0009',
              HINT = 'Acil durumlarda belge gerektiren izin türünü seçin.';
    END IF;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_min_leave_notice ON leave_requests;

CREATE TRIGGER trg_min_leave_notice
    BEFORE INSERT ON leave_requests
    FOR EACH ROW
    EXECUTE FUNCTION fn_min_leave_notice();

COMMENT ON TRIGGER trg_min_leave_notice ON leave_requests IS 
    'Minimum 24 saat önceden bildirim kuralını uygular';


-- #############################################################################
-- F. OTOMASYON TRIGGER'LARI
-- #############################################################################

-- =====================================================
-- F.1. YENİ ÇALIŞAN İÇİN OTOMATİK İZİN HAKKI
-- Trigger: trg_auto_create_entitlement
-- =====================================================
-- AMAÇ:
--   Yeni çalışan oluşturulduğunda otomatik olarak
--   mevcut yıl için izin hakkı kaydı oluşturmak
-- =====================================================

CREATE OR REPLACE FUNCTION fn_auto_create_entitlement()
RETURNS TRIGGER AS $$
DECLARE
    v_current_year INTEGER;
    v_base_hours DECIMAL(10,2) := 112;  -- 14 gün × 8 saat = temel hak
BEGIN
    v_current_year := EXTRACT(YEAR FROM CURRENT_DATE);
    
    -- Yeni çalışan için izin hakkı oluştur
    INSERT INTO leave_entitlements (
        employee_id,
        leave_year,
        total_hours_entitled,
        hours_used,
        carried_forward_hours,
        is_active,
        created_at,
        updated_at
    ) VALUES (
        NEW.id,
        v_current_year,
        v_base_hours,
        0,
        0,
        TRUE,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    );
    
    RAISE NOTICE '[trg_auto_create_entitlement] Çalışan ID: % için % yılı izin hakkı oluşturuldu (% saat)',
        NEW.id, v_current_year, v_base_hours;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_auto_create_entitlement ON employees;

CREATE TRIGGER trg_auto_create_entitlement
    AFTER INSERT ON employees
    FOR EACH ROW
    EXECUTE FUNCTION fn_auto_create_entitlement();

COMMENT ON TRIGGER trg_auto_create_entitlement ON employees IS 
    'Yeni çalışan için otomatik izin hakkı kaydı oluşturur';


-- =====================================================
-- F.2. EMAIL FORMAT KONTROLÜ
-- Trigger: trg_validate_email_format
-- =====================================================
-- AMAÇ:
--   Email adresinin geçerli formatta olmasını kontrol etmek
-- =====================================================

CREATE OR REPLACE FUNCTION fn_validate_email_format()
RETURNS TRIGGER AS $$
BEGIN
    -- Basit email format kontrolü
    IF NEW.email !~ '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$' THEN
        RAISE EXCEPTION 'GEÇERSİZ_EMAIL: Email formatı hatalı: %',
            NEW.email
        USING ERRCODE = 'P0010',
              HINT = 'Geçerli bir email adresi girin (örn: ad.soyad@sirket.com)';
    END IF;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_validate_email_format ON employees;

CREATE TRIGGER trg_validate_email_format
    BEFORE INSERT OR UPDATE OF email ON employees
    FOR EACH ROW
    EXECUTE FUNCTION fn_validate_email_format();

COMMENT ON TRIGGER trg_validate_email_format ON employees IS 
    'Email formatının geçerliliğini kontrol eder';


-- #############################################################################
-- G. CHECK CONSTRAINT'LER (ALTER TABLE)
-- #############################################################################

-- =====================================================
-- G.1. YAŞ KONTROLÜ - 18 yaşından büyük olmalı
-- =====================================================

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_employee_min_age'
    ) THEN
        ALTER TABLE employees
        ADD CONSTRAINT chk_employee_min_age
        CHECK (birth_date <= CURRENT_DATE - INTERVAL '18 years');
        
        RAISE NOTICE 'CHECK constraint eklendi: chk_employee_min_age';
    END IF;
END;
$$;

-- =====================================================
-- G.2. İŞE GİRİŞ TARİHİ - Gelecekte olamaz
-- =====================================================

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_hire_date_not_future'
    ) THEN
        ALTER TABLE employees
        ADD CONSTRAINT chk_hire_date_not_future
        CHECK (hire_date <= CURRENT_DATE);
        
        RAISE NOTICE 'CHECK constraint eklendi: chk_hire_date_not_future';
    END IF;
END;
$$;

-- =====================================================
-- G.3. GÜNLÜK ÇALIŞMA SAATİ - 0-24 saat arası
-- =====================================================

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_daily_work_hours_range'
    ) THEN
        ALTER TABLE employees
        ADD CONSTRAINT chk_daily_work_hours_range
        CHECK (daily_work_hours >= 0 AND daily_work_hours <= 24);
        
        RAISE NOTICE 'CHECK constraint eklendi: chk_daily_work_hours_range';
    END IF;
END;
$$;

-- =====================================================
-- G.4. İZİN YILI - Mantıklı aralıkta olmalı
-- =====================================================

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_leave_year_range'
    ) THEN
        ALTER TABLE leave_entitlements
        ADD CONSTRAINT chk_leave_year_range
        CHECK (leave_year >= 2000 AND leave_year <= 2100);
        
        RAISE NOTICE 'CHECK constraint eklendi: chk_leave_year_range';
    END IF;
END;
$$;

-- =====================================================
-- G.5. BAŞARISIZ GİRİŞ SAYISI - Negatif olamaz
-- =====================================================

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_failed_login_attempts'
    ) THEN
        ALTER TABLE users
        ADD CONSTRAINT chk_failed_login_attempts
        CHECK (failed_login_attempts >= 0);
        
        RAISE NOTICE 'CHECK constraint eklendi: chk_failed_login_attempts';
    END IF;
END;
$$;


-- =====================================================
-- TRIGGER ÖZETİ (GÜNCELLENMİŞ)
-- =====================================================
-- 
-- A. DENETİM TRIGGER'LARI:
--    1. trg_leave_status_history     → İzin durumu değişikliği kaydı
--    2. trg_log_login_attempt        → Giriş denemesi güvenlik analizi
--
-- B. İŞ KURALI TRIGGER'LARI:
--    3. trg_check_overlapping_leave  → Çakışan izin engelleme
--    4. trg_prevent_dept_delete      → Departman silme kısıtlaması
--
-- C. OTOMASYON TRIGGER'LARI:
--    5. trg_update_leave_balance     → İzin bakiyesi otomasyonu
--
-- D. YARDIMCI TRIGGER'LAR:
--    6. trg_*_updated_at             → Timestamp otomasyonu
--
-- E. KRİTİK VERİ BÜTÜNLÜĞÜ:
--    7. trg_prevent_negative_balance → Bakiye aşımı engelleme
--    8. trg_prevent_employee_delete  → Çalışan silme kısıtlaması
--    9. trg_validate_leave_dates     → Geçmiş tarih engelleme
--   10. trg_prevent_self_approval    → Kendi onay engeli
--   11. trg_max_consecutive_leave    → Maks 30 gün kuralı
--   12. trg_min_leave_notice         → Min 24 saat bildirim
--
-- F. OTOMASYON:
--   13. trg_auto_create_entitlement  → Otomatik izin hakkı
--   14. trg_validate_email_format    → Email format kontrolü
--
-- G. CHECK CONSTRAINT'LER:
--   15. chk_employee_min_age         → 18 yaş kontrolü
--   16. chk_hire_date_not_future     → İşe giriş tarihi kontrolü
--   17. chk_daily_work_hours_range   → Günlük saat aralığı
--   18. chk_leave_year_range         → İzin yılı aralığı
--   19. chk_failed_login_attempts    → Başarısız giriş sayısı
--
-- =====================================================

