package com.internship.splitwise.repository;

import com.internship.splitwise.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);

    @Query("SELECT gm.user FROM GroupMember gm WHERE gm.groupId = :groupId")
    List<User> findActiveMembersByGroupId(@Param("groupId") UUID groupId);
}
