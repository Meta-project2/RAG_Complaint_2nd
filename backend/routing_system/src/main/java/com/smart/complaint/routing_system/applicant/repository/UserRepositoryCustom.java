package com.smart.complaint.routing_system.applicant.repository;

import java.util.Optional;

import com.smart.complaint.routing_system.applicant.entity.User;

public interface UserRepositoryCustom {

    Optional<User> findByProviderIdLike(String providerId);

}
