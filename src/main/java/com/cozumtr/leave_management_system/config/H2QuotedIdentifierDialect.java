package com.cozumtr.leave_management_system.config;

import org.hibernate.dialect.H2Dialect;

/**
 * H2 için quoted identifier desteği sağlayan custom dialect.
 * PostgreSQL'den H2'ye geçiş yaparken case-sensitive tablo ve sütun isimlerinin
 * doğru şekilde işlenmesi için kullanılır.
 * 
 * Ayrıca H2'nin desteklemediği CASCADE ifadesini DROP TABLE komutlarından kaldırır.
 */
public class H2QuotedIdentifierDialect extends H2Dialect {

    @Override
    public boolean supportsIfExistsBeforeTableName() {
        return true;
    }

    @Override
    public boolean supportsIfExistsAfterTableName() {
        return true;
    }

    @Override
    public boolean supportsIfExistsBeforeConstraintName() {
        return true;
    }

    /**
     * H2, DROP TABLE komutlarında CASCADE'i desteklemez.
     * Bu yüzden CASCADE ifadesini kaldırarak sadece "DROP TABLE IF EXISTS table_name" 
     * formatını döndürüyoruz.
     */
    @Override
    public String getDropTableString(String tableName) {
        return "drop table if exists " + tableName;
    }
}

