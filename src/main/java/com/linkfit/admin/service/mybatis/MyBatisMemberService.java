package com.linkfit.admin.service.mybatis;

import com.linkfit.admin.domain.CrmSale;
import com.linkfit.admin.domain.Member;
import com.linkfit.admin.domain.MemberTicket;
import com.linkfit.admin.domain.Membership;
import com.linkfit.admin.domain.ProductPackage;
import com.linkfit.admin.domain.Sale;
import com.linkfit.admin.domain.TicketLog;
import com.linkfit.admin.mapper.CrmSalesMapper;
import com.linkfit.admin.mapper.MemberMapper;
import com.linkfit.admin.mapper.SaleMapper;
import com.linkfit.admin.service.MemberService;
import com.linkfit.admin.service.ProductPackageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class MyBatisMemberService implements MemberService {

    private static final int DEFAULT_UNLIMITED_DURATION_DAYS = 3650;

    private final MemberMapper memberMapper;
    private final ProductPackageService productPackageService;
    private final SaleMapper saleMapper;
    private final CrmSalesMapper crmSalesMapper;

    public MyBatisMemberService(MemberMapper memberMapper, ProductPackageService productPackageService,
                                 SaleMapper saleMapper, CrmSalesMapper crmSalesMapper) {
        this.crmSalesMapper = crmSalesMapper;
        this.memberMapper = memberMapper;
        this.productPackageService = productPackageService;
        this.saleMapper = saleMapper;
    }

    @Override
    public List<Member> findAll(String keyword, String status, String tier, Long gymId, List<String> trainerIds,
                                 Integer minDaysLeft, Integer maxDaysLeft, Integer minPtRemaining, Integer minAbsentDays,
                                 int page, int size) {
        return memberMapper.findAll(keyword, status, tier, gymId, trainerIds,
                minDaysLeft, maxDaysLeft, minPtRemaining, minAbsentDays, page * size, size);
    }

    @Override
    public long count(String keyword, String status, String tier, Long gymId, List<String> trainerIds,
                       Integer minDaysLeft, Integer maxDaysLeft, Integer minPtRemaining, Integer minAbsentDays) {
        return memberMapper.count(keyword, status, tier, gymId, trainerIds,
                minDaysLeft, maxDaysLeft, minPtRemaining, minAbsentDays);
    }

    @Override
    public Optional<Member> findById(String id, Long gymId) {
        return memberMapper.findById(id, gymId);
    }

    @Override
    public boolean existsByNameAndPhone(String name, String phone) {
        return memberMapper.existsByNameAndPhone(name, phone);
    }

    @Override
    public Member save(Member member, Long gymId) {
        if (member.getId() == null || member.getId().isBlank()) {
            member.setId(UUID.randomUUID().toString());
        }
        if (member.getEmail() != null && member.getEmail().isBlank()) {
            member.setEmail(null);
        }
        member.setPhone(stripNonDigits(member.getPhone()));
        memberMapper.insertUser(member);
        memberMapper.insertProfile(member);
        if (gymId != null) {
            memberMapper.insertUserGym(member.getId(), gymId);
        }
        return member;
    }

    @Override
    public Member update(String id, Member member, Long gymId) {
        member.setId(id);
        member.setPhone(stripNonDigits(member.getPhone()));
        memberMapper.update(member, gymId);
        return member;
    }

    private static String stripNonDigits(String phone) {
        if (phone == null) return null;
        String digits = phone.replaceAll("[^0-9]", "");
        return digits.isEmpty() ? null : digits;
    }

    @Override
    public void delete(String id, Long gymId) {
        memberMapper.delete(id, gymId);
    }

    @Override
    public void updateStatus(String id, String status, Long gymId) {
        memberMapper.updateStatus(id, "ACTIVE".equals(status) ? 1 : 0, gymId);
    }

    @Override
    public void updateTier(String id, String tier, Long gymId) {
        memberMapper.updateTier(id, tier, gymId);
    }

    @Override
    public void updateMemberType(String id, String memberType, Long gymId) {
        memberMapper.updateMemberType(id, memberType, gymId);
    }

    @Override
    public void updateRole(String id, String role, Long gymId) {
        memberMapper.updateRole(id, role, gymId);
    }

    @Override
    public void updateAssignedTrainer(String id, String trainerId, Long gymId) {
        memberMapper.updateAssignedTrainer(id, trainerId, gymId);
    }

    @Override
    public void freeze(String id, String startDate, String endDate, Long gymId) {
        memberMapper.insertFreeze(id, startDate, endDate, null);
        memberMapper.updateStatus(id, 0, gymId);
    }

    @Override
    public void withdraw(String id, Long gymId) {
        memberMapper.withdraw(id, gymId);
    }

    @Override
    public List<Membership> findMemberships(String id) {
        return memberMapper.findMembershipsByMemberId(id);
    }

    // unlimited: PT처럼 애초에 기간 개념이 없어 무기한 처리되는 구성인지 여부.
    // 이용권/락커/운동복은 개월/일을 0으로 남겨두면(입력을 빠뜨린 경우 포함) 무기한이 아니라
    // 그대로 0일(=시작일에 즉시 만료)로 계산한다 — 예전엔 0/0을 무기한(약 10년)으로 처리해서
    // 기간 입력을 빠뜨린 실수가 수년짜리 이용권으로 조용히 등록되는 문제가 있었음.
    // 대표(anchor) 선정 시 무기한(PT) 구성은 실제 기간이 있는 구성보다 우선순위가 낮아야 한다.
    private record ProductComponent(String type, java.time.LocalDate endDate, boolean unlimited) {}

    private static ProductComponent buildComponent(String type, java.time.LocalDate start, Integer months, Integer days) {
        int m = months != null ? months : 0;
        int d = days != null ? days : 0;
        return new ProductComponent(type, start.plusMonths(m).plusDays(d), false);
    }

    // 신규/재유입/재등록/단품결제 분류.
    // 이용권/PT가 포함되지 않은 구매(락커/운동복만)는 이력과 무관하게 단품결제로 고정한다.
    // 이용권/PT가 포함된 경우: 이 회원의 과거 이용권/PT 이력이 하나도 없으면 신규,
    // 있으면 마지막 만료일 기준으로 1개월 이내(또는 만료 전)면 재등록, 1개월을 초과해서
    // 재결제하면 재유입으로 분류한다.
    private String computeRegType(String memberId, boolean hasMembershipOrPt, LocalDate start) {
        if (!hasMembershipOrPt) return "SINGLE_ITEM";
        Map<String, Object> history = memberMapper.findMembershipHistorySummary(memberId);
        long cnt = history.get("cnt") instanceof Number n ? n.longValue() : 0;
        Object lastEndObj = history.get("lastEndDate");
        if (cnt == 0 || lastEndObj == null) return "NEW";
        LocalDate lastEnd = lastEndObj instanceof java.sql.Date d ? d.toLocalDate()
                : lastEndObj instanceof LocalDate ld ? ld
                : LocalDate.parse(lastEndObj.toString());
        return start.isAfter(lastEnd.plusMonths(1)) ? "RE_INFLOW" : "RE";
    }

    private static String toCrmSalesRegType(String regType) {
        if (regType == null) return null;
        return switch (regType) {
            case "NEW" -> "new";
            case "RE" -> "re";
            case "RE_INFLOW" -> "re_inflow";
            case "SINGLE_ITEM" -> "single_item";
            default -> null;
        };
    }

    // 개별 상품 부여(패키지 없이 이용권/PT/락커/운동복 중 하나만 단독 등록)에서 쓰는 유형별 한글 라벨.
    // 패키지명이 없는 개별 등록은 이 라벨을 매출 화면의 상품명으로 대신 사용한다.
    private static final Map<String, String> INDIVIDUAL_TYPE_LABEL =
            Map.of("MEMBERSHIP", "이용권", "PT", "PT", "LOCKER", "락커", "ITEM", "운동복");

    @Override
    @Transactional
    public void addMembership(Membership membership, Long gymId) {
        if (membership.getPackageId() == null) {
            String type = membership.getType();
            boolean hasMembershipOrPt = "MEMBERSHIP".equals(type) || "PT".equals(type);
            java.time.LocalDate start = membership.getStartDate();
            ProductComponent c = "PT".equals(type)
                    ? new ProductComponent("PT", start.plusDays(DEFAULT_UNLIMITED_DURATION_DAYS), true)
                    : buildComponent(type, start, membership.getDurationMonths(), membership.getDurationDays());
            membership.setEndDate(c.endDate());
            membership.setRegType(computeRegType(membership.getMemberId(), hasMembershipOrPt, start));
            memberMapper.insertMembership(membership);
            if ("PT".equals(type) && membership.getSessionCount() != null && membership.getSessionCount() != 0) {
                memberMapper.adjustPtSessions(membership.getMemberId(), membership.getSessionCount());
            }
            String productName = membership.getProductName() != null
                    ? membership.getProductName() : INDIVIDUAL_TYPE_LABEL.getOrDefault(type, type);
            recordSale(membership, type, productName, gymId);
            return;
        }
        ProductPackage pkg = productPackageService.findById(membership.getPackageId())
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다."));
        java.time.LocalDate start = membership.getStartDate();
        String regType = computeRegType(membership.getMemberId(),
                pkg.isMembershipEnabled() || pkg.isPtEnabled(), start);

        List<ProductComponent> components = new java.util.ArrayList<>();
        if (pkg.isMembershipEnabled()) {
            components.add(buildComponent("MEMBERSHIP", start, pkg.getMembershipDurationMonths(), pkg.getMembershipDurationDays()));
        }
        if (pkg.isPtEnabled()) {
            // PT는 기간 없이 횟수만 관리하므로 만료 개념을 두지 않고 무기한으로 처리한다.
            components.add(new ProductComponent("PT", start.plusDays(DEFAULT_UNLIMITED_DURATION_DAYS), true));
        }
        if (pkg.isLockerEnabled()) {
            components.add(buildComponent("LOCKER", start, pkg.getLockerDurationMonths(), pkg.getLockerDurationDays()));
        }
        if (pkg.isUniformEnabled()) {
            components.add(buildComponent("ITEM", start, pkg.getUniformDurationMonths(), pkg.getUniformDurationDays()));
        }
        if (components.isEmpty()) {
            throw new IllegalArgumentException("상품에 구성된 항목이 없습니다.");
        }

        // 실제 기간이 있는 구성 중 만료일이 가장 먼(기간이 가장 긴) 것을 대표(anchor)로 삼아
        // 금액/할인/납부액을 그 행에 기록하고, 나머지 구성은 0원으로 기록해 매출/미납금이
        // 중복 집계되지 않게 한다. 전부 무기한(PT만 있는 경우 등)이면 첫 구성을 대표로 한다.
        ProductComponent anchor = components.stream()
                .filter(c -> !c.unlimited())
                .max(Comparator.comparing(ProductComponent::endDate))
                .orElse(components.get(0));

        for (ProductComponent c : components) {
            Membership row = new Membership();
            row.setMemberId(membership.getMemberId());
            row.setPackageId(membership.getPackageId());
            row.setType(c.type());
            row.setStartDate(start);
            row.setEndDate(c.endDate());
            boolean isAnchor = c == anchor;
            row.setPrice(isAnchor ? membership.getPrice() : 0);
            row.setDiscountAmount(isAnchor ? membership.getDiscountAmount() : 0);
            row.setPaidAmount(isAnchor ? membership.getPaidAmount() : 0);
            row.setPaymentMethod(isAnchor ? membership.getPaymentMethod() : null);
            row.setRegType(isAnchor ? regType : null);
            row.setMemo(membership.getMemo());
            if ("PT".equals(c.type())) {
                row.setSessionCount(pkg.getPtSessionCount());
            }
            memberMapper.insertMembership(row);
            // PT 구성이 있으면 실제 PT 잔여 세션(user_profiles.pt_sessions_left)을 함께 충전한다.
            // 이 크레딧이 빠져있으면 결제는 기록되지만 앱에서 실제로 쓸 수 있는 PT 횟수가 늘지 않는다.
            if ("PT".equals(c.type()) && pkg.getPtSessionCount() != null && pkg.getPtSessionCount() != 0) {
                memberMapper.adjustPtSessions(membership.getMemberId(), pkg.getPtSessionCount());
            }
            if (isAnchor) {
                recordSale(row, anchor.type(), pkg.getName(), gymId);
            }
        }
    }

    // 회원관리에서 등록된 상품(대표/anchor 행)을 매출 관련 화면에 그대로 노출되도록 기록한다.
    // sale(통계/리포트 페이지)과 crm_sales(매출 관리 페이지) 두 곳 모두에 동일한 거래를 남긴다.
    // 실제 입금된 금액(paidAmount) 기준으로 매출을 집계한다.
    private void recordSale(Membership row, String productType, String productName, Long gymId) {
        Sale sale = new Sale();
        sale.setMemberId(row.getMemberId());
        sale.setProductName(productName);
        sale.setProductType(productType);
        sale.setAmount(row.getPaidAmount());
        sale.setPaymentMethod(row.getPaymentMethod() != null ? row.getPaymentMethod() : "미지정");
        sale.setSaleDate(row.getStartDate());
        sale.setMemo(row.getMemo());
        saleMapper.insert(sale);

        // crm_sales.sales_type은 membership/pt/feedback_ticket만 허용하므로,
        // PT를 제외한 모든 구성(이용권/락커/운동복)은 membership으로 매핑한다.
        CrmSale crmSale = new CrmSale();
        crmSale.setId(UUID.randomUUID().toString());
        crmSale.setGymId(gymId);
        crmSale.setMemberId(row.getMemberId());
        crmSale.setSalesType("PT".equals(productType) ? "pt" : "membership");
        crmSale.setRegType(toCrmSalesRegType(row.getRegType()));
        crmSale.setAmount(BigDecimal.valueOf(row.getPaidAmount()));
        crmSale.setSaleDate(row.getStartDate().toString());
        String note = productName != null ? productName : "";
        if (row.getMemo() != null && !row.getMemo().isEmpty()) {
            note = note.isEmpty() ? row.getMemo() : note + " · " + row.getMemo();
        }
        crmSale.setNote(note);
        crmSalesMapper.insert(crmSale);
    }

    @Override
    public List<MemberTicket> findTickets(String id) {
        return memberMapper.findTickets(id);
    }

    @Override
    public Map<String, Object> ticketTotals(Long gymId) {
        return memberMapper.ticketTotals(gymId);
    }

    @Override
    @Transactional
    public void chargeTicket(String id, String ticketType, int amount, String description) {
        int before = Optional.ofNullable(memberMapper.findTicketRemaining(id, ticketType)).orElse(0);
        memberMapper.upsertTicket(id, ticketType, amount);
        int after = Optional.ofNullable(memberMapper.findTicketRemaining(id, ticketType)).orElse(0);
        int actualDelta = after - before;

        String actionType = amount >= 0 ? "CHARGE" : "USE";
        String desc = (description != null && !description.isBlank()) ? description : (amount >= 0 ? "관리자 지급" : "관리자 차감");
        // 잔액 부족으로 요청한 만큼 전부 차감되지 못하고 0에서 멈춘 경우, 로그에 실제 적용된
        // 변동폭을 남긴다 (ticket_logs에는 amount 컬럼이 없어 description으로만 남길 수 있음).
        if (actualDelta != amount) {
            desc = desc + String.format(" (요청 %+d 중 실제 %+d 적용, 잔액부족)", amount, actualDelta);
        }
        memberMapper.insertTicketLog(id, ticketType, actionType, desc);
    }

    @Override
    public List<TicketLog> findTicketLogs(Long gymId, String ticketType, String keyword, int page, int size) {
        return memberMapper.findTicketLogs(gymId, ticketType, keyword, page * size, size);
    }

    @Override
    public long countTicketLogs(Long gymId, String ticketType, String keyword) {
        return memberMapper.countTicketLogs(gymId, ticketType, keyword);
    }

    // 회원 간 상품 양도 (이용권/락커/운동복 한정 — PT는 pool 단위라 transferPtSessions() 사용).
    // 회수(delete)와 동일하게 anchor 금액을 남은 형제 구성으로 이전한 뒤, 원 행은 삭제 대신
    // status='TRANSFERRED'로 표시해 이력을 보존하고, 대상 회원에게 같은 잔여 기간으로 새 행을 만든다.
    @Override
    @Transactional
    public void transferMembership(Long membershipId, String targetMemberId, Long gymId) {
        Membership target = memberMapper.findMembershipById(membershipId)
                .orElseThrow(() -> new IllegalArgumentException("이용권을 찾을 수 없습니다."));
        if ("PT".equals(target.getType())) {
            throw new IllegalArgumentException("PT는 개별 양도 대상이 아닙니다. PT 잔여 이전 기능을 이용해주세요.");
        }
        if (target.getStatus() != null) {
            throw new IllegalArgumentException("이미 양도되었거나 더 이상 유효하지 않은 이용권입니다.");
        }
        if (target.getMemberId().equals(targetMemberId)) {
            throw new IllegalArgumentException("본인에게는 양도할 수 없습니다.");
        }
        Member sourceMember = memberMapper.findById(target.getMemberId(), gymId).orElse(null);
        if (memberMapper.findById(targetMemberId, gymId).isEmpty()) {
            throw new IllegalArgumentException("대상 회원을 찾을 수 없습니다.");
        }

        if (target.getPackageId() != null) {
            List<Membership> siblings = memberMapper.findMembershipsByMemberId(target.getMemberId()).stream()
                    .filter(m -> target.getPackageId().equals(m.getPackageId())
                            && !m.getId().equals(membershipId) && m.getStatus() == null)
                    .toList();
            boolean holdsAmount = target.getPrice() > 0 || target.getDiscountAmount() > 0 || target.getPaidAmount() > 0;
            if (holdsAmount && !siblings.isEmpty()) {
                Membership newHolder = siblings.stream()
                        .filter(m -> !"PT".equals(m.getType()))
                        .max(Comparator.comparing(Membership::getEndDate))
                        .orElseGet(() -> siblings.stream()
                                .max(Comparator.comparing(Membership::getEndDate))
                                .orElse(siblings.get(0)));
                memberMapper.updateMembershipAmounts(newHolder.getId(),
                        target.getPrice(), target.getDiscountAmount(), target.getPaidAmount(),
                        target.getPaymentMethod(), target.getRegType());
            }
        }

        memberMapper.markMembershipTransferred(membershipId);

        Membership newRow = new Membership();
        newRow.setMemberId(targetMemberId);
        newRow.setType(target.getType());
        newRow.setStartDate(LocalDate.now());
        newRow.setEndDate(target.getEndDate());
        newRow.setPrice(0);
        newRow.setDiscountAmount(0);
        newRow.setPaidAmount(0);
        newRow.setRegType("TRANSFER");
        String sourceName = sourceMember != null && sourceMember.getName() != null ? sourceMember.getName() : target.getMemberId();
        newRow.setMemo(sourceName + "님으로부터 양도받음");
        memberMapper.insertMembership(newRow);
    }

    // PT는 특정 구매건 단위 잔여치를 추적할 수 없는 pool 구조라, 회원 전체 PT 잔여
    // (구매+서비스)를 그대로 대상 회원에게 이전한다. 구매분은 구매분끼리, 서비스분은
    // 서비스분끼리 옮겨서 대상 회원 쪽에서도 서비스 우선 차감 규칙이 유지되게 한다.
    @Override
    @Transactional
    public void transferPtSessions(String sourceMemberId, String targetMemberId, Long gymId) {
        if (sourceMemberId.equals(targetMemberId)) {
            throw new IllegalArgumentException("본인에게는 이전할 수 없습니다.");
        }
        Member source = memberMapper.findById(sourceMemberId, gymId)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));
        memberMapper.findById(targetMemberId, gymId)
                .orElseThrow(() -> new IllegalArgumentException("대상 회원을 찾을 수 없습니다."));

        int purchased = source.getPtSessionsLeft() != null ? source.getPtSessionsLeft() : 0;
        int service = source.getServicePtSessionsLeft() != null ? source.getServicePtSessionsLeft() : 0;
        if (purchased + service <= 0) {
            throw new IllegalArgumentException("이전할 PT 잔여 세션이 없습니다.");
        }
        if (purchased > 0) {
            memberMapper.adjustPtSessions(sourceMemberId, -purchased);
            memberMapper.adjustPtSessions(targetMemberId, purchased);
        }
        if (service > 0) {
            memberMapper.adjustServicePtSessions(sourceMemberId, -service);
            memberMapper.adjustServicePtSessions(targetMemberId, service);
        }
    }

    @Override
    @Transactional
    public void deleteMembership(Long id) {
        Membership target = memberMapper.findMembershipById(id).orElse(null);
        if (target != null && target.getPackageId() != null) {
            List<Membership> siblings = memberMapper.findMembershipsByMemberId(target.getMemberId()).stream()
                    .filter(m -> target.getPackageId().equals(m.getPackageId()) && !m.getId().equals(id))
                    .toList();
            boolean holdsAmount = target.getPrice() > 0 || target.getDiscountAmount() > 0 || target.getPaidAmount() > 0;
            if (holdsAmount && !siblings.isEmpty()) {
                // 이 구성이 금액을 들고 있었다면, 남은 구성 중 만료일이 가장 먼 것에 금액을 이전해
                // 개별 구성 회수만으로 전체 금액/미납금 표시가 사라지지 않게 한다.
                // PT는 기간 없이 무기한(가짜 만료일)으로 생성되므로, 실제 기간이 있는 구성이
                // 남아있다면 그쪽을 우선하고 PT만 남았을 때만 PT로 이전한다.
                Membership newHolder = siblings.stream()
                        .filter(m -> !"PT".equals(m.getType()))
                        .max(Comparator.comparing(Membership::getEndDate))
                        .orElseGet(() -> siblings.stream()
                                .max(Comparator.comparing(Membership::getEndDate))
                                .orElse(siblings.get(0)));
                memberMapper.updateMembershipAmounts(newHolder.getId(),
                        target.getPrice(), target.getDiscountAmount(), target.getPaidAmount(),
                        target.getPaymentMethod(), target.getRegType());
            }
        }
        // PT 구성을 회수하면 등록 시 충전했던 실제 PT 잔여 세션도 그만큼 되돌린다
        // (이미 사용한 세션이 있으면 0 밑으로는 안 내려가고 거기서 멈춘다).
        if (target != null && "PT".equals(target.getType()) && target.getSessionCount() != null && target.getSessionCount() != 0) {
            memberMapper.adjustPtSessions(target.getMemberId(), -target.getSessionCount());
        }
        memberMapper.deleteMembership(id);
    }

    @Override
    @Transactional
    public void deleteMembershipsByPackage(String memberId, Long packageId) {
        memberMapper.findMembershipsByMemberId(memberId).stream()
                .filter(m -> packageId.equals(m.getPackageId()) && "PT".equals(m.getType())
                        && m.getSessionCount() != null && m.getSessionCount() != 0)
                .forEach(m -> memberMapper.adjustPtSessions(memberId, -m.getSessionCount()));
        memberMapper.deleteMembershipsByMemberAndPackage(memberId, packageId);
    }
}
