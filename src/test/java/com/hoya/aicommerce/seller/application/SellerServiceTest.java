package com.hoya.aicommerce.seller.application;

import com.hoya.aicommerce.member.domain.Member;
import com.hoya.aicommerce.member.domain.MemberRepository;
import com.hoya.aicommerce.member.domain.MemberRole;
import com.hoya.aicommerce.seller.application.dto.RegisterSellerCommand;
import com.hoya.aicommerce.seller.application.dto.SellerResult;
import com.hoya.aicommerce.seller.domain.Seller;
import com.hoya.aicommerce.seller.domain.SellerRepository;
import com.hoya.aicommerce.seller.domain.SellerStatus;
import com.hoya.aicommerce.seller.exception.SellerException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SellerServiceTest {

    @Mock
    private SellerRepository sellerRepository;

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private SellerService sellerService;

    @Test
    void 판매자_등록이_정상_처리된다() {
        Member member = Member.create("test@example.com", "encoded", "홍길동");
        given(sellerRepository.findByMemberId(1L)).willReturn(Optional.empty());
        given(memberRepository.findById(1L)).willReturn(Optional.of(member));
        Seller seller = Seller.create(1L, "홍길동상회", "110-1234-567890");
        given(sellerRepository.save(any())).willReturn(seller);

        RegisterSellerCommand command = new RegisterSellerCommand(1L, "홍길동상회", "110-1234-567890");
        SellerResult result = sellerService.registerSeller(command);

        assertThat(result.memberId()).isEqualTo(1L);
        assertThat(result.businessName()).isEqualTo("홍길동상회");
        assertThat(result.status()).isEqualTo(SellerStatus.PENDING);
        assertThat(member.getRole()).isEqualTo(MemberRole.SELLER);
        verify(sellerRepository).save(any());
    }

    @Test
    void 이미_판매자_등록된_회원은_예외가_발생한다() {
        Seller existing = Seller.create(1L, "기존상회", "110-0000-000000");
        given(sellerRepository.findByMemberId(1L)).willReturn(Optional.of(existing));

        assertThatThrownBy(() -> sellerService.registerSeller(
                new RegisterSellerCommand(1L, "새상회", "110-1234-567890")))
                .isInstanceOf(SellerException.class);
    }

    @Test
    void 판매자를_조회한다() {
        Seller seller = Seller.create(1L, "홍길동상회", "110-1234-567890");
        ReflectionTestUtils.setField(seller, "id", 1L);
        given(sellerRepository.findById(1L)).willReturn(Optional.of(seller));

        SellerResult result = sellerService.getSeller(1L);

        assertThat(result.businessName()).isEqualTo("홍길동상회");
        assertThat(result.status()).isEqualTo(SellerStatus.PENDING);
    }

    @Test
    void 존재하지_않는_판매자_조회시_예외가_발생한다() {
        given(sellerRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> sellerService.getSeller(99L))
                .isInstanceOf(SellerException.class);
    }

    @Test
    void 판매자를_승인한다() {
        Seller seller = Seller.create(1L, "홍길동상회", "110-1234-567890");
        ReflectionTestUtils.setField(seller, "id", 1L);
        given(sellerRepository.findById(1L)).willReturn(Optional.of(seller));

        SellerResult result = sellerService.approveSeller(1L);

        assertThat(result.status()).isEqualTo(SellerStatus.APPROVED);
    }

    @Test
    void 승인된_판매자는_verifyApprovedSeller를_통과한다() {
        Seller seller = Seller.create(1L, "홍길동상회", "110-1234-567890");
        seller.approve();
        given(sellerRepository.findByMemberId(1L)).willReturn(Optional.of(seller));

        sellerService.verifyApprovedSeller(1L);  // 예외 없으면 통과
    }

    @Test
    void 판매자_등록_안된_회원은_verifyApprovedSeller에서_예외가_발생한다() {
        given(sellerRepository.findByMemberId(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> sellerService.verifyApprovedSeller(1L))
                .isInstanceOf(SellerException.class);
    }

    @Test
    void PENDING_판매자는_verifyApprovedSeller에서_예외가_발생한다() {
        Seller seller = Seller.create(1L, "홍길동상회", "110-1234-567890");
        given(sellerRepository.findByMemberId(1L)).willReturn(Optional.of(seller));

        assertThatThrownBy(() -> sellerService.verifyApprovedSeller(1L))
                .isInstanceOf(SellerException.class);
    }
}
