package com.transitfare.transit.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(schema = "transit", name = "transfer_discount")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class TransferDiscount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "from_operator_code", nullable = false)
    private String fromOperatorCode;

    @Column(name = "to_operator_code", nullable = false)
    private String toOperatorCode;

    @Column(name = "discount", nullable = false)
    private Integer discount;

    @Column(name = "note")
    private String note;

    @Column(name = "source_url", nullable = false)
    private String sourceUrl;

    @Column(name = "verified_on", nullable = false)
    private LocalDate verifiedOn;
}
