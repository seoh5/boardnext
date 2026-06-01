package com.board.controller;

import java.io.File;
import java.net.URLEncoder;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.board.dto.BoardDTO;
import com.board.dto.FileDTO;
import com.board.dto.ReplyInterface;
import com.board.entity.BoardEntity;
import com.board.entity.LikeEntity;
import com.board.entity.repository.BoardRepository;
import com.board.service.BoardService;
import com.board.util.PageUtil;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.AllArgsConstructor;

//@CrossOrigin(originPatterns = "http://www.boardreact.com")
//@CrossOrigin(originPatterns = "http://localhost:3000")
@RestController
@AllArgsConstructor
@Tag(name="게시판 관리 API", description="게시판 관리를 위한 REST API를 가지고 있는 클래스입니다.")
public class RESTBoardController {

	private final BoardRepository boardRepository;
	private final BoardService service;	
	
	//전체 게시물 목록 보기(테스트)
	@GetMapping("/apitest/listAll")
	public ResponseEntity<List<BoardDTO>> getListAll() {		
		List<BoardDTO> boardDTOs = new ArrayList<>();		
		boardRepository.findAll().stream().forEach(list-> boardDTOs.add(new BoardDTO(list)));
		return ResponseEntity.ok().body(boardDTOs);
	}
	
	//게시물 목록 보기
	@GetMapping("/api/board/list")
	public ResponseEntity<Page<BoardEntity>> getList(@RequestParam("page") int pageNum, @RequestParam(name="keyword",defaultValue="",required=false) String keyword) throws Exception{
		int postNum = 5; //한 화면에 보여지는 게시물 행의 갯수		
		return ResponseEntity.ok().body(service.list(pageNum, postNum, keyword));
	}
	
	//게시물 목록 페이지 리스트 보기
	@GetMapping("/api/board/pagelist")
	public ResponseEntity<String> getPageList(@RequestParam("page") int pageNum, @RequestParam(name="keyword",defaultValue="",required=false) String keyword) throws Exception {
		int postNum = 5; //한 화면에 보여지는 게시물 행의 갯수
		int pageListCount = 5; //화면 하단에 보여지는 페이지리스트 내의 페이지 갯수
		
		PageRequest pageRequest = PageRequest.of(pageNum-1, postNum, Sort.by(Direction.DESC,"seqno"));
		Page<BoardEntity> list = boardRepository
				.findByWriterContainingOrTitleContainingOrContentContaining(keyword, keyword, keyword, pageRequest);		
		PageUtil page = new PageUtil();
		int totalCount = (int)list.getTotalElements();
		String json = "{\"pagelist\":\"" + page.getPageList(pageNum, postNum, pageListCount, totalCount, keyword) 
			+ "\", \"totalCount\":\"" + totalCount + "\", \"postNum\":\"" + postNum + "\"}";
		return ResponseEntity.ok().body(json);
	}
	
	//게시물 내용 상세 보기 
	@GetMapping("/api/board/view")
	public ResponseEntity<BoardDTO> getView(@RequestParam("seqno") Long seqno,@RequestParam("email") String email) throws Exception {
		return ResponseEntity.ok().body(service.view(seqno)); 
	}
	
	//조회수 증가
	@GetMapping("/api/board/hitnoPlus")
	public ResponseEntity<?> getHitnoPlus(@RequestParam("seqno") Long seqno) {
		BoardDTO boardDTO = new BoardDTO();
		boardDTO.setSeqno(seqno);
		service.hitno(boardDTO);		
		return ResponseEntity.ok().build();
	}
	
	//게시물 이전 보기
	@GetMapping("/api/board/preseqno")
	public ResponseEntity<Long> getPreseqno(@RequestParam("seqno") Long seqno,
	@RequestParam(name="keyword",required=false) String keyword) throws Exception {
		return ResponseEntity.ok().body(service.pre_seqno(seqno, keyword));		
	}
	
	//게시물 다음 보기
	@GetMapping("/api/board/nextseqno")
	public ResponseEntity<Long> getNextseqno(@RequestParam("seqno") Long seqno, 
	@RequestParam(name="keyword",required=false) String keyword) throws Exception {	
		return ResponseEntity.ok().body(service.next_seqno(seqno, keyword)); 		
	}
	
