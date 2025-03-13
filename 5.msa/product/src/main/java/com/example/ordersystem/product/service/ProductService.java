package com.example.ordersystem.product.service;

import com.example.ordersystem.common.service.StockInventoryService;
import com.example.ordersystem.product.domain.Product;
import com.example.ordersystem.product.dtos.ProductRegisterDto;
import com.example.ordersystem.product.dtos.ProductResDto;
import com.example.ordersystem.product.dtos.ProductSerchDto;
import com.example.ordersystem.product.dtos.ProductUpdateStockDto;
import com.example.ordersystem.product.repository.ProductRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.apache.kafka.common.protocol.Message;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class ProductService {
    private final ProductRepository productRepository;

    private final StockInventoryService stockInventoryService;

    public ProductService(ProductRepository productRepository, StockInventoryService stockInventoryService) {
        this.productRepository = productRepository;
        this.stockInventoryService = stockInventoryService;
    }

    public Product productCreate(ProductRegisterDto dto) {
//         try {
//             //        member조회
//             Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

//             Product product = productRepository.save(dto.toEntity(authentication.getName()));
// //            redis 재고에 추가
//             stockInventoryService.increseStock(product.getId(), dto.getStockQuantity());

// //        aws에 image 저장 후에 url추출
// //        aws의 s3접근가능한 iam계정생성, iam계정을 통해 aws에 접근가능한 접근객체 생성
//             MultipartFile image = dto.getProductImage();
//             byte[] bytes = image.getBytes();
//             String filename = product.getId() + "_" + image.getOriginalFilename();
// //        먼저 local에 저장.
//             Path path = Paths.get("C:/Temp/temp", filename);
//             Files.write(path, bytes, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
//             return product;
//         } catch (IOException e) {
// //            redis는 트랜잭션의 대상이 아니므로, 에러시 별도의 decrease작업이 필요함.
//             throw new RuntimeException("이미지 저장 실패");
//         }
        return null;
    }

    public Page<ProductResDto> findAll(Pageable pageable, ProductSerchDto serchDto) {
//        검색을 위해 Specification 객체 사용
//        Specification객체는 복잡한 쿼리를 명세를 이용하여 정의하는 방식으로, 쿼리를 쉽게 생성.
        Specification<Product> specification = new Specification<Product>() {
            @Override
            public Predicate toPredicate(Root<Product> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
//                root : 엔티티의 속성을 접근하기 위한 객체, criteriaBuilder : 쿼리를 생성하기 위한 객체
                List<Predicate> predicates = new ArrayList<>();
                if (serchDto.getCategory() != null) {
                    predicates.add(criteriaBuilder.equal(root.get("category"), serchDto.getCategory()));
                }
                if (serchDto.getProductName() != null) {
                    predicates.add(criteriaBuilder.like(root.get("name"), "%"+serchDto.getProductName()+"%"));
                }
                Predicate[] predicatesArr = new Predicate[predicates.size()];
                for (int i = 0; i < predicates.size(); i++) {
                    predicatesArr[i] = predicates.get(i);
                }
                Predicate predicate = criteriaBuilder.and(predicatesArr);
                return predicate;
            }
        };

       Page<Product> productList = productRepository.findAll(specification, pageable);
       return productList.map(p->p.fromEntity());
    }

    public ProductResDto productDetail(Long id) {
        Product product = productRepository.findById(id).orElseThrow(()->new EntityNotFoundException("product not found"));
        return product.fromEntity();
    }

    public Product updateStockQuantity(ProductUpdateStockDto dto) {
        System.out.println(dto);
        Product product = productRepository.findById(dto.getProductId()).orElseThrow(()->new EntityNotFoundException("product is not found"));
        product.updateStockQuantity(dto.getProductQuantity());
        return product;
    }

    @KafkaListener(topics = "update-stock-topic", containerFactory = "kafkaListener")
    public void productConsumer(String message) {

        System.out.println("컨슈머 메세지 수신 : " + message);
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            ProductUpdateStockDto dto = objectMapper.readValue(message, ProductUpdateStockDto.class);
            this.updateStockQuantity(dto);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

    }
}
