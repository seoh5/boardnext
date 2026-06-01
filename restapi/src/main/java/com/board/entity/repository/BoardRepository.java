package com.board.entity.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.board.entity.BoardEntity;

public interface BoardRepository extends JpaRepository<BoardEntity,Long>{

	//게시물 목록 보기
	public Page<BoardEntity> findByWriterContainingOrTitleContainingOrContentContaining(String keyword1, String keyword2, String keyword3, Pageable pageable);
	//select * from jpa_board where writer like '%'||'aaa'||'%' or title like '%'||'aaa'||'%' or content like '%'||'aaa'||'%' 	
	//게시물 이전 보기 - JPQL(Java Persistent Query Language)
	@Query("select max(b.seqno) from board b where b.seqno < :seqno and (b.writer like %:keyword1% or b.title like %:keyword2% or b.content like %:keyword3%)")
	public Long findPreSeqno(@Param("seqno") Long seqno, @Param("keyword1") String keyword1,@Param("keyword2") String keyword2,@Param("keyword3") String keyword3); 
	
	//게시물 다음 보기 - JPQL(Java Persistent Query Language)
	@Query("select min(b.seqno) from board b where b.seqno > :seqno and (b.writer like %:keyword1% or b.title like %:keyword2% or b.content like %:keyword3%)")
	public Long findNextSeqno(@Param("seqno") Long seqno, @Param("keyword1") String keyword1,@Param("keyword2") String keyword2,@Param("keyword3") String keyword3); 
	
	//게시물 조회수 증가
	@Transactional
	@Modifying //엔티티 클래스에 적용될수 있도록 함.
	@Query(value="update jpa_board set hitno = (select nvl(hitno,0) from jpa_board where seqno=:seqno) + 1 where seqno=:seqno",nativeQuery=true)
	public void updateHitno(@Param("seqno") Long seqno); 
	
	//max seqno 구하기
	@Query(value="select max(seqno) from jpa_board where email = :email", nativeQuery=true)
	public Long getMaxSeqno(@Param("email") String email);
	
}