	//게시물 등록 및 수정
	@PostMapping("/api/board/write")
	public ResponseEntity<?> postFileUpload(BoardDTO board,@RequestParam("kind") String kind,
			@RequestParam(name="sendToFileList",required=false) List<MultipartFile> multipartFile,
			@RequestParam(name="deleteFileList",required=false) Long[] deleteFileList) throws Exception{

		//운영체제에 따라 이미지가 저장될 디렉토리 구조 설정 시작
		String os = System.getProperty("os.name").toLowerCase();		
		String path = "";
		
		if(os.contains("win"))
			path = "c:\\Repository\\file\\";
		else 
			path = "/var/opt/Repository/file/";		
		
		//디렉토리가 존재하는지 체크해서 없다면 생성
		File p = new File(path);
		if(!p.exists()) p.mkdirs();
		//운영체제에 따라 이미지가 저장될 디렉토리 구조 설정 종료
		
		Long seqno =0L;
		
		//게시물 등록
		if(kind.equals("I")) { 
			service.write(board);
			seqno = service.getMaxSeqno(board.getEmail().getEmail());
		}
		
		//게시물 수정
		if(kind.equals("U")) { 
			service.modify(board);
			seqno = board.getSeqno();
			
			if(deleteFileList != null) {				
				for(int i=0; i<deleteFileList.length; i++) {
					Map<String,Object > data = new HashMap<>();
					data.put("kind", "F");
					data.put("fileseqno", deleteFileList[i]);
					service.deleteFileList(data);		
				}				
			}			
		}
		
		if(multipartFile != null) {//파일 등록 및 수정 시 파일 업로드
			File targetFile = null; 
			
			for(MultipartFile mpr:multipartFile) {
				
				String org_filename = mpr.getOriginalFilename();	
				String org_fileExtension = org_filename.substring(org_filename.lastIndexOf("."));	
				String stored_filename = UUID.randomUUID().toString().replaceAll("-", "") + org_fileExtension;	
				long filesize = mpr.getSize() ;

				targetFile = new File(path + stored_filename);
				mpr.transferTo(targetFile);
				
				FileDTO fileDTO = FileDTO.builder()
								.seqno(seqno)
								.email(board.getEmail().getEmail())
								.org_filename(org_filename)
								.stored_filename(stored_filename)
								.filesize(filesize)
								.checkfile("Y")
								.build();
				service.fileInfoRegistry(fileDTO);
	
			}
		}	
		
		return ResponseEntity.ok().build();
	}
	
	//좋아요/싫어요 상태 보기
	@GetMapping("/api/board/likeCheckView")
	public ResponseEntity<String> getLikeCheckView(@RequestParam("seqno") Long seqno, @RequestParam("email") String email) throws Exception {
		
		LikeEntity likeCheckView = service.likeCheckView(seqno, email);
		String result = "";
		
		//초기에 좋아요/싫어요 등록이 안되어져 있을 경우 "N"으로 초기화		
		if(likeCheckView == null) { 
			result = "{\"myLikeCheck\":\"N\",\"myDislikeCheck\":\"N\"}";
		} else if(likeCheckView != null) 
			result = "{\"myLikeCheck\":\"" + likeCheckView.getMylikecheck()
			+ "\",\"myDislikeCheck\":\"" + likeCheckView.getMydislikecheck() + "\"}";
		return ResponseEntity.ok().body(result);	
	}
	
