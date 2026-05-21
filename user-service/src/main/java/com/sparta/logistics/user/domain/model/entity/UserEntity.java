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
@Builder
@Table(name="p_user")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Getter
@Setter
public class UserEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID Id;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String name;

    private String email;

    private String slackId;

    @Column(nullable = false)
    private Role role;

    @Column(nullable = false)
    private UserStatus status;

    private UUID hubId;

    private UUID companyId;

    private LocalDateTime last_login_at; // 마지막 로그인 일시

    @Builder
    public UserEntity(String username, String password, String name, String email,
                      String slackId, Role role, UserStatus status, UUID hubId, UUID companyId) {

        this.username = username;
        this.password = password;
        this.name = name;
        this.email = email;
        this.slackId = slackId;
        this.role = role;
        this.status = status;
        this.hubId = hubId;
        this.companyId = companyId;
        this.last_login_at = LocalDateTime.now();

        validateRoleConstraints();
    }

    public void validateRoleConstraints() {
        if (this.role == null) return;

        switch (this.role) {
            case HUB_MANAGER:
            case DELIVERY_MANAGER:
                if (this.hubId == null) {
                    throw new IllegalArgumentException(this.role + "은(는) hubId가 필수입니다.");
                }
                break;
            case COMPANY_MANAGER:
                if (this.companyId == null) {
                    throw new IllegalArgumentException(this.role + "은(는) companyId가 필수입니다.");
                }
                break;
            case MASTER:
                if (this.hubId != null || this.companyId != null) {
                    throw new IllegalArgumentException("MASTER는 hubId와 companyId를 가질 수 없습니다.");
                }
                break;
        }
    }

    public void updateRoleAndIds(Role role, UUID hubId, UUID companyId) {
        this.role = role;
        this.hubId = hubId;
        this.companyId = companyId;
        validateRoleConstraints(); // 변경 시에도 검증
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

}
