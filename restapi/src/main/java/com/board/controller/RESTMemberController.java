package com.board.controller;

import java.io.File;
import java.net.URLEncoder;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.board.dto.MemberDTO;
import com.board.entity.AddressEntity;
import com.board.entity.repository.AddressRepository;
import com.board.entity.repository.MemberRepository;
import com.board.service.MemberService;
import com.board.util.JWTUtil;
import com.board.util.PageUtil;
import com.board.util.PasswordMaker;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;

//@CrossOrigin(originPatterns = "http://www.boardreact.com")
//@CrossOrigin(originPatterns = "http://localhost:3000")
@RestController
@AllArgsConstructor
@Tag(name="회원 관리 API", description="회원 관리 API를 모아 놓은 회원관리 클래스")
@Log4j2
public class RESTMemberController {
	
	private final MemberService service;
	private final MemberRepository memberRepository;
	private final AddressRepository addressRepository;
	private final BCryptPasswordEncoder pwdEncoder;
	private final JWTUtil jwtUtil;	
	
	//로그인
	@PostMapping("/api/member/loginCheck")
	public ResponseEntity<String> postLogIn(MemberDTO loginData,HttpSession session, 
			@RequestParam("autoLogin") String autoLogin, HttpServletRequest request) throws Exception {
		
		String authkey = "";
		String accessToken = "";
		String refreshToken = "";
		String str = "";		
				
		//authkey가 클라이언트에 쿠키로 존재할 경우 로그인 과정 없이 세션 생성 후 게시판 목록 페이지로 이동  
		if(autoLogin.equals("PASS")) {		
			
			if(memberRepository.findByAuthkey(loginData.getAuthkey()) != null) {
				
				//패스워드 확인 후 마지막 패스워드 변경일이 30일이 경과 되었을 경우 ...
				//패스워드 변경 기한 도래 여부 확인
				LocalDateTime lastpwcheckDate = memberRepository.findByAuthkey(loginData.getAuthkey()).getLastpwcheckdate();
				int addedDate = 30;
				LocalDateTime reDate = lastpwcheckDate.plusDays(addedDate);
				LocalDateTime today = LocalDateTime.now();

				if(reDate.compareTo(today) < 0)  {//패스워드 변경일이 지났으면...
					str = "{\"message\":\"expired\"}";
					return ResponseEntity.ok().body(str);
				} else {
					str = "{\"message\":\"good\"}";
					log.info("자동 로그인 ...");
					return ResponseEntity.ok().body(str);
				}
			} else {
				str = "{\"message\":\"bad\"}";
				log.info("로그인 실패...");
				return ResponseEntity.ok().body(str);
			}				
		}
				
		//아이디 존재 여부 확인
		if(service.idCheck(loginData.getEmail()) == 0) {
			str = "{\"message\":\"ID_NOT_FOUND\"}";
			log.info("아이디가 존재 하지 않음");
			return ResponseEntity.ok().body(str);
		}			
		
		//아이디가 존재하면 읽어온 email로 로그인 정보 가져 오기
		MemberDTO member = service.memberInfo(loginData.getEmail());
		
		//패스워드 확인
		if(!pwdEncoder.matches(loginData.getPassword(),member.getPassword())) {
			str = "{\"message\":\"PASSWORD_NOT_FOUND\"}";
			log.info("패스워드가 틀렸음");
			return ResponseEntity.ok().body(str);
		}
		
		//Form에서 읽은 email, 패스워드 값으로 로그인
		if(autoLogin.equals("NEW")) {
			
			//authkey 생성 및 DB 저장 
			authkey = UUID.randomUUID().toString().replaceAll("-", ""); 
			member.setAuthkey(authkey);
			service.authkeyUpdate(member);
			
			//토큰 생성
			Map<String,Object> token = new HashMap<>();
			token.put("email", loginData.getEmail());
			//token.put("password", loginData.getPassword());
			accessToken = jwtUtil.generateToken(token, 1);
			refreshToken = jwtUtil.generateToken(token, 5);			

			//패스워드 확인 후 마지막 패스워드 변경일이 30일이 경과 되었을 경우 ...
			//패스워드 변경 기한 도래 여부 확인			
			LocalDateTime lastpwcheckDate = member.getLastpwcheckdate();
			int addedDate = 30;
			LocalDateTime reDate = lastpwcheckDate.plusDays(addedDate);
			LocalDateTime today = LocalDateTime.now();

			if(reDate.compareTo(today) < 0) {  //패스워드 변경일이 지났으면...
				//authkey, accessToken, refreshToken, username, role 값들을 JSON 타입으로 client에게로 전달
				str = "{\"message\":\"expired\",\"authkey\":\"" + member.getAuthkey() + "\",\"accessToken\":\"" + accessToken + "\",\"refreshToken\":\"" + refreshToken + 
						"\",\"username\":\"" + URLEncoder.encode(member.getUsername(),"UTF-8") + "\",\"role\":\"" + member.getRole() + "\"}";				
			}	
			
			//authkey, accessToken, refreshToken, username, role 값들을 JSON 타입으로 client에게로 전달
			str = "{\"message\":\"good\",\"authkey\":\"" + member.getAuthkey() + "\",\"accessToken\":\"" + accessToken + "\",\"refreshToken\":\"" + refreshToken + 
					"\",\"username\":\"" + URLEncoder.encode(member.getUsername(),"UTF-8") + "\",\"role\":\"" + member.getRole() + "\"}";
			
			//로그인 로그 정보 기록
			service.lastdateUpdate(loginData.getEmail(), "login");
			service.memberLogRegistry(loginData.getEmail(), "login");
			log.info("아이디/패스워드 로그인 ...");
			
		} 
		return ResponseEntity.ok().body(str);

	}
	
