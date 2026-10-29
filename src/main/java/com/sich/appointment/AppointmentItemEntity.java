package com.sich.appointment;

import java.math.BigDecimal;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import com.sich.common.base.AbstractEntity;
import com.sich.product.ProductEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Serviço de um agendamento. Nome, preço e duração são copiados do produto no momento
 * do agendamento, para o histórico não mudar quando o prestador alterar o produto.
 * Se o produto for excluído, {@code productid} vira nulo e a cópia continua.
 */
@Getter
@Setter
@Entity
@Table(name = "appointmentitems")
public class AppointmentItemEntity extends AbstractEntity {

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "appointmentid", nullable = false)
    private AppointmentEntity appointment;

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @JoinColumn(name = "productid")
    private ProductEntity product;

    @Column(name = "productname", nullable = false, length = 120)
    private String productName;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "durationminutes", nullable = false)
    private Integer durationMinutes;

    /** Ordem em que o serviço será prestado dentro do agendamento (0, 1, 2...). */
    @Column(nullable = false)
    private Integer position;
}
