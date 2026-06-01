package com.board.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.stereotype.Service;

import com.board.dto.BoardDTO;
import com.board.dto.FileDTO;
import com.board.dto.ReplyDTO;
import com.board.dto.ReplyInterface;
import com.board.entity.BoardEntity;
import com.board.entity.FileEntity;
import com.board.entity.LikeEntity;
import com.board.entity.MemberEntity;
import com.board.entity.ReplyEntity;
import com.board.entity.repository.BoardRepository;
import com.board.entity.repository.FileRepository;
import com.board.entity.repository.LikeRepository;
import com.board.entity.repository.MemberRepository;
import com.board.entity.repository.ReplyRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BoardServiceImpl implements BoardService{

	private final BoardRepository boardRepository;
	private final FileRepository fileRepository;
	private final LikeRepository likeRepository;
	private final MemberRepository memberRepository;
	private final ReplyRepository replyRepository;
	
	//게시물 목록 보기
	@Override
	public Page<BoardEntity> list(int pageNum,int postNum, String keyword){
		PageRequest pageRequest = PageRequest.of(pageNum-1, postNum, Sort.by(Direction.DESC,"seqno"));
		return boardRepository.findByWriterContainingOrTitleContainingOrContentContaining(keyword, keyword, keyword, pageRequest);
		
	}
	
	//게시물 번호 구하기
	@Override
	public Long getMaxSeqno(String email) {
		return boardRepository.getMaxSeqno(email);
	}	
	
	//게시물 등록
	@Override
	public void write(BoardDTO board) {
		board.setRegdate(LocalDateTime.now());
		boardRepository.save(board.dtoToEntity(board));//save의 인자는 Entity
	}
	
	//게시물 상세 보기
	@Override
	public BoardDTO view(Long seqno) {
		return boardRepository.findById(seqno)
				.map(view-> new BoardDTO(view))
				.orElseThrow(()->new RuntimeException("게시글이 없습니다."));				
	}
	
	//이전 보기 
	@Override
	public Long pre_seqno(Long seqno, String keyword) {
		//return boardRepository.findPreSeqno(seqno, keyword, keyword, keyword)==null?0:boardRepository.findPreSeqno(seqno, keyword, keyword, keyword);
		return Optional.ofNullable(boardRepository.findPreSeqno(seqno, keyword, keyword, keyword))
				.orElse(0L);
	}
	
	//다음 보기
	@Override
	public Long next_seqno(Long seqno, String keyword) {
		//return boardRepository.findNextSeqno(seqno, keyword, keyword, keyword)==null?0:boardRepository.findNextSeqno(seqno, keyword, keyword, keyword);
		return Optional.ofNullable(boardRepository.findNextSeqno(seqno, keyword, keyword, keyword))
				.orElse(0L);
	}
	
	//조회수 업데이트
	@Override
	public void hitno(BoardDTO board) {
		boardRepository.updateHitno(board.getSeqno());
	}
	
	//게시물 수정 
	@Override
	public void modify(BoardDTO board) {		
		BoardEntity boardEntity = boardRepository.findById(board.getSeqno()).get();
		boardEntity.setTitle(board.getTitle());
		boardEntity.setContent(board.getContent());		
		boardRepository.save(boardEntity);
	}
	
	//게시물 삭제
	@Override
	public void delete(Long seqno) {
		BoardEntity boardEntity = boardRepository.findById(seqno).get();
		boardRepository.delete(boardEntity);
	}
	
	//파일 업로드 정보 등록
	@Override
	public void fileInfoRegistry(FileDTO fileDTO) throws Exception{
		fileRepository.save(fileDTO.dtoToEntity(fileDTO));
	}

	//게시글 내에서 업로드된 파일 목록 보기
	@Override
	public List<FileEntity> fileListView(Long seqno) throws Exception{
		return fileRepository.findBySeqnoAndCheckfile(seqno, "Y");
	}

	//게시물 수정에서 파일 삭제--> jpa_file내의 checkfile을 "N"으로 변환
	@Override
	public void deleteFileList(Map<String, Object> data) throws Exception{
		
		FileEntity fileEntity = null;
		List<FileEntity> fileEntities = null;
		
		if(data.get("kind").equals("F")) {
			fileEntity = fileRepository.findById((Long)data.get("fileseqno")).get();
			fileEntity.setCheckfile("N");
			fileRepository.save(fileEntity);
		}
		if(data.get("kind").equals("B")) {
			fileEntities = fileRepository.findBySeqno((Long)data.get("seqno"));
			for(FileEntity file:fileEntities) {
				file.setSeqno((Long)data.get("seqno"));
				file.setCheckfile("N");
				fileRepository.save(file);
			}
		}		
			
	}
	
	//다운로드를 위한 파일 정보 보기
	@Override
	public FileDTO fileInfo(Long fileseqno) throws Exception{
		return fileRepository.findById(fileseqno).map(file->new FileDTO(file))
				.orElseThrow(() -> new RuntimeException("해당 파일 정보를 찾을 수 없습니다."));
	}

	//좋아요/싫어요 테이블에서 로그인 접속자가 게시물에 등록한 좋아요/싫어요 값 가져 오기 
	public LikeEntity likeCheckView(Long seqno,String email) throws Exception{
		BoardEntity boardEntity = boardRepository.findById(seqno).get();
		MemberEntity memberEntity = memberRepository.findById(email).get();		
		return likeRepository.findBySeqnoAndEmail(boardEntity, memberEntity);
	}
	
	//좋아요/싫어요 테이블에 등록
	@Override
	public void likeCheckRegistry(Long seqno,String email,String mylikeCheck,
			String mydislikeCheck,String likeDate,String dislikeDate) throws Exception {
		BoardEntity boardEntity = boardRepository.findById(seqno).get();
		MemberEntity memberEntity = memberRepository.findById(email).get();	
		LikeEntity likeEntity = LikeEntity.builder()
									.seqno(boardEntity)
									.email(memberEntity)
									.mylikecheck(mylikeCheck)
									.mydislikecheck(mydislikeCheck)
									.likedate(likeDate)
									.dislikedate(dislikeDate)
									.build();
		likeRepository.save(likeEntity);
	}
	
	//좋아요/싫어요 테이블에서 mylikeckeck, mydislikecheck 값(Y,N)을 수정
	@Override
	public void likeCheckUpdate(Long seqno,String email,String mylikeCheck,
			String mydislikeCheck,String likeDate,String dislikeDate) throws Exception {
		BoardEntity boardEntity = boardRepository.findById(seqno).get();
		MemberEntity memberEntity = memberRepository.findById(email).get();	
		
		System.out.println("좋아요/싫어요 업데이트");
		System.out.println("seqno = " + seqno + ",email = " + email + ", mylikeCheck = " + mylikeCheck + ", mydislikeCheck = " + mydislikeCheck);
			
		LikeEntity likeEntity = likeRepository.findBySeqnoAndEmail(boardEntity, memberEntity);
		likeEntity.setMylikecheck(mylikeCheck );
		likeEntity.setMydislikecheck(mydislikeCheck);
		likeEntity.setLikedate(likeDate);
		likeEntity.setDislikedate(dislikeDate);
		likeRepository.save(likeEntity);		
	}
	
	//좋아요/싫어요 갯수 수정하기 --> tbl_board내의 likecnt, dislikecnt 값을 변경
	@Override
	public void boardLikeUpdate(Long seqno, int likecnt, int dislikecnt) 
			throws Exception {
		
		BoardEntity boardEntity = boardRepository.findById(seqno).get();
		boardEntity.setLikecnt(likecnt);
		boardEntity.setDislikecnt(dislikecnt);
		boardRepository.save(boardEntity);		
	}


	//댓글 보기
	@Override
	public List<ReplyInterface> replyView(ReplyInterface reply) throws Exception {
		return replyRepository.replyView(reply.getSeqno());
	}
	
	//댓글 수정
	@Override
	public void replyUpdate(ReplyInterface reply) throws Exception {
		ReplyEntity replyEntity = replyRepository.findById(reply.getReplyseqno()).get();
		replyEntity.setReplycontent(reply.getReplycontent());
		replyRepository.save(replyEntity);
		}
	
	//댓글 등록 
	@Override
	public void replyRegistry(ReplyInterface reply) throws Exception {
		BoardEntity boardEntity = boardRepository.findById(reply.getSeqno()).get();
		MemberEntity memberEntity = memberRepository.findById(reply.getEmail()).get();	
		
		ReplyEntity replyEntity = ReplyEntity.builder()
									.seqno(boardEntity)
									.email(memberEntity)
									.replywriter(reply.getReplywriter())
									.replycontent(reply.getReplycontent())
									.replyregdate(LocalDateTime.now())
									.build();
		replyRepository.save(replyEntity);
	}
	
	//댓글 삭제
	@Override
	public void replyDelete(ReplyInterface reply) throws Exception {
		ReplyEntity replyEntity = replyRepository.findById(reply.getReplyseqno()).get();
		replyRepository.delete(replyEntity);
	}

}
