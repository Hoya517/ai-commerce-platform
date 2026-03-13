package com.hoya.aicommerce.seller.application;

import com.hoya.aicommerce.member.domain.Member;
import com.hoya.aicommerce.member.domain.MemberRepository;
import com.hoya.aicommerce.member.exception.MemberException;
import com.hoya.aicommerce.seller.application.dto.RegisterSellerCommand;
import com.hoya.aicommerce.seller.application.dto.SellerResult;
import com.hoya.aicommerce.seller.domain.Seller;
import com.hoya.aicommerce.seller.domain.SellerRepository;
import com.hoya.aicommerce.seller.exception.SellerException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SellerService {

    private final SellerRepository sellerRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public SellerResult registerSeller(RegisterSellerCommand command) {
        if (sellerRepository.findByMemberId(command.memberId()).isPresent()) {
            throw new SellerException("이미 판매자 등록된 회원입니다");
        }
        Member member = memberRepository.findById(command.memberId())
                .orElseThrow(() -> new MemberException("Member not found"));

        Seller seller = Seller.create(command.memberId(), command.businessName(), command.settlementAccount());
        member.promoteToSeller();

        return SellerResult.from(sellerRepository.save(seller));
    }

    @Transactional(readOnly = true)
    public SellerResult getSeller(Long sellerId) {
        Seller seller = sellerRepository.findById(sellerId)
                .orElseThrow(() -> new SellerException("판매자를 찾을 수 없습니다"));
        return SellerResult.from(seller);
    }

    @Transactional
    public SellerResult approveSeller(Long sellerId) {
        Seller seller = sellerRepository.findById(sellerId)
                .orElseThrow(() -> new SellerException("판매자를 찾을 수 없습니다"));
        seller.approve();
        return SellerResult.from(seller);
    }

    public Long verifyApprovedSeller(Long memberId) {
        Seller seller = sellerRepository.findByMemberId(memberId)
                .orElseThrow(() -> new SellerException("판매자 등록이 필요합니다"));
        if (!seller.isApproved()) {
            throw new SellerException("승인된 판매자만 상품을 등록할 수 있습니다");
        }
        return seller.getId();
    }
}
