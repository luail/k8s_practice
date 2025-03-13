package com.example.ordersystem.product.domain;

import com.example.ordersystem.common.domain.BaseTimeEntity;
import com.example.ordersystem.product.dtos.ProductResDto;
import jakarta.persistence.*;
import lombok.*;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@ToString
public class Product extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String category;
    private Integer price;
    private Integer stockQuantity;
    private String imagePath;

    private String memberEmail;

    public ProductResDto fromEntity() {
        return ProductResDto.builder().id(this.id).price(this.price).name(this.name).category(this.category).stockQuantity(this.stockQuantity).imagePath(this.imagePath).build();
    }

    public void updateImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public void updateStockQuantity(int quantity) {
        this.stockQuantity -= quantity;
    }

    public void orderCancel(Integer quantity) {
        this.stockQuantity += quantity;
    }
}