	//좋아요/싫어요 상태 변경
	@PostMapping(value = "/api/board/likeCheck")
	public ResponseEntity<String> postLikeCheck(@RequestParam("seqno") Long seqno, @RequestParam("email") String email,
			@RequestParam("myLikecheck") String mylikeCheck,@RequestParam("myDislikecheck") String mydislikeCheck,
			@RequestParam("checkCnt") String checkCnt) throws Exception {
	
		//현재 날짜, 시간 구해서 좋아요/싫어요 한 날짜/시간 입력 및 수정
		String likeDate = "";
		String dislikeDate = "";
		if(mylikeCheck.equals("Y")) 
			likeDate = LocalDateTime.now().toString();
		else if(mydislikeCheck.equals("Y")) 
			dislikeDate = LocalDateTime.now().toString();

		LikeEntity likeCheckView = service.likeCheckView(seqno, email);
		if(likeCheckView == null) {
			service.likeCheckRegistry(seqno,email,mylikeCheck,mydislikeCheck,likeDate,dislikeDate);
		}
		else {
			service.likeCheckUpdate(seqno,email,mylikeCheck,mydislikeCheck,likeDate,dislikeDate);
		}

		//TBL_BOARD 내의 likecnt,dislikecnt 입력/수정 
		BoardDTO board = service.view(seqno);
		
		int likeCnt = board.getLikecnt();
		int dislikeCnt = board.getDislikecnt();
			
		switch(checkCnt){
	    	case "1" : likeCnt --; break;
	    	case "2" : likeCnt ++; dislikeCnt --; break;
	    	case "3" : likeCnt ++; break;
	    	case "4" : dislikeCnt --; break;
	    	case "5" : likeCnt --; dislikeCnt ++; break;
	    	case "6" : dislikeCnt ++; break;
		}

		service.boardLikeUpdate(seqno,likeCnt,dislikeCnt);
		String json = "{\"likeCnt\":\"" + likeCnt + "\",\"dislikeCnt\":\"" + dislikeCnt + "\"}";
		return ResponseEntity.ok().body(json);
	}
	
	//파일 목록 보기
	@GetMapping("/api/board/fileView")
	public ResponseEntity<List<FileDTO>> getFileView(@RequestParam("seqno") Long seqno) throws Exception {
		List<FileDTO> fileDTOs = new ArrayList<>();
		service.fileListView(seqno).stream().forEach(file -> fileDTOs.add(new FileDTO(file)));
		return ResponseEntity.ok().body(fileDTOs);
	}
	
	//파일 다운로드
	@GetMapping("/api/member/filedownload")
	public ResponseEntity<?> filedownload(@RequestParam("fileseqno") Long fileseqno, HttpServletResponse rs) throws Exception {
		
		//운영체제에 따라 이미지가 저장될 디렉토리 구조 설정 시작
		String os = System.getProperty("os.name").toLowerCase();
		String path;
		if(os.contains("win"))
			path = "c:\\Repository\\file\\";
		else 
			path = "/var/opt/Repository/file/";
		
		//디렉토리가 존재하는지 체크해서 없다면 생성
		File p = new File(path);
		if(!p.exists()) p.mkdir();
		//운영체제에 따라 이미지가 저장될 디렉토리 구조 설정 종료
		
		FileDTO fileInfo = service.fileInfo(fileseqno);
		String org_filename = fileInfo.getOrg_filename();
		String stored_filename = fileInfo.getStored_filename();
		byte fileByte[] = FileUtils.readFileToByteArray(new File(path+stored_filename));
		
		//Content-Disposition 헤드를 구성하여 바이트 타입으로 변환된 Http Response 메세지를 전송 한다는 것은   
		//이 파일을 다운받게끔 하는 것임
		//Content-Disposition 헤드 구성 예) Content-Disposition: attachment; filename="hello.jpg";
		
		rs.setContentType("application/octet-stream");
		rs.setContentLength(fileByte.length);
		rs.setHeader("Content-Disposition", "attachment; filename=\"" + URLEncoder.encode(org_filename, "UTF-8") + "\";");
		rs.getOutputStream().write(fileByte);
		rs.getOutputStream().flush();
		rs.getOutputStream().close();	
		return ResponseEntity.ok().build();
	}
	
	//게시물 삭제
	@GetMapping("/api/board/delete")
	public ResponseEntity<?> getDelete(@RequestParam("seqno") Long seqno) throws Exception {

		Map<String, Object> data = new HashMap<>();
		data.put("kind", "B");
		data.put("seqno", seqno);		
		service.deleteFileList(data);
		service.delete(seqno);
		return ResponseEntity.ok().build();
	}
	
	//댓글 처리	
	@PostMapping("/api/board/reply")
	public ResponseEntity<List<ReplyInterface>> postReply(ReplyInterface reply,@RequestParam("option") String option)throws Exception{
		
		switch(option) {
		
		case "I" : service.replyRegistry(reply); //댓글 등록
				   break;
		case "U" : service.replyUpdate(reply); //댓글 수정
				   break;
		case "D" : service.replyDelete(reply); //댓글 삭제
				   break;
		}

		return ResponseEntity.ok().body(service.replyView(reply));
	}
		
}