	//로그아웃
	@GetMapping("/api/member/logout")
	public ResponseEntity<?> getLogout(@RequestParam("email") String email) throws Exception {
		
		//로그아웃 날짜 및 회원 로그 정보 등록
		service.lastdateUpdate(email, "logout");
		service.memberLogRegistry(email, "logout");
		return ResponseEntity.ok().build();
	}
	
	//토큰 유효성 검사
	@GetMapping("/api/member/validateToken")
	public String getValidate(HttpServletRequest request) throws Exception {
		String token = jwtUtil.getTokenFromAuthorization(request);
		if(token.equals("INVALID_HEADER")) //Authorization Header에 Bearer가 존재하지 않음 
			return "{\"message\":\"bad\"}";
		String jwtCheck = jwtUtil.validateToken(token);
		
		switch(jwtCheck) { //Bearer 내의 JWT의 상태			
			case "VALID_JWT" : return "{\"message\":\"VALID_JWT\", \"email\":\"" + (String)jwtUtil.getDataFromToken(token).get("email") + "\"}";			
			case "EXPIRED_JWT" : return "{\"message\":\"EXPIRED_JWT\"}";
			case "INVALID_JWT":
			case "UNSUPPORTED_JWT":
			case "EMPTY_JWT": return "{\"message\":\"INVALID_JWT\"}";													
		}
		return null;
	}
	
	//아이디 중복 확인
	@PostMapping("/api/member/idCheck")
	public ResponseEntity<String> getIdCheck(@RequestParam("email") String email) {	
		String json = service.idCheck(email) == 0 ? "{\"status\":\"good\"}":"{\"status\":\"bad\"}";
		return ResponseEntity.ok().body(json);
	}
	
