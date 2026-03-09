package com.example.flood_aid.repositories.specifications;

import com.example.flood_aid.models.Report;
import com.example.flood_aid.models.ReportAssistance;
import com.example.flood_aid.models.ReportAssistanceLog;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class ReportSpecifications {

    private ReportSpecifications() {
    }

    public static Specification<Report> withFilters(
            String subdistrict,
            String district,
            String province,
            String postalCode,
            Long reportStatusId,
            Timestamp startDate,
            Timestamp endDate,
            UUID userId,
            Long assistanceTypeId,
            String keyword) {
        return Specification.where(withUserId(userId))
                .and(withCreatedAtRange(startDate, endDate))
                .and(withLocationFilters(subdistrict, district, province, postalCode))
                .and(withReportStatus(reportStatusId))
                .and(withAssistanceType(assistanceTypeId))
                .and(withKeyword(keyword));
    }

    private static Specification<Report> withUserId(UUID userId) {
        return (root, query, criteriaBuilder) -> {
            if (userId == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("userId"), userId);
        };
    }

    private static Specification<Report> withCreatedAtRange(Timestamp startDate, Timestamp endDate) {
        return (root, query, criteriaBuilder) -> {
            if (startDate != null && endDate != null) {
                return criteriaBuilder.between(root.<Timestamp>get("createdAt"), startDate, endDate);
            }
            if (startDate != null) {
                return criteriaBuilder.greaterThanOrEqualTo(root.<Timestamp>get("createdAt"), startDate);
            }
            if (endDate != null) {
                return criteriaBuilder.lessThanOrEqualTo(root.<Timestamp>get("createdAt"), endDate);
            }
            return criteriaBuilder.conjunction();
        };
    }

    private static Specification<Report> withLocationFilters(
            String subdistrict,
            String district,
            String province,
            String postalCode) {
        return (root, query, criteriaBuilder) -> {
            if (!hasText(subdistrict) && !hasText(district) && !hasText(province) && !hasText(postalCode)) {
                return criteriaBuilder.conjunction();
            }

            Join<Object, Object> locationJoin = root.join("location", JoinType.INNER);
            List<Predicate> predicates = new ArrayList<>();

            if (hasText(subdistrict)) {
                predicates.add(criteriaBuilder.equal(
                        criteriaBuilder.lower(locationJoin.<String>get("subDistrict")),
                        subdistrict.trim().toLowerCase()));
            }
            if (hasText(district)) {
                predicates.add(criteriaBuilder.equal(
                        criteriaBuilder.lower(locationJoin.<String>get("district")),
                        district.trim().toLowerCase()));
            }
            if (hasText(province)) {
                predicates.add(criteriaBuilder.equal(
                        criteriaBuilder.lower(locationJoin.<String>get("province")),
                        province.trim().toLowerCase()));
            }
            if (hasText(postalCode)) {
                predicates.add(criteriaBuilder.equal(
                        criteriaBuilder.lower(locationJoin.<String>get("postalCode")),
                        postalCode.trim().toLowerCase()));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static Specification<Report> withReportStatus(Long reportStatusId) {
        return (root, query, criteriaBuilder) -> {
            if (reportStatusId == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("reportStatus").<Long>get("id"), reportStatusId);
        };
    }

    private static Specification<Report> withAssistanceType(Long assistanceTypeId) {
        return (root, query, criteriaBuilder) -> {
            if (assistanceTypeId == null) {
                return criteriaBuilder.conjunction();
            }

            Subquery<Long> assistanceSubQuery = query.subquery(Long.class);
            Root<ReportAssistanceLog> assistanceLogRoot = assistanceSubQuery.from(ReportAssistanceLog.class);
            assistanceSubQuery.select(assistanceLogRoot.get("report").<Long>get("id"));
            assistanceSubQuery.where(
                    criteriaBuilder.equal(assistanceLogRoot.get("report").<Long>get("id"), root.<Long>get("id")),
                    criteriaBuilder.equal(assistanceLogRoot.get("assistanceType").<Long>get("id"), assistanceTypeId),
                    criteriaBuilder.isTrue(assistanceLogRoot.get("isActive")));

            return criteriaBuilder.exists(assistanceSubQuery);
        };
    }

    private static Specification<Report> withKeyword(String keyword) {
        return (root, query, criteriaBuilder) -> {
            if (!hasText(keyword)) {
                return criteriaBuilder.conjunction();
            }

            String keywordPattern = "%" + keyword.trim().toLowerCase() + "%";
            Join<Object, Object> locationJoin = root.join("location", JoinType.LEFT);

            List<Predicate> predicates = new ArrayList<>();
            predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.<String>get("firstName")), keywordPattern));
            predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.<String>get("lastName")), keywordPattern));
            predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.<String>get("mainPhoneNumber")), keywordPattern));
            predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.<String>get("reservePhoneNumber")), keywordPattern));
            predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.<String>get("additionalDetail")), keywordPattern));
            predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.<String>get("afterAdditionalDetail")), keywordPattern));
            predicates.add(criteriaBuilder.like(criteriaBuilder.lower(locationJoin.<String>get("address")), keywordPattern));
            predicates.add(criteriaBuilder.like(criteriaBuilder.lower(locationJoin.<String>get("subDistrict")), keywordPattern));
            predicates.add(criteriaBuilder.like(criteriaBuilder.lower(locationJoin.<String>get("district")), keywordPattern));
            predicates.add(criteriaBuilder.like(criteriaBuilder.lower(locationJoin.<String>get("province")), keywordPattern));
            predicates.add(criteriaBuilder.like(criteriaBuilder.lower(locationJoin.<String>get("postalCode")), keywordPattern));

            Subquery<Long> assistanceNameSubQuery = query.subquery(Long.class);
            Root<ReportAssistance> assistanceRoot = assistanceNameSubQuery.from(ReportAssistance.class);
            Join<Object, Object> assistanceTypeJoin = assistanceRoot.join("assistanceType", JoinType.LEFT);
            assistanceNameSubQuery.select(assistanceRoot.get("report").<Long>get("id"));
            assistanceNameSubQuery.where(
                    criteriaBuilder.equal(assistanceRoot.get("report").<Long>get("id"), root.<Long>get("id")),
                    criteriaBuilder.like(criteriaBuilder.lower(assistanceTypeJoin.<String>get("name")), keywordPattern));

            predicates.add(criteriaBuilder.exists(assistanceNameSubQuery));
            return criteriaBuilder.or(predicates.toArray(new Predicate[0]));
        };
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
