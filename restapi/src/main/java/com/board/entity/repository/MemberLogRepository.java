package com.board.entity.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.config.JpaRepositoryConfigExtension;

import com.board.entity.LikeEntity;
import com.board.entity.LikeEntityID;
import com.board.entity.MemberLogEntity;
import com.board.entity.MemberLogEntityID;

public interface MemberLogRepository extends JpaRepository<MemberLogEntity, MemberLogEntityID>{

}