	//회원 등록 및 기본 정보 수정
	@PostMapping("/api/member/signup")
	public ResponseEntity<Map<String,String>> postSignup(MemberDTO member, @RequestParam("kind") String kind,
			@RequestParam(name="imgProfile",required=false) MultipartFile mpr) throws Exception {
		
		//운영체제에 따라 이미지가 저장될 디렉토리 구조 설정 시작
		String os = System.getProperty("os.name").toLowerCase();
		String path;
		if(os.contains("win"))
			path = "c:\\Repository\\profile\\";
		else 
			path = "/var/opt/Repository/profile/";
		
		//디렉토리가 존재하는지 체크해서 없다면 생성
		File p = new File(path);
		if(!p.exists()) p.mkdir();
		//운영체제에 따라 이미지가 저장될 디렉토리 구조 설정 종료
		
		File targetFile = null;
		String org_filename = "";
		String org_fileExtension = "";
		String stored_filename = "";
		
		if(mpr != null) {
			
			org_filename = mpr.getOriginalFilename();
			org_fileExtension = org_filename.substring(org_filename.lastIndexOf("."));			
			stored_filename = UUID.randomUUID().toString().replaceAll("-", "") + org_fileExtension;
			
			try {
				targetFile = new File(path + stored_filename);				
				mpr.transferTo(targetFile);
				
				member.setOrg_filename(org_filename);
				member.setStored_filename(stored_filename);
				member.setFilesize(mpr.getSize());				
				
			}catch(Exception e) {
				e.printStackTrace();
			}
		}
		
		//회원 등록시
		if(kind.equals("I")) {
			//패스워드 암호화
			member.setPassword(pwdEncoder.encode(member.getPassword()));
			//회원 등록
			service.signup(member);	
		}
		
		//회원 수정시
		if(kind.equals("U")) {
			
			//프로필 이미지 변경 시에 기존 프로필 이미지 삭제 
			if(mpr != null) {
				MemberDTO m = service.memberInfo(member.getEmail());
				File file = new File(path + m.getStored_filename());//수정되기 전의 stored_file
				file.delete();
			}		

			//회원 수정
			service.modifyMemberInfo(member);
		}
		
		//OAuth2로 가입한 회원의 정식 회원으로 전환
		if(kind.equals("OI")) {
			//기존에 OAuth2 로그인 시 저장했던 회원정보를 수정. 기본 정보와 패스워드 변경 
			service.modifyMemberInfo(member); 
			service.modifyMemberPassword(member.getEmail(), member.getPassword());
			service.lastdateUpdate(member.getEmail(), "password");
			service.lastdateUpdate(member.getEmail(), "pwcheck");
			
		}
				
		Map<String,String> data = new HashMap<>();
		data.put("status", "good");
		data.put("username", URLEncoder.encode(member.getUsername(),"UTF-8"));
		
		return ResponseEntity.ok().body(data);
		
	}
	
	//사용자 정보 보기
	@GetMapping("/api/board/memberInfo")
	public ResponseEntity<MemberDTO> getMemberInfo(@RequestParam("email") String email) {		
		return ResponseEntity.ok().body(service.memberInfo(email));
	}

	//회원 프로필 이미지 보기
	@GetMapping("/api/member/viewProfile/{email}")	
	public ResponseEntity<?> filedownload(@PathVariable String email, HttpServletResponse rs) throws Exception {		
		
		//운영체제에 따라 이미지가 저장될 디렉토리 구조 설정 시작
		String os = System.getProperty("os.name").toLowerCase();
		String path;
		if(os.contains("win"))
			path = "c:\\Repository\\profile\\";
		else 
			path = "/var/opt/Repository/profile/";
		
		//디렉토리가 존재하는지 체크해서 없다면 생성
		File p = new File(path);
		if(!p.exists()) p.mkdirs();
		//운영체제에 따라 이미지가 저장될 디렉토리 구조 설정 종료

		MemberDTO member = service.memberInfo(email);
		
		//다운로드할 파일의 경로와 파일명을 매개변수로 입력 받아 byte 데이터타입의 1차원 배열로 저장
		byte[] fileByte = FileUtils.readFileToByteArray(new File(path + member.getStored_filename()));
		
		//예) HTTP Response Header는 Content-Disposition: attachment; filename="hello.jpg";
		//   HTTP Response Body에는 1차원 바이트 타입으로 변환된 배열을 넣음
		rs.setContentType("application/octet-stream");
		rs.setContentLength(fileByte.length);
		rs.setHeader("Content-Disposition", "attachment; filename=\"" + URLEncoder.encode(member.getOrg_filename(), "UTF-8") + "\";");
		rs.getOutputStream().write(fileByte); //stream을 통해 1차원 byte 타입 배열로 변환된 데이터(추후 파일로 변환)를 Buffer에 씀...
		rs.getOutputStream().flush(); //버퍼에 있는 내용을 write
		rs.getOutputStream().close(); //스트림 닫기
		return ResponseEntity.ok().build();
	}
	
