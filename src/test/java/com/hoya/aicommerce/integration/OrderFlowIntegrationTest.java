package com.hoya.aicommerce.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hoya.aicommerce.cart.application.CartService;
import com.hoya.aicommerce.cart.application.dto.AddCartItemCommand;
import com.hoya.aicommerce.catalog.application.ProductService;
import com.hoya.aicommerce.catalog.application.dto.CreateProductCommand;
import com.hoya.aicommerce.catalog.application.dto.ProductResult;
import com.hoya.aicommerce.member.application.MemberService;
import com.hoya.aicommerce.member.application.dto.LoginResult;
import com.hoya.aicommerce.member.application.dto.RegisterMemberCommand;
import com.hoya.aicommerce.order.presentation.request.CreateOrderRequest;
import com.hoya.aicommerce.order.presentation.request.OrderItemRequest;
import com.hoya.aicommerce.seller.application.SellerService;
import com.hoya.aicommerce.seller.application.dto.RegisterSellerCommand;
import com.hoya.aicommerce.seller.application.dto.SellerResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class OrderFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private MemberService memberService;

    @Autowired
    private SellerService sellerService;

    @Autowired
    private ProductService productService;

    @Autowired
    private CartService cartService;

    private String buyerToken;
    private Long buyerMemberId;
    private Long productId;

    @BeforeEach
    void setUp() {
        // 구매자 등록
        memberService.registerMember(new RegisterMemberCommand("buyer@example.com", "password123", "구매자"));
        LoginResult buyerLogin = memberService.login(
                new com.hoya.aicommerce.member.application.dto.LoginMemberCommand("buyer@example.com", "password123")
        );
        buyerToken = buyerLogin.token();
        buyerMemberId = buyerLogin.memberId();

        // 판매자 등록 및 승인
        memberService.registerMember(new RegisterMemberCommand("seller@example.com", "password123", "판매자"));
        LoginResult sellerLogin = memberService.login(
                new com.hoya.aicommerce.member.application.dto.LoginMemberCommand("seller@example.com", "password123")
        );
        SellerResult seller = sellerService.registerSeller(
                new RegisterSellerCommand(sellerLogin.memberId(), "테스트 사업자", "국민은행 123-456")
        );
        sellerService.approveSeller(seller.id());

        // 상품 등록
        ProductResult product = productService.createProduct(
                new CreateProductCommand("테스트 상품", "설명", BigDecimal.valueOf(5000), 50, seller.id())
        );
        productId = product.id();
    }

    @Test
    void 상품_장바구니_추가_주문_생성_재고_차감_확인() throws Exception {
        // 장바구니에 상품 추가
        cartService.addItem(new AddCartItemCommand(buyerMemberId, productId, 2));

        // 주문 생성
        CreateOrderRequest orderRequest = new CreateOrderRequest(
                List.of(new OrderItemRequest(productId, 2))
        );

        mockMvc.perform(post("/orders")
                        .header("Authorization", "Bearer " + buyerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("CREATED"))
                .andExpect(jsonPath("$.data.totalAmount").value(10000))
                .andExpect(jsonPath("$.data.items[0].productId").value(productId));

        // 재고 차감 확인
        mockMvc.perform(get("/products/" + productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.stockQuantity").value(48));
    }
}
