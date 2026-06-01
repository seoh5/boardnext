package com.board.util;

public class PageUtil {
	
	public String getPageList(int pageNum, int postNum, int pageListCount, int totalCount, String keyword) {
		
		int totalPage = (int)Math.ceil(totalCount/(double)postNum);
		int totalSection = (int)Math.ceil(totalPage/(double)pageListCount);
		int section = (int)Math.ceil(pageNum/(double)pageListCount);
		String pageList = "";
		
		if(totalPage != 1) {
			
			for(int i=1;i<=pageListCount;i++) {
				if(section > 1 && i == 1)
					pageList += " <a href=/board/list?page=" 
						+ Integer.toString((section-2)*pageListCount + pageListCount) 
						+ "&keyword=" + keyword + ">◀</a>";

				if(totalPage < (section-1)*pageListCount+i) { 
					break; 
				} 

				if(pageNum != (section-1)*pageListCount + i)
					pageList += " <a href=/board/list?page=" 
						+ Integer.toString((section-1)*pageListCount+i) 
						+ "&keyword=" + keyword + ">" 
						+ Integer.toString((section-1)*pageListCount+i) + "</a>";		
				else 
					pageList += " <span style='font-weight: bold'>" 
						+ Integer.toString((section-1)*pageListCount+i) + "</span>";

				if(i == pageListCount && totalSection > 1 && totalPage >= i+(section-1)*pageListCount+1)
					pageList += " <a href=/board/list?page=" 
						+ Integer.toString(section*pageListCount+1) 
						+ "&keyword=" + keyword + ">▶</a>";
			}
		}		
		return pageList;		
	}
	
	public String getPageAddress(int pageNum, int postNum, int pageListCount, int totalCount, String keyword) {
		
		int totalPage = (int)Math.ceil(totalCount/(double)postNum);
		int totalSection = (int)Math.ceil(totalPage/(double)pageListCount);
		int section = (int)Math.ceil(pageNum/(double)pageListCount);
		String pageList = "";
		
		if(totalPage != 1) {
			for(int i=1;i<=pageListCount;i++) {

				if(section > 1 && i == 1)
					pageList += " <a href=/member/searchAddress?page=" 
						+ Integer.toString((section-2)*pageListCount + pageListCount) 
						+ "&addrSearch=" + keyword + ">◀</a>";

				if(totalPage < (section-1)*pageListCount+i) { 
					break; 
				} 

				if(pageNum != (section-1)*pageListCount+i)
					pageList += " <a href=/member/searchAddress?page=" 
						+ Integer.toString((section-1)*pageListCount+i) 
						+ "&addrSearch=" + keyword + ">" 
						+ Integer.toString((section-1)*pageListCount+i) + "</a>";		
				else 
					pageList += " <span style='font-weight: bold'>" 
						+ Integer.toString((section-1)*pageListCount+i) + "</span>";

				if(i == pageListCount && totalSection > 1 && totalPage >= i+(section-1)*pageListCount+1)
					pageList += " <a href=/member/searchAddress?page=" 
						+ Integer.toString(section*pageListCount+1) 
						+ "&addrSearch=" + keyword + ">▶</a>";				
			}			
		}		
		return pageList;
	}
}