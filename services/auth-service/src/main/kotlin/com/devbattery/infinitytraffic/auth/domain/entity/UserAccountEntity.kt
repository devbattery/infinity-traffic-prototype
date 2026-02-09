package com.devbattery.infinitytraffic.auth.domain.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

/**
 * 사용자 계정 영속 데이터를 저장하는 JPA 엔티티다.
 */
@Entity
@Table(name = "user_account")
class UserAccountEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    val id: Long? = null,

    @Column(name = "username", nullable = false, unique = true, length = 100)
    var username: String,

    @Column(name = "password_hash", nullable = false, length = 200)
    var passwordHash: String,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant,
)
