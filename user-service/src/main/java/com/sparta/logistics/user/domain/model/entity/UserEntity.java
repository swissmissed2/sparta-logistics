package com.sparta.logistics.user.domain.model.entity;

import com.sparta.logistics.common.domain.BaseEntity;
import com.sparta.logistics.common.domain.Role;
import com.sparta.logistics.common.exception.BusinessException;
import com.sparta.logistics.user.domain.model.enums.UserStatus;
import com.sparta.logistics.user.exception.UserErrorCode;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;


@Entity
@Table(name="p_user", indexes = {
        @Index(name = "idx_user_username_deleted_at", columnList = "username, deleted_at")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@Builder
public class UserEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 10)
    private String username;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(unique = true, length = 255)
    private String email;

    @Column(length = 255)
    private String slackId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, columnDefinition = "VARCHAR(20) DEFAULT 'PENDING'")
    @Builder.Default
    private UserStatus status = UserStatus.PENDING;

    private UUID hubId;

    private UUID companyId;

    private LocalDateTime lastLoginAt; // 마지막 로그인 일시

    public void validateRoleConstraints() {
        if (this.role == null) return;

        switch (this.role) {
            case HUB_MANAGER:
            case DELIVERY_MANAGER:
                if (this.hubId == null) {
                    throw new BusinessException(UserErrorCode.INVALID_ROLE_CONSTRAINT);
                }
                break;
            case COMPANY_MANAGER:
                if (this.companyId == null) {
                    throw new BusinessException(UserErrorCode.INVALID_ROLE_CONSTRAINT);
                }
                break;
            case MASTER:
                if (this.hubId != null || this.companyId != null) {
                    throw new BusinessException(UserErrorCode.INVALID_ROLE_CONSTRAINT);
                }
                break;
        }
    }

    public void updateLastLoginAt() {
        this.lastLoginAt = LocalDateTime.now();
    }

    public void approve(){
        if(this.status !=UserStatus.PENDING) {
            throw new BusinessException(UserErrorCode.ALREADY_PROCESSED);
        }
        this.status = UserStatus.APPROVED;
    }

    public void reject(){
        if(this.status !=UserStatus.PENDING){
            throw new BusinessException(UserErrorCode.ALREADY_PROCESSED);
        }
        this.status = UserStatus.REJECTED;
    }

    public void update(String name, String email,String slackId,Role role,UUID hubId,UUID companyId){
        this.name = name;
        this.email = email;
        this.slackId = slackId;
        this.role = role;
        this.hubId = hubId;
        this.companyId = companyId;

        validateRoleConstraints();
    }

}
