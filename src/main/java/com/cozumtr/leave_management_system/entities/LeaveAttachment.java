package com.cozumtr.leave_management_system.entities;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "leave_attachments")
@Getter
@Setter
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class LeaveAttachment extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    // --- İLİŞKİ ---

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id", nullable = false)
    @ToString.Exclude
    private LeaveRequest leaveRequest;

    // --- DOSYA BİLGİLERİ ---

    // Dosyanın orijinal adı (Örn: "doktor_raporu.pdf")
    @Column(name = "file_name", nullable = false)
    private String fileName;

    // Sunucudaki tam yolu (Örn: "/uploads/2025/10/rapor_123.pdf")
    // Veritabanına dosyanın kendisini (BLOB) DEĞİL, yolunu kaydediyoruz.
    // Bu, veritabanı performansını koruyan en doğru yaklaşımdır. ✅
    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath;

    // Dosya Tipi (Örn: "application/pdf")
    // İndirirken tarayıcıya "Bu bir PDF'tir" diyebilmek için lazım.
    @Column(name = "file_type", length = 50)
    private String fileType;

    @Column(name = "upload_date", nullable = false)
    private LocalDateTime uploadDate;
}