	//회원 패스워드 변경
	@PostMapping("/api/member/modifyMemberPassword")
	public ResponseEntity<String> postMemberPasswordModify(@RequestParam("old_password") String old_password, 
				@RequestParam("new_password") String new_password, @RequestParam("email") String email) throws Exception { 
		
		String json = "";
		//이전 패스워드가 제대로 된 패스워드인지 확인
		if(!pwdEncoder.matches(old_password, service.memberInfo(email).getPassword())) {
			json = "{\"message\":\"PASSWORD_NOT_FOUND\"}";
			return ResponseEntity.ok().body(json); 
		}
		
		//신규 패스워드로 수정
		service.modifyMemberPassword(email,new_password);
		
		//마지막 패스워드 변경일 등록
		service.lastdateUpdate(email, "password");
		json = "{\"message\":\"good\"}";
		return ResponseEntity.ok().body(json);
	}
	
	//아이디 찾기
	@PostMapping("/api/member/searchID")
	public ResponseEntity<String> postSearchID(MemberDTO member) {
		
		String email = service.SearchID(member) == null?"ID_NOT_FOUND":service.SearchID(member);	
		String json = "{\"message\":\"" + email + "\"}";
		return ResponseEntity.ok().body(json);
	}
	
	//패스워드 임시 발급
	@PostMapping("/api/member/searchPassword")
	public ResponseEntity<String> postSearchPassword(MemberDTO member) throws Exception{
		//아이디 존재 여부 확인
		String json = "";
		if(service.idCheck(member.getEmail()) == 0) {
			json = "{\"status\":\"ID_NOT_FOUND\"}";
			return ResponseEntity.ok().body(json);
		}
		//TELNO 확인
		if(!service.memberInfo(member.getEmail()).getTelno().equals(member.getTelno())) {
			json = "{\"status\":\"TELNO_NOT_FOUND\"}";
			return ResponseEntity.ok().body(json);
		}					
		//임시 패스워드 생성	
		PasswordMaker pMaker = new PasswordMaker();
		String rawTempPW = pMaker.tempPasswordMaker();
		//임시 패스워드로 패스워드 수정
		service.modifyMemberPassword(member.getEmail(),rawTempPW);
		//마지막 패스워드 변경일 등록
		service.lastdateUpdate(member.getEmail(), "password");
		json = "{\"status\":\"good\",\"password\":\"" + rawTempPW + "\"}";
		return ResponseEntity.ok().body(json);
	}

	//로그인 시 패스워드 변경 기한 30일 이후로 연기
	@GetMapping("/api/board/modifyPasswordAfter30")
	public ResponseEntity<?> getModifyPasswordAfter30(@RequestParam("email") String email) throws Exception {		
		service.modifyPasswordAfter30(email);
		service.lastdateUpdate(email, "pwcheck");
		return ResponseEntity.ok().build();
	}
	
	//주소 검색 리스트
	@GetMapping("/api/member/searchAddress")
	public ResponseEntity<Page<AddressEntity>> getAddrSearch(@RequestParam("page") int pageNum,  
			@RequestParam("addrSearch") String addrSearch) {
				
		int postNum = 5; //한 화면에 보여지는 게시물 행의 갯수
		return ResponseEntity.ok().body(service.addrSearch(pageNum, postNum, addrSearch));	
	}
	
	//주소 페이지 리스트 보기
	@GetMapping("/api/member/addressPagelist")
	public ResponseEntity<String> getPageList(@RequestParam("page") int pageNum, @RequestParam("addrSearch") String addrSearch) throws Exception {
		int postNum = 5; //한 화면에 보여지는 게시물 행의 갯수
		int pageListCount = 5; //화면 하단에 보여지는 페이지리스트 내의 페이지 갯수
		
		PageRequest pageRequest = PageRequest.of(pageNum-1, postNum,Sort.by(Direction.ASC,"zipcode"));
		Page<AddressEntity> list = addressRepository.findByRoadContainingOrBuildingContaining(addrSearch, addrSearch, pageRequest);		
		PageUtil page = new PageUtil();
		int totalCount = (int)list.getTotalElements();
		String result = "{\"addressPagelist\":\"" + page.getPageAddress(pageNum, postNum, pageListCount, totalCount, addrSearch) 
			+ "\", \"totalCount\":\"" + totalCount + "\"}";
		return ResponseEntity.ok().body(result);
	}
		
}
