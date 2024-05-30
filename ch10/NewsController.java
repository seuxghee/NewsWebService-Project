package ch10;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.*;
import java.sql.SQLException;
import java.util.List;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

import org.apache.commons.beanutils.BeanUtils;


@WebServlet(urlPatterns = "/news.nhn") // 호출 url 매핑
@MultipartConfig(maxFileSize=1024*1024*2, location="c:/Temp/img") //뉴스 이미지 파일 업로드 처리를 위해 추가
public class NewsController extends HttpServlet {
	private static final long serialVersionUID = 1L;


	private NewsDAO dao;
	private ServletContext ctx;
	
	//웹 리소스 기본 경로 지정
	private final String START_PAGE = "ch10/newsList.jsp"; //시작 페이지의 이동을 상수로 지정
    
	public void init(ServletConfig config) throws ServletException {
        super.init(config);
        dao = new NewsDAO();
        ctx = getServletContext();
    }

	
	protected void service(HttpServletRequest request, HttpServletResponse response) 
			throws ServletException, IOException {
		request.setCharacterEncoding("utf-8");
		String action = request.getParameter("action");
		dao = new NewsDAO();
		//자바 리플렉션을 사용해 if(switch)없이 요청에 따라 구현 메서드가 실해되도록 구성
		Method m;
		String view = null;
		// action 파라미터 없이 접근한 경우
		if (action == null) {
			action = "listNews";
		}
		try {
			//현재 클래스에서 action 이름과 HttpservletRequest를 파라미터로 하는 메서드 찾음
			m = this.getClass().getMethod(action, HttpServletRequest.class);
			//메서드 실행 후 리턴값 받아옴
			view = (String)m.invoke(this, request);
		}catch (NoSuchMethodException e) {
			e.printStackTrace();
			ctx.log("요청 action 없음!!");
			request.setAttribute("error", "action 파라미터가 잘못되었습니다!!");
			view = START_PAGE;
		}catch (Exception e) {
			e.printStackTrace();
		}
		//POST 요청에서는 리디렉션 방법으로 이동하도록 분기
		if(view.startsWith("redirect:/")) {
			String rview = view.substring("redirect:/".length());//redirect:/ 문자열 이후 경로만 가져옴
			response.sendRedirect(rview);
		} else {
			RequestDispatcher dispatcher = request.getRequestDispatcher(view);
			dispatcher.forward(request, response); // 지정된 뷰로 포워딩, 포워딩 시 콘텍스트 경로는 필요 없음
		}
	}
	// 뉴스 기사를 등록하기 위한 요청을 처리하는 메서드
	public String addNews(HttpServletRequest request) {
		News n = new News();
		try {
			//이미지 파일 저장
			Part part = request.getPart("file");
			String fileName = getFilename(part);
			if(fileName != null && !fileName.isEmpty()) {
				part.write(fileName);
			}
			//입력값을 News 객체로 매핑
			BeanUtils.populate(n, request.getParameterMap());
			//이미지 파일 이름을 News 객체에도 저장
			n.setImg("/img/"+fileName);
			dao.addNews(n);
		} catch (Exception e) {
			e.printStackTrace();
			ctx.log("뉴스 추가 과정에서 문제 발생!!");
			request.setAttribute("error", "뉴스가 정상적으로 등록되지 않았습니다!!");
			return listNews(request);	
		}
		return "redirect:/news.nhn?action=listNews";	
	}
	//뉴스를 삭제하기 위한 메서드
	public String deleteNews(HttpServletRequest request) {
		int aid = Integer.parseInt(request.getParameter("aid"));
		try {
			dao.delNews(aid);
		}   catch (SQLException e) {
			e.printStackTrace();
			ctx.log("뉴스 삭제 과정에서 문제 발생!!");
			request.setAttribute("error", "뉴스가 정상적으로 삭제되지 않았습니다!!");
			return listNews(request);
		}
		return "redirect:/news.nhn?action=listNews";
	}
	// newsList.jsp에서 뉴스 목록을 보여주기 위한 요청을 처리하는 메서드
	public String listNews(HttpServletRequest request) {
		List<News>list;
		
		try {
			list = dao.getAll();
			request.setAttribute("newslist", list);
		} catch (Exception e) {
			e.printStackTrace();
			ctx.log("뉴스 목록 생성 과정에서 문제 발생!!");
			request.setAttribute("error", "뉴스 목록이 정상적으로 처리되지 않았습니다!!");
		}
		return "ch10/newsList.jsp";
	}
	// 특정 뉴스 기사를 클릭했을 때 호출하기 위한 요청을  처리하는 메서드
	public String getNews(HttpServletRequest request) {
		int aid =Integer.parseInt(request.getParameter("aid"));
		try {
			News n = dao.getNews(aid);
			request.setAttribute("news", n);
		} catch (Exception e) {
			e.printStackTrace();
			ctx.log("뉴스를 가져오는 과정에서 문제 발생!!");
			request.setAttribute("error", "뉴스를 정상적으로 가져오지 못했습니다!!");
		}
		return "ch10/newsView.jsp";
	}
	
	//Part 객체로 전달된 이미지 파일로부터 파일 이름을 추출하기 위한 메서드
	private String getFilename(Part part) {
		String fileName = null;
		String header = part.getHeader("content-disposition");
		System.out.println("Header =>"+header);
		int start = header.indexOf("filename=");
		fileName = header.substring(start+10,header.length()-1);
		ctx.log("파일명:"+fileName);
		return fileName;
		
	}

}
