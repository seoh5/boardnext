package com.board.dto;

import java.time.LocalDateTime;

import com.board.entity.BoardEntity;
import com.board.entity.MemberEntity;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BoardDTO {
	
    private Long seqno;
    private MemberEntity email;
    private String writer;
    private String title;
    private LocalDateTime regdate;
    private String content;
    private int hitno;
    private int likecnt;
	private int dislikecnt;
	
	//생성자를 이용해서 Entity를 DTO로 이동
	public BoardDTO(BoardEntity boardEntity) {
		
		this.email = boardEntity.getEmail();
		this.seqno = boardEntity.getSeqno();
		this.writer = boardEntity.getWriter();
		this.title = boardEntity.getTitle();
		this.regdate = boardEntity.getRegdate();
		this.content = boardEntity.getContent();
		this.hitno = boardEntity.getHitno();
		this.likecnt = boardEntity.getLikecnt();
		this.dislikecnt = boardEntity.getDislikecnt();
		
	}
	
	//DTO --> Entity로 이동
	public BoardEntity dtoToEntity(BoardDTO dto) {
		
		BoardEntity boardEntity = BoardEntity.builder()
								.email(dto.getEmail())
								.seqno(dto.getSeqno())
								.writer(dto.getWriter())
								.title(dto.getTitle())
								.regdate(dto.getRegdate())
								.content(dto.getContent())
								.hitno(dto.getHitno())
								.likecnt(dto.getLikecnt())
								.dislikecnt(dto.getDislikecnt())
								.build();
		return boardEntity;
		
	}
	
}
