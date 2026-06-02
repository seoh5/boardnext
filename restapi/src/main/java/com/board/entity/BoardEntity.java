package com.board.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity(name="board")
@Table(name="jpa_board")
public class BoardEntity {

	/*
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "BOARD_SEQ")
     : primary key의 증가(increment) 전략 설정
   --> SEQUENCE : Oracle, PostgreSQL 등과 같은 시퀀스가 있는 DBMS에 적용
   --> IDENTITY : 기본키 생성을 데이터베이스에 완전히 일임하는 방식으로 MySQL(AUTO_INCREMENT)에 적용
        예) MySQL --> @GeneratedValue(strategy = GenerationType.SEQUENCE)
   --> TABLE : @TableGenerator를 만들어서 사용
        예)  
         @Entity
         @TableGenerator(
                 name = "BOARD_SEQ_GENERATOR",
                 table = "tbl_board",
                 pkColumnName = "BOARD_NAME",
                 valueColumnName = "NEXTVAL",
                 pkColumnValue = "BOARD_SEQ",
                 initialVlaue = 0,
                 allocationSize = 1
          )
          public class Board {
              @Id
              @GeneratedValue(strategy = GenerationType.TABLE, generator = "BOARD_SEQ_GENERATOR")
              Long id;
          }
   --> AUTO --> JPA가 데이터베이스 방언(Dialect)에 맞춰 위 3가지 중 하나를 선택해서 사용 --> 조금 주의
        ※ 데이터베이스 방언(Dialect) : ORM에서 특정 데이터베이스에서 사용하는 고유의 SQL 문법
           --> MySQL: spring.jpa.database-platform=org.hibernate.dialect.MySQLDialect
               Oracle: spring.jpa.database-platform=org.hibernate.dialect.OracleDialect
               PostgreSQL: spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
               H2 (테스트용 인메모리 DB): spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
           --> 최신 버전의 스프링부트에서는 spring.jpa.database=oracle 설정만으로도 JPA가 Dialect를 인식함.
        ** Oracle 또는 PostgreSQL과 연결되었을 때 JPA는 이 데이터베이스들이 시퀀스를 지원한다는 것을 알고 있어 
           내부적으로 GenerationType.SEQUENCE로 자동 전환하며, 데이터베이스에 HIBERNATE_SEQUENCE라는 
           기본 시퀀스 오브젝트를 만들어서 사용.
        ** MySQL 또는 MariaDB와 연결되었을 때는 JPA는 이 데이터베이스들이 시퀀스를 지원하지 않는다는 것을 알고 있으며, 
           Hibernate 5 버전 이후부터는 MySQL에서 AUTO를 사용하면 보통 GenerationType.TABLE 전략을 선택하여 
           키 관리용 테이블을 새로 만듬. (또는 버전에 따라 IDENTITY를 선택하기도 함.)	 
        ** MySQL에서 AUTO를 사용하면 개발자의 의도(AUTO_INCREMENT)와 다르게 TABLE 전략이 선택되는 경우가 많음.
           따라서, 실제 서비스를 구축할 때는 데이터베이스가 쉽게 바뀌지 않으므로, 
           데이터베이스 특성에 맞춰 명시적으로 지정하는 것이 안전함.
	 */
	
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "BOARD_SEQ")
	@SequenceGenerator(name="BOARD_SEQ", sequenceName = "jpa_board_seq", initialValue = 1, allocationSize = 1)
	@Column(name="seqno", nullable=false)
	private Long seqno; //JPA의 primary 키를 숫자로 사용할때는 만드시 long 타입으로 해야하고 wrapper 클래스 사용. 

	@Column(name="writer", length=50, nullable=false)
	private String writer;
	
	@Column(name="title", length=200, nullable=false)
	private String title;
	
	@Column(name="content", length=2000, nullable=false)
	private String content;
	
	@Column(name="regdate", nullable=false)
	private LocalDateTime regdate;
	
	@Column(name="hitno", nullable=true)
	private int hitno;
	
	@Column(name="likecnt", nullable=true)
	private int likecnt;
	
	@Column(name="dislikecnt", nullable=true)
	private int dislikecnt;

	//FK 만들기
	//FK 읽어 올때 Eager, Lazy 두가지 타입이 있음
	//Eager는 부모키가 있는 테이블부터 검사해서 부모키가 제대로 되어 있는지 확인하고 자식키를 읽음.-> 정확도는 높지만 성능이 저하
	//Lazy는 자식키가 있는 테이블만 읽음. -> 정확도는 떨어지지만 성능이 향상.
	//alter table tbl_board add constraint fk_tbl_board_email foreign key(email) REFERENCES TBL_member(email) on delete cascade ;
	@ManyToOne(fetch = FetchType.LAZY)
	@OnDelete(action = OnDeleteAction.CASCADE)
	@JoinColumn(name="email", nullable = false)
	private MemberEntity email;

}
