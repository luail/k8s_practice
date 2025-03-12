package com.example.ordersystem.product.service;

import com.example.ordersystem.common.service.StockInventoryService;
import com.example.ordersystem.member.domain.Member;
import com.example.ordersystem.member.repository.MemberRepository;
import com.example.ordersystem.product.domain.Product;
import com.example.ordersystem.product.dtos.ProductRegisterDto;
import com.example.ordersystem.product.dtos.ProductResDto;
import com.example.ordersystem.product.dtos.ProductSerchDto;
import com.example.ordersystem.product.repository.ProductRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
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
    private final MemberRepository memberRepository;
    private final S3Client s3Client;
    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    private final StockInventoryService stockInventoryService;

    public ProductService(ProductRepository productRepository, MemberRepository memberRepository, S3Client s3Client, StockInventoryService stockInventoryService) {
        this.productRepository = productRepository;
        this.memberRepository = memberRepository;
        this.s3Client = s3Client;
        this.stockInventoryService = stockInventoryService;
    }

    public Product productCreate(ProductRegisterDto dto) {
        try {
            // member 조회
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            Member member = memberRepository.findByEmail(authentication.getName())
                    .orElseThrow(() -> new EntityNotFoundException("member is not found"));

            Product product = productRepository.save(dto.toEntity(member));

            // redis 재고에 추가
            stockInventoryService.increseStock(product.getId(), dto.getStockQuantity());

            // aws에 image 저장 후 url 추출
            MultipartFile image = dto.getProductImage();
            byte[] bytes = image.getBytes();
            String fileName = product.getId() + "_" + image.getOriginalFilename();

            // S3 업로드 요청 객체 생성
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(fileName)
                    .build();

            // S3에 바이트 배열을 바로 업로드
            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(bytes));

            // S3 URL 추출 후 업데이트
            String s3Url = s3Client.utilities().getUrl(a -> a.bucket(bucket).key(fileName)).toExternalForm();
            product.updateImagePath(s3Url);

            return product;
        } catch (IOException e) {
            // redis는 트랜잭션의 대상이 아니므로, 에러 시 별도의 decrease 작업 필요
            throw new RuntimeException("이미지 저장 실패", e);
        }
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
}
